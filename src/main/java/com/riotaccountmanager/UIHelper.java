package com.riotaccountmanager;

import javax.swing.*;
import java.awt.*;

/**
 * Helper class cho UI components
 */
public class UIHelper {
    
    /**
     * Tạo nút với style hiện đại, có hiệu ứng hover và click
     */
    public static JButton createMaterialButton(String text, Color bgColor) {
        return createMaterialButton(text, bgColor, false);
    }
    
    public static JButton createMaterialButton(String text, Color bgColor, boolean isPrimary) {
        // Tính toán màu hover (sáng hơn 12-15%)
        final Color hoverColor = new Color(
            Math.min(255, (int)(bgColor.getRed() + (255 - bgColor.getRed()) * 0.15)),
            Math.min(255, (int)(bgColor.getGreen() + (255 - bgColor.getGreen()) * 0.15)),
            Math.min(255, (int)(bgColor.getBlue() + (255 - bgColor.getBlue()) * 0.15))
        );
        
        // Tính toán màu click (tối hơn 10%)
        final Color clickColor = new Color(
            Math.max(0, (int)(bgColor.getRed() * 0.9)),
            Math.max(0, (int)(bgColor.getGreen() * 0.9)),
            Math.max(0, (int)(bgColor.getBlue() * 0.9))
        );
        
        final Color finalBgColor = bgColor;
        
        // Tạo button với custom paint
        class CustomButton extends JButton {
            private Color currentColor = finalBgColor;
            
            public CustomButton(String text) {
                super(text);
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Vẽ button với rounded corners và màu hiện tại
                g2.setColor(currentColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                
                super.paintComponent(g);
            }
            
            public void updateColor(Color newColor) {
                currentColor = newColor;
                repaint();
            }
        }
        
        CustomButton button = new CustomButton(text);
        
        button.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        // Màu chữ: nếu nút xám thì dùng TEXT_PRIMARY, còn lại dùng WHITE
        if (bgColor.equals(UITheme.BUTTON_GRAY)) {
            button.setForeground(UITheme.TEXT_PRIMARY);
        } else {
            button.setForeground(Color.WHITE);
        }
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Hiệu ứng hover và click
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!button.getModel().isPressed()) {
                    button.updateColor(hoverColor);
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!button.getModel().isPressed()) {
                    button.updateColor(finalBgColor);
                }
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.updateColor(clickColor);
                
                // Animation: sau 100ms trở lại hover color hoặc bgColor
                Timer timer = new Timer(100, e -> {
                    if (button.getModel().isRollover()) {
                        button.updateColor(hoverColor);
                    } else {
                        button.updateColor(finalBgColor);
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }
        });
        
        if (isPrimary) {
            button.setPreferredSize(new Dimension(130, 36));
        } else {
            button.setPreferredSize(new Dimension(80, 36));
        }
        
        return button;
    }
    
    
    /**
     * Đặt icon cho window
     */
    public static void setWindowIcon(Window window, String iconPath) {
        ImageIcon icon = loadIcon(iconPath, 32, 32);
        if (icon != null && window instanceof JFrame) {
            ((JFrame) window).setIconImage(icon.getImage());
        } else if (icon != null && window instanceof JDialog) {
            ((JDialog) window).setIconImage(icon.getImage());
        }
    }
    
    /**
     * Tạo text field với style hiện đại, bo góc
     */
    public static JTextField createMaterialTextField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Vẽ nền trắng với bo góc
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                
                super.paintComponent(g);
            }
        };
        field.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        field.setBackground(UITheme.SURFACE_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        return field;
    }
    
    /**
     * Tạo password field với style hiện đại, bo góc
     */
    public static JPasswordField createMaterialPasswordField() {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Vẽ nền trắng với bo góc
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                
                super.paintComponent(g);
            }
        };
        field.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        field.setBackground(UITheme.SURFACE_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        return field;
    }
    
    /**
     * Tạo label với style hiện đại
     */
    public static JLabel createMaterialLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        label.setForeground(UITheme.TEXT_PRIMARY);
        return label;
    }
    
    /**
     * Tạo nút với icon từ file PNG - chỉ có hover effect
     */
    public static JButton createIconButton(String iconPath, int iconSize, String tooltip) {
        ImageIcon icon = loadIcon(iconPath, iconSize, iconSize);
        JButton button = new JButton();
        
        if (icon != null) {
            button.setIcon(icon);
        } else {
            // Fallback: nếu không load được icon, hiển thị text
            button.setText("?");
            button.setFont(new Font(UITheme.FONT_FAMILY, Font.BOLD, 16));
        }
        
        // Loại bỏ tất cả hiệu ứng mặc định
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        button.setPreferredSize(new Dimension(iconSize + 12, iconSize + 12));
        button.setToolTipText(tooltip);
        
        // Chỉ giữ hover effect - nền xám nhạt khi hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setOpaque(true);
                button.setBackground(new Color(240, 240, 240)); // Xám nhạt cho nền trắng
                button.repaint();
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setOpaque(false);
                button.setBackground(null);
                button.repaint();
            }
        });
        
        return button;
    }
    
    /**
     * Load icon từ resources (classpath) với kích thước cụ thể
     */
    public static ImageIcon loadIcon(String path, int width, int height) {
        try {
            // Đảm bảo đường dẫn bắt đầu bằng / để load từ root của classpath
            String resourcePath = path.startsWith("/") ? path : "/" + path;
            
            // Thử load từ resources (classpath) - hoạt động khi đóng gói trong JAR
            java.io.InputStream inputStream = UIHelper.class.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                try {
                    // Đọc tất cả bytes từ InputStream
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] data = new byte[4096];
                    int nRead;
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    byte[] imageBytes = buffer.toByteArray();
                    
                    ImageIcon icon = new ImageIcon(imageBytes);
                    Image image = icon.getImage();
                    Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaledImage);
                } finally {
                    inputStream.close();
                }
            }
            
            // Fallback: thử load từ file system (để hỗ trợ khi chạy từ IDE)
            java.io.File iconFile = new java.io.File(path);
            if (iconFile.exists()) {
                ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
                Image image = icon.getImage();
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.out.println("Không thể load icon: " + path + " - " + e.getMessage());
        }
        return null;
    }
}

