// Prevents an extra console window on Windows in release builds.
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
    if let Err(err) = riot_account_manager_lib::run() {
        log_startup_error(&err);
        show_fatal_error(&err);
        std::process::exit(1);
    }
}

fn log_startup_error(err: &str) {
    if let Ok(dir) = ram_core::paths::AppPaths::ensure_data_dir() {
        let path = dir.join("startup.log");
        let msg = format!("ERROR: {err}\n");
        let _ = std::fs::write(path, msg);
    }
}

#[cfg(windows)]
fn show_fatal_error(message: &str) {
    use std::ffi::OsStr;
    use std::os::windows::ffi::OsStrExt;

    #[link(name = "user32")]
    extern "system" {
        fn MessageBoxW(hwnd: *mut std::ffi::c_void, text: *const u16, caption: *const u16, utype: u32) -> i32;
    }

    fn wide(s: &str) -> Vec<u16> {
        OsStr::new(s).encode_wide().chain(Some(0)).collect()
    }

    let body = format!(
        "{message}\n\nLog: %LOCALAPPDATA%\\RiotAccountManager\\startup.log"
    );
    const MB_ICONERROR: u32 = 0x10;
    unsafe {
        MessageBoxW(
            std::ptr::null_mut(),
            wide(&body).as_ptr(),
            wide("Riot Account Manager").as_ptr(),
            MB_ICONERROR,
        );
    }
}

#[cfg(not(windows))]
fn show_fatal_error(message: &str) {
    eprintln!("{message}");
}
