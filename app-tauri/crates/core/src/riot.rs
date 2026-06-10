//! Interaction with the external Riot Client: process/window detection, focusing,
//! measuring the window, launching, and automated credential entry.
//!
//! Detection/focus/measure reuse the proven PowerShell + Win32 approach from the Java
//! version (behaviour parity). Input simulation uses `enigo` on Windows. Only standard
//! OS APIs and input simulation are used (no hooking/injection) — Vanguard-safe.

use crate::model::{Account, RiotStatus};

pub fn status() -> RiotStatus {
    RiotStatus {
        running: is_riot_client_running(),
        window_visible: is_riot_client_window_visible(),
    }
}

pub fn is_riot_client_running() -> bool {
    #[cfg(windows)]
    {
        use crate::process;
        if let Ok(out) = process::command("tasklist").output() {
            let text = String::from_utf8_lossy(&out.stdout).to_lowercase();
            return text.contains("riotclientservices.exe") || text.contains("riot client");
        }
    }
    false
}

pub fn is_riot_client_window_visible() -> bool {
    #[cfg(windows)]
    {
        use crate::process;
        let ps = "Get-Process | Where-Object {$_.MainWindowTitle -like '*Riot Client*' -or $_.MainWindowTitle -like '*Riot*'} | Select-Object -First 1 | ForEach-Object { $_.MainWindowTitle }";
        if let Ok(out) = process::command("powershell").args(["-Command", ps]).output() {
            let line = String::from_utf8_lossy(&out.stdout).to_lowercase();
            return line.contains("riot");
        }
    }
    false
}

pub fn launch_riot_client(path: &str) -> bool {
    if !std::path::Path::new(path).exists() {
        return false;
    }
    std::process::Command::new(path).spawn().is_ok()
}

#[cfg(windows)]
fn run_powershell_script(script: &str) -> Option<String> {
    use crate::process;
    use std::io::Write;
    let mut tmp = std::env::temp_dir();
    tmp.push(format!("riotClient_{}.ps1", std::process::id()));
    {
        let mut f = std::fs::File::create(&tmp).ok()?;
        f.write_all(script.as_bytes()).ok()?;
    }
    let out = process::command("powershell.exe")
        .args(["-ExecutionPolicy", "Bypass", "-File"])
        .arg(&tmp)
        .output()
        .ok()?;
    let _ = std::fs::remove_file(&tmp);
    Some(String::from_utf8_lossy(&out.stdout).to_string())
}

pub fn focus_riot_client_window() -> bool {
    #[cfg(windows)]
    {
        if let Some(out) = run_powershell_script(FOCUS_SCRIPT) {
            return out.lines().any(|l| l.trim() == "OK");
        }
    }
    false
}

/// Returns the client-area bounds (physical pixels) of the Riot Client window.
pub fn get_riot_client_window_bounds() -> Option<(i32, i32, i32, i32)> {
    #[cfg(windows)]
    {
        for attempt in 0..3 {
            if attempt > 0 {
                std::thread::sleep(std::time::Duration::from_millis(500));
            }
            if let Some(out) = run_powershell_script(WINDOW_BOUNDS_SCRIPT) {
                for line in out.lines() {
                    let line = line.trim();
                    let parts: Vec<&str> = line.split(',').collect();
                    if parts.len() == 4 {
                        if let (Ok(x), Ok(y), Ok(w), Ok(h)) = (
                            parts[0].trim().parse::<i32>(),
                            parts[1].trim().parse::<i32>(),
                            parts[2].trim().parse::<i32>(),
                            parts[3].trim().parse::<i32>(),
                        ) {
                            if w > 100 && h > 100 {
                                return Some((x, y, w, h));
                            }
                        }
                    }
                }
            }
        }
    }
    None
}

/// Fills the Riot Client login form for the given account, optionally submitting (Enter).
pub fn auto_login(account: &Account, auto_submit: bool) -> Result<(), String> {
    #[cfg(windows)]
    {
        use enigo::{
            Button, Coordinate, Direction, Enigo, Key, Keyboard, Mouse, Settings,
        };
        use std::thread::sleep;
        use std::time::Duration;

        if !is_riot_client_running() || !is_riot_client_window_visible() {
            return Err("Riot Client is not running or its window is not visible".into());
        }

        sleep(Duration::from_millis(400));
        focus_riot_client_window();
        sleep(Duration::from_millis(700));

        let (x, y, w, h) =
            get_riot_client_window_bounds().ok_or("Could not obtain Riot Client window bounds")?;

        // Username field ≈ 13.3% from left, 25% from top of the client area.
        let username_x_ratio = 0.38 * 0.35;
        let username_y_ratio = 0.25;
        let mut px = x + (w as f64 * username_x_ratio) as i32;
        let mut py = y + (h as f64 * username_y_ratio) as i32;
        let margin_x = (w as f64 * 0.02).max(10.0) as i32;
        let margin_y = (h as f64 * 0.02).max(10.0) as i32;
        px = px.clamp(x + margin_x, x + w - margin_x);
        py = py.clamp(y + margin_y, y + h - margin_y);

        let mut enigo = Enigo::new(&Settings::default()).map_err(|e| e.to_string())?;

        // Dismiss any transient dialog.
        let _ = enigo.key(Key::Escape, Direction::Click);
        sleep(Duration::from_millis(150));

        // Click the username field.
        enigo
            .move_mouse(px, py, Coordinate::Abs)
            .map_err(|e| e.to_string())?;
        sleep(Duration::from_millis(250));
        enigo
            .button(Button::Left, Direction::Click)
            .map_err(|e| e.to_string())?;
        sleep(Duration::from_millis(500));

        clear_field(&mut enigo)?;
        enigo.text(&account.username).map_err(|e| e.to_string())?;
        sleep(Duration::from_millis(150));

        enigo.key(Key::Tab, Direction::Click).map_err(|e| e.to_string())?;
        sleep(Duration::from_millis(200));

        clear_field(&mut enigo)?;
        enigo.text(&account.password).map_err(|e| e.to_string())?;
        sleep(Duration::from_millis(150));

        if auto_submit {
            sleep(Duration::from_millis(150));
            enigo.key(Key::Return, Direction::Click).map_err(|e| e.to_string())?;
        }
        Ok(())
    }
    #[cfg(not(windows))]
    {
        let _ = (account, auto_submit);
        Err("Auto login is only supported on Windows".into())
    }
}

#[cfg(windows)]
fn clear_field(enigo: &mut enigo::Enigo) -> Result<(), String> {
    use enigo::{Direction, Key, Keyboard};
    use std::thread::sleep;
    use std::time::Duration;
    enigo.key(Key::Control, Direction::Press).map_err(|e| e.to_string())?;
    enigo.key(Key::Unicode('a'), Direction::Click).map_err(|e| e.to_string())?;
    enigo.key(Key::Control, Direction::Release).map_err(|e| e.to_string())?;
    sleep(Duration::from_millis(50));
    enigo.key(Key::Delete, Direction::Click).map_err(|e| e.to_string())?;
    sleep(Duration::from_millis(80));
    Ok(())
}

// --- Embedded PowerShell scripts (Win32 interop), ported from the Java version. ---

#[cfg(windows)]
const WINDOW_BOUNDS_SCRIPT: &str = r#"$ErrorActionPreference = 'Stop'
$allProcesses = Get-Process | Where-Object {$_.MainWindowHandle -ne 0}
$process = $allProcesses | Where-Object {
    $_.MainWindowTitle -like '*Riot Client*' -and
    ($_.ProcessName -like '*RiotClient*' -or $_.ProcessName -like '*Riot*')
} | Select-Object -First 1
if ($process -eq $null) {
    $process = $allProcesses | Where-Object {
        $_.ProcessName -eq 'RiotClientServices' -or
        $_.ProcessName -eq 'RiotClientUx' -or
        $_.ProcessName -like 'RiotClient*'
    } | Where-Object {$_.MainWindowTitle -ne ''} | Select-Object -First 1
}
if ($process -ne $null -and $process.MainWindowHandle -ne 0) {
    try {
        Add-Type @'
        using System;
        using System.Runtime.InteropServices;
        public class Win32B {
            [DllImport("user32.dll")]
            public static extern bool GetClientRect(IntPtr hWnd, out RECT lpRect);
            [DllImport("user32.dll")]
            public static extern bool ClientToScreen(IntPtr hWnd, ref POINT lpPoint);
            [StructLayout(LayoutKind.Sequential)]
            public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
            [StructLayout(LayoutKind.Sequential)]
            public struct POINT { public int X; public int Y; }
        }
'@
        $hWnd = $process.MainWindowHandle
        $clientRect = New-Object Win32B+RECT
        $ok2 = [Win32B]::GetClientRect($hWnd, [ref]$clientRect)
        $clientPoint = New-Object Win32B+POINT
        $clientPoint.X = 0
        $clientPoint.Y = 0
        $ok3 = [Win32B]::ClientToScreen($hWnd, [ref]$clientPoint)
        if ($ok2 -and $ok3) {
            $w = $clientRect.Right - $clientRect.Left
            $h = $clientRect.Bottom - $clientRect.Top
            if ($w -gt 0 -and $h -gt 0) {
                Write-Output ($clientPoint.X.ToString() + ',' + $clientPoint.Y.ToString() + ',' + $w.ToString() + ',' + $h.ToString())
            }
        }
    } catch { }
}
"#;

#[cfg(windows)]
const FOCUS_SCRIPT: &str = r#"$ErrorActionPreference = 'Stop'
$allProcesses = Get-Process | Where-Object {$_.MainWindowHandle -ne 0}
$process = $allProcesses | Where-Object {
    $_.MainWindowTitle -like '*Riot Client*' -and
    ($_.ProcessName -like '*RiotClient*' -or $_.ProcessName -like '*Riot*')
} | Select-Object -First 1
if ($process -eq $null) {
    $process = $allProcesses | Where-Object {
        ($_.ProcessName -eq 'RiotClientServices' -or $_.ProcessName -eq 'RiotClientUx') -and
        $_.MainWindowTitle -ne ''
    } | Select-Object -First 1
}
if ($process -eq $null) {
    $process = Get-Process | Where-Object {
        $_.ProcessName -eq 'RiotClientServices' -or $_.ProcessName -eq 'RiotClientUx'
    } | Select-Object -First 1
}
if ($process -ne $null) {
    try {
        Add-Type @'
        using System;
        using System.Runtime.InteropServices;
        public class Win32F {
            [DllImport("user32.dll")]
            public static extern bool SetForegroundWindow(IntPtr hWnd);
            [DllImport("user32.dll")]
            public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
            [DllImport("user32.dll")]
            public static extern bool IsIconic(IntPtr hWnd);
            [DllImport("user32.dll")]
            public static extern bool BringWindowToTop(IntPtr hWnd);
            public const int SW_RESTORE = 9;
            public const int SW_SHOW = 5;
        }
'@
        if ($process.MainWindowHandle -ne 0) {
            $hWnd = $process.MainWindowHandle
            if ([Win32F]::IsIconic($hWnd)) {
                [Win32F]::ShowWindow($hWnd, [Win32F]::SW_RESTORE)
                Start-Sleep -Milliseconds 300
            } else {
                [Win32F]::ShowWindow($hWnd, [Win32F]::SW_SHOW)
                Start-Sleep -Milliseconds 100
            }
            [Win32F]::BringWindowToTop($hWnd)
            Start-Sleep -Milliseconds 100
            if ([Win32F]::SetForegroundWindow($hWnd)) {
                Write-Output 'OK'
                exit 0
            } else {
                [Win32F]::BringWindowToTop($hWnd)
                Start-Sleep -Milliseconds 200
                if ([Win32F]::SetForegroundWindow($hWnd)) { Write-Output 'OK'; exit 0 }
            }
        }
    } catch { }
}
"#;
