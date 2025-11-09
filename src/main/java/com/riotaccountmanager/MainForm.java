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
    
    // Components cần update khi đổi ngôn ngữ
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
        
        // Hiển thị welcome dialog nếu lần đầu sử dụng
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
        
        // Kích thước và vị trí - kích thước nhỏ, compact như cửa sổ bên trái
        setSize(430, 580);
        setResizable(true);
        setMinimumSize(new Dimension(350, 580));
        
        // Thêm icon
        UIHelper.setWindowIcon(this, "/change-user-icon.jpg");
        
        // Layout chính
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UITheme.BACKGROUND_COLOR);
        
        // Panel header
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Panel giữa: Danh sách tài khoản
        add(createAccountListPanel(), BorderLayout.CENTER);
        
        // Panel dưới: Cấu hình
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
        // Cập nhật table column headers
        if (tableModel != null) {
            tableModel.setColumnIdentifiers(new Object[]{
                LanguageManager.getString("account.table.username"),
                LanguageManager.getString("account.table.region"),
                LanguageManager.getString("account.table.note")
            });
        }
        // Cập nhật status và button text
        updateRiotClientStatus();
        // Cập nhật path display
        loadRiotClientPath();
        // Repaint
        revalidate();
        repaint();
    }
    
    /**
     * Tạo header panel với gradient
     */
    private JPanel createHeaderPanel() {
        // Sử dụng GradientPanel cho header
        GradientPanel titlePanel = new GradientPanel(
            UITheme.PRIMARY_GRADIENT_START, 
            UITheme.PRIMARY_GRADIENT_END
        );
        titlePanel.setLayout(new BorderLayout());
        // Tăng padding right để đảm bảo infoButton không bị cắt
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 20));
        titlePanel.setPreferredSize(new Dimension(0, 50));
        
        JPanel titleContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleContent.setOpaque(false);
        
        // Icon trong header
        ImageIcon icon = UIHelper.loadIcon("/change-user-icon.jpg", 24, 24);
        if (icon != null) {
            titleContent.add(new JLabel(icon));
        }
        
        titleLabel = new JLabel(LanguageManager.getString("app.title"));
        titleLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.BOLD, UITheme.FONT_SIZE_HEADER));
        titleLabel.setForeground(Color.WHITE);
        titleContent.add(titleLabel);
        titlePanel.add(titleContent, BorderLayout.WEST);
        
        // Panel chứa language selector và info button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightPanel.setOpaque(false);
        
        // Language selector - ComboBox - đơn giản, không bo góc
        String[] languages = {"VI", "EN"};
        languageComboBox = new JComboBox<String>(languages);
        languageComboBox.setSelectedItem(LanguageManager.getCurrentLanguage().toUpperCase());
        languageComboBox.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL - 2));
        // Giảm width từ 60 xuống 45 để ngắn hơn
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
        
        // Icon info ở góc phải - sử dụng icon từ file
        infoButton = UIHelper.createIconButton("/information-button.png", 21, LanguageManager.getString("about.title"));
        infoButton.setBackground(new Color(0, 0, 0, 0));
        infoButton.setForeground(Color.WHITE);
        // Đảm bảo button có kích thước cố định và không bị cắt
        infoButton.setPreferredSize(new Dimension(24, 24));
        infoButton.setMaximumSize(new Dimension(24, 24));
        infoButton.setMinimumSize(new Dimension(24, 24));
        infoButton.addActionListener(e -> {
            AboutDialog aboutDialog = new AboutDialog(this);
            aboutDialog.setVisible(true);
        });
        
        // Hiệu ứng hover cho info button - màu trắng trên nền purple
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
        // Padding tối ưu
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Card container với bo góc
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
        
        // Header của card
        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardHeader.setBackground(UITheme.SURFACE_COLOR);
        // Giảm padding để cửa sổ gọn hơn
        cardHeader.setBorder(BorderFactory.createEmptyBorder(8, 12, 6, 12));
        cardTitle = new JLabel(LanguageManager.getString("account.list.title"));
        cardTitle.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.BOLD, UITheme.FONT_SIZE_TITLE));
        cardTitle.setForeground(UITheme.TEXT_PRIMARY);
        cardHeader.add(cardTitle);
        cardPanel.add(cardHeader, BorderLayout.NORTH);
        
        // Bảng tài khoản
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
        
        // Header của bảng
        accountTable.getTableHeader().setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.PLAIN, UITheme.FONT_SIZE_NORMAL - 2));
        accountTable.getTableHeader().setBackground(UITheme.HEADER_BACKGROUND);
        accountTable.getTableHeader().setForeground(UITheme.TEXT_PRIMARY);
        accountTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        accountTable.getTableHeader().setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.DIVIDER_COLOR),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Selection color - tím nhạt
        accountTable.setSelectionBackground(UITheme.TABLE_ROW_SELECTED);
        accountTable.setSelectionForeground(UITheme.TEXT_PRIMARY);
        
        // Đặt độ rộng cột - điều chỉnh để phù hợp với cửa sổ nhỏ hơn
        accountTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        accountTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        accountTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        
        // Cell renderer với hover effect
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
                    // Hover effect - kiểm tra mouse position
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
        
        // Thêm mouse motion listener để repaint khi hover
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
        
        // Panel nút điều khiển
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        buttonPanel.setBackground(UITheme.SURFACE_COLOR);
        // Giảm padding để cửa sổ gọn hơn
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.DIVIDER_COLOR),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Tạo các nút với icon từ file PNG
        addButton = UIHelper.createIconButton("/add_account.png", 32, LanguageManager.getString("account.add"));
        editButton = UIHelper.createIconButton("/edit_account.png", 34, LanguageManager.getString("account.edit"));
        deleteButton = UIHelper.createIconButton("/delete_account.png", 32, LanguageManager.getString("account.delete"));

        // Nút đăng nhập chỉ có icon, không text, tách biệt bằng border
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
        
        // Thêm border dọc để tách biệt icon login, tăng khoảng cách sang phải
        JPanel separator = new JPanel();
        separator.setPreferredSize(new Dimension(1, 36));
        separator.setBackground(UITheme.DIVIDER_COLOR);
        // Tăng padding để tạo khoảng cách lớn hơn giữa 3 icon và icon login
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
        
        // Card container với bo góc
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
                // Đặt caret ở đầu để hiển thị phần đầu của text
                if (text != null && text.length() > 0) {
                    // Sử dụng invokeLater với delay nhỏ để đảm bảo layout đã hoàn tất
                    SwingUtilities.invokeLater(() -> {
                        try {
                            setCaretPosition(0);
                            // Đảm bảo text field hiển thị từ đầu bằng cách move caret về đầu
                            moveCaretPosition(0);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
                }
            }
            
            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                // Đảm bảo text field có đủ không gian tối thiểu
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
        // Căn trái để hiển thị đầy đủ từ đầu (không bị khuất chữ R)
        riotClientPathField.setHorizontalAlignment(JTextField.LEFT);
        // Cho phép scroll ngang bằng cách cho phép focus (nhưng không edit)
        riotClientPathField.setFocusable(true);
        
        // Thêm tooltip để hiển thị đầy đủ đường dẫn khi hover
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
        
        // Nút "Chọn" - màu xám
        browseButton = UIHelper.createMaterialButton(LanguageManager.getString("config.riot.path.select"), UITheme.BUTTON_GRAY);
        browseButton.setPreferredSize(new Dimension(80, 36));
        browseButton.addActionListener(e -> browseRiotClientPath());
        
        // Nút "Mở Riot Client" - màu tím chủ đạo
        launchRiotClientButton = UIHelper.createMaterialButton(LanguageManager.getString("config.riot.client.open"), UITheme.PRIMARY_COLOR);
        launchRiotClientButton.setPreferredSize(new Dimension(130, 36));
        launchRiotClientButton.addActionListener(e -> launchRiotClient());
        
        // Panel chứa các nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.setBackground(UITheme.SURFACE_COLOR);
        buttonPanel.add(browseButton);
        buttonPanel.add(launchRiotClientButton);
        
        // Label hiển thị trạng thái Riot Client
        riotClientStatusLabel = new JLabel(LanguageManager.getString("config.riot.client.status.checking"));
        riotClientStatusLabel.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL - 2));
        riotClientStatusLabel.setForeground(UITheme.TEXT_SECONDARY);
        riotClientStatusLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        
        // JLabel hiển thị đường dẫn đầy đủ (bên dưới text field)
        // Sử dụng JLabel với HTML để tự động wrap text khi đường dẫn dài, đảm bảo hiển thị đầy đủ
        fullPathLabel = new JLabel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                // Đảm bảo label có thể mở rộng theo chiều rộng của container
                // Chiều cao sẽ tự động điều chỉnh khi text wrap
                Container parent = getParent();
                if (parent != null) {
                    int parentWidth = parent.getWidth();
                    if (parentWidth > 0) {
                        // Sử dụng toàn bộ chiều rộng có sẵn, trừ đi một chút margin
                        dim.width = Math.max(150, parentWidth - 40);
                    } else {
                        // Nếu parent chưa có width, sử dụng width từ container cha
                        Container grandParent = parent.getParent();
                        if (grandParent != null && grandParent.getWidth() > 0) {
                            // Tính toán dựa trên grandParent width, trừ đi padding và button width
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
        // Cho phép wrap text để hiển thị đầy đủ đường dẫn dài
        fullPathLabel.setVerticalAlignment(JLabel.TOP);
        fullPathLabel.setVerticalTextPosition(JLabel.TOP);
        fullPathLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        // Sử dụng HTML để wrap text và đảm bảo hiển thị đầy đủ
        // HTML sẽ tự động wrap text khi quá dài
        
        // Panel chứa text field và label đường dẫn đầy đủ
        // Sử dụng BorderLayout để đảm bảo cả hai field có thể mở rộng ngang đầy đủ
        JPanel pathFieldPanel = new JPanel(new BorderLayout(0, 4));
        pathFieldPanel.setBackground(UITheme.SURFACE_COLOR);
        pathFieldPanel.add(riotClientPathField, BorderLayout.NORTH);
        // fullPathLabel ở dưới, có thể wrap text để hiển thị đầy đủ đường dẫn dài
        pathFieldPanel.add(fullPathLabel, BorderLayout.CENTER);
        
        // Panel chứa text field và button - đảm bảo text field có đủ không gian
        // Sử dụng BorderLayout để text field có thể mở rộng ngang
        JPanel topRowPanel = new JPanel(new BorderLayout(8, 0));
        topRowPanel.setBackground(UITheme.SURFACE_COLOR);
        // Text field panel chiếm phần lớn không gian và có thể mở rộng
        topRowPanel.add(pathFieldPanel, BorderLayout.CENTER);
        // Button panel có kích thước cố định, không chiếm quá nhiều không gian
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
        
        // Cập nhật trạng thái Riot Client ban đầu
        updateRiotClientStatus();
        
        // Tự động cập nhật trạng thái mỗi 3 giây
        javax.swing.Timer statusTimer = new javax.swing.Timer(3000, e -> updateRiotClientStatus());
        statusTimer.start();
        
        return configPanel;
    }
    
    /**
     * Đặt cửa sổ ở góc phải trên
     */
    private void setLocationToTopRight() {
        // Đảm bảo window đã có size trước khi tính toán vị trí
        // Nếu chưa có size, sử dụng size mặc định
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
            // Hiển thị tên file trong text field (gọn gàng hơn)
            java.io.File file = new java.io.File(path);
            String fileName = file.getName();
            riotClientPathField.setText(fileName);
            riotClientPathField.setHorizontalAlignment(JTextField.LEFT);
            riotClientPathField.setToolTipText(path);
            // Đảm bảo hiển thị từ đầu (caret position 0 sẽ được set trong override setText)
            
            // Hiển thị đường dẫn đầy đủ trong JLabel bên dưới
            // Sử dụng HTML để wrap text và hiển thị đầy đủ, không bị truncate
            // Escape HTML special characters
            String escapedPath = path.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            // Sử dụng HTML với word-break để wrap text dài
            // Đảm bảo hiển thị đầy đủ ngay cả khi cửa sổ nhỏ
            // Sử dụng <nobr> với break ở dấu backslash để dễ đọc hơn
            fullPathLabel.setText("<html><div style='word-wrap: break-word; word-break: break-all;'>" + escapedPath + "</div></html>");
            fullPathLabel.setToolTipText(path);
            // Đảm bảo label có thể wrap bằng cách validate và repaint
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
                        // Process đang chạy nhưng window có thể bị ẩn trong system tray
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
                    // Nếu đã chạy, chỉ cần focus vào cửa sổ
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
                    // Nếu chưa chạy, mở Riot Client
                    SwingUtilities.invokeLater(() -> {
                        riotClientStatusLabel.setText(LanguageManager.getString("config.riot.client.status.launching"));
                    });
                    
                    if (AutoLoginHelper.launchRiotClient(clientPath)) {
                        // Đợi Riot Client mở
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
                // Cập nhật lại trạng thái sau khi thay đổi đường dẫn
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
                
                // Kiểm tra Riot Client có đang chạy không
                // Chỉ cần process chạy là đủ (có thể window bị ẩn trong system tray)
                boolean isRunning = AutoLoginHelper.isRiotClientRunning();
                boolean isWindowVisible = AutoLoginHelper.isRiotClientWindowVisible();
                
                // Nếu Riot Client chưa chạy, dừng lại và hiển thị thông báo
                if (!isRunning) {
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        loginButton.setEnabled(true);
                        showMessage(LanguageManager.getString("login.client.notRunning"), 
                            LanguageManager.getString("message.title"), JOptionPane.WARNING_MESSAGE);
                    });
                    return;
                }
                
                // Nếu process chạy nhưng window không visible (nằm trong tray), 
                // tự động restore window trước khi login
                if (!isWindowVisible) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(LanguageManager.getString("login.progress.restoring"));
                    });
                    // Thử restore window từ system tray
                    if (!AutoLoginHelper.focusRiotClientWindow()) {
                        // Nếu không restore được, đợi một chút rồi thử lại
                        Thread.sleep(500);
                        AutoLoginHelper.focusRiotClientWindow();
                    }
                    // Đợi window restore
                    Thread.sleep(800);
                }
                
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText(LanguageManager.getString("login.progress.ready"));
                });
                
                // clientPath không còn cần thiết vì chỉ kiểm tra Riot Client đã mở chưa
                // Nhưng vẫn truyền vào để giữ tương thích với hàm autoLogin (có thể dùng sau này)
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
            // Đặt vị trí ở góc trên bên phải sau khi form đã được khởi tạo
            form.setLocationToTopRight();
            form.setVisible(true);
        });
    }
}
