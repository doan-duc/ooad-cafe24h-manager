package ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;

public final class PillLabel extends JLabel {
    private final Color fill;

    public PillLabel(String text, Color fill, Color foreground) {
        super(text, CENTER);
        this.fill = fill;
        setForeground(foreground);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setColor(fill);
        copy.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        copy.dispose();
        super.paintComponent(graphics);
    }
}
