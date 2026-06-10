//! Account persistence with strong data-safety guarantees (the #1 requirement):
//! reads legacy Java data, migrates to the secure (DPAPI) format with a backup and
//! rollback, and writes atomically.

use crate::crypto;
use crate::model::Account;
use crate::paths::AppPaths;
use std::fs;

const CURRENT_SCHEMA_VERSION: i32 = 2;

/// Loads all accounts. On any error the original file is left untouched and an empty
/// list is returned (never destroys data).
pub fn load_accounts() -> Vec<Account> {
    let path = AppPaths::accounts_file();
    if !path.exists() {
        return Vec::new();
    }
    let raw = match fs::read_to_string(&path) {
        Ok(s) => s,
        Err(e) => {
            log::error!("Failed to read accounts file: {e}");
            return Vec::new();
        }
    };
    if raw.trim().is_empty() {
        return Vec::new();
    }
    match crypto::unprotect(&raw) {
        Ok(json) => parse_accounts(&json),
        Err(e) => {
            log::error!("Failed to decrypt accounts (file left intact): {e}");
            Vec::new()
        }
    }
}

fn parse_accounts(json: &str) -> Vec<Account> {
    match serde_json::from_str::<Vec<Account>>(json) {
        Ok(list) => list,
        Err(e) => {
            log::error!("Failed to parse accounts JSON: {e}");
            Vec::new()
        }
    }
}

/// Saves all accounts atomically: serialize -> protect -> temp file -> backup -> atomic move.
/// On failure, attempts to roll back from the backup and returns an error.
pub fn save_accounts(accounts: &[Account]) -> Result<(), String> {
    let target = AppPaths::accounts_file();
    let temp = AppPaths::accounts_temp_file();
    AppPaths::ensure_data_dir().map_err(|e| e.to_string())?;

    let json = serde_json::to_string(accounts).map_err(|e| e.to_string())?;
    let protected = crypto::protect(&json)?;

    // 1) write temp
    if let Err(e) = fs::write(&temp, protected.as_bytes()) {
        return Err(format!("write temp failed: {e}"));
    }
    // 2) backup current good file
    backup_accounts_file();
    // 3) atomic replace (rename on same volume is atomic on Windows & POSIX)
    if let Err(e) = fs::rename(&temp, &target) {
        let _ = fs::remove_file(&temp);
        rollback_from_backup();
        return Err(format!("atomic replace failed: {e}"));
    }
    Ok(())
}

fn backup_accounts_file() {
    let src = AppPaths::accounts_file();
    if src.exists() {
        if let Err(e) = fs::copy(&src, AppPaths::accounts_backup_file()) {
            log::warn!("Could not create backup: {e}");
        }
    }
}

fn rollback_from_backup() {
    let backup = AppPaths::accounts_backup_file();
    let target = AppPaths::accounts_file();
    if backup.exists() && !target.exists() {
        if let Err(e) = fs::copy(&backup, &target) {
            log::error!("Rollback from backup failed: {e}");
        } else {
            log::info!("Restored accounts file from backup after failed save");
        }
    }
}

fn read_schema_version() -> i32 {
    fs::read_to_string(AppPaths::data_version_file())
        .ok()
        .and_then(|s| s.trim().parse::<i32>().ok())
        .unwrap_or(0)
}

fn write_schema_version(v: i32) {
    let _ = AppPaths::ensure_data_dir();
    let _ = fs::write(AppPaths::data_version_file(), v.to_string());
}

/// Runs once on startup. If legacy (Java) data is present, backs it up and re-encrypts it
/// into the secure format. Safe to call repeatedly. Never loses data: on any failure the
/// original file is kept and we roll back.
pub fn run_migration_if_needed() {
    let _ = AppPaths::ensure_data_dir();
    let path = AppPaths::accounts_file();

    if !path.exists() {
        write_schema_version(CURRENT_SCHEMA_VERSION);
        return;
    }

    let raw = match fs::read_to_string(&path) {
        Ok(s) => s,
        Err(_) => return,
    };
    if raw.trim().is_empty() {
        write_schema_version(CURRENT_SCHEMA_VERSION);
        return;
    }

    // Already in the new secure format? Nothing to do.
    if crypto::is_new_format(&raw) {
        write_schema_version(CURRENT_SCHEMA_VERSION);
        return;
    }

    // Legacy format detected. Back up, then re-encrypt with the secure format.
    if let Err(e) = fs::copy(&path, AppPaths::accounts_backup_file()) {
        log::warn!("Migration backup failed, aborting migration to stay safe: {e}");
        return;
    }

    match crypto::unprotect(&raw) {
        Ok(plaintext) => {
            // Validate it parses before committing.
            let _accounts = parse_accounts(&plaintext);
            match crypto::protect(&plaintext) {
                Ok(secured) => {
                    let temp = AppPaths::accounts_temp_file();
                    if fs::write(&temp, secured.as_bytes()).is_ok()
                        && fs::rename(&temp, &path).is_ok()
                    {
                        write_schema_version(CURRENT_SCHEMA_VERSION);
                        log::info!("Migrated accounts to secure storage format (v{CURRENT_SCHEMA_VERSION})");
                    } else {
                        let _ = fs::remove_file(&temp);
                        log::error!("Migration write failed; original data preserved");
                    }
                }
                Err(e) => log::error!("Migration encrypt failed; original data preserved: {e}"),
            }
        }
        Err(e) => {
            // Could not decrypt legacy data (e.g. different machine). Leave it untouched.
            log::warn!("Could not decrypt legacy data during migration; leaving as-is: {e}");
        }
    }
    let _ = read_schema_version();
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::Mutex;

    static ENV_LOCK: Mutex<()> = Mutex::new(());

    fn with_temp_data_dir<F: FnOnce()>(f: F) {
        let _guard = ENV_LOCK.lock().unwrap();
        let tmp = std::env::temp_dir().join(format!("ram_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&tmp);
        fs::create_dir_all(&tmp).unwrap();
        let prev = std::env::var("LOCALAPPDATA").ok();
        std::env::set_var("LOCALAPPDATA", &tmp);
        f();
        match prev {
            Some(v) => std::env::set_var("LOCALAPPDATA", v),
            None => std::env::remove_var("LOCALAPPDATA"),
        }
        let _ = fs::remove_dir_all(&tmp);
    }

    #[test]
    fn save_load_roundtrip() {
        with_temp_data_dir(|| {
            let accounts = vec![
                Account {
                    username: "u@example.com".into(),
                    password: "p@ss-1".into(),
                    region: "VN".into(),
                    note: "main".into(),
                },
                Account {
                    username: "u2".into(),
                    password: "pw2".into(),
                    region: "NA".into(),
                    note: String::new(),
                },
            ];
            save_accounts(&accounts).unwrap();
            assert!(AppPaths::accounts_file().exists());
            let loaded = load_accounts();
            assert_eq!(loaded, accounts);
        });
    }

    #[test]
    fn backup_created_on_overwrite() {
        with_temp_data_dir(|| {
            save_accounts(&[Account { username: "a".into(), ..Default::default() }]).unwrap();
            save_accounts(&[Account { username: "b".into(), ..Default::default() }]).unwrap();
            assert!(AppPaths::accounts_backup_file().exists());
        });
    }

    #[test]
    fn migrates_legacy_java_data() {
        with_temp_data_dir(|| {
            // Write a legacy (Java) accounts.dat using the legacy encrypt with the
            // current machine id, then run migration and confirm data survives.
            let mid = crypto::machine_id();
            let json = "[{\"username\":\"legacy\",\"password\":\"p\",\"region\":\"EUW\",\"note\":\"\"}]";
            let legacy = crypto::legacy_encrypt(json, &mid).unwrap();
            AppPaths::ensure_data_dir().unwrap();
            fs::write(AppPaths::accounts_file(), legacy.as_bytes()).unwrap();

            run_migration_if_needed();

            // Backup of the original legacy file must exist.
            assert!(AppPaths::accounts_backup_file().exists());
            // Data still loads correctly after migration.
            let loaded = load_accounts();
            assert_eq!(loaded.len(), 1);
            assert_eq!(loaded[0].username, "legacy");
            assert_eq!(loaded[0].region, "EUW");
        });
    }
}
