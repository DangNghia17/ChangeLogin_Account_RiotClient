package com.riotaccountmanager.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the "Run at Windows Startup" feature by writing the per-user
 * {@code HKCU\Software\Microsoft\Windows\CurrentVersion\Run} registry value.
 *
 * <p>Per-user (HKCU) is used intentionally so the feature does not require admin rights
 * and works reliably on both Windows 10 and 11.
 */
public final class StartupManager {

    private static final Logger LOG = Logger.getLogger(StartupManager.class.getName());

    private static final String RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "RiotAccountManager";

    private StartupManager() {
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Best-effort resolution of the command Windows should run at login.
     *
     * <p>Order of preference:
     * <ol>
     *   <li>The jpackage launcher exe ({@code jpackage.app-path} system property).</li>
     *   <li>A sibling {@code .exe} next to the running JAR (Launch4j portable layout).</li>
     *   <li>{@code javaw -jar <app.jar>} as a last resort.</li>
     * </ol>
     */
    public static Optional<String> resolveLaunchCommand() {
        // 1) jpackage sets this to the absolute path of the native launcher.
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && new File(appPath).exists()) {
            return Optional.of(quote(appPath));
        }

        // 2) Locate the running JAR, then look for a sibling .exe.
        try {
            File codeSource = new File(StartupManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (codeSource.isFile() && codeSource.getName().toLowerCase().endsWith(".jar")) {
                File dir = codeSource.getParentFile();
                if (dir != null) {
                    File exe = new File(dir, "RiotAccountManager.exe");
                    if (exe.exists()) {
                        return Optional.of(quote(exe.getAbsolutePath()));
                    }
                }
                // 3) Fallback: run the jar with javaw.
                String javaHome = System.getProperty("java.home");
                File javaw = new File(javaHome, "bin/javaw.exe");
                String javaCmd = javaw.exists() ? quote(javaw.getAbsolutePath()) : "javaw";
                return Optional.of(javaCmd + " -jar " + quote(codeSource.getAbsolutePath()));
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not resolve launch command for startup", e);
        }
        return Optional.empty();
    }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }

    /** Registers the app to run at login. Returns {@code true} on success. */
    public static boolean enable() {
        if (!isWindows()) {
            return false;
        }
        Optional<String> command = resolveLaunchCommand();
        if (!command.isPresent()) {
            LOG.warning("Could not determine launch command; startup not registered");
            return false;
        }
        List<String> args = new ArrayList<>();
        args.add("reg");
        args.add("add");
        args.add(RUN_KEY);
        args.add("/v");
        args.add(VALUE_NAME);
        args.add("/t");
        args.add("REG_SZ");
        args.add("/d");
        args.add(command.get());
        args.add("/f");
        return run(args) == 0;
    }

    /** Removes the startup registration. Returns {@code true} on success (or if absent). */
    public static boolean disable() {
        if (!isWindows()) {
            return false;
        }
        if (!isEnabled()) {
            return true;
        }
        List<String> args = new ArrayList<>();
        args.add("reg");
        args.add("delete");
        args.add(RUN_KEY);
        args.add("/v");
        args.add(VALUE_NAME);
        args.add("/f");
        return run(args) == 0;
    }

    /** Whether the startup registry value currently exists. */
    public static boolean isEnabled() {
        if (!isWindows()) {
            return false;
        }
        List<String> args = new ArrayList<>();
        args.add("reg");
        args.add("query");
        args.add(RUN_KEY);
        args.add("/v");
        args.add(VALUE_NAME);
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean found = false;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(VALUE_NAME)) {
                        found = true;
                    }
                }
            }
            process.waitFor();
            return found;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not query startup registry", e);
            return false;
        }
    }

    private static int run(List<String> args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {
                    // Drain output so the process doesn't block.
                }
            }
            return process.waitFor();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Registry command failed: " + args, e);
            return -1;
        }
    }
}
