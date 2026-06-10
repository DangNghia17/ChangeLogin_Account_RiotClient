package com.riotaccountmanager.storage;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads/writes {@code config.json} (Riot Client path + user settings).
 *
 * <p>Backward compatible: the legacy file only contained {@code riotClientPath}. New
 * boolean settings are read with defaults via {@code optBoolean}, so an old config still
 * loads, and new keys are simply added on the next save.
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());

    public static final String KEY_RIOT_PATH = "riotClientPath";
    public static final String KEY_RUN_AT_STARTUP = "runAtStartup";
    public static final String KEY_AUTO_CLICK_LOGIN = "autoClickLogin";
    public static final String KEY_LANGUAGE = "language";

    private AppConfig() {
    }

    private static JSONObject read() {
        File configFile = AppPaths.configFile();
        if (!configFile.exists()) {
            return new JSONObject();
        }
        try {
            String content = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                return new JSONObject();
            }
            return new JSONObject(content);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not read config.json", e);
            return new JSONObject();
        }
    }

    private static void write(JSONObject json) {
        AppPaths.ensureDataDir();
        try {
            Files.write(AppPaths.configFile().toPath(),
                    json.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not write config.json", e);
        }
    }

    // --- Riot Client path -------------------------------------------------

    public static String findDefaultRiotClientPath() {
        String[] defaultPaths = {
                "C:\\Riot Games\\Riot Client\\RiotClientServices.exe",
                "C:\\Program Files\\Riot Games\\Riot Client\\RiotClientServices.exe",
                "C:\\Program Files (x86)\\Riot Games\\Riot Client\\RiotClientServices.exe"
        };
        for (String path : defaultPaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return path;
            }
        }
        return null;
    }

    public static String getRiotClientPath() {
        JSONObject json = read();
        String path = json.optString(KEY_RIOT_PATH, null);
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            return path;
        }
        String defaultPath = findDefaultRiotClientPath();
        if (defaultPath != null) {
            setRiotClientPath(defaultPath);
            return defaultPath;
        }
        return null;
    }

    public static void setRiotClientPath(String path) {
        JSONObject json = read();
        json.put(KEY_RIOT_PATH, path);
        write(json);
    }

    public static boolean validateRiotClientPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isFile()
                && path.toLowerCase().endsWith("riotclientservices.exe");
    }

    // --- Generic boolean settings ----------------------------------------

    public static boolean getBoolean(String key, boolean defaultValue) {
        return read().optBoolean(key, defaultValue);
    }

    public static void setBoolean(String key, boolean value) {
        JSONObject json = read();
        json.put(key, value);
        write(json);
    }
}
