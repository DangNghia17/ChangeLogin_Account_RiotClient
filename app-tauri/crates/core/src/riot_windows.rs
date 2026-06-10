//! Native Windows APIs for Riot Client detection — no tasklist/powershell (no CMD flash).

#[cfg(windows)]
pub fn is_riot_client_running() -> bool {
    use windows::Win32::Foundation::CloseHandle;
    use windows::Win32::System::Diagnostics::ToolHelp::{
        CreateToolhelp32Snapshot, Process32FirstW, Process32NextW, PROCESSENTRY32W,
        TH32CS_SNAPPROCESS,
    };

    const TARGETS: &[&str] = &[
        "riotclientservices.exe",
        "riotclientux.exe",
        "riotclient.exe",
    ];

    unsafe {
        let snap = match CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0) {
            Ok(h) => h,
            Err(_) => return false,
        };

        let mut entry = PROCESSENTRY32W {
            dwSize: std::mem::size_of::<PROCESSENTRY32W>() as u32,
            ..Default::default()
        };

        let mut found = false;
        if Process32FirstW(snap, &mut entry).is_ok() {
            loop {
                let name = wchar_to_string(&entry.szExeFile).to_lowercase();
                if TARGETS.iter().any(|t| name == *t) {
                    found = true;
                    break;
                }
                if Process32NextW(snap, &mut entry).is_err() {
                    break;
                }
            }
        }
        let _ = CloseHandle(snap);
        found
    }
}

#[cfg(windows)]
pub fn is_riot_client_window_visible() -> bool {
    use windows::Win32::Foundation::{BOOL, HWND, LPARAM};
    use windows::Win32::UI::WindowsAndMessaging::{
        EnumWindows, GetWindowTextW, IsWindowVisible,
    };

    struct Ctx {
        found: bool,
    }

    unsafe extern "system" fn callback(hwnd: HWND, lparam: LPARAM) -> BOOL {
        let ctx = &mut *(lparam.0 as *mut Ctx);
        if !IsWindowVisible(hwnd).as_bool() {
            return BOOL(1);
        }
        let mut buf = [0u16; 512];
        let len = GetWindowTextW(hwnd, &mut buf);
        if len > 0 {
            let title = String::from_utf16_lossy(&buf[..len as usize]).to_lowercase();
            if title.contains("riot client") || title.contains("riot") {
                ctx.found = true;
                return BOOL(0);
            }
        }
        BOOL(1)
    }

    let mut ctx = Ctx { found: false };
    unsafe {
        let _ = EnumWindows(Some(callback), LPARAM(&mut ctx as *mut _ as isize));
    }
    ctx.found
}

#[cfg(windows)]
fn wchar_to_string(buf: &[u16]) -> String {
    let end = buf.iter().position(|&c| c == 0).unwrap_or(buf.len());
    String::from_utf16_lossy(&buf[..end])
}
