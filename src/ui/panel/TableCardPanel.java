package ui.panel;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import model.TableInfo;
import ui.Motion;
import ui.Theme;

public final class TableCardPanel extends JPanel {
    private final TableInfo table;
    private final Color statusColor;
    private float hoverProgress = 0f;
    private final Timer timer;
    private long startedAt;
    private boolean hovered;

    public TableCardPanel(TableInfo table, Color statusColor) {
        this.table = table;
        this.statusColor = statusColor;
        
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(300, 300));
        setBorder(new EmptyBorder(17, 18, 17, 18));

        timer = new Timer(16, e -> {
            float elapsed = (System.nanoTime() - startedAt) / 1_000_000f;
            float progress = Math.min(1f, elapsed / 120f);
            hoverProgress = hovered ? progress : 1f - progress;
            repaint();
            if (progress >= 1f) {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.setCoalesce(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                startedAt = System.nanoTime();
                if (!timer.isRunning()) {
                    timer.start();
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
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = 22;

        // Reserve space below the card so the shadow is not clipped.
        int cardH = h - 10;
        int cardY = 2;

        float glowAlpha = 0.08f + hoverProgress * 0.16f;
        g2.setComposite(AlphaComposite.SrcOver.derive(glowAlpha));
        g2.setColor(statusColor);
        int glowOffset = Math.round(hoverProgress * 3);
        g2.fillRoundRect(
                6 - glowOffset,
                cardY + 5 - glowOffset,
                w - 12 + glowOffset * 2,
                cardH - 3 + glowOffset * 2,
                arc + 2,
                arc + 2
        );

        g2.setComposite(AlphaComposite.SrcOver.derive(0.06f));
        g2.setColor(Color.BLACK);
        g2.fillRoundRect(2, cardY + 5, w - 4, cardH - 2, arc, arc);

        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, cardY, w - 1, cardH, arc, arc);

        // Blend the status color into the border while hovering.
        if (hoverProgress > 0) {
            g2.setColor(Motion.mix(Theme.BORDER, statusColor, hoverProgress));
            g2.setStroke(new java.awt.BasicStroke(1f + hoverProgress * 1f));
        } else {
            g2.setColor(Theme.BORDER);
            g2.setStroke(new java.awt.BasicStroke(1f));
        }
        g2.drawRoundRect(0, cardY, w - 1, cardH, arc, arc);

        // Busy tables use a pulsing badge for quick visual recognition.
        if (table.thoiGianVao() != null) {
            g2.setColor(statusColor);
            int dotSize = 8;
            g2.fillOval(w - 18, cardY + 12, dotSize, dotSize);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
