package ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;
import javax.swing.Timer;

public final class RoundedPanel extends JPanel {
    private final int arc;
    private int mouseX = -9999;
    private int mouseY = -9999;
    private float highlightAlpha = 0f;
    private final Timer fadeTimer;

    public RoundedPanel(int arc) {
        this.arc = arc;
        setOpaque(false);
        setBackground(Theme.SURFACE);

        fadeTimer = new Timer(16, e -> {
            if (mouseX != -9999) {
                highlightAlpha = Math.min(1.0f, highlightAlpha + 0.08f);
            } else {
                highlightAlpha = Math.max(0.0f, highlightAlpha - 0.08f);
            }
            repaint();
            if ((mouseX == -9999 && highlightAlpha <= 0f) || (mouseX != -9999 && highlightAlpha >= 1f)) {
                ((Timer) e.getSource()).stop();
            }
        });
        fadeTimer.setCoalesce(true);

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (Motion.enabled()) {
                    mouseX = e.getX();
                    mouseY = e.getY();
                    if (!fadeTimer.isRunning() && highlightAlpha < 1f) {
                        fadeTimer.start();
                    }
                    repaint();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (Motion.enabled()) {
                    mouseX = e.getX();
                    mouseY = e.getY();
                    if (!fadeTimer.isRunning()) {
                        fadeTimer.start();
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (Motion.enabled()) {
                    mouseX = -9999;
                    mouseY = -9999;
                    if (!fadeTimer.isRunning()) {
                        fadeTimer.start();
                    }
                }
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    @Override
    public void removeNotify() {
        if (fadeTimer != null) {
            fadeTimer.stop();
        }
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth() - 1;
        int height = getHeight() - 3;

        // Map pointer position to a small parallax offset.
        double dx = 0;
        double dy = 0;
        if (mouseX != -9999 && width > 0 && height > 0) {
            dx = (mouseX - width / 2.0) / (width / 2.0);
            dy = (mouseY - height / 2.0) / (height / 2.0);
            dx = Math.max(-1.0, Math.min(1.0, dx));
            dy = Math.max(-1.0, Math.min(1.0, dy));
        }

        // Offset the shadow and glow to follow the parallax direction.
        int shadowOffsetX = (int) (1.0 - dx * 3.0 * highlightAlpha);
        int shadowOffsetY = (int) (3.0 - dy * 3.0 * highlightAlpha);
        
        if (highlightAlpha > 0) {
            Color accent = Theme.ACCENT;
            Color glowColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (highlightAlpha * (Theme.isDark() ? 25 : 35)));
            copy.setColor(glowColor);
            copy.fillRoundRect(shadowOffsetX - 2, shadowOffsetY + 1, width + 4, height + 1, arc, arc);
        }
        
        copy.setColor(Theme.isDark()
                ? new Color(0, 0, 0, 75)
                : new Color(15, 23, 42, 16));
        copy.fillRoundRect(shadowOffsetX, shadowOffsetY, width, height, arc, arc);

        // Blend the surface and accent colors into a translucent backing.
        Color bg = getBackground();
        Color pColor = Theme.PRIMARY;
        Color aColor = Theme.ACCENT;

        Color glassStart = Theme.isDark()
                ? new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 175)
                : new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 155);
        Color glassEnd = Theme.isDark()
                ? new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 45)
                : new Color(aColor.getRed(), aColor.getGreen(), aColor.getBlue(), 35);

        LinearGradientPaint glassGrad = new LinearGradientPaint(
                0, 0, width, height,
                new float[] { 0f, 1f },
                new Color[] { glassStart, glassEnd }
        );
        copy.setPaint(glassGrad);
        copy.fillRoundRect(0, 0, width, height, arc, arc);

        // Keep reflection layers inside the rounded glass boundary.
        Shape oldClip = copy.getClip();
        RoundRectangle2D roundClip = new RoundRectangle2D.Float(0, 0, width, height, arc, arc);
        copy.clip(roundClip);

        // A curved reflection gives the panel its glass depth.
        Path2D.Float glossPath = new Path2D.Float();
        glossPath.moveTo(0, 0);
        glossPath.lineTo(width, 0);
        // Move the reflection slightly with the vertical parallax offset.
        double curveControlY1 = height * (0.35 + dy * 0.05);
        double curveControlY2 = height * (0.50 + dy * 0.05);
        double endY = height * (0.42 + dy * 0.03);
        
        glossPath.lineTo(width, (float) (height * 0.40));
        glossPath.curveTo(
                (float) (width * 0.70 + dx * 15.0), (float) curveControlY1,
                (float) (width * 0.30 + dx * 15.0), (float) curveControlY2,
                0, (float) endY
        );
        glossPath.closePath();

        LinearGradientPaint sheen = new LinearGradientPaint(
                0, 0, 0, (float) (height * 0.5),
                new float[] { 0f, 1f },
                new Color[] {
                        new Color(255, 255, 255, Theme.isDark() ? 35 : 60),
                        new Color(255, 255, 255, 0)
                }
        );
        copy.setPaint(sheen);
        copy.fill(glossPath);

        // The specular highlight follows the horizontal pointer position.
        if (Motion.enabled() && highlightAlpha > 0 && mouseX >= 0 && mouseX < getWidth() && mouseY >= 0 && mouseY < getHeight()) {
            int glowRadius = Math.max(140, Math.min(width, height) / 2);
            Point2D center = new Point2D.Double(mouseX, mouseY);
            Color centerColor = new Color(255, 255, 255, (int) (highlightAlpha * (Theme.isDark() ? 40 : 65)));
            Color transColor = new Color(255, 255, 255, 0);
            try {
                RadialGradientPaint rgp = new RadialGradientPaint(
                        center,
                        glowRadius,
                        new float[] { 0f, 1f },
                        new Color[] { centerColor, transColor }
                );
                copy.setPaint(rgp);
                copy.fill(roundClip);
            } catch (Exception ignored) {}
        }
        
        copy.setClip(oldClip);

        // Opposing edge tones simulate refraction around the glass boundary.
        LinearGradientPaint borderGrad = new LinearGradientPaint(
                (float) (0 - dx * 10.0 * highlightAlpha), (float) (0 - dy * 10.0 * highlightAlpha),
                (float) (width - dx * 10.0 * highlightAlpha), (float) (height - dy * 10.0 * highlightAlpha),
                new float[] { 0f, 0.45f, 1f },
                new Color[] {
                        new Color(255, 255, 255, Theme.isDark() ? 120 : 180), // Edge gloss highlight
                        new Color(Theme.BORDER.getRed(), Theme.BORDER.getGreen(), Theme.BORDER.getBlue(), 90),
                        new Color(0, 0, 0, Theme.isDark() ? 80 : 35)         // Depth shadow
                }
        );
        copy.setPaint(borderGrad);
        copy.drawRoundRect(0, 0, width, height, arc, arc);

        copy.dispose();
        super.paintComponent(graphics);
    }
}
