package com.riotaccountmanager;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {
    private Color startColor;
    private Color endColor;
    
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
        
        GradientPaint gradient = new GradientPaint(
            0, 0, startColor,
            getWidth(), 0, endColor
        );
        
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        g2d.dispose();
    }
}
