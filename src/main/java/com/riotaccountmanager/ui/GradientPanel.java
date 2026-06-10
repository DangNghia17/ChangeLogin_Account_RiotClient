package com.riotaccountmanager.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A panel that paints a horizontal gradient background. */
public class GradientPanel extends JPanel {

    private final Color startColor;
    private final Color endColor;

    public GradientPanel(Color startColor, Color endColor) {
        this.startColor = startColor;
        this.endColor = endColor;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setPaint(new GradientPaint(0, 0, startColor, getWidth(), 0, endColor));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.dispose();
    }
}
