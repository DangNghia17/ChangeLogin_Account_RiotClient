package com.riotaccountmanager;

import com.riotaccountmanager.storage.AppSettings;
import com.riotaccountmanager.ui.MainForm;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point.
 *
 * <p>Sets the native look and feel, reconciles the Windows startup registration with the
 * stored preference, then shows the main window on the Swing event dispatch thread.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        // Keep the "run at startup" registry entry consistent with the saved setting.
        AppSettings.syncStartupRegistration();

        SwingUtilities.invokeLater(() -> {
            MainForm form = new MainForm();
            form.placeTopRight();
            form.setVisible(true);
        });
    }
}
