package com.riotaccountmanager;

import java.awt.*;
import java.awt.MouseInfo;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class AutoLoginHelper {
    private static final int TIMEOUT_SECONDS = 30;
    
    public static boolean isRiotClientRunning() {
        try {
            Process process = Runtime.getRuntime().exec("tasklist");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("riotclientservices.exe") ||
                    line.toLowerCase().contains("riot client")) {
                    return true;
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean isRiotClientWindowVisible() {
        try {
            String command = "powershell -Command \"Get-Process | Where-Object {$_.MainWindowTitle -like '*Riot Client*' -or $_.MainWindowTitle -like '*Riot*'} | Select-Object -First 1 | ForEach-Object { $_.MainWindowTitle }\"";
            Process process = Runtime.getRuntime().exec(command);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String line = reader.readLine();
            reader.close();
            process.waitFor();
            
            if (line != null && !line.trim().isEmpty() && 
                (line.toLowerCase().contains("riot client") || line.toLowerCase().contains("riot"))) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Không thể kiểm tra window, sử dụng check process: " + e.getMessage());
        }
        return false;
    }
    
    public static Rectangle getRiotClientWindowBounds() {
        for (int retry = 0; retry < 3; retry++) {
            try {
                if (retry > 0) {
                    System.out.println("Retry lấy thông tin cửa sổ Riot Client (lần " + (retry + 1) + ")...");
                    Thread.sleep(500);
                }
                
                java.io.File tempScript = java.io.File.createTempFile("getRiotWindow", ".ps1");
                tempScript.deleteOnExit();
                
                String scriptContent = 
                    "$ErrorActionPreference = 'Stop'\n" +
                    "# Tìm process Riot Client với logic ưu tiên (giống focusRiotClientWindow)\n" +
                    "$process = $null\n" +
                    "$allProcesses = Get-Process | Where-Object {$_.MainWindowHandle -ne 0}\n" +
                    "\n" +
                    "# Ưu tiên 1: Tìm theo MainWindowTitle chứa 'Riot Client' (chính xác)\n" +
                    "$process = $allProcesses | Where-Object {\n" +
                    "    $_.MainWindowTitle -like '*Riot Client*' -and\n" +
                    "    ($_.ProcessName -like '*RiotClient*' -or $_.ProcessName -like '*Riot*')\n" +
                    "} | Select-Object -First 1\n" +
                    "\n" +
                    "# Ưu tiên 2: Nếu không tìm thấy, tìm theo process name cụ thể\n" +
                    "if ($process -eq $null) {\n" +
                    "    $process = $allProcesses | Where-Object {\n" +
                    "        $_.ProcessName -eq 'RiotClientServices' -or\n" +
                    "        $_.ProcessName -eq 'RiotClientUx' -or\n" +
                    "        $_.ProcessName -like 'RiotClient*'\n" +
                    "    } | Where-Object {$_.MainWindowTitle -ne ''} | Select-Object -First 1\n" +
                    "}\n" +
                    "\n" +
                    "# Chỉ xử lý nếu tìm thấy process hợp lệ\n" +
                    "if ($process -ne $null -and $process.MainWindowHandle -ne 0) {\n" +
                    "    try {\n" +
                    "        Add-Type @'\n" +
                    "        using System;\n" +
                    "        using System.Runtime.InteropServices;\n" +
                    "        public class Win32 {\n" +
                    "            [DllImport(\"user32.dll\")]\n" +
                    "            public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);\n" +
                    "            [DllImport(\"user32.dll\")]\n" +
                    "            public static extern bool GetClientRect(IntPtr hWnd, out RECT lpRect);\n" +
                    "            [DllImport(\"user32.dll\")]\n" +
                    "            public static extern bool ClientToScreen(IntPtr hWnd, ref POINT lpPoint);\n" +
                    "            [StructLayout(LayoutKind.Sequential)]\n" +
                    "            public struct RECT {\n" +
                    "                public int Left;\n" +
                    "                public int Top;\n" +
                    "                public int Right;\n" +
                    "                public int Bottom;\n" +
                    "            }\n" +
                    "            [StructLayout(LayoutKind.Sequential)]\n" +
                    "            public struct POINT {\n" +
                    "                public int X;\n" +
                    "                public int Y;\n" +
                    "            }\n" +
                    "        }\n" +
                    "'@\n" +
                    "        $hWnd = $process.MainWindowHandle\n" +
                    "        \n" +
                    "        # Lấy window rectangle (bao gồm border và title bar)\n" +
                    "        $windowRect = New-Object Win32+RECT\n" +
                    "        $success1 = [Win32]::GetWindowRect($hWnd, [ref]$windowRect)\n" +
                    "        \n" +
                    "        # Lấy client rectangle (chỉ phần client area, không bao gồm border và title bar)\n" +
                    "        $clientRect = New-Object Win32+RECT\n" +
                    "        $success2 = [Win32]::GetClientRect($hWnd, [ref]$clientRect)\n" +
                    "        \n" +
                    "        # Lấy vị trí client area trên màn hình\n" +
                    "        $clientPoint = New-Object Win32+POINT\n" +
                    "        $clientPoint.X = 0\n" +
                    "        $clientPoint.Y = 0\n" +
                    "        $success3 = [Win32]::ClientToScreen($hWnd, [ref]$clientPoint)\n" +
                    "        \n" +
                    "        if ($success1 -and $success2 -and $success3) {\n" +
                    "            # Sử dụng client area (không bao gồm border và title bar)\n" +
                    "            $clientX = $clientPoint.X\n" +
                    "            $clientY = $clientPoint.Y\n" +
                    "            $clientWidth = $clientRect.Right - $clientRect.Left\n" +
                    "            $clientHeight = $clientRect.Bottom - $clientRect.Top\n" +
                    "            \n" +
                    "            if ($clientWidth -gt 0 -and $clientHeight -gt 0) {\n" +
                    "                Write-Output ($clientX.ToString() + ',' + $clientY.ToString() + ',' + $clientWidth.ToString() + ',' + $clientHeight.ToString())\n" +
                    "            }\n" +
                    "        }\n" +
                    "    } catch {\n" +
                    "        # Ignore errors\n" +
                    "    }\n" +
                    "} else {\n" +
                    "    # Process not found\n" +
                    "}\n";
                
                java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(tempScript), 
                    java.nio.charset.StandardCharsets.UTF_8
                );
                writer.write(scriptContent);
                writer.close();
                
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", tempScript.getAbsolutePath());
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(),                     java.nio.charset.StandardCharsets.UTF_8)
                );
                
                String result = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("^-?\\d+,-?\\d+,-?\\d+,-?\\d+$")) {
                        result = line;
                        break;
                    }
                }
                reader.close();
                
                int exitCode = process.waitFor();
                
                if (result != null && !result.isEmpty()) {
                    String[] parts = result.split(",");
                    if (parts.length == 4) {
                        try {
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            int width = Integer.parseInt(parts[2].trim());
                            int height = Integer.parseInt(parts[3].trim());
                            
                            if (width > 100 && height > 100) {
                                System.out.println("✓ Tìm thấy cửa sổ Riot Client: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);
                                return new Rectangle(x, y, width, height);
                            } else {
                                System.out.println("Kích thước cửa sổ không hợp lệ: width=" + width + ", height=" + height);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Lỗi parse window bounds: " + result + " - " + e.getMessage());
                        }
                    } else {
                        System.out.println("Kết quả không đúng format (cần 4 số): " + result);
                    }
                } else {
                    System.out.println("Không tìm thấy thông tin cửa sổ Riot Client từ PowerShell (exit code: " + exitCode + ")");
                }
            } catch (Exception e) {
                System.out.println("Lỗi khi lấy thông tin cửa sổ Riot Client (lần " + (retry + 1) + "): " + e.getMessage());
                if (retry == 2) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("✗ Không thể lấy thông tin cửa sổ Riot Client sau 3 lần thử");
        return null;
    }
    
    public static boolean focusRiotClientWindow() {
        try {
            java.io.File tempScript = java.io.File.createTempFile("focusRiotWindow", ".ps1");
            tempScript.deleteOnExit();
            
            String scriptContent = 
                "$ErrorActionPreference = 'Stop'\n" +
                "# Tìm process Riot Client với logic ưu tiên:\n" +
                "# 1. Ưu tiên: MainWindowTitle chính xác chứa 'Riot Client' (không phải chỉ 'Riot')\n" +
                "# 2. Process name phải là RiotClientServices hoặc RiotClientUx\n" +
                "$process = $null\n" +
                "$allProcesses = Get-Process | Where-Object {$_.MainWindowHandle -ne 0}\n" +
                "\n" +
                "# Ưu tiên 1: Tìm theo MainWindowTitle chứa 'Riot Client' (chính xác)\n" +
                "$process = $allProcesses | Where-Object {\n" +
                "    $_.MainWindowTitle -like '*Riot Client*' -and\n" +
                "    ($_.ProcessName -like '*RiotClient*' -or $_.ProcessName -like '*Riot*')\n" +
                "} | Select-Object -First 1\n" +
                "\n" +
                "# Ưu tiên 2: Nếu không tìm thấy, tìm theo process name cụ thể (có MainWindowTitle)\n" +
                "# Loại trừ các process phụ như CrashHandler\n" +
                "if ($process -eq $null) {\n" +
                "    $process = $allProcesses | Where-Object {\n" +
                "        ($_.ProcessName -eq 'RiotClientServices' -or $_.ProcessName -eq 'RiotClientUx') -and\n" +
                "        $_.ProcessName -ne 'RiotClientCrashHandler' -and\n" +
                "        $_.ProcessName -ne 'RiotClientFling' -and\n" +
                "        $_.ProcessName -ne 'RiotClientBroker' -and\n" +
                "        $_.MainWindowTitle -ne ''\n" +
                "    } | Select-Object -First 1\n" +
                "}\n" +
                "\n" +
                "# Ưu tiên 3: Tìm process chính Riot Client (không phải crash handler hay các process phụ)\n" +
                "# Loại trừ: RiotClientCrashHandler, RiotClientFling, RiotClientBroker, etc.\n" +
                "# Chỉ tìm process chính: RiotClientServices hoặc RiotClientUx\n" +
                "if ($process -eq $null) {\n" +
                "    $mainProcesses = Get-Process | Where-Object {\n" +
                "        ($_.ProcessName -eq 'RiotClientServices' -or $_.ProcessName -eq 'RiotClientUx') -and\n" +
                "        $_.ProcessName -ne 'RiotClientCrashHandler' -and\n" +
                "        $_.ProcessName -ne 'RiotClientFling' -and\n" +
                "        $_.ProcessName -ne 'RiotClientBroker'\n" +
                "    } | Sort-Object {$_.ProcessName -eq 'RiotClientServices'} -Descending\n" +
                "    \n" +
                "    # Ưu tiên RiotClientServices trước, sau đó RiotClientUx\n" +
                "    $process = $mainProcesses | Select-Object -First 1\n" +
                "    \n" +
                "    if ($process -ne $null) {\n" +
                "        Write-Host \"Found Riot Client main process (may be in tray): $($process.ProcessName)\"\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "# Debug: In thông tin process tìm được\n" +
                "if ($process -ne $null) {\n" +
                "    Write-Host \"Found process: $($process.ProcessName), Title: $($process.MainWindowTitle)\"\n" +
                "}\n" +
                "\n" +
                "if ($process -ne $null) {\n" +
                "    try {\n" +
                "        Add-Type @'\n" +
                "        using System;\n" +
                "        using System.Runtime.InteropServices;\n" +
                "        public class Win32 {\n" +
                "            [DllImport(\"user32.dll\")]\n" +
                "            public static extern bool SetForegroundWindow(IntPtr hWnd);\n" +
                "            [DllImport(\"user32.dll\")]\n" +
                "            public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);\n" +
                "            [DllImport(\"user32.dll\")]\n" +
                "            public static extern bool IsIconic(IntPtr hWnd);\n" +
                "            [DllImport(\"user32.dll\")]\n" +
                "            public static extern bool BringWindowToTop(IntPtr hWnd);\n" +
                "            [DllImport(\"user32.dll\")]\n" +
                "            public static extern bool EnumWindows(IntPtr lpEnumFunc, IntPtr lParam);\n" +
                "            [DllImport(\"user32.dll\")]\n" +
                "            public static extern int GetWindowThreadProcessId(IntPtr hWnd, out int lpdwProcessId);\n" +
                "            public const int SW_RESTORE = 9;\n" +
                "            public const int SW_SHOW = 5;\n" +
                "            public const int SW_SHOWMAXIMIZED = 3;\n" +
                "        }\n" +
                "'@\n" +
                "        \n" +
                "        # Nếu process có MainWindowHandle, sử dụng nó\n" +
                "        if ($process.MainWindowHandle -ne 0) {\n" +
                "            $hWnd = $process.MainWindowHandle\n" +
                "            \n" +
                "            # Restore nếu đang minimize\n" +
                "            if ([Win32]::IsIconic($hWnd)) {\n" +
                "                [Win32]::ShowWindow($hWnd, [Win32]::SW_RESTORE)\n" +
                "                Start-Sleep -Milliseconds 300\n" +
                "            } else {\n" +
                "                [Win32]::ShowWindow($hWnd, [Win32]::SW_SHOW)\n" +
                "                Start-Sleep -Milliseconds 100\n" +
                "            }\n" +
                "            \n" +
                "            # Bring to top\n" +
                "            [Win32]::BringWindowToTop($hWnd)\n" +
                "            Start-Sleep -Milliseconds 100\n" +
                "            \n" +
                "            # Set foreground\n" +
                "            $success = [Win32]::SetForegroundWindow($hWnd)\n" +
                "            if ($success) {\n" +
                "                Write-Output \"OK\"\n" +
                "                exit 0\n" +
                "            } else {\n" +
                "                # Retry với BringWindowToTop trước\n" +
                "                [Win32]::BringWindowToTop($hWnd)\n" +
                "                Start-Sleep -Milliseconds 200\n" +
                "                $success2 = [Win32]::SetForegroundWindow($hWnd)\n" +
                "                if ($success2) {\n" +
                "                    Write-Output \"OK\"\n" +
                "                    exit 0\n" +
                "                }\n" +
                "            }\n" +
                "        } else {\n" +
                "            # Process không có MainWindowHandle (có thể trong system tray)\n" +
                "            # Thử tìm window bằng cách enumerate tất cả windows và match process ID\n" +
                "            Write-Host \"Process found but no MainWindowHandle, trying to find window by PID: $($process.Id)\"\n" +
                "            \n" +
                "            # Thêm hàm enumerate windows để tìm window thuộc process này\n" +
                "            Add-Type @'\n" +
                "            using System;\n" +
                "            using System.Runtime.InteropServices;\n" +
                "            using System.Text;\n" +
                "            using System.Collections.Generic;\n" +
                "            public class Win32Enum {\n" +
                "                [DllImport(\"user32.dll\")]\n" +
                "                public static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);\n" +
                "                [DllImport(\"user32.dll\")]\n" +
                "                public static extern bool EnumChildWindows(IntPtr hWndParent, EnumWindowsProc lpEnumFunc, IntPtr lParam);\n" +
                "                [DllImport(\"user32.dll\")]\n" +
                "                public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);\n" +
                "                [DllImport(\"user32.dll\")]\n" +
                "                public static extern bool IsWindowVisible(IntPtr hWnd);\n" +
                "                [DllImport(\"user32.dll\")]\n" +
                "                public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);\n" +
                "                [DllImport(\"user32.dll\")]\n" +
                "                public static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);\n" +
                "                [DllImport(\"user32.dll\")]\n" +
                "                public static extern IntPtr SendMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);\n" +
                "                [DllImport(\"user32.dll\")]\n" +
                "                public static extern IntPtr PostMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);\n" +
                "                public const uint WM_SYSCOMMAND = 0x0112;\n" +
                "                public const uint SC_RESTORE = 0xF120;\n" +
                "                public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);\n" +
                "            }\n" +
                "'@\n" +
                "            \n" +
                "            $script:targetPid = $process.Id\n" +
                "            $script:foundWindows = New-Object System.Collections.ArrayList\n" +
                "            \n" +
                "            # Enumerate tất cả windows để tìm window thuộc process này\n" +
                "            # Lưu tất cả windows của process, không chỉ window có title\n" +
                "            $enumProc = [Win32Enum+EnumWindowsProc] {\n" +
                "                param([IntPtr]$hWnd, [IntPtr]$lParam)\n" +
                "                $pid = 0\n" +
                "                [Win32Enum]::GetWindowThreadProcessId($hWnd, [ref]$pid)\n" +
                "                if ($pid -eq $script:targetPid) {\n" +
                "                    # Lưu tất cả windows của process này\n" +
                "                    $title = New-Object System.Text.StringBuilder(256)\n" +
                "                    [Win32Enum]::GetWindowText($hWnd, $title, 256)\n" +
                "                    $titleStr = $title.ToString()\n" +
                "                    \n" +
                "                    $className = New-Object System.Text.StringBuilder(256)\n" +
                "                    [Win32Enum]::GetClassName($hWnd, $className, 256)\n" +
                "                    $classNameStr = $className.ToString()\n" +
                "                    \n" +
                "                    # Lưu window info (handle, title, class, priority)\n" +
                "                    $windowInfo = @{\n" +
                "                        Handle = $hWnd\n" +
                "                        Title = $titleStr\n" +
                "                        ClassName = $classNameStr\n" +
                "                        Priority = 0\n" +
                "                    }\n" +
                "                    \n" +
                "                    # Đặt priority: window có title 'Riot Client' = priority 3 (cao nhất)\n" +
                "                    if ($titleStr -like '*Riot Client*') {\n" +
                "                        $windowInfo.Priority = 3\n" +
                "                    }\n" +
                "                    # Window có title không rỗng và không phải Crash = priority 2\n" +
                "                    elseif ($titleStr.Length -gt 0 -and $titleStr -notlike '*Crash*') {\n" +
                "                        $windowInfo.Priority = 2\n" +
                "                    }\n" +
                "                    # Window có class name hợp lệ = priority 1\n" +
                "                    elseif ($classNameStr.Length -gt 0) {\n" +
                "                        $windowInfo.Priority = 1\n" +
                "                    }\n" +
                "                    \n" +
                "                    # Chỉ lưu windows có priority > 0 (có title hoặc class name)\n" +
                "                    if ($windowInfo.Priority -gt 0) {\n" +
                "                        $script:foundWindows.Add($windowInfo) | Out-Null\n" +
                "                    }\n" +
                "                }\n" +
                "                return $true  # Tiếp tục enumeration\n" +
                "            }\n" +
                "            \n" +
                "            # Thử enumerate windows (cả top-level và child windows)\n" +
                "            [Win32Enum]::EnumWindows($enumProc, [IntPtr]::Zero)\n" +
                "            \n" +
                "            # Lấy kết quả từ script scope và sắp xếp theo priority\n" +
                "            $allWindows = $script:foundWindows\n" +
                "            \n" +
                "            Write-Host \"Found $($allWindows.Count) windows for process $($process.Id)\"\n" +
                "            \n" +
                "            # Sắp xếp windows theo priority (cao đến thấp)\n" +
                "            $sortedWindows = $allWindows | Sort-Object -Property Priority -Descending\n" +
                "            \n" +
                "            # Thử restore từng window theo thứ tự priority\n" +
                "            $foundWindow = $null\n" +
                "            foreach ($winInfo in $sortedWindows) {\n" +
                "                $hWnd = $winInfo.Handle\n" +
                "                Write-Host \"Trying window: Handle=$hWnd, Title='$($winInfo.Title)', Class='$($winInfo.ClassName)', Priority=$($winInfo.Priority)\"\n" +
                "                \n" +
                "                # Thử nhiều cách restore:\n" +
                "                # 1. Gửi WM_SYSCOMMAND với SC_RESTORE\n" +
                "                [Win32Enum]::PostMessage($hWnd, [Win32Enum]::WM_SYSCOMMAND, [IntPtr][Win32Enum]::SC_RESTORE, [IntPtr]::Zero)\n" +
                "                Start-Sleep -Milliseconds 300\n" +
                "                \n" +
                "                # 2. ShowWindow với SW_RESTORE\n" +
                "                [Win32]::ShowWindow($hWnd, [Win32]::SW_RESTORE)\n" +
                "                Start-Sleep -Milliseconds 300\n" +
                "                \n" +
                "                # 3. ShowWindow với SW_SHOW\n" +
                "                [Win32]::ShowWindow($hWnd, [Win32]::SW_SHOW)\n" +
                "                Start-Sleep -Milliseconds 200\n" +
                "                \n" +
                "                # 4. BringWindowToTop\n" +
                "                [Win32]::BringWindowToTop($hWnd)\n" +
                "                Start-Sleep -Milliseconds 200\n" +
                "                \n" +
                "                # 5. SetForegroundWindow\n" +
                "                $success = [Win32]::SetForegroundWindow($hWnd)\n" +
                "                \n" +
                "                # Kiểm tra xem window có được restore không\n" +
                "                $process.Refresh()\n" +
                "                if ($process.MainWindowHandle -ne 0 -or $success) {\n" +
                "                    Write-Host \"Successfully restored window: $hWnd\"\n" +
                "                    $foundWindow = $hWnd\n" +
                "                    break\n" +
                "                }\n" +
                "            }\n" +
                "            \n" +
                "            # Nếu tìm thấy window và restore thành công\n" +
                "            if ($foundWindow -ne $null -and $foundWindow -ne [IntPtr]::Zero) {\n" +
                "                $hWnd = $foundWindow\n" +
                "                \n" +
                "                # Đợi window restore hoàn toàn\n" +
                "                Start-Sleep -Milliseconds 500\n" +
                "                \n" +
                "                # Đảm bảo window ở foreground\n" +
                "                [Win32]::BringWindowToTop($hWnd)\n" +
                "                Start-Sleep -Milliseconds 200\n" +
                "                $success = [Win32]::SetForegroundWindow($hWnd)\n" +
                "                if ($success) {\n" +
                "                    Write-Output \"OK\"\n" +
                "                    exit 0\n" +
                "                }\n" +
                "            }\n" +
                "            \n" +
                "            # Nếu vẫn không restore được, thử refresh process và đợi lâu hơn\n" +
                "            Write-Host \"No window found by enumeration, trying to refresh process...\"\n" +
                "            Start-Sleep -Milliseconds 1000\n" +
                "            \n" +
                "            try {\n" +
                "                $process.Refresh()\n" +
                "                if ($process.MainWindowHandle -ne 0) {\n" +
                "                    Write-Host \"Got MainWindowHandle after refresh: $($process.MainWindowHandle)\"\n" +
                "                    $hWnd = $process.MainWindowHandle\n" +
                "                    [Win32]::ShowWindow($hWnd, [Win32]::SW_RESTORE)\n" +
                "                    Start-Sleep -Milliseconds 500\n" +
                "                    [Win32]::BringWindowToTop($hWnd)\n" +
                "                    Start-Sleep -Milliseconds 200\n" +
                "                    $success = [Win32]::SetForegroundWindow($hWnd)\n" +
                "                    if ($success) {\n" +
                "                        Write-Output \"OK\"\n" +
                "                        exit 0\n" +
                "                    }\n" +
                "                }\n" +
                "            } catch {\n" +
                "                Write-Host \"Error refreshing process: $($_.Exception.Message)\"\n" +
                "            }\n" +
                "            \n" +
                "            Write-Host \"Could not find or restore Riot Client window\"\n" +
                "        }\n" +
                "    } catch {\n" +
                "        Write-Host \"Error: $($_.Exception.Message)\"\n" +
                "    }\n" +
                "} else {\n" +
                "    Write-Host \"Riot Client process not found\"\n" +
                "}\n";
            
            // Ghi script vào file với UTF-8 encoding
            java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(tempScript), 
                java.nio.charset.StandardCharsets.UTF_8
            );
            writer.write(scriptContent);
            writer.close();
            
            // Chạy script PowerShell
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", tempScript.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Đọc output
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)
            );
            
            String result = null;
            StringBuilder debugInfo = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("OK")) {
                    result = line;
                } else if (line.length() > 0 && !line.startsWith("Error:")) {
                    // Lưu debug info (process name, title)
                    debugInfo.append(line).append("\n");
                }
            }
            reader.close();
            
            process.waitFor();
            
            if (result != null && result.equals("OK")) {
                if (debugInfo.length() > 0) {
                    System.out.println("✓ Đã focus vào cửa sổ Riot Client: " + debugInfo.toString().trim());
                } else {
                    System.out.println("✓ Đã focus vào cửa sổ Riot Client");
                }
                return true;
            } else {
                if (debugInfo.length() > 0) {
                    System.out.println("✗ Không thể focus cửa sổ. Debug: " + debugInfo.toString().trim());
                } else {
                    System.out.println("✗ Không tìm thấy cửa sổ Riot Client để focus");
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi focus cửa sổ Riot Client: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Mở Riot Client từ đường dẫn
     */
    public static boolean launchRiotClient(String clientPath) {
        try {
            File file = new File(clientPath);
            if (!file.exists()) {
                return false;
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(clientPath);
            processBuilder.start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gõ ký tự đặc biệt
     */
    private static void typeChar(Robot robot, char c) {
        if (Character.isUpperCase(c)) {
            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.keyPress(Character.toUpperCase(c));
            robot.keyRelease(Character.toUpperCase(c));
            robot.keyRelease(KeyEvent.VK_SHIFT);
        } else if (c >= 'a' && c <= 'z') {
            int keyCode = KeyEvent.VK_A + (c - 'a');
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        } else if (c >= '0' && c <= '9') {
            int keyCode = KeyEvent.VK_0 + (c - '0');
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        } else {
            switch (c) {
                case '@':
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_2);
                    robot.keyRelease(KeyEvent.VK_2);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    break;
                case '.':
                    robot.keyPress(KeyEvent.VK_PERIOD);
                    robot.keyRelease(KeyEvent.VK_PERIOD);
                    break;
                case '_':
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_MINUS);
                    robot.keyRelease(KeyEvent.VK_MINUS);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    break;
                case '-':
                    robot.keyPress(KeyEvent.VK_MINUS);
                    robot.keyRelease(KeyEvent.VK_MINUS);
                    break;
                default:
                    // Gõ ký tự khác
                    robot.keyPress(Character.toUpperCase(c));
                    robot.keyRelease(Character.toUpperCase(c));
                    break;
            }
        }
    }
    
    /**
     * Tự động đăng nhập vào Riot Client
     * Quy trình đơn giản: Bật Riot Client lên cao nhất -> Trỏ vào input -> Clean -> Nhập
     * Lưu ý: Riot Client phải đã được mở và cửa sổ phải hiển thị trước khi gọi hàm này
     */
    public static boolean autoLogin(Account account, String clientPath) {
        try {
            // Bước 1: Kiểm tra Riot Client có đang chạy và window có hiển thị không
            boolean isRunning = isRiotClientRunning();
            boolean isWindowVisible = isRiotClientWindowVisible();
            
            if (!isRunning || !isWindowVisible) {
                System.out.println("LỖI: Riot Client chưa được mở hoặc cửa sổ chưa hiển thị.");
                System.out.println("Vui lòng mở Riot Client và đảm bảo cửa sổ đang hiển thị trước khi đăng nhập.");
                return false;
            }
            
            System.out.println("Riot Client đã sẵn sàng. Bắt đầu đăng nhập...");
            // Đợi một chút để đảm bảo window sẵn sàng
            Thread.sleep(500);
            
            // Bước 2: Focus vào cửa sổ Riot Client sử dụng Windows API
            System.out.println("Đang focus vào cửa sổ Riot Client...");
            if (!focusRiotClientWindow()) {
                System.out.println("CẢNH BÁO: Không thể focus vào cửa sổ Riot Client, nhưng vẫn tiếp tục thử...");
            }
            Thread.sleep(800); // Đợi để cửa sổ được focus hoàn toàn
            
            Robot robot = new Robot();
            robot.setAutoDelay(10);
            
            // Bước 3: Lấy vị trí và kích thước cửa sổ Riot Client TRƯỚC KHI click
            System.out.println("Đang lấy thông tin cửa sổ Riot Client...");
            Rectangle windowBounds = getRiotClientWindowBounds();
            if (windowBounds == null) {
                System.out.println("LỖI: Không thể lấy thông tin cửa sổ Riot Client!");
                System.out.println("Vui lòng đảm bảo cửa sổ Riot Client đang hiển thị và thử lại.");
                return false;
            }
            
            // Nhấn Escape để đóng dialog (nếu có)
            System.out.println("Đang click vào trường username...");
            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);
            robot.delay(150);
            
            // Tính toán vị trí click dựa trên cửa sổ Riot Client thực tế
            // Layout của Riot Client (theo mô tả):
            // - Cửa sổ Riot Client có form login ở bên TRÁI (panel trắng), chiếm ~1/3 bề rộng cửa sổ
            // - Panel trắng có: Logo VNGGAMES, Title "Đăng nhập", Username field, Password field
            // - Username field nằm ở giữa panel trắng theo chiều ngang, khoảng 33% từ trên cửa sổ
            
            // Lấy kích thước và vị trí cửa sổ
            int windowX = windowBounds.x;
            int windowY = windowBounds.y;
            int windowWidth = windowBounds.width;
            int windowHeight = windowBounds.height;
            
            // Tính toán vị trí click tương đối trong CLIENT AREA của cửa sổ Riot Client
            // (Client area đã loại bỏ border và title bar)
            // 
            // Layout thực tế:
            // - Form login (panel trắng) nằm ở bên TRÁI, chiếm khoảng 40-45% bề rộng client area
            // - Username field nằm ở giữa panel trắng theo chiều ngang
            // - Username field nằm ở khoảng 30-35% từ trên client area
            
            // Điều chỉnh tỷ lệ dựa trên feedback: cần sang trái và lên nhiều
            // Panel trắng chiếm khoảng 35-40% bề rộng client area
            double loginPanelWidthRatio = 0.38; // Panel trắng chiếm 38% bề rộng (giảm từ 40%)
            // Điều chỉnh: click vào giữa/bên trong ô username, không phải góc phải
            // Giảm từ 48% xuống 35% để click vào phần giữa-trái của ô input
            double usernameXInPanel = 0.35; // Username field ở 35% từ trái panel (để click vào bên trong ô, không phải góc phải)
            double usernameXRatio = loginPanelWidthRatio * usernameXInPanel; // ~13.3% từ trái client area
            
            // Username field nằm ở khoảng 24-26% từ trên client area
            // (Đã loại bỏ title bar rồi nên tính từ trên client area)
            // Điều chỉnh: giảm nhiều hơn để click vào ô username thay vì ô password
            double usernameYRatio = 0.25; // 25% từ trên client area (giảm từ 28% để click lên cao hơn)
            
            // Tính toán vị trí tuyệt đối trên màn hình
            // (windowBounds đã là client area, không cần cộng thêm title bar)
            int usernameX = windowX + (int) (windowWidth * usernameXRatio);
            int usernameY = windowY + (int) (windowHeight * usernameYRatio);
            
            // Đảm bảo vị trí click nằm trong client area (với margin nhỏ)
            int marginX = Math.max(10, (int) (windowWidth * 0.02)); // Margin tối thiểu 10px hoặc 2%
            int marginY = Math.max(10, (int) (windowHeight * 0.02));
            
            // Clamp vào client area
            usernameX = Math.max(windowX + marginX, Math.min(usernameX, windowX + windowWidth - marginX));
            usernameY = Math.max(windowY + marginY, Math.min(usernameY, windowY + windowHeight - marginY));
            
            // Log thông tin chi tiết để debug
            System.out.println("=== Tính toán vị trí click (Client Area) ===");
            System.out.println("Client Area của Riot Client:");
            System.out.println("  - Vị trí: (" + windowX + ", " + windowY + ")");
            System.out.println("  - Kích thước: " + windowWidth + " x " + windowHeight);
            System.out.println("Tính toán vị trí click:");
            System.out.println("  - Panel trắng chiếm: " + String.format("%.1f%%", loginPanelWidthRatio * 100) + " bề rộng");
            System.out.println("  - Username field trong panel: " + String.format("%.1f%%", usernameXInPanel * 100) + " từ trái");
            System.out.println("  - Tỷ lệ X (từ trái client area): " + String.format("%.1f%%", usernameXRatio * 100));
            System.out.println("  - Tỷ lệ Y (từ trên client area): " + String.format("%.1f%%", usernameYRatio * 100));
            System.out.println("  - Vị trí click tuyệt đối: (" + usernameX + ", " + usernameY + ")");
            System.out.println("  - Offset từ góc trái-trên client area: (" + (usernameX - windowX) + ", " + (usernameY - windowY) + ")");
            System.out.println("  - Tọa độ chuột hiện tại sẽ được di chuyển đến: (" + usernameX + ", " + usernameY + ")");
            
            // Di chuyển chuột đến vị trí click với từng bước để đảm bảo chính xác
            System.out.println("Đang di chuyển chuột đến vị trí click...");
            
            // Lấy vị trí chuột hiện tại
            Point currentMousePos = MouseInfo.getPointerInfo().getLocation();
            System.out.println("  - Vị trí chuột hiện tại: (" + currentMousePos.x + ", " + currentMousePos.y + ")");
            
            // Di chuyển chuột trực tiếp đến vị trí đích
            robot.mouseMove(usernameX, usernameY);
            robot.delay(300); // Đợi chuột di chuyển
            
            // Verify vị trí chuột
            Point newMousePos = MouseInfo.getPointerInfo().getLocation();
            System.out.println("  - Vị trí chuột sau khi di chuyển: (" + newMousePos.x + ", " + newMousePos.y + ")");
            System.out.println("  - Chênh lệch: (" + (newMousePos.x - usernameX) + ", " + (newMousePos.y - usernameY) + ")");
            
            // Click vào ô username
            System.out.println("Đang click vào ô username tại (" + usernameX + ", " + usernameY + ")...");
            robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(80);
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(600); // Đợi để đảm bảo focus vào ô username
            
            // Bước 4: Clean hết nội dung trong trường username
            System.out.println("Đang clean trường username...");
            // Ctrl+A để select all
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.delay(50);
            // Xóa
            robot.keyPress(KeyEvent.VK_DELETE);
            robot.keyRelease(KeyEvent.VK_DELETE);
            robot.delay(100);
            
            // Làm thêm một lần để chắc chắn
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.delay(50);
            robot.keyPress(KeyEvent.VK_BACK_SPACE);
            robot.keyRelease(KeyEvent.VK_BACK_SPACE);
            robot.delay(100);
            
            // Bước 5: Nhập username
            System.out.println("Đang nhập username: " + account.getUsername());
            String username = account.getUsername();
            for (char c : username.toCharArray()) {
                typeChar(robot, c);
                robot.delay(25);
            }
            robot.delay(150);
            
            // Bước 6: Tab để chuyển sang trường password
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.delay(200);
            
            // Bước 7: Clean hết nội dung trong trường password
            System.out.println("Đang clean trường password...");
            // Ctrl+A để select all
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.delay(50);
            // Xóa
            robot.keyPress(KeyEvent.VK_DELETE);
            robot.keyRelease(KeyEvent.VK_DELETE);
            robot.delay(100);
            
            // Làm thêm một lần để chắc chắn
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.delay(50);
            robot.keyPress(KeyEvent.VK_BACK_SPACE);
            robot.keyRelease(KeyEvent.VK_BACK_SPACE);
            robot.delay(100);
            
            // Bước 8: Nhập password
            System.out.println("Đang nhập password...");
            String password = account.getPassword();
            if (password != null && !password.isEmpty()) {
                for (char c : password.toCharArray()) {
                    typeChar(robot, c);
                    robot.delay(25);
                }
            }
            robot.delay(150);
            
            System.out.println("Hoàn thành điền thông tin đăng nhập!");
            
            return true;
        } catch (Exception e) {
            System.out.println("Lỗi: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
