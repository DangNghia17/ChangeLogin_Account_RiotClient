package com.riotaccountmanager.ui;

import com.riotaccountmanager.i18n.LanguageManager;
import com.riotaccountmanager.model.Account;
import com.riotaccountmanager.riot.RiotClientService;
import com.riotaccountmanager.storage.AccountStore;
import com.riotaccountmanager.storage.AppConfig;
import com.riotaccountmanager.storage.AppSettings;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.io.File;

/**
 * Main application window. Owns the account table, Riot Client configuration panel and the
 * login flow. Heavy lifting (storage, encryption, Riot interaction) is delegated to the
 * dedicated service classes; this class is purely presentation + orchestration.
 */
public class MainForm extends JFrame {

    private final AccountStore accountStore;

    private JTable accountTable;
    private DefaultTableModel tableModel;
    private JTextField riotClientPathField;
    private JButton loginButton;
    private JButton launchRiotClientButton;
    private JLabel riotClientStatusLabel;
    private JLabel fullPathLabel;

    private JLabel titleLabel;
    private JLabel cardTitle;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JLabel pathLabel;
    private JButton browseButton;
    private JComboBox<String> languageComboBox;
    private JButton infoButton;
    private JButton settingsButton;

    private javax.swing.Timer statusTimer;

    public MainForm() {
        this.accountStore = new AccountStore();
        initializeUI();
        loadRiotClientPath();
        refreshAccountTable();

        if (WelcomeDialog.shouldShowWelcome()) {
            SwingUtilities.invokeLater(() -> new WelcomeDialog(this).setVisible(true));
        }
    }

    private void initializeUI() {
        updateTitle();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setSize(440, 600);
        setResizable(true);
        setMinimumSize(new Dimension(360, 560));

        UIHelper.setWindowIcon(this, "/change-user-icon.jpg");

        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UITheme.BACKGROUND_COLOR);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createAccountListPanel(), BorderLayout.CENTER);
        add(createConfigPanel(), BorderLayout.SOUTH);
    }

    private void updateTitle() {
        setTitle(LanguageManager.getString("app.title"));
    }

    private void updateUI() {
        updateTitle();
        if (titleLabel != null) titleLabel.setText(LanguageManager.getString("app.title"));
        if (cardTitle != null) cardTitle.setText(LanguageManager.getString("account.list.title"));
        if (addButton != null) addButton.setToolTipText(LanguageManager.getString("account.add"));
        if (editButton != null) editButton.setToolTipText(LanguageManager.getString("account.edit"));
        if (deleteButton != null) deleteButton.setToolTipText(LanguageManager.getString("account.delete"));
        if (loginButton != null) loginButton.setToolTipText(LanguageManager.getString("account.login"));
        if (pathLabel != null) pathLabel.setText(LanguageManager.getString("config.riot.path"));
        if (browseButton != null) browseButton.setText(LanguageManager.getString("config.riot.path.select"));
        if (infoButton != null) infoButton.setToolTipText(LanguageManager.getString("about.title"));
        if (settingsButton != null) settingsButton.setToolTipText(LanguageManager.getString("settings.title"));
        if (tableModel != null) {
            tableModel.setColumnIdentifiers(new Object[]{
                    LanguageManager.getString("account.table.username"),
                    LanguageManager.getString("account.table.region"),
                    LanguageManager.getString("account.table.note")
            });
        }
        updateRiotClientStatus();
        loadRiotClientPath();
        revalidate();
        repaint();
    }

    private JPanel createHeaderPanel() {
        GradientPanel titlePanel = new GradientPanel(
                UITheme.PRIMARY_GRADIENT_START, UITheme.PRIMARY_GRADIENT_END);
        titlePanel.setLayout(new BorderLayout());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 20));
        titlePanel.setPreferredSize(new Dimension(0, 50));

        JPanel titleContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleContent.setOpaque(false);

        javax.swing.ImageIcon icon = UIHelper.loadIcon("/change-user-icon.jpg", 24, 24);
        if (icon != null) {
            titleContent.add(new JLabel(icon));
        }

        titleLabel = new JLabel(LanguageManager.getString("app.title"));
        titleLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.BOLD, UITheme.FONT_SIZE_HEADER));
        titleLabel.setForeground(Color.WHITE);
        titleContent.add(titleLabel);
        titlePanel.add(titleContent, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightPanel.setOpaque(false);

        languageComboBox = new JComboBox<>(new String[]{"VI", "EN"});
        languageComboBox.setSelectedItem(LanguageManager.getCurrentLanguage().toUpperCase());
        languageComboBox.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL - 2));
        languageComboBox.setPreferredSize(new Dimension(50, 28));
        languageComboBox.setMaximumSize(new Dimension(50, 28));
        languageComboBox.setBackground(Color.WHITE);
        languageComboBox.setForeground(UITheme.TEXT_PRIMARY);
        languageComboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        languageComboBox.addActionListener(e -> {
            String selected = (String) languageComboBox.getSelectedItem();
            LanguageManager.setLanguage("VI".equals(selected) ? "vi" : "en");
            updateUI();
        });
        rightPanel.add(languageComboBox);

        settingsButton = createHeaderTextButton("\u2699", LanguageManager.getString("settings.title"));
        settingsButton.addActionListener(e -> new SettingsDialog(this).setVisible(true));
        rightPanel.add(settingsButton);

        infoButton = UIHelper.createIconButton("/information-button.png", 21, LanguageManager.getString("about.title"));
        infoButton.setBackground(new Color(0, 0, 0, 0));
        infoButton.setForeground(Color.WHITE);
        infoButton.setPreferredSize(new Dimension(24, 24));
        infoButton.setMaximumSize(new Dimension(24, 24));
        infoButton.setMinimumSize(new Dimension(24, 24));
        infoButton.addActionListener(e -> new AboutDialog(this).setVisible(true));
        infoButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                infoButton.setOpaque(true);
                infoButton.setBackground(new Color(255, 255, 255, 30));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                infoButton.setOpaque(false);
                infoButton.setBackground(new Color(0, 0, 0, 0));
            }
        });
        rightPanel.add(infoButton);

        titlePanel.add(rightPanel, BorderLayout.EAST);
        return titlePanel;
    }

    private JButton createHeaderTextButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 16));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        button.setPreferredSize(new Dimension(26, 26));
        button.setToolTipText(tooltip);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setOpaque(true);
                button.setBackground(new Color(255, 255, 255, 30));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setOpaque(false);
                button.setBackground(new Color(0, 0, 0, 0));
            }
        });
        return button;
    }

    private JPanel createAccountListPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setBackground(UITheme.BACKGROUND_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel cardPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        cardPanel.setBackground(UITheme.SURFACE_COLOR);
        cardPanel.setOpaque(false);

        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardHeader.setBackground(UITheme.SURFACE_COLOR);
        cardHeader.setBorder(BorderFactory.createEmptyBorder(8, 12, 6, 12));
        cardTitle = new JLabel(LanguageManager.getString("account.list.title"));
        cardTitle.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.BOLD, UITheme.FONT_SIZE_TITLE));
        cardTitle.setForeground(UITheme.TEXT_PRIMARY);
        cardHeader.add(cardTitle);
        cardPanel.add(cardHeader, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{
                LanguageManager.getString("account.table.username"),
                LanguageManager.getString("account.table.region"),
                LanguageManager.getString("account.table.note")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        accountTable = new JTable(tableModel);
        accountTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountTable.setRowHeight(38);
        accountTable.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        accountTable.setForeground(UITheme.TEXT_PRIMARY);
        accountTable.setBackground(UITheme.SURFACE_COLOR);
        accountTable.setGridColor(UITheme.DIVIDER_COLOR);
        accountTable.setShowGrid(true);
        accountTable.setShowHorizontalLines(true);
        accountTable.setShowVerticalLines(false);

        accountTable.getTableHeader().setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.PLAIN, UITheme.FONT_SIZE_NORMAL - 2));
        accountTable.getTableHeader().setBackground(UITheme.HEADER_BACKGROUND);
        accountTable.getTableHeader().setForeground(UITheme.TEXT_PRIMARY);
        accountTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        accountTable.getTableHeader().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.DIVIDER_COLOR),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        accountTable.setSelectionBackground(UITheme.TABLE_ROW_SELECTED);
        accountTable.setSelectionForeground(UITheme.TEXT_PRIMARY);

        accountTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        accountTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        accountTable.getColumnModel().getColumn(2).setPreferredWidth(150);

        accountTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            {
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                setHorizontalAlignment(JLabel.CENTER);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(UITheme.TABLE_ROW_SELECTED);
                    c.setForeground(UITheme.TEXT_PRIMARY);
                } else {
                    Point mousePos = table.getMousePosition();
                    if (mousePos != null && table.rowAtPoint(mousePos) == row) {
                        c.setBackground(UITheme.TABLE_ROW_HOVER);
                    } else {
                        c.setBackground(UITheme.SURFACE_COLOR);
                    }
                    c.setForeground(UITheme.TEXT_PRIMARY);
                }
                return c;
            }
        });

        accountTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                accountTable.repaint();
            }
        });

        // Double-click a row to log in quickly.
        accountTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && accountTable.getSelectedRow() >= 0) {
                    performLogin();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(accountTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.SURFACE_COLOR);
        scrollPane.setViewportBorder(null);
        cardPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        buttonPanel.setBackground(UITheme.SURFACE_COLOR);
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.DIVIDER_COLOR),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        addButton = UIHelper.createIconButton("/add_account.png", 32, LanguageManager.getString("account.add"));
        editButton = UIHelper.createIconButton("/edit_account.png", 34, LanguageManager.getString("account.edit"));
        deleteButton = UIHelper.createIconButton("/delete_account.png", 32, LanguageManager.getString("account.delete"));
        loginButton = UIHelper.createIconButton("/login__account.png", 34, LanguageManager.getString("account.login"));

        addButton.addActionListener(e -> showAddEditAccountDialog(null, -1));
        editButton.addActionListener(e -> {
            int selectedRow = accountTable.getSelectedRow();
            if (selectedRow >= 0) {
                showAddEditAccountDialog(accountStore.getAccount(selectedRow), selectedRow);
            } else {
                Toast.show(this, LanguageManager.getString("account.select.toEdit"), Toast.Type.WARNING);
            }
        });
        deleteButton.addActionListener(e -> {
            int selectedRow = accountTable.getSelectedRow();
            if (selectedRow >= 0) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        LanguageManager.getString("account.delete.confirm"),
                        LanguageManager.getString("account.delete.title"),
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    accountStore.removeAccount(selectedRow);
                    refreshAccountTable();
                }
            } else {
                Toast.show(this, LanguageManager.getString("account.select.toDelete"), Toast.Type.WARNING);
            }
        });
        loginButton.addActionListener(e -> performLogin());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        JPanel separator = new JPanel();
        separator.setPreferredSize(new Dimension(1, 36));
        separator.setBackground(UITheme.DIVIDER_COLOR);
        separator.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        buttonPanel.add(separator);

        buttonPanel.add(loginButton);
        cardPanel.add(buttonPanel, BorderLayout.SOUTH);

        centerPanel.add(cardPanel, BorderLayout.CENTER);
        return centerPanel;
    }

    private JPanel createConfigPanel() {
        JPanel configPanel = new JPanel(new BorderLayout(0, 0));
        configPanel.setBackground(UITheme.BACKGROUND_COLOR);
        configPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel configCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        configCard.setBackground(UITheme.SURFACE_COLOR);
        configCard.setOpaque(false);
        configCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel pathPanel = new JPanel(new BorderLayout(8, 8));
        pathPanel.setBackground(UITheme.SURFACE_COLOR);

        pathLabel = new JLabel(LanguageManager.getString("config.riot.path"));
        pathLabel.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        pathLabel.setForeground(UITheme.TEXT_PRIMARY);

        riotClientPathField = new JTextField() {
            @Override
            public void setText(String text) {
                super.setText(text);
                if (text != null && text.length() > 0) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            setCaretPosition(0);
                            moveCaretPosition(0);
                        } catch (Exception ignored) {
                        }
                    });
                }
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width = Math.max(dim.width, 200);
                return dim;
            }
        };
        riotClientPathField.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        riotClientPathField.setEditable(false);
        riotClientPathField.setBackground(UITheme.SURFACE_COLOR);
        riotClientPathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        riotClientPathField.setForeground(UITheme.TEXT_PRIMARY);
        riotClientPathField.setHorizontalAlignment(JTextField.LEFT);

        browseButton = UIHelper.createMaterialButton(LanguageManager.getString("config.riot.path.select"), UITheme.BUTTON_GRAY);
        browseButton.setPreferredSize(new Dimension(80, 36));
        browseButton.addActionListener(e -> browseRiotClientPath());

        launchRiotClientButton = UIHelper.createMaterialButton(LanguageManager.getString("config.riot.client.open"), UITheme.PRIMARY_COLOR);
        launchRiotClientButton.setPreferredSize(new Dimension(130, 36));
        launchRiotClientButton.addActionListener(e -> launchRiotClient());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.setBackground(UITheme.SURFACE_COLOR);
        buttonRow.add(browseButton);
        buttonRow.add(launchRiotClientButton);

        riotClientStatusLabel = new JLabel(LanguageManager.getString("config.riot.client.status.checking"));
        riotClientStatusLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL - 2));
        riotClientStatusLabel.setForeground(UITheme.TEXT_SECONDARY);
        riotClientStatusLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        fullPathLabel = new JLabel();
        fullPathLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 10));
        fullPathLabel.setForeground(new Color(100, 100, 100));
        fullPathLabel.setOpaque(false);
        fullPathLabel.setVerticalAlignment(JLabel.TOP);
        fullPathLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JPanel pathFieldPanel = new JPanel(new BorderLayout(0, 4));
        pathFieldPanel.setBackground(UITheme.SURFACE_COLOR);
        pathFieldPanel.add(riotClientPathField, BorderLayout.NORTH);
        pathFieldPanel.add(fullPathLabel, BorderLayout.CENTER);

        JPanel topRowPanel = new JPanel(new BorderLayout(8, 0));
        topRowPanel.setBackground(UITheme.SURFACE_COLOR);
        topRowPanel.add(pathFieldPanel, BorderLayout.CENTER);
        buttonRow.setPreferredSize(new Dimension(
                browseButton.getPreferredSize().width + launchRiotClientButton.getPreferredSize().width + 16,
                riotClientPathField.getPreferredSize().height));
        topRowPanel.add(buttonRow, BorderLayout.EAST);

        pathPanel.add(pathLabel, BorderLayout.NORTH);
        pathPanel.add(topRowPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(UITheme.SURFACE_COLOR);
        statusPanel.add(riotClientStatusLabel, BorderLayout.WEST);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        configCard.add(pathPanel, BorderLayout.CENTER);
        configCard.add(statusPanel, BorderLayout.SOUTH);
        configPanel.add(configCard, BorderLayout.CENTER);

        updateRiotClientStatus();

        statusTimer = new javax.swing.Timer(3000, e -> updateRiotClientStatus());
        statusTimer.start();

        return configPanel;
    }

    /** Places the window in the top-right corner of the primary screen. */
    public void placeTopRight() {
        int width = getWidth() > 0 ? getWidth() : 440;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width - width - 20, 20);
    }

    private void showAddEditAccountDialog(Account account, int index) {
        AccountDialog dialog = new AccountDialog(this, account);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            Account newAccount = dialog.getAccount();
            if (newAccount != null) {
                if (account == null) {
                    accountStore.addAccount(newAccount);
                } else {
                    accountStore.updateAccount(index, newAccount);
                }
                refreshAccountTable();
            } else {
                Toast.show(this, LanguageManager.getString("account.fill.required"), Toast.Type.ERROR);
            }
        }
    }

    private void refreshAccountTable() {
        tableModel.setRowCount(0);
        for (Account account : accountStore.getAllAccounts()) {
            tableModel.addRow(new Object[]{account.getUsername(), account.getRegion(), account.getNote()});
        }
    }

    private void loadRiotClientPath() {
        updatePathDisplay(AppConfig.getRiotClientPath());
    }

    private void updatePathDisplay(String path) {
        if (path != null && !path.isEmpty()) {
            File file = new File(path);
            riotClientPathField.setText(file.getName());
            riotClientPathField.setToolTipText(path);
            String escaped = path.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            fullPathLabel.setText("<html><div style='word-wrap: break-word; word-break: break-all;'>" + escaped + "</div></html>");
            fullPathLabel.setToolTipText(path);
        } else {
            riotClientPathField.setText(LanguageManager.getString("config.riot.path.notFound"));
            riotClientPathField.setToolTipText(null);
            fullPathLabel.setText("");
            fullPathLabel.setToolTipText(null);
        }
    }

    private void updateRiotClientStatus() {
        new Thread(() -> {
            boolean isRunning = RiotClientService.isRiotClientRunning();
            boolean isWindowVisible = RiotClientService.isRiotClientWindowVisible();
            SwingUtilities.invokeLater(() -> {
                if (isRunning) {
                    riotClientStatusLabel.setText(isWindowVisible
                            ? LanguageManager.getString("config.riot.client.status.running")
                            : LanguageManager.getString("config.riot.client.status.running.tray"));
                    riotClientStatusLabel.setForeground(UITheme.SUCCESS_COLOR);
                    launchRiotClientButton.setText(LanguageManager.getString("config.riot.client.focus"));
                } else {
                    riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.notRunning"));
                    riotClientStatusLabel.setForeground(UITheme.ERROR_COLOR);
                    launchRiotClientButton.setText(LanguageManager.getString("config.riot.client.open"));
                }
            });
        }, "riot-status-check").start();
    }

    private void launchRiotClient() {
        String clientPath = AppConfig.getRiotClientPath();
        if (clientPath == null || clientPath.isEmpty()) {
            Toast.show(this, LanguageManager.getString("config.riot.client.path.required"), Toast.Type.WARNING);
            return;
        }

        launchRiotClientButton.setEnabled(false);
        riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.processing"));

        new Thread(() -> {
            try {
                boolean isRunning = RiotClientService.isRiotClientRunning();
                boolean isWindowVisible = RiotClientService.isRiotClientWindowVisible();

                if (isRunning && isWindowVisible) {
                    boolean focused = RiotClientService.focusRiotClientWindow();
                    SwingUtilities.invokeLater(() -> {
                        Toast.show(this, LanguageManager.getString(focused
                                ? "config.riot.client.focused" : "config.riot.client.focus.failed"),
                                focused ? Toast.Type.SUCCESS : Toast.Type.ERROR);
                        launchRiotClientButton.setEnabled(true);
                        updateRiotClientStatus();
                    });
                    return;
                }

                SwingUtilities.invokeLater(() ->
                        riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.launching")));

                if (RiotClientService.launchRiotClient(clientPath)) {
                    for (int waited = 0; waited < 30; waited++) {
                        Thread.sleep(1000);
                        if (RiotClientService.isRiotClientRunning() && RiotClientService.isRiotClientWindowVisible()) {
                            SwingUtilities.invokeLater(() -> {
                                Toast.show(this, LanguageManager.getString("config.riot.client.opened"), Toast.Type.SUCCESS);
                                launchRiotClientButton.setEnabled(true);
                                updateRiotClientStatus();
                            });
                            return;
                        }
                    }
                    SwingUtilities.invokeLater(() -> launchRiotClientButton.setEnabled(true));
                } else {
                    SwingUtilities.invokeLater(() -> {
                        Toast.show(this, LanguageManager.getString("config.riot.client.launch.failed"), Toast.Type.ERROR);
                        launchRiotClientButton.setEnabled(true);
                        updateRiotClientStatus();
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    Toast.show(this, LanguageManager.getString("error.title") + ": " + e.getMessage(), Toast.Type.ERROR);
                    launchRiotClientButton.setEnabled(true);
                });
            }
        }, "riot-launch").start();
    }

    private void browseRiotClientPath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(LanguageManager.getString("filechooser.riot.title"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().equalsIgnoreCase("RiotClientServices.exe");
            }

            @Override
            public String getDescription() {
                return LanguageManager.getString("filechooser.riot.filter");
            }
        });

        String currentPath = AppConfig.getRiotClientPath();
        if (currentPath != null && !currentPath.isEmpty() && new File(currentPath).exists()) {
            fileChooser.setCurrentDirectory(new File(currentPath).getParentFile());
        } else {
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home", ".")));
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            if (AppConfig.validateRiotClientPath(path)) {
                AppConfig.setRiotClientPath(path);
                updatePathDisplay(path);
                Toast.show(this, LanguageManager.getString("config.riot.path.saved"), Toast.Type.SUCCESS);
                updateRiotClientStatus();
            } else {
                Toast.show(this, LanguageManager.getString("config.riot.path.invalid"), Toast.Type.ERROR);
            }
        }
    }

    private void performLogin() {
        int selectedRow = accountTable.getSelectedRow();
        if (selectedRow < 0) {
            Toast.show(this, LanguageManager.getString("login.select.account"), Toast.Type.WARNING);
            return;
        }

        Account account = accountStore.getAccount(selectedRow);
        if (account == null) {
            Toast.show(this, LanguageManager.getString("login.select.account"), Toast.Type.ERROR);
            return;
        }

        loginButton.setEnabled(false);

        JDialog progressDialog = new JDialog(this, LanguageManager.getString("login.progress.title"), false);
        progressDialog.setSize(320, 100);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressPanel.setBackground(Color.WHITE);
        JLabel statusLabel = new JLabel(LanguageManager.getString("login.progress.checking"), JLabel.CENTER);
        statusLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 12));
        progressPanel.add(statusLabel, BorderLayout.CENTER);
        progressDialog.add(progressPanel);
        progressDialog.setVisible(true);

        new Thread(() -> {
            try {
                boolean isRunning = RiotClientService.isRiotClientRunning();
                if (!isRunning) {
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        loginButton.setEnabled(true);
                        Toast.show(this, LanguageManager.getString("login.client.notRunning"), Toast.Type.WARNING);
                    });
                    return;
                }

                if (!RiotClientService.isRiotClientWindowVisible()) {
                    SwingUtilities.invokeLater(() ->
                            statusLabel.setText(LanguageManager.getString("login.progress.restoring")));
                    if (!RiotClientService.focusRiotClientWindow()) {
                        Thread.sleep(500);
                        RiotClientService.focusRiotClientWindow();
                    }
                    Thread.sleep(800);
                }

                SwingUtilities.invokeLater(() ->
                        statusLabel.setText(LanguageManager.getString("login.progress.ready")));

                boolean autoSubmit = AppSettings.isAutoClickLogin();
                boolean success = RiotClientService.autoLogin(account, autoSubmit);

                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    loginButton.setEnabled(true);
                    if (success) {
                        Toast.show(this, LanguageManager.getString(autoSubmit
                                ? "login.success.submitted" : "login.success"), Toast.Type.SUCCESS);
                    } else {
                        Toast.show(this, LanguageManager.getString("login.failed.details"), Toast.Type.ERROR);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    loginButton.setEnabled(true);
                    Toast.show(this, LanguageManager.getString("error.title") + ": " + e.getMessage(), Toast.Type.ERROR);
                });
            }
        }, "riot-login").start();
    }
}
