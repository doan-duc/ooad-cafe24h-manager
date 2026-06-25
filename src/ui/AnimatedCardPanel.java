package ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.Timer;

public final class AnimatedCardPanel extends JPanel {
    private static final int DURATION_MS = 220;
    private float progress = 1f;
    private long startedAt;
    private final Timer timer;

    public AnimatedCardPanel(LayoutManager layout) {
        super(layout);
        timer = new Timer(16, event -> tick());
        timer.setCoalesce(true);
        setOpaque(false);
    }

    public void animateIn() {
        if (!Motion.enabled()) {
            progress = 1f;
            repaint();
            return;
        }
        progress = 0f;
        startedAt = System.nanoTime();
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void tick() {
        long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
        progress = Math.min(1f, elapsed / (float) DURATION_MS);
        repaint();
        if (progress >= 1f) {
            timer.stop();
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
    }

    @Override
    public void removeNotify() {
        if (timer != null) {
            timer.stop();
        }
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        // The tint keeps workspace cards distinct from the sidebar.
        copy.setColor(Theme.isDark() ? new Color(0, 0, 0, 15) : new Color(255, 255, 255, 10));
        copy.fillRect(0, 0, getWidth(), getHeight());
        copy.dispose();
        super.paintComponent(graphics);
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        if (progress >= 1f) {
            super.paintChildren(graphics);
            return;
        }
        float eased = Motion.easeOutCubic(progress);
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        copy.translate(0, Math.round((1f - eased) * 12f));
        copy.setComposite(AlphaComposite.SrcOver.derive(0.25f + 0.75f * eased));
        super.paintChildren(copy);
        copy.dispose();
    }
}
