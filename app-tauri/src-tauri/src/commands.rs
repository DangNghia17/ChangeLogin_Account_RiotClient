//! Tauri command layer (IPC). Thin wrappers over the `ram_core` services; all return
//! `Result<T, String>` so the frontend can surface errors as toasts.

use ram_core::model::{Account, RiotStatus, Settings};
use ram_core::{config, riot, settings, store};

#[tauri::command]
pub fn list_accounts() -> Vec<Account> {
    store::load_accounts()
}

#[tauri::command]
pub fn add_account(account: Account) -> Result<Vec<Account>, String> {
    let mut accounts = store::load_accounts();
    accounts.push(account);
    store::save_accounts(&accounts)?;
    Ok(accounts)
}

#[tauri::command]
pub fn update_account(index: usize, account: Account) -> Result<Vec<Account>, String> {
    let mut accounts = store::load_accounts();
    if index >= accounts.len() {
        return Err("account index out of range".into());
    }
    accounts[index] = account;
    store::save_accounts(&accounts)?;
    Ok(accounts)
}

#[tauri::command]
pub fn delete_account(index: usize) -> Result<Vec<Account>, String> {
    let mut accounts = store::load_accounts();
    if index >= accounts.len() {
        return Err("account index out of range".into());
    }
    accounts.remove(index);
    store::save_accounts(&accounts)?;
    Ok(accounts)
}

/// Replaces the entire account list (used when restoring an encrypted backup).
#[tauri::command]
pub fn replace_accounts(accounts: Vec<Account>) -> Result<Vec<Account>, String> {
    store::save_accounts(&accounts)?;
    Ok(accounts)
}

/// Reads a UTF-8 text file from an absolute path (used to load a backup file the
/// user picked via the native dialog).
#[tauri::command]
pub fn read_text_file(path: String) -> Result<String, String> {
    std::fs::read_to_string(&path).map_err(|e| format!("read failed: {e}"))
}

/// Writes UTF-8 text to an absolute path (used to save an encrypted backup file).
#[tauri::command]
pub fn write_text_file(path: String, contents: String) -> Result<(), String> {
    std::fs::write(&path, contents.as_bytes()).map_err(|e| format!("write failed: {e}"))
}

#[tauri::command]
pub fn riot_status() -> RiotStatus {
    riot::status()
}

#[tauri::command]
pub fn focus_riot() -> bool {
    riot::focus_riot_client_window()
}

#[tauri::command]
pub fn launch_riot() -> Result<bool, String> {
    let path = config::get_riot_client_path().ok_or("Riot Client path is not configured")?;
    Ok(riot::launch_riot_client(&path))
}

#[tauri::command]
pub fn auto_login(index: usize) -> Result<(), String> {
    let accounts = store::load_accounts();
    let account = accounts.get(index).ok_or("account index out of range")?;
    let auto_submit = settings::get().auto_click_login;
    riot::auto_login(account, auto_submit)
}

#[tauri::command]
pub fn get_riot_path() -> Option<String> {
    config::get_riot_client_path()
}

#[tauri::command]
pub fn set_riot_path(path: String) -> Result<(), String> {
    if !config::validate_riot_client_path(&path) {
        return Err("invalid path: must point to RiotClientServices.exe".into());
    }
    config::set_riot_client_path(&path)
}

#[tauri::command]
pub fn default_riot_path() -> Option<String> {
    config::find_default_riot_client_path()
}

#[tauri::command]
pub fn validate_riot_path(path: String) -> bool {
    config::validate_riot_client_path(&path)
}

#[tauri::command]
pub fn get_settings() -> Settings {
    settings::get()
}

#[tauri::command]
pub fn set_settings(settings: Settings) -> Result<Settings, String> {
    settings::set(settings)
}

#[tauri::command]
pub fn set_language(language: String) -> Result<(), String> {
    config::set_language(&language)
}
