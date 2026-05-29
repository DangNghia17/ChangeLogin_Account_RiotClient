//! Typed settings access, keeping the persisted preference and the OS-level startup
//! registration consistent.

use crate::config;
use crate::model::Settings;
use crate::startup;

pub fn get() -> Settings {
    config::get_settings()
}

/// Persists settings and applies the startup registry change. Returns the (possibly
/// adjusted) settings; if the OS startup change fails, `run_at_startup` reflects reality.
pub fn set(mut settings: Settings) -> Result<Settings, String> {
    let startup_result = if settings.run_at_startup {
        startup::enable()
    } else {
        startup::disable()
    };
    if startup_result.is_err() && settings.run_at_startup {
        // Could not register; reflect the real state so the UI doesn't lie.
        log::warn!("Could not register startup: {:?}", startup_result);
        settings.run_at_startup = false;
    }
    config::set_settings(&settings)?;
    Ok(settings)
}

/// Reconciles the OS startup entry with the stored preference. Safe to call on launch.
pub fn sync_startup_registration() {
    let s = config::get_settings();
    let actual = startup::is_enabled();
    if s.run_at_startup && !actual {
        let _ = startup::enable();
    } else if !s.run_at_startup && actual {
        let _ = startup::disable();
    } else if s.run_at_startup {
        // Keep the path fresh in case the executable moved.
        let _ = startup::enable();
    }
}
