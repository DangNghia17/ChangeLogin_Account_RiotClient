package com.riotaccountmanager.i18n;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Multi-language support (VI/EN) backed by UTF-8 {@code .properties} resource bundles.
 * The selected language is persisted via the Java Preferences API.
 */
public final class LanguageManager {

    private static final String PREF_KEY = "language";
    private static final String DEFAULT_LANGUAGE = "vi";

    private static ResourceBundle bundle;
    private static String currentLanguage = DEFAULT_LANGUAGE;

    static {
        loadLanguage();
    }

    private LanguageManager() {
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
        bundle = loadBundleForLanguage(currentLanguage);
        if (bundle == null && !currentLanguage.equals(DEFAULT_LANGUAGE)) {
            bundle = loadBundleForLanguage(DEFAULT_LANGUAGE);
        }
    }

    private static ResourceBundle loadBundleForLanguage(String lang) {
        String resourceName = "messages_" + lang + ".properties";
        try (InputStream stream = LanguageManager.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream != null) {
                return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            System.err.println("Error loading resource bundle for " + lang + ": " + e.getMessage());
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
        return args.length > 0 ? String.format(template, args) : template;
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
        } catch (Exception ignored) {
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
