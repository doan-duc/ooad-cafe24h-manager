package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.prefs.Preferences;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

public final class Theme {
    private static final Preferences PREFERENCES =
            Preferences.userNodeForPackage(Theme.class);
    private static boolean dark = initialDarkMode();
    private static int themeIndex = initialThemeIndex();

    public static Color INK;
    public static Color MUTED;
    public static Color CANVAS;
    public static Color SURFACE;
    public static Color PRIMARY;
    public static Color PRIMARY_DARK;
    public static Color ACCENT;
    public static Color DANGER;
    public static Color BORDER;
    public static Color SIDEBAR;

    private static final String DISPLAY_FONT = findFont(
            "Segoe UI Variable Display", "Segoe UI Semibold", "Segoe UI");
    private static final String TEXT_FONT = findFont(
            "Segoe UI Variable Text", "Segoe UI", "Arial");

    private Theme() {
    }

    public static void install() {
        applyPalette();
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        applyDefaults();
    }

    public static boolean isDark() {
        return dark;
    }

    public static void toggle() {
        dark = !dark;
        PREFERENCES.putBoolean("darkMode", dark);
        install();
    }

    public static int getThemeIndex() {
        return themeIndex;
    }

    public static void setThemeIndex(int index) {
        themeIndex = index;
        PREFERENCES.putInt("themeIndex", themeIndex);
        install();
    }

    public static int themeCount() {
        return 4;
    }

    public static String themeName(int index) {
        return switch (index) {
            case 1 -> "Cyberpunk Neon";
            case 2 -> "Cosmic Ocean";
            case 3 -> "Sunset Volcano";
            default -> "Emerald Forest";
        };
    }

    public static Color softPrimary() {
        if (dark) {
            return switch (themeIndex) {
                case 1 -> new Color(60, 20, 50);
                case 2 -> new Color(20, 35, 65);
                case 3 -> new Color(55, 25, 15);
                default -> new Color(25, 67, 57); // Forest
            };
        } else {
            return switch (themeIndex) {
                case 1 -> new Color(253, 230, 245);
                case 2 -> new Color(224, 242, 254);
                case 3 -> new Color(255, 237, 213);
                default -> new Color(232, 247, 240); // Forest
            };
        }
    }

    public static Color softAccent() {
        if (dark) {
            return switch (themeIndex) {
                case 1 -> new Color(20, 50, 60);
                case 2 -> new Color(60, 20, 35);
                case 3 -> new Color(55, 45, 15);
                default -> new Color(70, 50, 37); // Forest
            };
        } else {
            return switch (themeIndex) {
                case 1 -> new Color(224, 242, 254);
                case 2 -> new Color(255, 228, 230);
                case 3 -> new Color(254, 249, 195);
                default -> new Color(255, 249, 242); // Forest
            };
        }
    }

    public static Color secondarySurface() {
        if (dark) {
            return switch (themeIndex) {
                case 1 -> new Color(35, 25, 55);
                case 2 -> new Color(25, 35, 60);
                case 3 -> new Color(50, 30, 30);
                default -> new Color(43, 51, 61); // Forest
            };
        } else {
            return switch (themeIndex) {
                case 1 -> new Color(245, 235, 250);
                case 2 -> new Color(235, 242, 255);
                case 3 -> new Color(255, 240, 230);
                default -> new Color(241, 245, 249); // Forest
            };
        }
    }

    private static void applyPalette() {
        // Text colors are tuned separately to preserve theme contrast.
        if (dark) {
            INK = new Color(241, 245, 249);
            MUTED = new Color(166, 177, 190);
            DANGER = new Color(251, 113, 133); // Vibrant Neon Coral Red
            
            switch (themeIndex) {
                case 1 -> { // Cyberpunk Neon
                    CANVAS = new Color(11, 8, 20);      // Deep Midnight Violet
                    SURFACE = new Color(22, 15, 38);     // Obsidian Purple
                    SIDEBAR = new Color(17, 10, 30);     // Very Dark Purple
                    PRIMARY = new Color(236, 72, 153);    // Cyber Pink
                    PRIMARY_DARK = new Color(219, 39, 119);
                    ACCENT = new Color(6, 182, 212);     // Neon Cyan
                    BORDER = new Color(50, 30, 75);
                }
                case 2 -> { // Cosmic Ocean
                    CANVAS = new Color(7, 11, 22);       // Deep Dark Blue
                    SURFACE = new Color(15, 23, 42);      // Space Slate
                    SIDEBAR = new Color(10, 18, 35);      // Indigo Dark
                    PRIMARY = new Color(59, 130, 246);    // Sky Blue
                    PRIMARY_DARK = new Color(37, 99, 235);
                    ACCENT = new Color(244, 63, 94);     // Hot Coral Rose
                    BORDER = new Color(30, 41, 59);
                }
                case 3 -> { // Sunset Volcano
                    CANVAS = new Color(20, 10, 10);      // Obsidian Orange-Red
                    SURFACE = new Color(38, 20, 20);      // Dark Crimson
                    SIDEBAR = new Color(30, 12, 12);      // Deep Maroon
                    PRIMARY = new Color(249, 115, 22);    // Neon Orange
                    PRIMARY_DARK = new Color(234, 88, 12);
                    ACCENT = new Color(234, 179, 8);     // Gold
                    BORDER = new Color(60, 30, 30);
                }
                default -> { // Emerald Forest (Theme 0)
                    CANVAS = new Color(12, 18, 16);      // Dark Jade Canvas
                    SURFACE = new Color(20, 32, 28);      // Deep Forest Green
                    SIDEBAR = new Color(8, 25, 20);       // Dark Emerald
                    PRIMARY = new Color(52, 211, 153);    // Vibrant Mint
                    PRIMARY_DARK = new Color(16, 185, 129);
                    ACCENT = new Color(251, 191, 36);     // Amber/Gold Accent
                    BORDER = new Color(40, 58, 52);
                }
            }
        } else {
            INK = new Color(15, 23, 42); // Modern Slate Ink
            MUTED = new Color(100, 116, 139);
            DANGER = new Color(225, 29, 72); // Bright Rose Danger
            
            switch (themeIndex) {
                case 1 -> { // Cyberpunk Neon
                    CANVAS = new Color(253, 244, 255);    // Light Pink-Violet
                    SURFACE = Color.WHITE;
                    SIDEBAR = new Color(45, 15, 75);      // Deep Purple Sidebar
                    PRIMARY = new Color(219, 39, 119);    // Bright Magenta
                    PRIMARY_DARK = new Color(190, 24, 93);
                    ACCENT = new Color(8, 145, 178);     // Dark Cyan
                    BORDER = new Color(243, 207, 250);
                }
                case 2 -> { // Cosmic Ocean
                    CANVAS = new Color(240, 246, 255);    // Ice Blue
                    SURFACE = Color.WHITE;
                    SIDEBAR = new Color(15, 32, 67);      // Navy Sidebar
                    PRIMARY = new Color(37, 99, 235);     // Royal Blue
                    PRIMARY_DARK = new Color(29, 78, 216);
                    ACCENT = new Color(225, 29, 72);     // Rose Accent
                    BORDER = new Color(203, 221, 250);
                }
                case 3 -> { // Sunset Volcano
                    CANVAS = new Color(254, 250, 240);    // Warm Ivory
                    SURFACE = Color.WHITE;
                    SIDEBAR = new Color(67, 20, 7);       // Burnt Orange-Crimson Sidebar
                    PRIMARY = new Color(234, 88, 12);     // Sunset Orange
                    PRIMARY_DARK = new Color(194, 65, 12);
                    ACCENT = new Color(202, 138, 4);     // Deep Gold Accent
                    BORDER = new Color(250, 225, 200);
                }
                default -> { // Emerald Forest (Theme 0)
                    CANVAS = new Color(243, 248, 246);    // Very light teal/grey
                    SURFACE = Color.WHITE;
                    SIDEBAR = new Color(20, 55, 46);      // Deep Teal Sidebar
                    PRIMARY = new Color(16, 185, 129);    // Rich Green
                    PRIMARY_DARK = new Color(5, 150, 105);
                    ACCENT = new Color(245, 158, 11);     // Orange/Amber Accent
                    BORDER = new Color(209, 230, 222);
                }
            }
        }
    }

    private static void applyDefaults() {
        Font base = text(Font.PLAIN, 14f);
        UIManager.put("defaultFont", base);
        UIManager.put("Component.arc", 18);
        UIManager.put("Button.arc", 18);
        UIManager.put("TextComponent.arc", 16);
        UIManager.put("ProgressBar.arc", 18);
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.selectionBackground",
                dark ? new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 60)
                     : new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 35));
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("TableHeader.background", secondarySurface());
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.height", 40);
        UIManager.put("TabbedPane.tabArc", 14);
        UIManager.put("TabbedPane.contentSeparatorHeight", 0);
        UIManager.put("TabbedPane.background", CANVAS);
        UIManager.put("TabbedPane.foreground", INK);
        UIManager.put("TabbedPane.selectedBackground", SURFACE);
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("PasswordField.background", SURFACE);
        UIManager.put("PasswordField.foreground", INK);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("OptionPane.messageFont", base);
        UIManager.put("OptionPane.buttonFont", base.deriveFont(Font.BOLD));
    }

    public static Font display(int style, float size) {
        return new Font(DISPLAY_FONT, style, Math.round(size)).deriveFont(size);
    }

    public static Font text(int style, float size) {
        return new Font(TEXT_FONT, style, Math.round(size)).deriveFont(size);
    }

    private static String findFont(String... candidates) {
        java.util.Set<String> available = new java.util.HashSet<>(
                java.util.Arrays.asList(
                        GraphicsEnvironment
                                .getLocalGraphicsEnvironment()
                                .getAvailableFontFamilyNames()));
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    private static boolean initialDarkMode() {
        String requested = System.getProperty("cafe24h.theme", "").trim();
        if ("dark".equalsIgnoreCase(requested)) {
            return true;
        }
        if ("light".equalsIgnoreCase(requested)) {
            return false;
        }
        return PREFERENCES.getBoolean("darkMode", false);
    }

    private static int initialThemeIndex() {
        return PREFERENCES.getInt("themeIndex", 0);
    }
}
