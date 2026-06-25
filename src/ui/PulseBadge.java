package ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

public final class PulseBadge extends JLabel {
    private final Color fill;
    private final Color ink;
    private float phase;
    private final Timer timer = new Timer(40, event -> {
        phase += 0.045f;
        repaint();
    });

    public PulseBadge(String text, Color fill, Color ink) {
        super(text, CENTER);
        this.fill = fill;
        this.ink = ink;
        setForeground(ink);
        setOpaque(false);
        setBorder(new EmptyBorder(9, 16, 9, 16));
        setFont(Theme.text(java.awt.Font.BOLD, 12f));
        timer.setCoalesce(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (Motion.enabled()) {
            timer.start();
        }
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        float pulse = (float) ((Math.sin(phase) + 1d) / 2d);
        int inset = Math.round(2 + pulse * 2);
        copy.setComposite(AlphaComposite.SrcOver.derive(0.11f + pulse * 0.08f));
        copy.setColor(ink);
        copy.fillRoundRect(
                0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        copy.setComposite(AlphaComposite.SrcOver);
        copy.setColor(fill);
        copy.fillRoundRect(
                inset, inset,
                getWidth() - inset * 2,
                getHeight() - inset * 2,
                getHeight(),
                getHeight());
        copy.dispose();
        super.paintComponent(graphics);
    }
}
