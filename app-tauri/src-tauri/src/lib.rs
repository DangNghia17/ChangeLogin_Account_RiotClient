mod commands;

/// Application entry used by both the desktop binary and (potentially) mobile targets.
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    // Data-safety first: migrate legacy data (with backup + rollback) before the UI opens.
    ram_core::store::run_migration_if_needed();
    // Keep the OS startup entry consistent with the saved setting.
    ram_core::settings::sync_startup_registration();

    tauri::Builder::default()
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_dialog::init())
        .invoke_handler(tauri::generate_handler![
            commands::list_accounts,
            commands::add_account,
            commands::update_account,
            commands::delete_account,
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
        .expect("error while running tauri application");
}
