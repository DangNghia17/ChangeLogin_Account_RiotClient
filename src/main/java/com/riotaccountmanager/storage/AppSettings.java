package com.riotaccountmanager.storage;

import com.riotaccountmanager.system.StartupManager;

/**
 * High-level, typed accessor for user-facing settings, backed by {@link AppConfig}.
 *
 * <p>Keeps the rest of the app decoupled from how settings are persisted and ensures
 * side effects (such as registering/unregistering Windows startup) stay consistent with
 * the stored value.
 */
public final class AppSettings {

    private AppSettings() {
    }

    /** Whether the app registers itself to run automatically when Windows starts. */
    public static boolean isRunAtStartup() {
        return AppConfig.getBoolean(AppConfig.KEY_RUN_AT_STARTUP, false);
    }

    /**
     * Enables/disables "run at Windows startup". Persists the preference and applies the
     * corresponding registry change. Returns {@code true} if the OS-level change succeeded.
     */
    public static boolean setRunAtStartup(boolean enabled) {
        AppConfig.setBoolean(AppConfig.KEY_RUN_AT_STARTUP, enabled);
        return enabled ? StartupManager.enable() : StartupManager.disable();
    }

    /** Whether to automatically submit the login form after filling credentials. */
    public static boolean isAutoClickLogin() {
        return AppConfig.getBoolean(AppConfig.KEY_AUTO_CLICK_LOGIN, true);
    }

    public static void setAutoClickLogin(boolean enabled) {
        AppConfig.setBoolean(AppConfig.KEY_AUTO_CLICK_LOGIN, enabled);
    }

    /**
     * Reconciles the OS startup registration with the stored preference. Safe to call on
     * each launch so the registry entry stays correct (e.g. after the app is moved).
     */
    public static void syncStartupRegistration() {
        try {
            boolean desired = isRunAtStartup();
            boolean actual = StartupManager.isEnabled();
            if (desired && !actual) {
                StartupManager.enable();
            } else if (!desired && actual) {
                StartupManager.disable();
            } else if (desired) {
                // Keep the path fresh in case the executable location changed.
                StartupManager.enable();
            }
        } catch (Exception ignored) {
            // Non-fatal: startup is a convenience feature.
        }
    }
}
