package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class ThemeSelectorPanel extends JPanel {
    private final Runnable callback;

    public ThemeSelectorPanel(Runnable callback) {
        super(new FlowLayout(FlowLayout.CENTER, 10, 5));
        this.callback = callback;
        setOpaque(false);

        JButton modeToggle = new JButton(Theme.isDark() ? "🌙" : "☀") {
            private float hoverScale = 0f;
            
            {
                setFont(Theme.text(java.awt.Font.BOLD, 15f));
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(30, 30));
                
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hoverScale = 1f;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        hoverScale = 0f;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int size = 26;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                
                if (hoverScale > 0) {
                    g2.setColor(Theme.isDark() ? new Color(255, 255, 255, 20) : new Color(0, 0, 0, 15));
                    g2.fillOval(x - 2, y - 2, size + 4, size + 4);
                }
                
                g2.setColor(Theme.INK);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        modeToggle.addActionListener(e -> {
            Theme.toggle();
            if (this.callback != null) {
                this.callback.run();
            }
        });
        
        add(modeToggle);

        for (int i = 0; i < Theme.themeCount(); i++) {
            ColorCircleButton btn = new ColorCircleButton(i);
            add(btn);
        }
    }

    private final class ColorCircleButton extends JButton {
        private final int index;
        private float hoverProgress = 0f;
        private final Timer hoverTimer;
        private long startedAt;
        private boolean hovered;

        public ColorCircleButton(int index) {
            this.index = index;
            setPreferredSize(new Dimension(22, 22));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(Theme.themeName(index));

            hoverTimer = new Timer(16, e -> {
                float elapsed = (System.nanoTime() - startedAt) / 1_000_000f;
                float progress = Math.min(1f, elapsed / 100f);
                hoverProgress = hovered ? progress : 1f - progress;
                repaint();
                if (progress >= 1f) {
                    ((Timer) e.getSource()).stop();
                }
            });
            hoverTimer.setCoalesce(true);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    startedAt = System.nanoTime();
                    hoverTimer.start();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    startedAt = System.nanoTime();
                    hoverTimer.start();
                }
            });

            addActionListener(e -> {
                if (Theme.getThemeIndex() != index) {
                    Theme.setThemeIndex(index);
                    if (callback != null) {
                        callback.run();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int circleD = 14;
            int x = (w - circleD) / 2;
            int y = (h - circleD) / 2;

            Color circleColor = switch (index) {
                case 1 -> new Color(236, 72, 153); // Pink
                case 2 -> new Color(59, 130, 246);  // Blue
                case 3 -> new Color(249, 115, 22);  // Orange
                default -> new Color(16, 185, 129);  // Green
            };

            boolean isCurrent = Theme.getThemeIndex() == index;

            if (isCurrent) {
                g2.setColor(circleColor);
                g2.drawOval(x - 3, y - 3, circleD + 5, circleD + 5);
            } else if (hoverProgress > 0) {
                g2.setColor(new Color(circleColor.getRed(), circleColor.getGreen(), circleColor.getBlue(), (int) (hoverProgress * 120)));
                g2.drawOval(x - 3, y - 3, circleD + 5, circleD + 5);
            }

            g2.setColor(circleColor);
            g2.fillOval(x, y, circleD, circleD);

            if (isCurrent) {
                g2.setColor(Color.WHITE);
                g2.fillOval(x + circleD / 2 - 2, y + circleD / 2 - 2, 4, 4);
            }

            g2.dispose();
        }
    }
}
