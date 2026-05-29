package com.riotaccountmanager.ui;

import com.riotaccountmanager.i18n.LanguageManager;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.prefs.Preferences;

/** First-run guide dialog. */
public class WelcomeDialog extends JDialog {

    private static final String PREF_KEY = "welcome_shown";

    public WelcomeDialog(JFrame parent) {
        super(parent, LanguageManager.getString("app.welcome.dialog.title"), true);
        setResizable(false);

        UIHelper.setWindowIcon(this, "/change-user-icon.jpg");

        initializeUI();

        pack();
        setSize(520, Math.max(getHeight(), 380));
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.SURFACE_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel titleLabel = new JLabel(LanguageManager.getString("app.welcome.title"), JLabel.CENTER);
        titleLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.BOLD, 18));
        titleLabel.setForeground(UITheme.PRIMARY_COLOR);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(UITheme.SURFACE_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] instructions = {
                LanguageManager.getString("app.welcome.instruction1"),
                LanguageManager.getString("app.welcome.instruction2"),
                LanguageManager.getString("app.welcome.instruction3"),
                LanguageManager.getString("app.welcome.instruction4"),
                LanguageManager.getString("app.welcome.instruction5")
        };

        for (String instruction : instructions) {
            JLabel label = new JLabel(instruction);
            label.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 12));
            label.setForeground(UITheme.TEXT_PRIMARY);
            label.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(label);
        }

        panel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout(10, 0));
        buttonPanel.setBackground(UITheme.SURFACE_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        buttonPanel.setPreferredSize(new Dimension(0, 50));

        JCheckBox dontShowAgain = new JCheckBox(LanguageManager.getString("app.welcome.dontShowAgain"));
        dontShowAgain.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 11));
        dontShowAgain.setBackground(UITheme.SURFACE_COLOR);

        JButton okButton = UIHelper.createMaterialButton(LanguageManager.getString("app.welcome.close"), UITheme.PRIMARY_COLOR);
        okButton.setPreferredSize(new Dimension(90, 32));
        okButton.addActionListener(e -> {
            if (dontShowAgain.isSelected()) {
                savePreference();
            }
            dispose();
        });

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setBackground(UITheme.SURFACE_COLOR);
        leftPanel.add(dontShowAgain);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setBackground(UITheme.SURFACE_COLOR);
        rightPanel.add(okButton);

        buttonPanel.add(leftPanel, BorderLayout.WEST);
        buttonPanel.add(rightPanel, BorderLayout.EAST);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void savePreference() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(getClass());
            prefs.putBoolean(PREF_KEY, true);
            prefs.flush();
        } catch (Exception ignored) {
        }
    }

    public static boolean shouldShowWelcome() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(WelcomeDialog.class);
            return !prefs.getBoolean(PREF_KEY, false);
        } catch (Exception e) {
            return true;
        }
    }

    public static void resetWelcomePreference() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(WelcomeDialog.class);
            prefs.remove(PREF_KEY);
            prefs.flush();
        } catch (Exception ignored) {
        }
    }
}
