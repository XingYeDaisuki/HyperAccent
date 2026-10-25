package com.hyperaccent.miuix.hook;

import java.util.Locale;

/**
 * Decides which resource names represent the MIUI / Miuix accent.
 *
 * <p>The important distinction is:
 * <ul>
 *   <li>{@code miuix_default_color_primary_*} / {@code miuix_color_blue_*} feed Miuix
 *       dialogs, buttons, switches, progress bars and selected list items.</li>
 *   <li>{@code system_accent*}, {@code material_dynamic_*}, {@code settingslib_*},
 *       {@code sud_*} are AOSP / Material You colors and are intentionally left alone.
 *       They are the "原生主题色" that this module is not meant to replace.</li>
 * </ul>
 */
public final class AccentNameMatcher {
    private AccentNameMatcher() {
    }

    /**
     * Names that must never be replaced, even if their stock value happens to equal
     * MIUI blue. This protects Monet, SettingsLib, alert severity colors, etc.
     */
    public static boolean isExcluded(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        return n.startsWith("system_")
                || n.startsWith("material")
                || n.startsWith("settingslib_")
                || n.startsWith("sud_")
                || n.startsWith("suw_")
                || n.startsWith("android_")
                || n.startsWith("kg_")
                || n.startsWith("de_")
                || n.startsWith("tv_")
                || n.startsWith("status_bar_battery_")
                || n.startsWith("battery_indicator_")
                || n.startsWith("banner_accent_attention_")
                || n.startsWith("preference_fallback_accent_")
                || n.startsWith("sc_shield_accent")
                || n.equals("ringtone_auth_accent")
                || n.startsWith("accent_material_")
                || n.startsWith("accent_select_")
                || n.equals("accent_tint_color_selector")
                || n.equals("color_accent_selector")
                || n.equals("icon_accent");
    }

    public static boolean isAccent(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        if (isExcluded(n)) {
            return false;
        }

        if (n.equals("accent")
                || n.equals("accent_color")
                || n.equals("color_accent")
                || n.equals("coloraccent")
                || n.equals("miuixappcompatcoloraccent")
                || n.equals("textcoloraccent")
                || n.equals("colorcontrolactivated")
                || n.equals("theme_accent")
                || n.equals("theme_accent_color")
                || n.equals("theme_color")
                || n.equals("theme_color_accent")
                || n.equals("miui_theme_color")
                || n.equals("miui_accent")
                || n.equals("miui_accent_color")
                || n.equals("miuix_accent")
                || n.equals("miuix_accent_color")
                || n.equals("miuix_color_accent")) {
            return true;
        }

        // Miuix design-system accent palette.
        if (n.startsWith("miuix_default_color_primary_")
                || n.startsWith("miuix_appcompat_primary_color_")
                || n.startsWith("miuix_appcompat_floating_primary_color_")) {
            return true;
        }
        // Derived Miuix components such as
        // miuix_appcompat_dialog_default_button_bg_primary_color_light.
        // Exclude text-on-surface variants: "primary text" is content, not accent.
        if (n.startsWith("miuix_appcompat_")
                && n.contains("primary")
                && !n.contains("text")
                && !n.contains("on_primary")
                && !n.contains("surface")
                && !n.contains("container")) {
            return true;
        }
        if (n.startsWith("miuix_color_blue_")) {
            // level1/2/3 are the translucent accent tints, solid_level1 is the solid
            // accent. Do not recolor solid_level2/3, solid_10..90 (opaque light/dark
            // palette) or the secondary_* surface states; those are backgrounds.
            if (n.endsWith("_solid_level2") || n.endsWith("_solid_level3")) {
                return false;
            }
            return n.endsWith("_level1")
                    || n.endsWith("_level2")
                    || n.endsWith("_level3")
                    || n.contains("_primary_")
                    || n.endsWith("_solid_level1");
        }

        // MIUI/Miuix-namespaced accent resources. Deliberately do not use a global
        // "contains(accent)" rule: that would also hit alert severity colors,
        // AndroidX preference fallbacks, Security Center shield colors and
        // unrelated resources such as ringtone_auth_accent.
        if ((n.startsWith("miui_") || n.startsWith("miuix_")) && n.contains("accent")) {
            return true;
        }

        // Connected WiFi/Bluetooth rows and checkable preference backgrounds.
        if (n.startsWith("miuix_preference_checkable_item_fill_color_checked_")
                || n.startsWith("miuix_preference_connect_bg_connected_color")
                || n.equals("miuix_preference_connect_bg_connected_color")) {
            return true;
        }

        return false;
    }

    /**
     * Material container tones. These are opaque pale backgrounds (the connected
     * WiFi/Bluetooth row, selected cards, chips) rather than the accent itself.
     * They are mapped to a translucent version of the custom accent so the row gets
     * a matching tint instead of the stock pale blue.
     */
    public static boolean isConnectedBackground(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        return n.startsWith("miuix_preference_connect_bg_connected_color")
                || n.startsWith("miuix_preference_checkable_item_fill_color_checked_");
    }

    public static boolean isNativeContainer(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        return n.equals("system_accent1_50")
                || n.equals("system_accent1_100")
                || n.equals("material_dynamic_primary_container")
                || n.equals("materialcolorprimarycontainer");
    }

    /**
     * Optional native-theme accent roles. These are only used when the user enables
     * "also modify native colors"; by default they stay untouched.
     *
     * <p>Only primary accent tones are included. Container/background tones such as
     * system_accent1_50/100 are deliberately left alone to avoid flattening Monets
     * tonal surfaces into one solid color.
     */
    public static boolean isNativeAccent(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        if (n.startsWith("system_accent")) {
            int underscore = n.lastIndexOf('_');
            if (underscore < 0) {
                return false;
            }
            String tone = n.substring(underscore + 1);
            return tone.equals("200")
                    || tone.equals("300")
                    || tone.equals("500")
                    || tone.equals("600")
                    || tone.equals("700");
        }
        if (n.startsWith("material_dynamic_primary")) {
            return true;
        }
        if (n.equals("materialcolorprimary")
                || n.equals("materialcolorsecondary")
                || n.equals("materialcolortertiary")
                || n.equals("materialcolorcontrolactivated")
                || n.equals("settingslib_dialog_accent")) {
            return true;
        }
        return n.startsWith("settingslib_accent")
                || n.equals("settingslib_coloraccentprimary")
                || n.equals("settingslib_coloraccentsecondary");
    }

    public static boolean isNativeOnAccent(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        return n.equals("materialcoloronprimary")
                || n.equals("settingslib_text_color_on_accent_device_default")
                || n.startsWith("system_on_primary");
    }

    /**
     * Text/icons drawn on top of the accent must flip between black and white when
     * the user picks a light accent (yellow, mint, ...).
     */
    public static boolean isOnAccent(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        // Token-aware matching. A naive contains("on_primary") also matches
        // "notification_primary_text_color", which would turn notification text
        // white. Require an actual "_on_primary" / "_on_accent" token pair.
        String[] parts = name.toLowerCase(Locale.ROOT).split("_");
        for (int i = 0; i + 1 < parts.length; i++) {
            if (!parts[i].equals("on")
                    || (!parts[i + 1].equals("primary") && !parts[i + 1].equals("accent"))) {
                continue;
            }
            if (i + 2 < parts.length) {
                String next = parts[i + 2];
                if (next.equals("container") || next.equals("fixed") || next.equals("variant")) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }
}
