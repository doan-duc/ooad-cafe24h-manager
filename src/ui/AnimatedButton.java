package ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
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

public final class AnimatedButton extends JButton {
    public enum Type {
        PRIMARY, SECONDARY, DANGER
    }

    private final Type type;
    private float hoverProgress = 0f;
    private final Timer timer;
    private long startedAt;
    private boolean hovered;

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

    public AnimatedButton(String text, Type type) {
        super(text);
        this.type = type;
        setFont(Theme.text(java.awt.Font.BOLD, 13f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setBorder(new EmptyBorder(10, 18, 10, 18));

        timer = new Timer(16, e -> {
            float elapsed = (System.nanoTime() - startedAt) / 1_000_000f;
            float progress = Math.min(1f, elapsed / 150f);
            hoverProgress = hovered ? progress : 1f - progress;
            repaint();
            if (progress >= 1f) {
                ((Timer) e.getSource()).stop();
            }
        });
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
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    hovered = true;
                    startedAt = System.nanoTime();
                    if (!timer.isRunning()) {
                        timer.start();
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                startedAt = System.nanoTime();
                if (!timer.isRunning()) {
                    timer.start();
                }
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

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        if (!b) {
            hovered = false;
            hoverProgress = 0f;
            ripples.clear();
            if (rippleTimer != null) {
                rippleTimer.stop();
            }
        }
        repaint();
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
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = 18;

        // Inset the button while pressed to create tactile feedback.
        boolean pressed = getModel().isPressed() && getModel().isArmed();
        int inset = pressed ? 2 : 0;
        int cardW = w - 1 - inset * 2;
        int cardH = h - 1 - inset * 2;
        int cardX = inset;
        int cardY = inset;

        Color bgStart;
        Color bgEnd;
        Color fgColor;
        Color glowColor;

        if (!isEnabled()) {
            bgStart = Theme.isDark() ? new Color(55, 65, 81) : new Color(229, 231, 235);
            bgEnd = bgStart;
            fgColor = Theme.isDark() ? new Color(156, 163, 175) : new Color(156, 163, 175);
            glowColor = null;
        } else {
            switch (type) {
                case PRIMARY -> {
                    bgStart = Theme.PRIMARY;
                    bgEnd = Motion.mix(Theme.PRIMARY, Theme.PRIMARY_DARK, 0.4f);
                    glowColor = Theme.PRIMARY;
                    if (Theme.isDark() && (Theme.getThemeIndex() == 0 || Theme.getThemeIndex() == 1 || Theme.getThemeIndex() == 2)) {
                        fgColor = Color.BLACK; // high contrast dark text
                    } else {
                        fgColor = Color.WHITE;
                    }
                }
                case DANGER -> {
                    bgStart = Theme.DANGER;
                    bgEnd = new Color(Math.max(0, Theme.DANGER.getRed() - 40), Theme.DANGER.getGreen(), Theme.DANGER.getBlue());
                    fgColor = Color.WHITE;
                    glowColor = Theme.DANGER;
                }
                default -> { // SECONDARY
                    bgStart = Theme.secondarySurface();
                    bgEnd = Motion.mix(Theme.secondarySurface(), Theme.BORDER, 0.5f);
                    fgColor = Theme.INK;
                    glowColor = Theme.isDark() ? Color.WHITE : Theme.PRIMARY;
                }
            }
        }

        if (isEnabled() && hoverProgress > 0 && glowColor != null && type != Type.SECONDARY) {
            g2.setComposite(AlphaComposite.SrcOver.derive(hoverProgress * 0.25f));
            g2.setColor(glowColor);
            g2.fillRoundRect(cardX - 1, cardY - 1, cardW + 2, cardH + 2, arc, arc);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        // Blend toward the hover palette without abrupt color changes.
        Color cStart = bgStart;
        Color cEnd = bgEnd;
        if (isEnabled() && hoverProgress > 0) {
            if (type == Type.PRIMARY) {
                cStart = Motion.mix(bgStart, Theme.ACCENT, hoverProgress * 0.35f);
                cEnd = Motion.mix(bgEnd, Theme.PRIMARY, hoverProgress * 0.2f);
            } else if (type == Type.DANGER) {
                cStart = Motion.mix(bgStart, new Color(251, 113, 133), hoverProgress * 0.3f);
                cEnd = Motion.mix(bgEnd, Theme.DANGER, hoverProgress * 0.2f);
            } else { // SECONDARY
                cStart = Motion.mix(bgStart, Theme.BORDER, hoverProgress * 0.3f);
                cEnd = Motion.mix(bgEnd, Theme.secondarySurface(), hoverProgress * 0.3f);
            }
        }

        g2.setPaint(new LinearGradientPaint(
                cardX, cardY, cardX, cardY + cardH,
                new float[] { 0f, 1f },
                new Color[] { cStart, cEnd }
        ));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, arc - inset, arc - inset);

        if (Motion.enabled() && !ripples.isEmpty()) {
            Shape oldClip = g2.getClip();
            RoundRectangle2D clipShape = new RoundRectangle2D.Float(
                    cardX, cardY, cardW, cardH, arc - inset, arc - inset);
            g2.clip(clipShape);

            long now = System.currentTimeMillis();
            for (Ripple r : ripples) {
                long elapsed = now - r.startTime;
                float progress = Math.min(1.0f, elapsed / 400.0f);
                float maxRadius = Math.max(w, h) * 1.6f;
                float radius = progress * maxRadius;
                float alpha = 1.0f - progress;

                Color rippleColor;
                if (type == Type.PRIMARY || type == Type.DANGER) {
                    rippleColor = new Color(255, 255, 255, (int) (alpha * 0.30f * 255));
                } else {
                    Color primary = Theme.PRIMARY;
                    rippleColor = new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), (int) (alpha * 0.20f * 255));
                }

                g2.setColor(rippleColor);
                g2.fillOval((int) (r.x - radius), (int) (r.y - radius), (int) (radius * 2), (int) (radius * 2));
            }
            g2.setClip(oldClip);
        }

        if (isEnabled() && isFocusOwner()) {
            g2.setColor(Theme.ACCENT);
            g2.drawRoundRect(cardX + 1, cardY + 1, cardW - 2, cardH - 2, arc - inset, arc - inset);
        } else if (isEnabled() && type == Type.SECONDARY && hoverProgress > 0) {
            g2.setColor(Motion.mix(Theme.BORDER, Theme.PRIMARY, hoverProgress));
            g2.drawRoundRect(cardX, cardY, cardW, cardH, arc - inset, arc - inset);
        } else {
            g2.setColor(Theme.BORDER);
            g2.setComposite(AlphaComposite.SrcOver.derive(0.35f));
            g2.drawRoundRect(cardX, cardY, cardW, cardH, arc - inset, arc - inset);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        setForeground(fgColor);
        g2.dispose();
        
        super.paintComponent(g);
    }
}
