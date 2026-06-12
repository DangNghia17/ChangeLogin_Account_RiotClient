mod commands;

use tauri::Manager;

/// Application entry used by both the desktop binary and (potentially) mobile targets.
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() -> Result<(), String> {
    write_startup_log("starting");

    // Data-safety first: migrate legacy data (with backup + rollback) before the UI opens.
    ram_core::store::run_migration_if_needed();
    // Keep the OS startup entry consistent with the saved setting.
    ram_core::settings::sync_startup_registration();

    write_startup_log("launching tauri");

    tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, _argv, _cwd| {
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.unminimize();
                let _ = window.show();
                let _ = window.set_focus();
            }
        }))
        .plugin(tauri_plugin_dialog::init())
        .invoke_handler(tauri::generate_handler![
            commands::list_accounts,
            commands::add_account,
            commands::update_account,
            commands::delete_account,
            commands::replace_accounts,
            commands::read_text_file,
            commands::write_text_file,
            commands::riot_status,
            commands::focus_riot,
            commands::launch_riot,
            commands::auto_login,
            commands::get_riot_path,
            commands::set_riot_path,
            commands::default_riot_path,
            commands::validate_riot_path,
            commands::get_settings,
            commands::set_settings,
            commands::set_language,
        ])
        .run(tauri::generate_context!())
        .map_err(|e| format!("Tauri runtime error: {e}"))
}

fn write_startup_log(line: &str) {
    if let Ok(dir) = ram_core::paths::AppPaths::ensure_data_dir() {
        let path = dir.join("startup.log");
        let ts = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);
        let msg = format!("[{ts}] {line}\n");
        use std::io::Write;
        if let Ok(mut f) = std::fs::OpenOptions::new().create(true).append(true).open(path) {
            let _ = f.write_all(msg.as_bytes());
        }
    }
}

