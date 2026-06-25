package ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

public final class ImageCache {
    private static final ConcurrentHashMap<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

    private ImageCache() {
    }

    public static void load(String src, Consumer<BufferedImage> onLoad) {
        if (src == null || src.isBlank()) return;
        BufferedImage cached = CACHE.get(src);
        if (cached != null) {
            onLoad.accept(cached);
            return;
        }
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                BufferedImage raw;
                if (src.startsWith("http://") || src.startsWith("https://")) {
                    raw = ImageIO.read(new URL(src));
                } else {
                    try (InputStream in = ImageCache.class.getResourceAsStream(src)) {
                        raw = in == null ? null : ImageIO.read(in);
                    }
                }
                if (raw != null) CACHE.put(src, raw);
                return raw;
            }

            @Override
            protected void done() {
                try {
                    BufferedImage img = get();
                    if (img != null) onLoad.accept(img);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    /** Load a classpath resource synchronously (for startup images). */
    public static BufferedImage loadSync(String resourcePath) {
        try (InputStream in = ImageCache.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            BufferedImage img = ImageIO.read(in);
            if (img != null) CACHE.put(resourcePath, img);
            return img;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Scale and crop image to fit target dimensions (cover strategy). */
    public static BufferedImage cover(BufferedImage src, int w, int h) {
        if (src == null) return null;
        double scaleX = (double) w / src.getWidth();
        double scaleY = (double) h / src.getHeight();
        double scale = Math.max(scaleX, scaleY);
        int scaledW = (int) Math.ceil(src.getWidth() * scale);
        int scaledH = (int) Math.ceil(src.getHeight() * scale);
        int offsetX = (scaledW - w) / 2;
        int offsetY = (scaledH - h) / 2;
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, -offsetX, -offsetY, scaledW, scaledH, null);
        g.dispose();
        return result;
    }
}
