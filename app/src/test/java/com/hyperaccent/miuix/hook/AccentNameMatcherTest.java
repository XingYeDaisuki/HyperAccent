package com.hyperaccent.miuix.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccentNameMatcherTest {
    @Test
    public void miuixAccentResourcesMatch() {
        assertTrue(AccentNameMatcher.isAccent("miuix_default_color_primary_light"));
        assertTrue(AccentNameMatcher.isAccent("miuix_default_color_primary_dark"));
        assertTrue(AccentNameMatcher.isAccent("miuix_appcompat_accent_color_light"));
        assertTrue(AccentNameMatcher.isAccent("miuix_appcompat_accent_text"));
        assertTrue(AccentNameMatcher.isAccent("miuix_appcompat_primary_color_dark"));
        assertTrue(AccentNameMatcher.isAccent("miuix_appcompat_dialog_default_button_bg_primary_color_light"));
        assertTrue(AccentNameMatcher.isAccent("miuix_appcompat_progress_primary_color_light"));
        assertFalse(AccentNameMatcher.isAccent("miuix_appcompat_action_sheet_item_text_primary_color"));
        assertTrue(AccentNameMatcher.isAccent("miuix_color_blue_dark_level1"));
        assertTrue(AccentNameMatcher.isAccent("miuix_color_blue_light_level2"));
        assertTrue(AccentNameMatcher.isAccent("miuix_color_blue_light_primary_disable"));
        assertTrue(AccentNameMatcher.isAccent("miuix_color_blue_light_solid_level1"));
        assertFalse(AccentNameMatcher.isAccent("miuix_color_blue_solid_10"));
        assertFalse(AccentNameMatcher.isAccent("miuix_color_blue_solid_90"));
        assertFalse(AccentNameMatcher.isAccent("miuix_color_blue_dark_secondary_hover"));
        assertFalse(AccentNameMatcher.isAccent("miuix_color_blue_light_solid_level2"));
        assertTrue(AccentNameMatcher.isAccent("colorAccent"));
        assertTrue(AccentNameMatcher.isAccent("miuixAppcompatColorAccent"));
        assertTrue(AccentNameMatcher.isAccent("textColorAccent"));
        assertTrue(AccentNameMatcher.isAccent("colorControlActivated"));
        assertTrue(AccentNameMatcher.isAccent("theme_color"));
    }

    @Test
    public void nativeThemeColorsAreNotTouched() {
        assertFalse(AccentNameMatcher.isAccent("system_accent1_500"));
        assertFalse(AccentNameMatcher.isAccent("system_accent2_100"));
        assertFalse(AccentNameMatcher.isAccent("material_dynamic_primary40"));
        assertFalse(AccentNameMatcher.isAccent("settingslib_accent_primary_device_default"));
        assertFalse(AccentNameMatcher.isAccent("settingslib_colorAccentPrimary"));
        assertFalse(AccentNameMatcher.isAccent("sud_color_accent_glif_light"));
        assertFalse(AccentNameMatcher.isAccent("miuix_appcompat_dialog_default_button_text_color_light"));
        assertFalse(AccentNameMatcher.isAccent("miuix_default_color_on_primary_light"));
        assertFalse(AccentNameMatcher.isAccent("banner_accent_attention_high"));
        assertFalse(AccentNameMatcher.isAccent("preference_fallback_accent_color"));
        assertFalse(AccentNameMatcher.isAccent("sc_shield_accent"));
        assertFalse(AccentNameMatcher.isAccent("ringtone_auth_accent"));
        assertFalse(AccentNameMatcher.isAccent("accent_material_light"));
        assertFalse(AccentNameMatcher.isAccent("materialColorOnPrimaryContainer"));
        assertFalse(AccentNameMatcher.isAccent("status_bar_battery_performance"));
        assertFalse(AccentNameMatcher.isAccent("battery_indicator_performance"));
        assertFalse(AccentNameMatcher.isAccent("notification_primary_text_color_light"));
    }

    @Test
    public void excludedNamesAreProtectedFromValueMatching() {
        assertTrue(AccentNameMatcher.isExcluded("system_accent1_500"));
        assertTrue(AccentNameMatcher.isExcluded("settingslib_colorAccentPrimary"));
        assertTrue(AccentNameMatcher.isExcluded("banner_accent_attention_high"));
        assertTrue(AccentNameMatcher.isExcluded("sc_shield_accent"));
        assertFalse(AccentNameMatcher.isExcluded("miuix_default_color_primary_light"));
    }

    @Test
    public void nativeAccentRoles() {
        assertTrue(AccentNameMatcher.isNativeAccent("system_accent1_500"));
        assertTrue(AccentNameMatcher.isNativeAccent("system_accent3_200"));
        assertTrue(AccentNameMatcher.isNativeAccent("materialColorPrimary"));
        assertTrue(AccentNameMatcher.isNativeAccent("settingslib_accent_primary_device_default"));
        assertFalse(AccentNameMatcher.isNativeAccent("system_accent1_100"));
        assertFalse(AccentNameMatcher.isNativeAccent("materialColorSurface"));
        assertTrue(AccentNameMatcher.isNativeOnAccent("materialColorOnPrimary"));
    }

    @Test
    public void monetRolesAreSeparateFromSettingsLib() {
        assertTrue(AccentNameMatcher.isMonetAccent("system_accent1_500"));
        assertTrue(AccentNameMatcher.isMonetAccent("material_dynamic_primary40"));
        assertTrue(AccentNameMatcher.isMonetAccent("materialColorPrimary"));
        assertTrue(AccentNameMatcher.isMonetOnAccent("materialColorOnPrimary"));
        assertTrue(AccentNameMatcher.isMonetOnAccent("system_on_primary_light"));
        assertFalse(AccentNameMatcher.isMonetAccent("settingslib_accent_primary_device_default"));
        assertFalse(AccentNameMatcher.isMonetOnAccent(
                "settingslib_text_color_on_accent_device_default"));
    }

    @Test
    public void onAccentDetection() {
        assertTrue(AccentNameMatcher.isOnAccent("miuix_default_color_on_primary_light"));
        assertTrue(AccentNameMatcher.isOnAccent("miui_text_on_accent"));
        assertFalse(AccentNameMatcher.isOnAccent("miuix_default_color_on_primary_container_light"));
        assertFalse(AccentNameMatcher.isOnAccent("miuix_default_color_primary_light"));
        assertFalse(AccentNameMatcher.isOnAccent("notification_primary_text_color_light"));
        assertFalse(AccentNameMatcher.isOnAccent("materialColorOnPrimaryContainer"));
    }

    @Test
    public void contrastPicksBlackForYellowAndWhiteForBlue() {
        assertEquals(0xFF000000, AccentColorMath.contrastOn(0xFFFFC107));
        assertEquals(0xFFFFFFFF, AccentColorMath.contrastOn(0xFF3482FF));
        assertEquals(0xFFFFFFFF, AccentColorMath.contrastOn(0xFF7C4DFF));
    }

    @Test
    public void stockAccentValuesAreRecognized() {
        assertTrue(AccentColorMath.isStockMiuixAccent(0xFF3482FF));
        assertTrue(AccentColorMath.isStockMiuixAccent(0x4D4788FF));
        assertTrue(AccentColorMath.isStockMiuixAccent(0xFF277AF7));
        assertTrue(AccentColorMath.isStockMiuixAccent(0xFF0D84FF));
        assertFalse(AccentColorMath.isStockMiuixAccent(0xFFECF0FF));
        assertFalse(AccentColorMath.isStockMiuixAccent(0xFF17263D));
    }

    @Test
    public void alphaIsPreserved() {
        assertEquals(0x4D3482FF, AccentColorMath.withAlpha(0xFF3482FF, 0x4D));
        assertEquals(0x4D, AccentColorMath.defaultAlphaForName("miuix_color_blue_dark_primary_disable"));
        assertEquals(0xFF, AccentColorMath.defaultAlphaForName("miuix_color_blue_dark_level1"));
    }
}
