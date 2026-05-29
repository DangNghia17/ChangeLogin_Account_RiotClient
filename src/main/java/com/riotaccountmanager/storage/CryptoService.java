package com.riotaccountmanager.storage;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Machine-bound encryption for sensitive data ({@code accounts.dat}).
 *
 * <p><b>Data-safety contract:</b> the on-disk format is kept byte-compatible with the
 * legacy implementation so that:
 * <ul>
 *   <li>The new version can decrypt data written by any previous version.</li>
 *   <li>A previous version can still decrypt data written by the new version
 *       (safe rollback).</li>
 * </ul>
 *
 * <p>Format: {@code Base64( AES/ECB/PKCS5Padding( UTF-8(plaintext) ) )} where the key is
 * {@code SHA-256(machineId)} and {@code machineId} is the Windows {@code MachineGuid}.
 *
 * <p>Note on security: ECB is cryptographically weak (identical plaintext blocks map to
 * identical ciphertext blocks). It is intentionally retained to guarantee zero data loss
 * on upgrade/downgrade. A future migration to AES/GCM can be layered on top using a
 * self-describing prefix without breaking existing files.
 */
public final class CryptoService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /** Cached so the key is stable for the whole process lifetime (avoids R6 in ANALYSIS.md). */
    private static volatile String cachedMachineId;

    private CryptoService() {
    }

    private static String getMachineId() {
        if (cachedMachineId != null) {
            return cachedMachineId;
        }
        synchronized (CryptoService.class) {
            if (cachedMachineId != null) {
                return cachedMachineId;
            }
            cachedMachineId = resolveMachineId();
            return cachedMachineId;
        }
    }

    private static String resolveMachineId() {
        try {
            Process process = Runtime.getRuntime().exec(
                    "reg query HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography /v MachineGuid"
            );
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
            );
            String line;
            String result = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("MachineGuid")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 3) {
                        result = parts[parts.length - 1];
                        break;
                    }
                }
            }
            reader.close();
            process.waitFor();
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            // Fall through to the deterministic fallback below.
        }
        // Deterministic fallback identical to the legacy implementation so the derived
        // key matches data encrypted by older versions on the same machine.
        try {
            return System.getProperty("user.name")
                    + System.getProperty("os.name")
                    + System.getProperty("os.version");
        } catch (Exception ex) {
            return "default-machine-id";
        }
    }

    private static SecretKeySpec getSecretKey() throws Exception {
        String machineId = getMachineId();
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(machineId.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, ALGORITHM);
    }

    public static String encrypt(String data) throws Exception {
        if (data == null || data.isEmpty()) {
            return data;
        }
        SecretKeySpec secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    public static String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        SecretKeySpec secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}
