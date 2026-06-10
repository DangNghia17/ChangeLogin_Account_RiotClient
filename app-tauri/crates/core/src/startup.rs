//! "Run at Windows startup" via the per-user registry Run key
//! (`HKCU\Software\Microsoft\Windows\CurrentVersion\Run`, value `RiotAccountManager`).
//! Per-user scope needs no admin rights and works on Windows 10/11. Matches the Java
//! implementation's value name so the entry stays consistent across the migration.

#[cfg(windows)]
const RUN_KEY: &str = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
#[cfg(windows)]
const VALUE_NAME: &str = "RiotAccountManager";

#[cfg(windows)]
fn launch_command() -> Option<String> {
    let exe = std::env::current_exe().ok()?;
    Some(format!("\"{}\"", exe.to_string_lossy()))
}

#[cfg(windows)]
pub fn enable() -> Result<(), String> {
    use std::process::Command;
    let cmd = launch_command().ok_or("could not resolve current executable path")?;
    let status = Command::new("reg")
        .args(["add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", &cmd, "/f"])
        .status()
        .map_err(|e| e.to_string())?;
    if status.success() {
        Ok(())
    } else {
        Err("reg add failed".to_string())
    }
}

#[cfg(windows)]
pub fn disable() -> Result<(), String> {
    use std::process::Command;
    if !is_enabled() {
        return Ok(());
    }
    let status = Command::new("reg")
        .args(["delete", RUN_KEY, "/v", VALUE_NAME, "/f"])
        .status()
        .map_err(|e| e.to_string())?;
    if status.success() {
        Ok(())
    } else {
        Err("reg delete failed".to_string())
    }
}

#[cfg(windows)]
pub fn is_enabled() -> bool {
    use std::process::Command;
    Command::new("reg")
        .args(["query", RUN_KEY, "/v", VALUE_NAME])
        .output()
        .map(|o| String::from_utf8_lossy(&o.stdout).contains(VALUE_NAME))
        .unwrap_or(false)
}

// Non-Windows stubs so the crate builds/tests everywhere.
#[cfg(not(windows))]
pub fn enable() -> Result<(), String> {
    Err("startup registration is only supported on Windows".to_string())
}

#[cfg(not(windows))]
pub fn disable() -> Result<(), String> {
    Ok(())
}

#[cfg(not(windows))]
pub fn is_enabled() -> bool {
    false
}
