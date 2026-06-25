package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

public final class AnimatedNavButton extends JButton {
    private Color current = Theme.SIDEBAR;
    private Color start = Theme.SIDEBAR;
    private Color target = Theme.SIDEBAR;
    private boolean hovered;
    private boolean selectedState;
    private long startedAt;
    private final Timer timer;

    // Ripple state is shared by the timer and paint cycle.
    private final List<Ripple> ripples = new CopyOnWriteArrayList<>();
    private final Timer rippleTimer;

    private static final class Ripple {
        final int x;
        final int y;
        final long startTime;

        Ripple(int x, int y, long startTime) {
            this.x = x;
            this.y = y;
            this.startTime = startTime;
        }
    }

    public AnimatedNavButton(String text) {
        super(text);
        setHorizontalAlignment(LEFT);
        setForeground(new Color(203, 213, 225));
        setFocusPainted(true);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 14, 10, 14));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));

        timer = new Timer(16, event -> tick());
        timer.setCoalesce(true);

        rippleTimer = new Timer(16, e -> {
            long now = System.currentTimeMillis();
            boolean hasActive = false;
            for (Ripple r : ripples) {
                if (now - r.startTime >= 400) {
                    ripples.remove(r);
                } else {
                    hasActive = true;
                }
            }
            repaint();
            if (!hasActive) {
                ((Timer) e.getSource()).stop();
            }
        });
        rippleTimer.setCoalesce(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                animateTo(resolveTarget());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                animateTo(resolveTarget());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled() && Motion.enabled() && e.getButton() == MouseEvent.BUTTON1) {
                    ripples.add(new Ripple(e.getX(), e.getY(), System.currentTimeMillis()));
                    if (!rippleTimer.isRunning()) {
                        rippleTimer.start();
                    }
                }
            }
        });
    }

    public void setSelectedState(boolean selected) {
        selectedState = selected;
        setForeground(selected ? Color.WHITE : new Color(203, 213, 225));
        animateTo(resolveTarget());
    }

    private Color resolveTarget() {
        if (selectedState) {
            return Theme.isDark()
                    ? new Color(28, 91, 74)
                    : new Color(35, 96, 79);
        }
        if (hovered) {
            return Theme.isDark()
                    ? new Color(22, 66, 56)
                    : new Color(29, 73, 63);
        }
        return Theme.SIDEBAR;
    }

    private void animateTo(Color next) {
        if (!Motion.enabled()) {
            current = next;
            repaint();
            return;
        }
        start = current;
        target = next;
        startedAt = System.nanoTime();
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void tick() {
        float elapsed = (System.nanoTime() - startedAt) / 1_000_000f;
        float progress = Math.min(1f, elapsed / 160f);
        current = Motion.mix(start, target, Motion.easeOutCubic(progress));
        repaint();
        if (progress >= 1f) {
            timer.stop();
        }
    }

    @Override
    public void removeNotify() {
        if (timer != null) {
            timer.stop();
        }
        if (rippleTimer != null) {
            rippleTimer.stop();
        }
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        int arc = 14;

        copy.setColor(current);
        copy.fillRoundRect(0, 0, w, h, arc, arc);

        if (Motion.enabled() && !ripples.isEmpty()) {
            Shape oldClip = copy.getClip();
            RoundRectangle2D clipShape = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
            copy.clip(clipShape);

            long now = System.currentTimeMillis();
            for (Ripple r : ripples) {
                long elapsed = now - r.startTime;
                float progress = Math.min(1.0f, elapsed / 400.0f);
                float maxRadius = Math.max(w, h) * 1.5f;
                float radius = progress * maxRadius;
                float alpha = 1.0f - progress;

                // Preserve contrast in both light and dark themes.
                Color rippleColor = new Color(255, 255, 255, (int) (alpha * 0.16f * 255));
                copy.setColor(rippleColor);
                copy.fillOval((int) (r.x - radius), (int) (r.y - radius), (int) (radius * 2), (int) (radius * 2));
            }
            copy.setClip(oldClip);
        }

        if (selectedState) {
            copy.setColor(Theme.ACCENT);
            copy.fillRoundRect(5, getHeight() / 2 - 11, 4, 22, 4, 4);
        }
        if (isFocusOwner()) {
            copy.setColor(Theme.ACCENT);
            copy.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
        }
        copy.dispose();
        super.paintComponent(graphics);
    }
}
