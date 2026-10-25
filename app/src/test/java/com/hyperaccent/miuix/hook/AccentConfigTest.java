package com.hyperaccent.miuix.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccentConfigTest {
    @Test
    public void accentReplacementPreservesOriginalAlpha() {
        AccentConfig config = new AccentConfig(true, 0xFF36D167, true);
        assertEquals(0x8036D167,
                (int) config.resolve("miuix_default_color_primary_light", 0x80FFFFFF, true));
    }

    @Test
    public void contrastResourceFollowsAccentLuminance() {
        AccentConfig light = new AccentConfig(true, 0xFFFFC107, true);
        assertEquals(0xFF000000, (int) light.resolve("miuix_default_color_on_primary_light", 0xFFFFFFFF, true));

        AccentConfig dark = new AccentConfig(true, 0xFF3482FF, true);
        assertEquals(0xFFFFFFFF, (int) dark.resolve("miuix_default_color_on_primary_light", 0xFFFFFFFF, true));
    }

    @Test
    public void unrelatedResourcesReturnNull() {
        AccentConfig config = new AccentConfig(true, 0xFF36D167, true);
        assertNull(config.resolve("miuix_appcompat_dialog_default_button_text_color_light", 0xFFFFFFFF, true));
        assertEquals(0xFF36D167, (int) config.resolve("system_accent1_500", 0xFF3482FF, true));
        assertNull(config.resolve("notification_primary_text_color_light", 0xFFFFFFFF, true));
        assertNull(config.resolve("materialColorOnPrimaryContainer", 0xFFFFFFFF, true));
        assertNull(config.resolve("status_bar_battery_performance", 0xFF3482FF, true));

        AccentConfig disabled = new AccentConfig(false, 0xFF36D167, true);
        assertNull(disabled.resolve("miuix_default_color_primary_light", 0xFFFFFFFF, true));
    }

    @Test
    public void stockAccentValueCatchesDerivedResourceNames() {
        AccentConfig config = new AccentConfig(true, 0xFF36D167, true);
        assertEquals(0xFF36D167,
                (int) config.resolve("miuix_appcompat_some_derived_button_color", 0xFF3482FF, true));
        assertNull(config.resolve("miuix_appcompat_some_derived_background", 0xFFECF0FF, true));
    }

    @Test
    public void packageChecklistControlsResolution() {
        java.util.Set<String> packages = new java.util.LinkedHashSet<>();
        packages.add("com.miui.home");
        AccentConfig config = new AccentConfig(true, 0xFF36D167, true, packages);
        assertTrue(config.isPackageEnabled("com.miui.home"));
        assertFalse(config.isPackageEnabled("com.android.fileexplorer"));
    }

    @Test
    public void galleryPluginInheritsGalleryToggle() {
        java.util.Set<String> packages = new java.util.LinkedHashSet<>();
        packages.add("com.miui.gallery");
        AccentConfig config = new AccentConfig(true, 0xFF36D167, true, packages);
        assertTrue(config.isPackageEnabled("com.hyper.gallery.plugin"));
        assertFalse(config.isPackageEnabled("com.other.plugin"));
    }

    @Test
    public void monetColorsCanBeDisabledButSettingsLibStaysNative() {
        AccentConfig off = new AccentConfig(true, 0xFF36D167, true, false, ScopePackages.defaultEnabled());
        assertNull(off.resolve("system_accent1_500", 0xFF3482FF, true));
        assertNull(off.resolve("materialColorPrimary", 0xFF3482FF, true));
        assertEquals(0xFF36D167, (int) off.resolve(
                "settingslib_accent_primary_device_default", 0xFF3482FF, true));

        AccentConfig on = new AccentConfig(true, 0xFF36D167, true, true, ScopePackages.defaultEnabled());
        assertEquals(0xFF36D167, (int) on.resolve("system_accent1_500", 0xFF3482FF, true));
        assertEquals(0xFF36D167, (int) on.resolve("materialColorPrimary", 0xFF3482FF, true));
        assertEquals(0xFF000000, (int) on.resolve("materialColorOnPrimary", 0xFFFFFFFF, true));
        assertEquals(0x2636D167, (int) on.resolve("system_accent1_100", 0xFFE8F0FE, true));
        assertEquals(0x1436D167, (int) on.resolve("system_accent1_50", 0xFFF3F6FE, true));
        assertEquals(0xFF36D167, (int) on.resolve(
                "miuix_preference_connect_bg_connected_color_light", 0xFF3482FF, true));
    }

    @Test
    public void contrastCanBeDisabled() {
        AccentConfig config = new AccentConfig(true, 0xFFFFC107, false);
        assertNull(config.resolve("miuix_default_color_on_primary_light", 0xFFFFFFFF, true));
    }
}
