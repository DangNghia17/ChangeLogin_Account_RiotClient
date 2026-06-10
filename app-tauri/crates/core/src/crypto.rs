//! Encryption for `accounts.dat`.
//!
//! Two formats are supported on disk, distinguished by a self-describing prefix:
//!
//! * **Legacy (Java)** — no prefix. `Base64( AES-256/ECB/PKCS7( UTF8(json) ) )` with
//!   `key = SHA-256(MachineGuid)`. This is read for backward compatibility so existing
//!   users keep all their accounts.
//! * **New (secure)** — `RAM2:` prefix. On Windows the payload is protected with
//!   **DPAPI** (`CryptProtectData`, current-user scope): no hard-coded key, no plaintext.
//!   On non-Windows (dev/CI only) it falls back to the legacy machine-key scheme so the
//!   round-trip remains testable.
//!
//! Writing prefers the secure format; reading auto-detects.

use aes::cipher::{block_padding::Pkcs7, BlockDecryptMut, BlockEncryptMut, KeyInit};
use base64::engine::general_purpose::STANDARD as B64;
use base64::Engine;
use sha2::{Digest, Sha256};

type Aes256EcbEnc = ecb::Encryptor<aes::Aes256>;
type Aes256EcbDec = ecb::Decryptor<aes::Aes256>;

const NEW_PREFIX: &str = "RAM2:";

/// Derives the 256-bit AES key from a machine id, identical to the Java implementation.
pub fn key_from_machine_id(machine_id: &str) -> [u8; 32] {
    let mut hasher = Sha256::new();
    hasher.update(machine_id.as_bytes());
    let digest = hasher.finalize();
    let mut key = [0u8; 32];
    key.copy_from_slice(&digest);
    key
}

/// Legacy AES-256/ECB/PKCS7 decrypt of a Base64 string (matches Java `CryptoService`).
pub fn legacy_decrypt(b64: &str, machine_id: &str) -> Result<String, String> {
    if b64.is_empty() {
        return Ok(String::new());
    }
    let key = key_from_machine_id(machine_id);
    let mut buf = B64
        .decode(b64.trim().as_bytes())
        .map_err(|e| format!("base64 decode failed: {e}"))?;
    let dec = Aes256EcbDec::new(&key.into());
    let pt = dec
        .decrypt_padded_mut::<Pkcs7>(&mut buf)
        .map_err(|e| format!("AES decrypt failed: {e}"))?;
    String::from_utf8(pt.to_vec()).map_err(|e| format!("utf8 decode failed: {e}"))
}

/// Legacy AES-256/ECB/PKCS7 encrypt to a Base64 string (matches Java `CryptoService`).
pub fn legacy_encrypt(plaintext: &str, machine_id: &str) -> Result<String, String> {
    if plaintext.is_empty() {
        return Ok(String::new());
    }
    let key = key_from_machine_id(machine_id);
    let enc = Aes256EcbEnc::new(&key.into());
    let pt = plaintext.as_bytes();
    let block = 16usize;
    let mut buf = vec![0u8; pt.len() + block];
    buf[..pt.len()].copy_from_slice(pt);
    let ct = enc
        .encrypt_padded_mut::<Pkcs7>(&mut buf, pt.len())
        .map_err(|e| format!("AES encrypt failed: {e}"))?;
    Ok(B64.encode(ct))
}

/// Reads the Windows MachineGuid (matches Java). Falls back to a deterministic string.
pub fn machine_id() -> String {
    #[cfg(windows)]
    {
        if let Some(guid) = read_machine_guid_windows() {
            return guid;
        }
    }
    // Fallback mirrors the Java fallback (`user.name + os.name + os.version`) as closely
    // as possible. Primarily used on non-Windows dev/CI; on Windows the GUID is used.
    let user = std::env::var("USERNAME")
        .or_else(|_| std::env::var("USER"))
        .unwrap_or_default();
    let os = std::env::consts::OS;
    if user.is_empty() {
        "default-machine-id".to_string()
    } else {
        format!("{user}{os}")
    }
}

#[cfg(windows)]
fn read_machine_guid_windows() -> Option<String> {
    use crate::process;
    let out = process::command("reg")
        .args([
            "query",
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography",
            "/v",
            "MachineGuid",
        ])
        .output()
        .ok()?;
    let text = String::from_utf8_lossy(&out.stdout);
    for line in text.lines() {
        if line.contains("MachineGuid") {
            let parts: Vec<&str> = line.split_whitespace().collect();
            if let Some(last) = parts.last() {
                if !last.is_empty() {
                    return Some(last.to_string());
                }
            }
        }
    }
    None
}

/// Encrypts plaintext for storage using the most secure format available on this platform.
pub fn protect(plaintext: &str) -> Result<String, String> {
    #[cfg(windows)]
    {
        let blob = dpapi_protect(plaintext.as_bytes())?;
        return Ok(format!("{NEW_PREFIX}{}", B64.encode(blob)));
    }
    #[cfg(not(windows))]
    {
        // Dev/CI fallback: write the legacy format (no prefix) so it stays readable and testable.
        legacy_encrypt(plaintext, &machine_id())
    }
}

/// Decrypts stored data, auto-detecting the format (new `RAM2:` vs legacy Java).
pub fn unprotect(data: &str) -> Result<String, String> {
    let data = data.trim();
    if data.is_empty() {
        return Ok(String::new());
    }
    if let Some(payload) = data.strip_prefix(NEW_PREFIX) {
        #[cfg(windows)]
        {
            let blob = B64
                .decode(payload.as_bytes())
                .map_err(|e| format!("base64 decode failed: {e}"))?;
            let plain = dpapi_unprotect(&blob)?;
            return String::from_utf8(plain).map_err(|e| format!("utf8 decode failed: {e}"));
        }
        #[cfg(not(windows))]
        {
            let _ = payload;
            return Err("DPAPI-protected data can only be read on Windows".to_string());
        }
    }
    // No prefix → legacy Java format.
    legacy_decrypt(data, &machine_id())
}

/// Whether the stored blob is already in the new secure format.
pub fn is_new_format(data: &str) -> bool {
    data.trim_start().starts_with(NEW_PREFIX)
}

#[cfg(windows)]
fn dpapi_protect(data: &[u8]) -> Result<Vec<u8>, String> {
    use windows::Win32::Foundation::{HLOCAL, LocalFree};
    use windows::Win32::Security::Cryptography::{CryptProtectData, CRYPT_INTEGER_BLOB};

    unsafe {
        let mut in_blob = CRYPT_INTEGER_BLOB {
            cbData: data.len() as u32,
            pbData: data.as_ptr() as *mut u8,
        };
        let mut out_blob = CRYPT_INTEGER_BLOB::default();
        CryptProtectData(
            &mut in_blob,
            None,
            None,
            None,
            None,
            0,
            &mut out_blob,
        )
        .map_err(|e| format!("CryptProtectData failed: {e}"))?;

        let slice = std::slice::from_raw_parts(out_blob.pbData, out_blob.cbData as usize);
        let result = slice.to_vec();
        let _ = LocalFree(HLOCAL(out_blob.pbData as *mut _));
        Ok(result)
    }
}

#[cfg(windows)]
fn dpapi_unprotect(data: &[u8]) -> Result<Vec<u8>, String> {
    use windows::Win32::Foundation::{HLOCAL, LocalFree};
    use windows::Win32::Security::Cryptography::{CryptUnprotectData, CRYPT_INTEGER_BLOB};

    unsafe {
        let mut in_blob = CRYPT_INTEGER_BLOB {
            cbData: data.len() as u32,
            pbData: data.as_ptr() as *mut u8,
        };
        let mut out_blob = CRYPT_INTEGER_BLOB::default();
        CryptUnprotectData(
            &mut in_blob,
            None,
            None,
            None,
            None,
            0,
            &mut out_blob,
        )
        .map_err(|e| format!("CryptUnprotectData failed: {e}"))?;

        let slice = std::slice::from_raw_parts(out_blob.pbData, out_blob.cbData as usize);
        let result = slice.to_vec();
        let _ = LocalFree(HLOCAL(out_blob.pbData as *mut _));
        Ok(result)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    // Deterministic vector produced by the Java CryptoService (see
    // docs/data-compatibility-analysis.md §3.1). Proves byte-for-byte compatibility.
    const MACHINE_ID: &str = "TEST-MACHINE-GUID-1234";
    const PLAINTEXT: &str =
        "[{\"username\":\"u@example.com\",\"password\":\"p@ss_w0rd-1\",\"region\":\"VN\",\"note\":\"main\"}]";
    const CIPHER_B64: &str = "MRIryDr0wxwARmbPntThUZI8YfKYLdjEBf2vEZcENUVnmes37AyQE4dv6xOGbhK5Z4CQOBzW5TwayetBaI/7tSGPW96bfVXEZcXR1NiloS/UH5eqrKQyqiKUfKZhpvRj";

    #[test]
    fn decrypts_java_produced_data() {
        let pt = legacy_decrypt(CIPHER_B64, MACHINE_ID).expect("decrypt");
        assert_eq!(pt, PLAINTEXT);
    }

    #[test]
    fn encrypt_roundtrips_byte_for_byte_with_java() {
        let ct = legacy_encrypt(PLAINTEXT, MACHINE_ID).expect("encrypt");
        assert_eq!(ct, CIPHER_B64, "Rust ciphertext must match Java exactly (ECB is deterministic)");
    }

    #[test]
    fn legacy_roundtrip() {
        let ct = legacy_encrypt(PLAINTEXT, MACHINE_ID).unwrap();
        let pt = legacy_decrypt(&ct, MACHINE_ID).unwrap();
        assert_eq!(pt, PLAINTEXT);
    }

    #[test]
    fn empty_is_passthrough() {
        assert_eq!(legacy_encrypt("", MACHINE_ID).unwrap(), "");
        assert_eq!(legacy_decrypt("", MACHINE_ID).unwrap(), "");
    }

    #[cfg(not(windows))]
    #[test]
    fn protect_unprotect_roundtrip_non_windows() {
        // On non-Windows, protect() writes legacy format (no prefix) and unprotect() reads it.
        let blob = protect(PLAINTEXT).unwrap();
        assert!(!is_new_format(&blob));
        let pt = unprotect(&blob).unwrap();
        assert_eq!(pt, PLAINTEXT);
    }
}
