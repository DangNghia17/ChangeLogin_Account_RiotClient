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
