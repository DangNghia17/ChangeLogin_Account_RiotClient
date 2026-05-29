use serde::{Deserialize, Serialize};

/// A single Riot account. Field names match the legacy Java JSON schema exactly
/// (`username`, `password`, `region`, `note`) to preserve data compatibility.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Default)]
pub struct Account {
    #[serde(default)]
    pub username: String,
    #[serde(default)]
    pub password: String,
    #[serde(default)]
    pub region: String,
    #[serde(default)]
    pub note: String,
}

/// Riot Client live status, surfaced to the UI.
#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct RiotStatus {
    pub running: bool,
    pub window_visible: bool,
}

/// User settings persisted in `config.json` (alongside the Riot Client path).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Settings {
    #[serde(default)]
    pub run_at_startup: bool,
    #[serde(default = "default_true")]
    pub auto_click_login: bool,
    #[serde(default = "default_language")]
    pub language: String,
}

fn default_true() -> bool {
    true
}

fn default_language() -> String {
    "vi".to_string()
}

impl Default for Settings {
    fn default() -> Self {
        Settings {
            run_at_startup: false,
            auto_click_login: true,
            language: "vi".to_string(),
        }
    }
}
