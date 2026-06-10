package com.riotaccountmanager.ui;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Factory helpers for consistently styled Swing components. */
public final class UIHelper {

    private UIHelper() {
    }

    public static JButton createMaterialButton(String text, Color bgColor) {
        return createMaterialButton(text, bgColor, false);
    }

    public static JButton createMaterialButton(String text, Color bgColor, boolean isPrimary) {
        final Color hoverColor = new Color(
                Math.min(255, (int) (bgColor.getRed() + (255 - bgColor.getRed()) * 0.15)),
                Math.min(255, (int) (bgColor.getGreen() + (255 - bgColor.getGreen()) * 0.15)),
                Math.min(255, (int) (bgColor.getBlue() + (255 - bgColor.getBlue()) * 0.15)));

        final Color clickColor = new Color(
                Math.max(0, (int) (bgColor.getRed() * 0.9)),
                Math.max(0, (int) (bgColor.getGreen() * 0.9)),
                Math.max(0, (int) (bgColor.getBlue() * 0.9)));

        final Color finalBgColor = bgColor;

        class CustomButton extends JButton {
            private Color currentColor = finalBgColor;

            CustomButton(String text) {
                super(text);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }

            void updateColor(Color newColor) {
                currentColor = newColor;
                repaint();
            }
        }

        CustomButton button = new CustomButton(text);
        button.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        button.setForeground(bgColor.equals(UITheme.BUTTON_GRAY) ? UITheme.TEXT_PRIMARY : Color.WHITE);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

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
                Timer timer = new Timer(100, e ->
                        button.updateColor(button.getModel().isRollover() ? hoverColor : finalBgColor));
                timer.setRepeats(false);
                timer.start();
            }
        });

        button.setPreferredSize(isPrimary ? new Dimension(130, 36) : new Dimension(80, 36));
        return button;
    }

    public static void setWindowIcon(Window window, String iconPath) {
        ImageIcon icon = loadIcon(iconPath, 32, 32);
        if (icon == null) {
            return;
        }
        if (window instanceof JFrame) {
            ((JFrame) window).setIconImage(icon.getImage());
        } else if (window instanceof JDialog) {
            ((JDialog) window).setIconImage(icon.getImage());
        }
    }

    public static JTextField createMaterialTextField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleInput(field);
        return field;
    }

    public static JPasswordField createMaterialPasswordField() {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleInput(field);
        return field;
    }

    private static void styleInput(JTextField field) {
        field.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        field.setBackground(UITheme.SURFACE_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
    }

    public static JLabel createMaterialLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(UITheme.FONT_FAMILY_SEMIBOLD, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        label.setForeground(UITheme.TEXT_PRIMARY);
        return label;
    }

    public static JButton createIconButton(String iconPath, int iconSize, String tooltip) {
        ImageIcon icon = loadIcon(iconPath, iconSize, iconSize);
        JButton button = new JButton();
        if (icon != null) {
            button.setIcon(icon);
        } else {
            button.setText("?");
            button.setFont(new Font(UITheme.FONT_FAMILY, Font.BOLD, 16));
        }
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        button.setPreferredSize(new Dimension(iconSize + 12, iconSize + 12));
        button.setToolTipText(tooltip);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setOpaque(true);
                button.setBackground(new Color(240, 240, 240));
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

    public static ImageIcon loadIcon(String path, int width, int height) {
        try {
            String resourcePath = path.startsWith("/") ? path : "/" + path;
            try (InputStream inputStream = UIHelper.class.getResourceAsStream(resourcePath)) {
                if (inputStream != null) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[4096];
                    int nRead;
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    ImageIcon icon = new ImageIcon(buffer.toByteArray());
                    Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            }
            java.io.File iconFile = new java.io.File(path);
            if (iconFile.exists()) {
                ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
                Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            System.err.println("Could not load icon: " + path + " - " + e.getMessage());
        }
        return null;
    }
}
