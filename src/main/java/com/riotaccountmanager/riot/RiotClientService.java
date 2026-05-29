package com.riotaccountmanager.riot;

import com.riotaccountmanager.model.Account;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All interaction with the external Riot Client: process/window detection, focusing,
 * measuring the window and automated credential entry.
 *
 * <p>Implementation notes / robustness improvements over the original:
 * <ul>
 *   <li><b>DPI-aware coordinates.</b> Win32 reports physical pixels while Java's
 *       {@link Robot} works in logical (scaled) coordinates on HiDPI displays. We convert
 *       physical -> logical using the screen's default transform so clicks land correctly
 *       regardless of Windows display scaling.</li>
 *   <li><b>Client-area based targeting.</b> We position relative to the client area (no
 *       title bar / border), so it is independent of where the window sits on screen.</li>
 *   <li><b>Configurable auto-submit.</b> Optionally presses Enter to submit the login
 *       form, removing the need for a manual click.</li>
 * </ul>
 *
 * <p>Only standard Windows APIs and input simulation are used (no hooking / injection),
 * keeping the tool compatible with Riot Vanguard.
 */
public final class RiotClientService {

    private static final Logger LOG = Logger.getLogger(RiotClientService.class.getName());

    private RiotClientService() {
    }

    // ---------------------------------------------------------------------
    // Process / window detection
    // ---------------------------------------------------------------------

    public static boolean isRiotClientRunning() {
        try {
            Process process = Runtime.getRuntime().exec("tasklist");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String lower = line.toLowerCase();
                    if (lower.contains("riotclientservices.exe") || lower.contains("riot client")) {
                        return true;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not check Riot Client process", e);
        }
        return false;
    }

    public static boolean isRiotClientWindowVisible() {
        try {
            String command = "powershell -Command \"Get-Process | Where-Object {$_.MainWindowTitle -like '*Riot Client*' -or $_.MainWindowTitle -like '*Riot*'} | Select-Object -First 1 | ForEach-Object { $_.MainWindowTitle }\"";
            Process process = Runtime.getRuntime().exec(command);
            String line;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                line = reader.readLine();
            }
            process.waitFor();
            if (line != null && !line.trim().isEmpty()) {
                String lower = line.toLowerCase();
                return lower.contains("riot client") || lower.contains("riot");
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Window visibility check failed, falling back to process check", e);
        }
        return false;
    }

    /** Launches the Riot Client executable. */
    public static boolean launchRiotClient(String clientPath) {
        try {
            File file = new File(clientPath);
            if (!file.exists()) {
                return false;
            }
            new ProcessBuilder(clientPath).start();
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not launch Riot Client", e);
            return false;
        }
    }

    private static Process runPowerShellScript(String scriptContent) throws Exception {
        File tempScript = File.createTempFile("riotClient", ".ps1");
        tempScript.deleteOnExit();
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(tempScript), StandardCharsets.UTF_8)) {
            writer.write(scriptContent);
        }
        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-ExecutionPolicy", "Bypass", "-File", tempScript.getAbsolutePath());
        pb.redirectErrorStream(true);
        return pb.start();
    }

    /** Returns the client-area bounds (physical pixels) of the Riot Client window, or {@code null}. */
    public static Rectangle getRiotClientWindowBounds() {
        for (int retry = 0; retry < 3; retry++) {
            try {
                if (retry > 0) {
                    Thread.sleep(500);
                }
                Process process = runPowerShellScript(WINDOW_BOUNDS_SCRIPT);
                String result = null;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.matches("^-?\\d+,-?\\d+,-?\\d+,-?\\d+$")) {
                            result = line;
                            break;
                        }
                    }
                }
                process.waitFor();

                if (result != null) {
                    String[] parts = result.split(",");
                    if (parts.length == 4) {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int width = Integer.parseInt(parts[2].trim());
                        int height = Integer.parseInt(parts[3].trim());
                        if (width > 100 && height > 100) {
                            return new Rectangle(x, y, width, height);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "getRiotClientWindowBounds attempt " + (retry + 1) + " failed", e);
            }
        }
        return null;
    }

    /** Brings the Riot Client window to the foreground (restoring from tray if needed). */
    public static boolean focusRiotClientWindow() {
        try {
            Process process = runPowerShellScript(FOCUS_SCRIPT);
            boolean ok = false;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().equals("OK")) {
                        ok = true;
                    }
                }
            }
            process.waitFor();
            return ok;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not focus Riot Client window", e);
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // Automated login
    // ---------------------------------------------------------------------

    /**
     * Fills the Riot Client login form for the given account.
     *
     * @param account     credentials to enter
     * @param autoSubmit  if {@code true}, presses Enter to submit (auto-click Login)
     * @return {@code true} if the fill sequence completed
     */
    public static boolean autoLogin(Account account, boolean autoSubmit) {
        try {
            if (!isRiotClientRunning() || !isRiotClientWindowVisible()) {
                LOG.warning("Riot Client is not running or its window is not visible");
                return false;
            }

            Thread.sleep(400);
            focusRiotClientWindow();
            Thread.sleep(700);

            Rectangle bounds = getRiotClientWindowBounds();
            if (bounds == null) {
                LOG.warning("Could not obtain Riot Client window bounds");
                return false;
            }

            Robot robot = new Robot();
            robot.setAutoDelay(10);

            Point usernamePoint = computeUsernamePoint(bounds);

            // Dismiss any transient dialog, then focus the username field.
            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);
            robot.delay(150);

            robot.mouseMove(usernamePoint.x, usernamePoint.y);
            robot.delay(250);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(80);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(500);

            clearField(robot);
            typeString(robot, account.getUsername());
            robot.delay(150);

            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.delay(200);

            clearField(robot);
            typeString(robot, account.getPassword());
            robot.delay(150);

            if (autoSubmit) {
                robot.delay(150);
                robot.keyPress(KeyEvent.VK_ENTER);
                robot.keyRelease(KeyEvent.VK_ENTER);
            }
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Auto login failed", e);
            return false;
        }
    }

    /**
     * Computes the username-field click point in Java logical coordinates.
     *
     * <p>The Riot Client login form sits in a panel on the left; the username field is at
     * roughly 13% from the left and 25% from the top of the client area. The physical
     * pixel target is converted to logical coordinates to be correct under any Windows
     * display scaling (DPI).
     */
    private static Point computeUsernamePoint(Rectangle clientBounds) {
        double usernameXRatio = 0.38 * 0.35; // ~13.3% from the left of the client area
        double usernameYRatio = 0.25;        // 25% from the top of the client area

        int physX = clientBounds.x + (int) (clientBounds.width * usernameXRatio);
        int physY = clientBounds.y + (int) (clientBounds.height * usernameYRatio);

        int marginX = Math.max(10, (int) (clientBounds.width * 0.02));
        int marginY = Math.max(10, (int) (clientBounds.height * 0.02));
        physX = clamp(physX, clientBounds.x + marginX, clientBounds.x + clientBounds.width - marginX);
        physY = clamp(physY, clientBounds.y + marginY, clientBounds.y + clientBounds.height - marginY);

        return toLogical(physX, physY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /** Converts physical screen pixels (Win32) to Java logical coordinates (Robot space). */
    private static Point toLogical(int physX, int physY) {
        try {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            AffineTransform tx = gc.getDefaultTransform();
            double scaleX = tx.getScaleX();
            double scaleY = tx.getScaleY();
            if (scaleX > 0 && scaleY > 0) {
                return new Point((int) Math.round(physX / scaleX), (int) Math.round(physY / scaleY));
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "DPI scale detection failed; using raw coordinates", e);
        }
        return new Point(physX, physY);
    }

    private static void clearField(Robot robot) {
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.delay(50);
        robot.keyPress(KeyEvent.VK_DELETE);
        robot.keyRelease(KeyEvent.VK_DELETE);
        robot.delay(80);
    }

    private static void typeString(Robot robot, String text) {
        if (text == null) {
            return;
        }
        for (char c : text.toCharArray()) {
            typeChar(robot, c);
            robot.delay(25);
        }
    }

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
                    shiftType(robot, KeyEvent.VK_2);
                    break;
                case '.':
                    robot.keyPress(KeyEvent.VK_PERIOD);
                    robot.keyRelease(KeyEvent.VK_PERIOD);
                    break;
                case '_':
                    shiftType(robot, KeyEvent.VK_MINUS);
                    break;
                case '-':
                    robot.keyPress(KeyEvent.VK_MINUS);
                    robot.keyRelease(KeyEvent.VK_MINUS);
                    break;
                default:
                    robot.keyPress(Character.toUpperCase(c));
                    robot.keyRelease(Character.toUpperCase(c));
                    break;
            }
        }
    }

    private static void shiftType(Robot robot, int keyCode) {
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        robot.keyRelease(KeyEvent.VK_SHIFT);
    }

    // ---------------------------------------------------------------------
    // Embedded PowerShell scripts (Win32 interop). Kept verbatim from the proven
    // implementation; only relocated here behind a clean Java API.
    // ---------------------------------------------------------------------

    private static final String WINDOW_BOUNDS_SCRIPT =
            "$ErrorActionPreference = 'Stop'\n" +
            "$process = $null\n" +
            "$allProcesses = Get-Process | Where-Object {$_.MainWindowHandle -ne 0}\n" +
            "$process = $allProcesses | Where-Object {\n" +
            "    $_.MainWindowTitle -like '*Riot Client*' -and\n" +
            "    ($_.ProcessName -like '*RiotClient*' -or $_.ProcessName -like '*Riot*')\n" +
            "} | Select-Object -First 1\n" +
            "if ($process -eq $null) {\n" +
            "    $process = $allProcesses | Where-Object {\n" +
            "        $_.ProcessName -eq 'RiotClientServices' -or\n" +
            "        $_.ProcessName -eq 'RiotClientUx' -or\n" +
            "        $_.ProcessName -like 'RiotClient*'\n" +
            "    } | Where-Object {$_.MainWindowTitle -ne ''} | Select-Object -First 1\n" +
            "}\n" +
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
            "            public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }\n" +
            "            [StructLayout(LayoutKind.Sequential)]\n" +
            "            public struct POINT { public int X; public int Y; }\n" +
            "        }\n" +
            "'@\n" +
            "        $hWnd = $process.MainWindowHandle\n" +
            "        $clientRect = New-Object Win32+RECT\n" +
            "        $ok2 = [Win32]::GetClientRect($hWnd, [ref]$clientRect)\n" +
            "        $clientPoint = New-Object Win32+POINT\n" +
            "        $clientPoint.X = 0\n" +
            "        $clientPoint.Y = 0\n" +
            "        $ok3 = [Win32]::ClientToScreen($hWnd, [ref]$clientPoint)\n" +
            "        if ($ok2 -and $ok3) {\n" +
            "            $w = $clientRect.Right - $clientRect.Left\n" +
            "            $h = $clientRect.Bottom - $clientRect.Top\n" +
            "            if ($w -gt 0 -and $h -gt 0) {\n" +
            "                Write-Output ($clientPoint.X.ToString() + ',' + $clientPoint.Y.ToString() + ',' + $w.ToString() + ',' + $h.ToString())\n" +
            "            }\n" +
            "        }\n" +
            "    } catch { }\n" +
            "}\n";

    private static final String FOCUS_SCRIPT =
            "$ErrorActionPreference = 'Stop'\n" +
            "$process = $null\n" +
            "$allProcesses = Get-Process | Where-Object {$_.MainWindowHandle -ne 0}\n" +
            "$process = $allProcesses | Where-Object {\n" +
            "    $_.MainWindowTitle -like '*Riot Client*' -and\n" +
            "    ($_.ProcessName -like '*RiotClient*' -or $_.ProcessName -like '*Riot*')\n" +
            "} | Select-Object -First 1\n" +
            "if ($process -eq $null) {\n" +
            "    $process = $allProcesses | Where-Object {\n" +
            "        ($_.ProcessName -eq 'RiotClientServices' -or $_.ProcessName -eq 'RiotClientUx') -and\n" +
            "        $_.MainWindowTitle -ne ''\n" +
            "    } | Select-Object -First 1\n" +
            "}\n" +
            "if ($process -eq $null) {\n" +
            "    $process = Get-Process | Where-Object {\n" +
            "        $_.ProcessName -eq 'RiotClientServices' -or $_.ProcessName -eq 'RiotClientUx'\n" +
            "    } | Select-Object -First 1\n" +
            "}\n" +
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
            "            public const int SW_RESTORE = 9;\n" +
            "            public const int SW_SHOW = 5;\n" +
            "        }\n" +
            "'@\n" +
            "        if ($process.MainWindowHandle -ne 0) {\n" +
            "            $hWnd = $process.MainWindowHandle\n" +
            "            if ([Win32]::IsIconic($hWnd)) {\n" +
            "                [Win32]::ShowWindow($hWnd, [Win32]::SW_RESTORE)\n" +
            "                Start-Sleep -Milliseconds 300\n" +
            "            } else {\n" +
            "                [Win32]::ShowWindow($hWnd, [Win32]::SW_SHOW)\n" +
            "                Start-Sleep -Milliseconds 100\n" +
            "            }\n" +
            "            [Win32]::BringWindowToTop($hWnd)\n" +
            "            Start-Sleep -Milliseconds 100\n" +
            "            if ([Win32]::SetForegroundWindow($hWnd)) {\n" +
            "                Write-Output 'OK'\n" +
            "                exit 0\n" +
            "            } else {\n" +
            "                [Win32]::BringWindowToTop($hWnd)\n" +
            "                Start-Sleep -Milliseconds 200\n" +
            "                if ([Win32]::SetForegroundWindow($hWnd)) { Write-Output 'OK'; exit 0 }\n" +
            "            }\n" +
            "        }\n" +
            "    } catch { }\n" +
            "}\n";
}
