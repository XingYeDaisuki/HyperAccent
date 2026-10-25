package com.hyperaccent.miuix.hook;

/**
 * Small, Android-free color helpers so the hook logic can be covered by plain JVM tests.
 */
public final class AccentColorMath {
    private AccentColorMath() {
    }

    public static int opaque(int color) {
        return color | 0xFF000000;
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    /**
     * The core RGB values of Miuix's stock blue accent (light/dark primary and its
     * pressed/hover states). Some Miuix resources reference a derived color instead
     * of the base primary resource; matching the resolved value as well as the name
     * lets those references follow the custom accent too.
     *
     * <p>Deliberately excludes pale tints such as #ECF0FF and dark surfaces such as
     * #17263D, which are backgrounds rather than the accent itself.
     */
    public static boolean isStockMiuixAccent(int color) {
        int rgb = color & 0x00FFFFFF;
        switch (rgb) {
            case 0x3482FF: // light level1 / primary_default / solid60
            case 0x4788FF: // dark level1 / solid_level1
            case 0x277AF7: // dark primary_default / solid70
            case 0x3482F7: // dark primary_hover
            case 0x3885F8: // dark primary_pressed
            case 0x317AF0: // light primary_hover
            case 0x3078EB: // light primary_pressed
            case 0x4C8EFF: // solid50
            case 0x0D84FF: // Settings / WiFi / Bluetooth list accent
                return true;
            default:
                return false;
        }
    }

    /**
     * A resource such as {@code ..._primary_disable} normally carries ~30% alpha.
     * MIUI theme values can omit the entry entirely (returning null), so derive a
     * sensible alpha from the resource name in that case.
     */
    public static int defaultAlphaForName(String name) {
        String n = name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
        if (n.contains("disable")) {
            return 0x4D;
        }
        if (n.contains("hint")) {
            return 0x80;
        }
        return 0xFF;
    }

    /**
     * Black for light accents, white for normal/dark accents.
     *
     * <p>A pure WCAG comparison picks black for MIUI blue (#3482FF) because its
     * calculated contrast is slightly higher, but that looks wrong against MIUI's
     * visual language. The 0.45 luminance threshold keeps blue/purple/red white and
     * switches yellow/mint to black.
     */
    public static int contrastOn(int accent) {
        return relativeLuminance(accent) > 0.45 ? 0xFF000000 : 0xFFFFFFFF;
    }

    public static double relativeLuminance(int color) {
        double r = linear((color >> 16) & 0xFF);
        double g = linear((color >> 8) & 0xFF);
        double b = linear(color & 0xFF);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
