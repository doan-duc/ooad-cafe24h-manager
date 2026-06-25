package ui;

import java.awt.Color;

public final class Motion {
    private Motion() {
    }

    public static boolean enabled() {
        return !"false".equalsIgnoreCase(
                System.getProperty("cafe24h.motion", "true"));
    }

    public static float easeOutCubic(float value) {
        float remaining = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - remaining * remaining * remaining;
    }

    public static Color mix(Color from, Color to, float amount) {
        float value = Math.max(0f, Math.min(1f, amount));
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * value),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * value),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * value),
                Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * value));
    }
}
