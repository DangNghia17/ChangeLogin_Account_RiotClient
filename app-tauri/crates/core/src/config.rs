//! Reads/writes `config.json`. Backward compatible with the Java app, which used the
//! same file and the keys `riotClientPath`, `runAtStartup`, `autoClickLogin`, `language`.
//! Unknown keys are preserved on write.

use crate::model::Settings;
use crate::paths::AppPaths;
use serde_json::{Map, Value};
use std::fs;
use std::path::Path;

const KEY_RIOT_PATH: &str = "riotClientPath";
const KEY_RUN_AT_STARTUP: &str = "runAtStartup";
const KEY_AUTO_CLICK_LOGIN: &str = "autoClickLogin";
const KEY_LANGUAGE: &str = "language";

fn read_object() -> Map<String, Value> {
    let path = AppPaths::config_file();
    if !path.exists() {
        return Map::new();
    }
    match fs::read_to_string(&path) {
        Ok(s) if !s.trim().is_empty() => serde_json::from_str::<Value>(&s)
            .ok()
            .and_then(|v| v.as_object().cloned())
            .unwrap_or_default(),
        _ => Map::new(),
    }
}

fn write_object(obj: &Map<String, Value>) -> Result<(), String> {
    AppPaths::ensure_data_dir().map_err(|e| e.to_string())?;
    let pretty = serde_json::to_string_pretty(&Value::Object(obj.clone()))
        .map_err(|e| e.to_string())?;
    fs::write(AppPaths::config_file(), pretty).map_err(|e| e.to_string())
}

pub fn get_riot_client_path() -> Option<String> {
    let obj = read_object();
    if let Some(Value::String(p)) = obj.get(KEY_RIOT_PATH) {
        if !p.is_empty() && Path::new(p).exists() {
            return Some(p.clone());
        }
    }
    // Auto-detect default and persist it (matches Java behaviour).
    if let Some(def) = find_default_riot_client_path() {
        let _ = set_riot_client_path(&def);
        return Some(def);
    }
    None
}

pub fn set_riot_client_path(path: &str) -> Result<(), String> {
    let mut obj = read_object();
    obj.insert(KEY_RIOT_PATH.to_string(), Value::String(path.to_string()));
    write_object(&obj)
}

pub fn find_default_riot_client_path() -> Option<String> {
    let candidates = [
        "C:\\Riot Games\\Riot Client\\RiotClientServices.exe",
        "C:\\Program Files\\Riot Games\\Riot Client\\RiotClientServices.exe",
        "C:\\Program Files (x86)\\Riot Games\\Riot Client\\RiotClientServices.exe",
    ];
    candidates
        .iter()
        .find(|p| Path::new(p).is_file())
        .map(|p| p.to_string())
}

pub fn validate_riot_client_path(path: &str) -> bool {
    if path.is_empty() {
        return false;
    }
    let p = Path::new(path);
    p.is_file()
        && path
            .to_lowercase()
            .ends_with("riotclientservices.exe")
}

pub fn get_settings() -> Settings {
    let obj = read_object();
    Settings {
        run_at_startup: obj.get(KEY_RUN_AT_STARTUP).and_then(Value::as_bool).unwrap_or(false),
        auto_click_login: obj.get(KEY_AUTO_CLICK_LOGIN).and_then(Value::as_bool).unwrap_or(true),
        language: obj
            .get(KEY_LANGUAGE)
            .and_then(Value::as_str)
            .unwrap_or("vi")
            .to_string(),
    }
}

pub fn set_settings(settings: &Settings) -> Result<(), String> {
    let mut obj = read_object();
    obj.insert(KEY_RUN_AT_STARTUP.to_string(), Value::Bool(settings.run_at_startup));
    obj.insert(KEY_AUTO_CLICK_LOGIN.to_string(), Value::Bool(settings.auto_click_login));
    obj.insert(KEY_LANGUAGE.to_string(), Value::String(settings.language.clone()));
    write_object(&obj)
}

pub fn set_language(lang: &str) -> Result<(), String> {
    let mut obj = read_object();
    obj.insert(KEY_LANGUAGE.to_string(), Value::String(lang.to_string()));
    write_object(&obj)
}
