package com.riotaccountmanager;

import javax.swing.*;
import java.awt.*;

public class AccountDialog extends JDialog {
    private static final String[] REGIONS = {
        "VN", "NA", "EUW", "EUNE", "KR", "JP", "BR", "LAN", "LAS", "OCE", "RU", "TR"
    };
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> regionCombo;
    private JTextField noteField;
    private boolean saved = false;
    
    public AccountDialog(JFrame parent, Account account) {
        super(parent, account == null ? LanguageManager.getString("account.add.title") : LanguageManager.getString("account.edit.title"), true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        UIHelper.setWindowIcon(this, "/change-user-icon.jpg");
        
        initializeUI(account);
        
        pack();
        setSize(420, Math.max(getHeight(), 380));
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI(Account account) {
        setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.SURFACE_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(UIHelper.createMaterialLabel(LanguageManager.getString("account.username") + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        usernameField = UIHelper.createMaterialTextField();
        if (account != null) {
            usernameField.setText(account.getUsername());
        }
        panel.add(usernameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(UIHelper.createMaterialLabel(LanguageManager.getString("account.password") + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passwordField = UIHelper.createMaterialPasswordField();
        if (account != null) {
            passwordField.setText(account.getPassword());
        }
        panel.add(passwordField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(UIHelper.createMaterialLabel(LanguageManager.getString("account.region") + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        regionCombo = new JComboBox<>(REGIONS);
        regionCombo.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 12));
        regionCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.DIVIDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        if (account != null) {
            regionCombo.setSelectedItem(account.getRegion());
        } else {
            regionCombo.setSelectedItem("VN");
        }
        panel.add(regionCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(UIHelper.createMaterialLabel(LanguageManager.getString("account.note") + ":"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        noteField = UIHelper.createMaterialTextField();
        if (account != null) {
            noteField.setText(account.getNote());
        }
        panel.add(noteField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.insets = new Insets(20, 12, 10, 12);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonPanel.setBackground(UITheme.SURFACE_COLOR);
        buttonPanel.setOpaque(false);
        JButton saveButton = UIHelper.createMaterialButton(LanguageManager.getString("account.save"), UITheme.SUCCESS_COLOR);
        JButton cancelButton = UIHelper.createMaterialButton(LanguageManager.getString("account.cancel"), new Color(158, 158, 158));
        
        saveButton.addActionListener(e -> {
            saved = true;
            dispose();
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);
        
        add(panel, BorderLayout.CENTER);
        
        getRootPane().setDefaultButton(saveButton);
    }
    
    public boolean isSaved() {
        return saved;
    }
    
    public Account getAccount() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String region = (String) regionCombo.getSelectedItem();
        String note = noteField.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            return null;
        }
        
        return new Account(username, password, region, note);
    }
}
