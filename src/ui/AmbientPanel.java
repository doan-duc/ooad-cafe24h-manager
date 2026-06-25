package ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

import javax.swing.JPanel;

public final class AmbientPanel extends JPanel {
    public AmbientPanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Color c1;
        Color c2;
        Color c3;
        Color wash1;
        Color wash2;

        if (Theme.isDark()) {
            c1 = Theme.SIDEBAR;
            c2 = Theme.CANVAS;
            c3 = Theme.SURFACE;
            wash1 = new Color(
                    Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(),
                    Theme.PRIMARY.getBlue(), 28);
            wash2 = new Color(
                    Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(),
                    Theme.ACCENT.getBlue(), 18);
        } else {
            c1 = Theme.CANVAS;
            c2 = Theme.secondarySurface();
            c3 = new Color(250, 252, 249);
            wash1 = new Color(
                    Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(),
                    Theme.PRIMARY.getBlue(), 18);
            wash2 = new Color(
                    Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(),
                    Theme.ACCENT.getBlue(), 14);
        }

        int width = getWidth();
        int height = getHeight();

        copy.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, 0),
                new Point2D.Float(width, height),
                new float[] {0f, 0.55f, 1f},
                new Color[] { c1, c2, c3 }
        ));
        copy.fillRect(0, 0, width, height);

        copy.setComposite(AlphaComposite.SrcOver);
        copy.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, height * 0.16f),
                new Point2D.Float(width, height * 0.84f),
                new float[] {0f, 1f},
                new Color[] {wash1, new Color(wash1.getRed(), wash1.getGreen(), wash1.getBlue(), 0)}
        ));
        copy.fillRect(0, 0, width, height);

        copy.setPaint(new LinearGradientPaint(
                new Point2D.Float(width, 0),
                new Point2D.Float(0, height),
                new float[] {0f, 1f},
                new Color[] {wash2, new Color(wash2.getRed(), wash2.getGreen(), wash2.getBlue(), 0)}
        ));
        copy.fillRect(0, 0, width, height);

        copy.dispose();
        super.paintComponent(graphics);
    }
}
