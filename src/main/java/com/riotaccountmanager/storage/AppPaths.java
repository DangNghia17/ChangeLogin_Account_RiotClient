package com.riotaccountmanager.storage;

import java.io.File;

/**
 * Single source of truth for where the application stores its data.
 *
 * <p><b>Data-safety contract:</b> The data directory and file names below are kept
 * IDENTICAL to all previous versions so that an upgrade automatically picks up existing
 * user data with zero manual migration:
 * <ul>
 *   <li>Directory: {@code %LOCALAPPDATA%\RiotAccountManager}</li>
 *   <li>Accounts:  {@code accounts.dat} (AES encrypted, see {@link CryptoService})</li>
 *   <li>Config:    {@code config.json} (plaintext)</li>
 * </ul>
 *
 * <p>On non-Windows machines (e.g. CI / developer boxes) a sensible fallback under the
 * user home directory is used so the code can at least run and be tested.
 */
public final class AppPaths {

    private static final String DIR_NAME = "RiotAccountManager";

    private AppPaths() {
    }

    /** Root data directory (created on demand by callers). */
    public static File dataDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        File base;
        if (localAppData != null && !localAppData.trim().isEmpty()) {
            base = new File(localAppData);
        } else {
            // Fallback for non-Windows environments so the app still runs.
            base = new File(System.getProperty("user.home", "."));
        }
        return new File(base, DIR_NAME);
    }

    public static File accountsFile() {
        return new File(dataDir(), "accounts.dat");
    }

    /** Rolling backup written before every successful save / before any migration. */
    public static File accountsBackupFile() {
        return new File(dataDir(), "accounts.dat.bak");
    }

    /** Temp file used for atomic writes. */
    public static File accountsTempFile() {
        return new File(dataDir(), "accounts.dat.tmp");
    }

    public static File configFile() {
        return new File(dataDir(), "config.json");
    }

    /** Stores the data schema version separately so {@code accounts.dat} stays format-compatible. */
    public static File dataVersionFile() {
        return new File(dataDir(), "data.version");
    }

    /** Ensures the data directory exists. Returns the directory. */
    public static File ensureDataDir() {
        File dir = dataDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
