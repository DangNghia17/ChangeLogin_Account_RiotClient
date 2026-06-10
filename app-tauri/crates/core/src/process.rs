//! Windows subprocess helpers — hide console windows (no CMD flash).

#[cfg(windows)]
pub fn command(program: &str) -> std::process::Command {
    use std::os::windows::process::CommandExt;
    const CREATE_NO_WINDOW: u32 = 0x0800_0000;
    let mut cmd = std::process::Command::new(program);
    cmd.creation_flags(CREATE_NO_WINDOW);
    cmd
}

#[cfg(not(windows))]
pub fn command(program: &str) -> std::process::Command {
    std::process::Command::new(program)
}

/// PowerShell with hidden window flags (for focus/bounds scripts only — not polling).
#[cfg(windows)]
pub fn powershell() -> std::process::Command {
    let mut cmd = command("powershell.exe");
    cmd.args([
        "-NoProfile",
        "-NonInteractive",
        "-WindowStyle",
        "Hidden",
        "-ExecutionPolicy",
        "Bypass",
    ]);
    cmd
}

#[cfg(not(windows))]
pub fn powershell() -> std::process::Command {
    std::process::Command::new("powershell")
}
