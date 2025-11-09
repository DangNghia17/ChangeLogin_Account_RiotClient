package com.riotaccountmanager;

import javax.swing.*;
import java.awt.*;

public class AboutDialog extends JDialog {
    
    public AboutDialog(JFrame parent) {
        super(parent, LanguageManager.getString("about.title"), true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setSize(500, 450);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        UIHelper.setWindowIcon(this, "/change-user-icon.jpg");
        
        initializeUI();
        
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = findScrollPane(this);
            if (scrollPane != null) {
                scrollPane.getVerticalScrollBar().setValue(0);
            }
        });
    }
    
    private JScrollPane findScrollPane(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JScrollPane) {
                return (JScrollPane) comp;
            }
            if (comp instanceof Container) {
                JScrollPane found = findScrollPane((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.SURFACE_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(UITheme.SURFACE_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        
        JLabel titleLabel = new JLabel(LanguageManager.getString("app.title"));
        titleLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.BOLD, 18));
        titleLabel.setForeground(UITheme.PRIMARY_COLOR);
        headerPanel.add(titleLabel);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(UITheme.SURFACE_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));
        
        addNumberedSection(contentPanel, 1, LanguageManager.getString("about.section1.title"), 
            LanguageManager.getString("about.section1.content"));
        
        addNumberedSection(contentPanel, 2, LanguageManager.getString("about.section2.title"), 
            LanguageManager.getString("about.section2.content"));
        
        addNumberedSection(contentPanel, 3, LanguageManager.getString("about.section3.title"), 
            LanguageManager.getString("about.section3.content"));
        
        addNumberedSection(contentPanel, 4, LanguageManager.getString("about.section4.title"), 
            LanguageManager.getString("about.section4.content"));
        
        addNumberedSection(contentPanel, 5, LanguageManager.getString("about.section5.title"), 
            LanguageManager.getString("about.section5.content"));
        
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(UITheme.SURFACE_COLOR);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setValue(0);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(UITheme.SURFACE_COLOR);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JLabel versionLabel = new JLabel("Version 1.0.0 | MIT License");
        versionLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 10));
        versionLabel.setForeground(UITheme.TEXT_SECONDARY);
        footerPanel.add(versionLabel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttonPanel.setBackground(UITheme.SURFACE_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        
        JButton showWelcomeButton = UIHelper.createMaterialButton(LanguageManager.getString("about.showWelcome"), new Color(100, 100, 100));
        showWelcomeButton.setPreferredSize(new Dimension(160, 32));
        showWelcomeButton.addActionListener(e -> {
            WelcomeDialog.resetWelcomePreference();
            dispose();
            SwingUtilities.invokeLater(() -> {
                WelcomeDialog welcomeDialog = new WelcomeDialog((JFrame) getParent());
                welcomeDialog.setVisible(true);
            });
        });
        
        JButton closeButton = UIHelper.createMaterialButton(LanguageManager.getString("about.close"), UITheme.PRIMARY_COLOR);
        closeButton.setPreferredSize(new Dimension(100, 32));
        closeButton.addActionListener(e -> dispose());
        
        buttonPanel.add(showWelcomeButton);
        buttonPanel.add(closeButton);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UITheme.SURFACE_COLOR);
        bottomPanel.add(footerPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void addNumberedSection(JPanel parent, int number, String title, String content) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(UITheme.SURFACE_COLOR);
        section.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(UITheme.SURFACE_COLOR);
        titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel numberLabel = new JLabel(number + ". ");
        numberLabel.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.BOLD, 12));
        numberLabel.setForeground(UITheme.PRIMARY_COLOR);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.BOLD, 12));
        titleLabel.setForeground(UITheme.PRIMARY_COLOR);
        
        titlePanel.add(numberLabel);
        titlePanel.add(titleLabel);
        
        section.add(titlePanel);
        
        JTextArea contentArea = new JTextArea(content);
        contentArea.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 11));
        contentArea.setForeground(UITheme.TEXT_PRIMARY);
        contentArea.setBackground(UITheme.SURFACE_COLOR);
        contentArea.setEditable(false);
        contentArea.setWrapStyleWord(true);
        contentArea.setLineWrap(true);
        contentArea.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        contentArea.setOpaque(false);
        contentArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(contentArea);
        
        parent.add(section);
    }
}
