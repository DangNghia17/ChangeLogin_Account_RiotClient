package com.riotaccountmanager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;

/**
 * Form chính của ứng dụng Riot Account Manager
 */
public class MainForm extends JFrame {
    private AccountManager accountManager;
    private JTable accountTable;
    private DefaultTableModel tableModel;
    private JTextField riotClientPathField;
    private JButton loginButton;
    private JButton launchRiotClientButton;
    private JLabel riotClientStatusLabel;
    private JLabel fullPathLabel; // Hiển thị đường dẫn đầy đủ (có thể wrap nhiều dòng)
    

    private JLabel titleLabel;
    private JLabel cardTitle;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JLabel pathLabel;
    private JButton browseButton;
    private JComboBox<String> languageComboBox;
    private JButton infoButton;
    
    public MainForm() {
        accountManager = new AccountManager();
        initializeUI();
        loadRiotClientPath();
        refreshAccountTable();
        

        if (WelcomeDialog.shouldShowWelcome()) {
            SwingUtilities.invokeLater(() -> {
                WelcomeDialog welcomeDialog = new WelcomeDialog(this);
                welcomeDialog.setVisible(true);
            });
        }
    }
    
    /**
     * Khởi tạo giao diện
     */
    private void initializeUI() {
        updateTitle();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        

        setSize(430, 580);
        setResizable(true);
        setMinimumSize(new Dimension(350, 580));
        

        UIHelper.setWindowIcon(this, "/change-user-icon.jpg");
        

        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UITheme.BACKGROUND_COLOR);
        

        add(createHeaderPanel(), BorderLayout.NORTH);
        

        add(createAccountListPanel(), BorderLayout.CENTER);
        

        add(createConfigPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * Cập nhật title của cửa sổ
     */
    private void updateTitle() {
        setTitle(LanguageManager.getString("app.title"));
    }
    
    /**
     * Cập nhật tất cả text trong UI khi đổi ngôn ngữ
     */
    private void updateUI() {
        updateTitle();
        if (titleLabel != null) {
            titleLabel.setText(LanguageManager.getString("app.title"));
        }
        if (cardTitle != null) {
            cardTitle.setText(LanguageManager.getString("account.list.title"));
        }
        if (addButton != null) {
            addButton.setToolTipText(LanguageManager.getString("account.add"));
        }
        if (editButton != null) {
            editButton.setToolTipText(LanguageManager.getString("account.edit"));
        }
        if (deleteButton != null) {
            deleteButton.setToolTipText(LanguageManager.getString("account.delete"));
        }
        if (loginButton != null) {
            loginButton.setToolTipText(LanguageManager.getString("account.login"));
        }
        if (pathLabel != null) {
            pathLabel.setText(LanguageManager.getString("config.riot.path"));
        }
        if (browseButton != null) {
            browseButton.setText(LanguageManager.getString("config.riot.path.select"));
        }
        if (infoButton != null) {
            infoButton.setToolTipText(LanguageManager.getString("about.title"));
        }

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
    
    /**
     * Tạo header panel với gradient
     */
    private JPanel createHeaderPanel() {

        GradientPanel titlePanel = new GradientPanel(
            UITheme.PRIMARY_GRADIENT_START, 
            UITheme.PRIMARY_GRADIENT_END
        );
        titlePanel.setLayout(new BorderLayout());

        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 20));
        titlePanel.setPreferredSize(new Dimension(0, 50));
        
        JPanel titleContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleContent.setOpaque(false);
        

        ImageIcon icon = UIHelper.loadIcon("/change-user-icon.jpg", 24, 24);
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
        

        String[] languages = {"VI", "EN"};
        languageComboBox = new JComboBox<String>(languages);
        languageComboBox.setSelectedItem(LanguageManager.getCurrentLanguage().toUpperCase());
        languageComboBox.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL - 2));

        languageComboBox.setPreferredSize(new Dimension(50, 28));
        languageComboBox.setMaximumSize(new Dimension(50, 28)); // Đảm bảo không mở rộng
        languageComboBox.setBackground(Color.WHITE);
        languageComboBox.setForeground(UITheme.TEXT_PRIMARY);
        languageComboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        languageComboBox.addActionListener(e -> {
            String selected = (String) languageComboBox.getSelectedItem();
            if ("VI".equals(selected)) {
                LanguageManager.setLanguage("vi");
            } else if ("EN".equals(selected)) {
                LanguageManager.setLanguage("en");
            }
            updateUI();
        });
        rightPanel.add(languageComboBox);
        

        infoButton = UIHelper.createIconButton("/information-button.png", 21, LanguageManager.getString("about.title"));
        infoButton.setBackground(new Color(0, 0, 0, 0));
        infoButton.setForeground(Color.WHITE);

        infoButton.setPreferredSize(new Dimension(24, 24));
        infoButton.setMaximumSize(new Dimension(24, 24));
        infoButton.setMinimumSize(new Dimension(24, 24));
        infoButton.addActionListener(e -> {
            AboutDialog aboutDialog = new AboutDialog(this);
            aboutDialog.setVisible(true);
        });
        

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
    
    /**
     * Tạo panel danh sách tài khoản
     */
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
        cardPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        

        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardHeader.setBackground(UITheme.SURFACE_COLOR);

        cardHeader.setBorder(BorderFactory.createEmptyBorder(8, 12, 6, 12));
        cardTitle = new JLabel(LanguageManager.getString("account.list.title"));
        cardTitle.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.BOLD, UITheme.FONT_SIZE_TITLE));
        cardTitle.setForeground(UITheme.TEXT_PRIMARY);
        cardHeader.add(cardTitle);
        cardPanel.add(cardHeader, BorderLayout.NORTH);
        

        String[] columnNames = {
            LanguageManager.getString("account.table.username"),
            LanguageManager.getString("account.table.region"),
            LanguageManager.getString("account.table.note")
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
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
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        

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
                    if (mousePos != null) {
                        int hoverRow = table.rowAtPoint(mousePos);
                        if (hoverRow == row) {
                            c.setBackground(UITheme.TABLE_ROW_HOVER);
                        } else {
                            c.setBackground(UITheme.SURFACE_COLOR);
                        }
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
        
        JScrollPane scrollPane = new JScrollPane(accountTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.SURFACE_COLOR);
        scrollPane.setViewportBorder(null);
        cardPanel.add(scrollPane, BorderLayout.CENTER);
        

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        buttonPanel.setBackground(UITheme.SURFACE_COLOR);

        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.DIVIDER_COLOR),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        

        addButton = UIHelper.createIconButton("/add_account.png", 32, LanguageManager.getString("account.add"));
        editButton = UIHelper.createIconButton("/edit_account.png", 34, LanguageManager.getString("account.edit"));
        deleteButton = UIHelper.createIconButton("/delete_account.png", 32, LanguageManager.getString("account.delete"));


        loginButton = UIHelper.createIconButton("/login__account.png", 34, LanguageManager.getString("account.login"));
        
        addButton.addActionListener(e -> showAddEditAccountDialog(null, -1));
        editButton.addActionListener(e -> {
            int selectedRow = accountTable.getSelectedRow();
            if (selectedRow >= 0) {
                Account account = accountManager.getAccount(selectedRow);
                showAddEditAccountDialog(account, selectedRow);
            } else {
                showMessage(LanguageManager.getString("account.select.toEdit"), 
                    LanguageManager.getString("message.title"), JOptionPane.WARNING_MESSAGE);
            }
        });
        deleteButton.addActionListener(e -> {
            int selectedRow = accountTable.getSelectedRow();
            if (selectedRow >= 0) {
                int confirm = JOptionPane.showConfirmDialog(this, 
                    LanguageManager.getString("account.delete.confirm"), 
                    LanguageManager.getString("account.delete.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    accountManager.removeAccount(selectedRow);
                    refreshAccountTable();
                }
            } else {
                showMessage(LanguageManager.getString("account.select.toDelete"), 
                    LanguageManager.getString("message.title"), JOptionPane.WARNING_MESSAGE);
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
    
    /**
     * Tạo panel cấu hình
     */
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
                        } catch (Exception e) {

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
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        riotClientPathField.setForeground(UITheme.TEXT_PRIMARY);

        riotClientPathField.setHorizontalAlignment(JTextField.LEFT);

        riotClientPathField.setFocusable(true);
        

        riotClientPathField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                String path = ConfigManager.getRiotClientPath();
                if (path != null && !path.isEmpty()) {
                    riotClientPathField.setToolTipText(path);
                } else {
                    riotClientPathField.setToolTipText(null);
                }
            }
        });
        

        browseButton = UIHelper.createMaterialButton(LanguageManager.getString("config.riot.path.select"), UITheme.BUTTON_GRAY);
        browseButton.setPreferredSize(new Dimension(80, 36));
        browseButton.addActionListener(e -> browseRiotClientPath());
        

        launchRiotClientButton = UIHelper.createMaterialButton(LanguageManager.getString("config.riot.client.open"), UITheme.PRIMARY_COLOR);
        launchRiotClientButton.setPreferredSize(new Dimension(130, 36));
        launchRiotClientButton.addActionListener(e -> launchRiotClient());
        

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.setBackground(UITheme.SURFACE_COLOR);
        buttonPanel.add(browseButton);
        buttonPanel.add(launchRiotClientButton);
        

        riotClientStatusLabel = new JLabel(LanguageManager.getString("config.riot.client.status.checking"));
        riotClientStatusLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL - 2));
        riotClientStatusLabel.setForeground(UITheme.TEXT_SECONDARY);
        riotClientStatusLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        


        fullPathLabel = new JLabel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();


                Container parent = getParent();
                if (parent != null) {
                    int parentWidth = parent.getWidth();
                    if (parentWidth > 0) {

                        dim.width = Math.max(150, parentWidth - 40);
                    } else {

                        Container grandParent = parent.getParent();
                        if (grandParent != null && grandParent.getWidth() > 0) {

                            dim.width = Math.max(150, grandParent.getWidth() - 120);
                        }
                    }
                }
                return dim;
            }
        };
        fullPathLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, 10));
        fullPathLabel.setForeground(new Color(100, 100, 100));
        fullPathLabel.setBackground(UITheme.SURFACE_COLOR);
        fullPathLabel.setOpaque(false);

        fullPathLabel.setVerticalAlignment(JLabel.TOP);
        fullPathLabel.setVerticalTextPosition(JLabel.TOP);
        fullPathLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));


        


        JPanel pathFieldPanel = new JPanel(new BorderLayout(0, 4));
        pathFieldPanel.setBackground(UITheme.SURFACE_COLOR);
        pathFieldPanel.add(riotClientPathField, BorderLayout.NORTH);

        pathFieldPanel.add(fullPathLabel, BorderLayout.CENTER);
        


        JPanel topRowPanel = new JPanel(new BorderLayout(8, 0));
        topRowPanel.setBackground(UITheme.SURFACE_COLOR);

        topRowPanel.add(pathFieldPanel, BorderLayout.CENTER);

        buttonPanel.setPreferredSize(new Dimension(
            browseButton.getPreferredSize().width + launchRiotClientButton.getPreferredSize().width + 16, 
            riotClientPathField.getPreferredSize().height));
        buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
        topRowPanel.add(buttonPanel, BorderLayout.EAST);
        
        pathPanel.add(pathLabel, BorderLayout.NORTH);
        pathPanel.add(topRowPanel, BorderLayout.CENTER);
        
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(UITheme.SURFACE_COLOR);
        statusPanel.add(riotClientStatusLabel, BorderLayout.WEST);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UITheme.SURFACE_COLOR);
        bottomPanel.add(statusPanel, BorderLayout.NORTH);
        
        configCard.add(pathPanel, BorderLayout.CENTER);
        configCard.add(bottomPanel, BorderLayout.SOUTH);
        configPanel.add(configCard, BorderLayout.CENTER);
        

        updateRiotClientStatus();
        

        javax.swing.Timer statusTimer = new javax.swing.Timer(3000, e -> updateRiotClientStatus());
        statusTimer.start();
        
        return configPanel;
    }
    
    /**
     * Đặt cửa sổ ở góc phải trên
     */
    private void setLocationToTopRight() {


        int width = getWidth();
        if (width <= 0) {
            width = 380; // Size mặc định
        }
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - width - 20;
        int y = 20;
        setLocation(x, y);
    }
    
    /**
     * Hiển thị thông báo
     */
    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
    
    /**
     * Hiển thị dialog thêm/sửa tài khoản
     */
    private void showAddEditAccountDialog(Account account, int index) {
        AccountDialog dialog = new AccountDialog(this, account);
        dialog.setVisible(true);
        
        if (dialog.isSaved()) {
            Account newAccount = dialog.getAccount();
            if (newAccount != null) {
                if (account == null) {
                    accountManager.addAccount(newAccount);
                } else {
                    accountManager.updateAccount(index, newAccount);
                }
                refreshAccountTable();
            } else {
                showMessage(LanguageManager.getString("account.fill.required"), 
                    LanguageManager.getString("error.title"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Làm mới bảng tài khoản
     */
    private void refreshAccountTable() {
        tableModel.setRowCount(0);
        for (Account account : accountManager.getAllAccounts()) {
            tableModel.addRow(new Object[]{
                account.getUsername(),
                account.getRegion(),
                account.getNote()
            });
        }
    }
    
    /**
     * Tải đường dẫn Riot Client
     */
    private void loadRiotClientPath() {
        String path = ConfigManager.getRiotClientPath();
        updatePathDisplay(path);
    }
    
    /**
     * Cập nhật hiển thị đường dẫn
     */
    private void updatePathDisplay(String path) {
        if (path != null && !path.isEmpty()) {

            java.io.File file = new java.io.File(path);
            String fileName = file.getName();
            riotClientPathField.setText(fileName);
            riotClientPathField.setHorizontalAlignment(JTextField.LEFT);
            riotClientPathField.setToolTipText(path);

            



            String escapedPath = path.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");



            fullPathLabel.setText("<html><div style='word-wrap: break-word; word-break: break-all;'>" + escapedPath + "</div></html>");
            fullPathLabel.setToolTipText(path);

            SwingUtilities.invokeLater(() -> {
                fullPathLabel.validate();
                fullPathLabel.repaint();
            });
        } else {
            riotClientPathField.setText(LanguageManager.getString("config.riot.path.notFound"));
            riotClientPathField.setToolTipText(null);
            riotClientPathField.setHorizontalAlignment(JTextField.LEFT);
            fullPathLabel.setText("");
            fullPathLabel.setToolTipText(null);
        }
    }
    
    /**
     * Cập nhật trạng thái Riot Client (đang chạy/chưa chạy)
     */
    private void updateRiotClientStatus() {
        new Thread(() -> {
            boolean isRunning = AutoLoginHelper.isRiotClientRunning();
            boolean isWindowVisible = AutoLoginHelper.isRiotClientWindowVisible();
            
            SwingUtilities.invokeLater(() -> {
                if (isRunning) {
                    if (isWindowVisible) {
                        riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.running"));
                        riotClientStatusLabel.setForeground(UITheme.SUCCESS_COLOR);
                    } else {

                        riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.running.tray"));
                        riotClientStatusLabel.setForeground(UITheme.SUCCESS_COLOR);
                    }
                    launchRiotClientButton.setText(LanguageManager.getString("config.riot.client.focus"));
                } else {
                    riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.notRunning"));
                    riotClientStatusLabel.setForeground(UITheme.ERROR_COLOR);
                    launchRiotClientButton.setText(LanguageManager.getString("config.riot.client.open"));
                }
            });
        }).start();
    }
    
    /**
     * Mở hoặc focus vào Riot Client
     */
    private void launchRiotClient() {
        String clientPath = ConfigManager.getRiotClientPath();
        if (clientPath == null || clientPath.isEmpty()) {
            showMessage(LanguageManager.getString("config.riot.client.path.required"), 
                LanguageManager.getString("message.title"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        launchRiotClientButton.setEnabled(false);
        riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.processing"));
        
        new Thread(() -> {
            try {
                boolean isRunning = AutoLoginHelper.isRiotClientRunning();
                boolean isWindowVisible = AutoLoginHelper.isRiotClientWindowVisible();
                
                if (isRunning && isWindowVisible) {

                    SwingUtilities.invokeLater(() -> {
                        riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.focusing"));
                    });
                    
                    if (AutoLoginHelper.focusRiotClientWindow()) {
                        SwingUtilities.invokeLater(() -> {
                            riotClientStatusLabel.setText("✓ Đã focus vào Riot Client");
                            riotClientStatusLabel.setForeground(new Color(0, 150, 0));
                            launchRiotClientButton.setEnabled(true);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            riotClientStatusLabel.setText("✗ Không thể focus vào Riot Client");
                            riotClientStatusLabel.setForeground(new Color(200, 0, 0));
                            launchRiotClientButton.setEnabled(true);
                        });
                    }
                } else {

                    SwingUtilities.invokeLater(() -> {
                        riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.launching"));
                    });
                    
                    if (AutoLoginHelper.launchRiotClient(clientPath)) {

                        int maxWait = 30;
                        int waited = 0;
                        while (waited < maxWait) {
                            Thread.sleep(1000);
                            waited++;
                            
                            boolean nowRunning = AutoLoginHelper.isRiotClientRunning();
                            boolean nowVisible = AutoLoginHelper.isRiotClientWindowVisible();
                            
                            if (nowRunning && nowVisible) {
                                SwingUtilities.invokeLater(() -> {
                                    riotClientStatusLabel.setText("✓ Riot Client đã được mở");
                                    riotClientStatusLabel.setForeground(new Color(0, 150, 0));
                                    launchRiotClientButton.setText("Focus Riot Client");
                                    launchRiotClientButton.setEnabled(true);
                                });
                                return;
                            }
                        }
                        
                        SwingUtilities.invokeLater(() -> {
                            riotClientStatusLabel.setText("⚠ Riot Client đang mở, vui lòng đợi...");
                            riotClientStatusLabel.setForeground(new Color(255, 140, 0));
                            launchRiotClientButton.setEnabled(true);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            showMessage(LanguageManager.getString("config.riot.client.launch.failed"), 
                                LanguageManager.getString("error.title"), JOptionPane.ERROR_MESSAGE);
                            riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.notRunning"));
                            riotClientStatusLabel.setForeground(new Color(200, 0, 0));
                            launchRiotClientButton.setEnabled(true);
                        });
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    showMessage(LanguageManager.getString("error.title") + ": " + e.getMessage(), 
                        LanguageManager.getString("error.title"), JOptionPane.ERROR_MESSAGE);
                    riotClientStatusLabel.setText("✗ Lỗi: " + e.getMessage());
                    riotClientStatusLabel.setForeground(new Color(200, 0, 0));
                    launchRiotClientButton.setEnabled(true);
                });
            }
        }).start();
    }
    
    /**
     * Chọn đường dẫn Riot Client
     */
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
        
        String currentPath = riotClientPathField.getText();
        if (currentPath != null && !currentPath.isEmpty() && new File(currentPath).exists()) {
            fileChooser.setCurrentDirectory(new File(currentPath).getParentFile());
        } else {
            fileChooser.setCurrentDirectory(new File("C:\\"));
        }
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();
            
            if (ConfigManager.validateRiotClientPath(path)) {
                ConfigManager.setRiotClientPath(path);
                updatePathDisplay(path);
                showMessage(LanguageManager.getString("config.riot.path.saved"), 
                    LanguageManager.getString("message.title"), JOptionPane.INFORMATION_MESSAGE);

                updateRiotClientStatus();
            } else {
                showMessage(LanguageManager.getString("config.riot.path.invalid"), 
                    LanguageManager.getString("error.title"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Thực hiện đăng nhập
     */
    private void performLogin() {
        int selectedRow = accountTable.getSelectedRow();
        if (selectedRow < 0) {
            showMessage(LanguageManager.getString("login.select.account"), 
                LanguageManager.getString("message.title"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Account account = accountManager.getAccount(selectedRow);
        if (account == null) {
            showMessage(LanguageManager.getString("login.select.account"), 
                LanguageManager.getString("error.title"), JOptionPane.ERROR_MESSAGE);
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
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText(LanguageManager.getString("login.progress.checking"));
                });
                


                boolean isRunning = AutoLoginHelper.isRiotClientRunning();
                boolean isWindowVisible = AutoLoginHelper.isRiotClientWindowVisible();
                

                if (!isRunning) {
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        loginButton.setEnabled(true);
                        showMessage(LanguageManager.getString("login.client.notRunning"), 
                            LanguageManager.getString("message.title"), JOptionPane.WARNING_MESSAGE);
                    });
                    return;
                }
                


                if (!isWindowVisible) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(LanguageManager.getString("login.progress.restoring"));
                    });

                    if (!AutoLoginHelper.focusRiotClientWindow()) {

                        Thread.sleep(500);
                        AutoLoginHelper.focusRiotClientWindow();
                    }

                    Thread.sleep(800);
                }
                
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText(LanguageManager.getString("login.progress.ready"));
                });
                


                String clientPath = ConfigManager.getRiotClientPath();
                boolean success = AutoLoginHelper.autoLogin(account, clientPath != null ? clientPath : "");
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    loginButton.setEnabled(true);
                    
                    if (success) {
                        showMessage(LanguageManager.getString("login.success"), 
                            LanguageManager.getString("login.success.title"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        showMessage(LanguageManager.getString("login.failed.details"), 
                            LanguageManager.getString("error.title"), JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    loginButton.setEnabled(true);
                    showMessage(LanguageManager.getString("error.title") + ": " + e.getMessage() + "\n\n" + e.getClass().getSimpleName(), 
                        LanguageManager.getString("error.title"), JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            MainForm form = new MainForm();

            form.setLocationToTopRight();
            form.setVisible(true);
        });
    }
}
