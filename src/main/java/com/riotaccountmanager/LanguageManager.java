package com.riotaccountmanager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class LanguageManager {
    private static final String PREF_KEY = "language";
    private static final String DEFAULT_LANGUAGE = "vi";
    private static ResourceBundle bundle;
    private static String currentLanguage = DEFAULT_LANGUAGE;
    
    static {
        loadLanguage();
    }
    
    private static void loadLanguage() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(LanguageManager.class);
            currentLanguage = prefs.get(PREF_KEY, DEFAULT_LANGUAGE);
        } catch (Exception e) {
            currentLanguage = DEFAULT_LANGUAGE;
        }
        loadBundle();
    }
    
    private static void loadBundle() {
        try {
            bundle = loadBundleForLanguage(currentLanguage);
            if (bundle == null && !currentLanguage.equals(DEFAULT_LANGUAGE)) {
                bundle = loadBundleForLanguage(DEFAULT_LANGUAGE);
            }
            if (bundle == null) {
                System.err.println("Không thể load ResourceBundle cho ngôn ngữ: " + currentLanguage);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load ResourceBundle: " + e.getMessage());
            e.printStackTrace();
            bundle = null;
        }
    }
    
    private static ResourceBundle loadBundleForLanguage(String lang) {
        try {
            String resourceName = "messages_" + lang + ".properties";
            InputStream stream = LanguageManager.class.getClassLoader().getResourceAsStream(resourceName);
            if (stream != null) {
                try {
                    return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            } else {
                System.err.println("Không tìm thấy file: " + resourceName);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load bundle cho ngôn ngữ " + lang + ": " + e.getMessage());
        }
        return null;
    }
    
    public static String getString(String key) {
        if (bundle != null) {
            try {
                return bundle.getString(key);
            } catch (Exception e) {
                return key;
            }
        }
        return key;
    }
    
    public static String getString(String key, Object... args) {
        String template = getString(key);
        if (args.length > 0) {
            return String.format(template, args);
        }
        return template;
    }
    
    public static void setLanguage(String lang) {
        if (lang == null || lang.isEmpty()) {
            lang = DEFAULT_LANGUAGE;
        }
        currentLanguage = lang;
        try {
            Preferences prefs = Preferences.userNodeForPackage(LanguageManager.class);
            prefs.put(PREF_KEY, lang);
            prefs.flush();
        } catch (Exception e) {
        }
        loadBundle();
    }
    
    public static String getCurrentLanguage() {
        return currentLanguage;
    }
    
    public static boolean isVietnamese() {
        return "vi".equals(currentLanguage);
    }
}
