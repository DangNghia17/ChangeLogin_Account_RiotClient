package com.riotaccountmanager.ui;

import com.riotaccountmanager.i18n.LanguageManager;
import com.riotaccountmanager.storage.AppSettings;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/** Settings dialog: "Run at Windows startup" and "Auto-submit login". */
public class SettingsDialog extends JDialog {

    public SettingsDialog(JFrame parent) {
        super(parent, LanguageManager.getString("settings.title"), true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setResizable(false);

        UIHelper.setWindowIcon(this, "/change-user-icon.jpg");

        initializeUI();

        pack();
        setSize(440, Math.max(getHeight(), 260));
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.SURFACE_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel titleLabel = new JLabel(LanguageManager.getString("settings.title"));
        titleLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.BOLD, 16));
        titleLabel.setForeground(UITheme.PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(16));

        final JCheckBox startupCheck = createCheck(
                LanguageManager.getString("settings.runAtStartup"),
                LanguageManager.getString("settings.runAtStartup.desc"),
                AppSettings.isRunAtStartup(), panel);

        panel.add(Box.createVerticalStrut(14));

        final JCheckBox autoLoginCheck = createCheck(
                LanguageManager.getString("settings.autoClickLogin"),
                LanguageManager.getString("settings.autoClickLogin.desc"),
                AppSettings.isAutoClickLogin(), panel);

        add(panel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonPanel.setBackground(UITheme.SURFACE_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));

        JButton saveButton = UIHelper.createMaterialButton(
                LanguageManager.getString("account.save"), UITheme.PRIMARY_COLOR);
        saveButton.setPreferredSize(new Dimension(100, 34));
        saveButton.addActionListener(e -> {
            AppSettings.setAutoClickLogin(autoLoginCheck.isSelected());
            boolean startupOk = AppSettings.setRunAtStartup(startupCheck.isSelected());
            JFrame owner = (JFrame) getParent();
            dispose();
            if (startupCheck.isSelected() && !startupOk) {
                Toast.show(owner, LanguageManager.getString("settings.startup.failed"), Toast.Type.WARNING);
            } else {
                Toast.show(owner, LanguageManager.getString("settings.saved"), Toast.Type.SUCCESS);
            }
        });

        JButton cancelButton = UIHelper.createMaterialButton(
                LanguageManager.getString("account.cancel"), new Color(158, 158, 158));
        cancelButton.setPreferredSize(new Dimension(100, 34));
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JCheckBox createCheck(String title, String description, boolean selected, JPanel parent) {
        JCheckBox check = new JCheckBox(title, selected);
        check.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.PLAIN, 13));
        check.setBackground(UITheme.SURFACE_COLOR);
        check.setForeground(UITheme.TEXT_PRIMARY);
        check.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(check);

        JLabel desc = new JLabel(description);
        desc.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 11));
        desc.setForeground(UITheme.TEXT_SECONDARY);
        desc.setBorder(BorderFactory.createEmptyBorder(2, 26, 0, 0));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(desc);
        return check;
    }
}
