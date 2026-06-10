use std::fs;
use std::path::PathBuf;

/// Single source of truth for data locations. Kept IDENTICAL to the Java app so the
/// migrated app automatically picks up existing user data:
///   %LOCALAPPDATA%\RiotAccountManager\{accounts.dat, config.json, ...}
///
/// On non-Windows (dev/CI), falls back to the user's home directory.
pub struct AppPaths;

impl AppPaths {
    pub fn data_dir() -> PathBuf {
        let base = std::env::var("LOCALAPPDATA")
            .ok()
            .filter(|s| !s.trim().is_empty())
            .map(PathBuf::from)
            .or_else(|| dirs_home())
            .unwrap_or_else(|| PathBuf::from("."));
        base.join("RiotAccountManager")
    }

    pub fn accounts_file() -> PathBuf {
        Self::data_dir().join("accounts.dat")
    }

    pub fn accounts_backup_file() -> PathBuf {
        Self::data_dir().join("accounts.dat.bak")
    }

    pub fn accounts_temp_file() -> PathBuf {
        Self::data_dir().join("accounts.dat.tmp")
    }

    pub fn config_file() -> PathBuf {
        Self::data_dir().join("config.json")
    }

    pub fn data_version_file() -> PathBuf {
        Self::data_dir().join("data.version")
    }

    /// Ensures the data directory exists and returns it.
    pub fn ensure_data_dir() -> std::io::Result<PathBuf> {
        let dir = Self::data_dir();
        if !dir.exists() {
            fs::create_dir_all(&dir)?;
        }
        Ok(dir)
    }
}

fn dirs_home() -> Option<PathBuf> {
    std::env::var("HOME")
        .or_else(|_| std::env::var("USERPROFILE"))
        .ok()
        .map(PathBuf::from)
}
