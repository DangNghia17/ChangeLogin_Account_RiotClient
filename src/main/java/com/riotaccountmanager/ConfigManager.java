package com.riotaccountmanager;

import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Helper class để quản lý cấu hình ứng dụng
 * Lưu trữ đường dẫn Riot Client vào file config.json
 */
public class ConfigManager {
    private static final String CONFIG_DIR = System.getenv("LOCALAPPDATA") + "\\RiotAccountManager";
    private static final String CONFIG_FILE = CONFIG_DIR + "\\config.json";
    
    /**
     * Đảm bảo thư mục cấu hình tồn tại
     */
    private static void ensureConfigDir() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Tìm đường dẫn Riot Client mặc định
     */
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
    
    /**
     * Lấy đường dẫn Riot Client từ cấu hình
     */
    public static String getRiotClientPath() {
        ensureConfigDir();
        
        // Nếu file config không tồn tại, tìm đường dẫn mặc định
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
            
            // Validate đường dẫn
            if (path != null && new File(path).exists()) {
                return path;
            }
            
            // Nếu đường dẫn không hợp lệ, tìm lại đường dẫn mặc định
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
    
    /**
     * Lưu đường dẫn Riot Client vào cấu hình
     */
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
    
    /**
     * Validate đường dẫn Riot Client
     */
    public static boolean validateRiotClientPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        File file = new File(path);
        return file.exists() && file.isFile() && 
               path.toLowerCase().endsWith("riotclientservices.exe");
    }
}

