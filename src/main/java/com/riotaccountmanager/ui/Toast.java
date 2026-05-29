package com.riotaccountmanager.ui;

import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;

/**
 * A modern, non-blocking toast notification.
 *
 * <p>Replaces the old blocking {@link javax.swing.JOptionPane} confirmation after login.
 * It auto-dismisses, never steals focus from the Riot Client, and lets the user close the
 * application at any time (it does not require clicking "OK").
 */
public final class Toast {

    /** Notification severity, used to pick an accent color. */
    public enum Type {
        SUCCESS, ERROR, INFO, WARNING
    }

    private Toast() {
    }

    public static void show(Window owner, String message, Type type) {
        show(owner, message, type, 3200);
    }

    public static void show(final Window owner, final String message, final Type type, final int durationMs) {
        SwingUtilities.invokeLater(() -> display(owner, message, type, durationMs));
    }

    private static void display(Window owner, String message, Type type, int durationMs) {
        final Color accent = accentFor(type);

        JWindow window = new JWindow(owner);
        window.setFocusableWindowState(false); // never steal focus
        window.setAlwaysOnTop(true);

        JLabel label = new JLabel(toHtml(message)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(33, 33, 33, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 6, getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setOpaque(false);
        label.setForeground(Color.WHITE);
        label.setFont(new Font(UITheme.FONT_FAMILY, Font.PLAIN, UITheme.FONT_SIZE_NORMAL));
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 22, 14, 22));

        window.setLayout(new BorderLayout());
        window.add(label, BorderLayout.CENTER);
        window.setBackground(new Color(0, 0, 0, 0));
        window.pack();

        Dimension size = window.getSize();
        if (size.width > 420) {
            window.setSize(420, size.height);
        }

        Point location = computeLocation(owner, window.getSize());
        window.setLocation(location);
        window.setVisible(true);

        Timer dismiss = new Timer(durationMs, e -> window.dispose());
        dismiss.setRepeats(false);
        dismiss.start();
    }

    private static Point computeLocation(Window owner, Dimension toastSize) {
        if (owner != null && owner.isShowing()) {
            Point ownerLoc = owner.getLocationOnScreen();
            int x = ownerLoc.x + (owner.getWidth() - toastSize.width) / 2;
            int y = ownerLoc.y + owner.getHeight() - toastSize.height - 24;
            return new Point(Math.max(x, ownerLoc.x + 10), y);
        }
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        return new Point(screen.width - toastSize.width - 24, screen.height - toastSize.height - 60);
    }

    private static Color accentFor(Type type) {
        switch (type) {
            case SUCCESS:
                return UITheme.SUCCESS_COLOR;
            case ERROR:
                return UITheme.ERROR_COLOR;
            case WARNING:
                return UITheme.WARNING_COLOR;
            case INFO:
            default:
                return UITheme.PRIMARY_COLOR;
        }
    }

    private static String toHtml(String message) {
        String escaped = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><div style='width: 320px;'>" + escaped + "</div></html>";
    }
}
