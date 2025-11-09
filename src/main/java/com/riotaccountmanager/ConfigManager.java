package com.riotaccountmanager;

import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigManager {
    private static final String CONFIG_DIR = System.getenv("LOCALAPPDATA") + "\\RiotAccountManager";
    private static final String CONFIG_FILE = CONFIG_DIR + "\\config.json";
    
    private static void ensureConfigDir() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
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
        ensureConfigDir();
        
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            String defaultPath = findDefaultRiotClientPath();
            if (defaultPath != null) {
                setRiotClientPath(defaultPath);
                return defaultPath;
            }
            return null;
        }
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)), "UTF-8");
            JSONObject json = new JSONObject(content);
            String path = json.optString("riotClientPath", null);
            
            if (path != null && new File(path).exists()) {
                return path;
            }
            
            String defaultPath = findDefaultRiotClientPath();
            if (defaultPath != null) {
                setRiotClientPath(defaultPath);
                return defaultPath;
            }
            
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void setRiotClientPath(String path) {
        ensureConfigDir();
        
        try {
            JSONObject json = new JSONObject();
            json.put("riotClientPath", path);
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                writer.write(json.toString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static boolean validateRiotClientPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        File file = new File(path);
        return file.exists() && file.isFile() && 
               path.toLowerCase().endsWith("riotclientservices.exe");
    }
}
