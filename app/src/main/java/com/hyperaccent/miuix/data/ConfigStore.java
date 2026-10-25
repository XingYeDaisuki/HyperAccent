package com.hyperaccent.miuix.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.hyperaccent.miuix.hook.AccentColorMath;
import com.hyperaccent.miuix.hook.ScopePackages;

import java.util.LinkedHashSet;
import java.util.Set;

/** Constants and persistence for the module settings. */
public final class ConfigStore {
    public static final String PREFS = "hyperaccent";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_ACCENT = "accent_color";
    public static final String KEY_CONTRAST = "contrast_text";
    public static final String KEY_NATIVE = "native_colors";
    public static final String KEY_NATIVE_MIGRATED = "native_colors_default_v2";
    public static final String KEY_PACKAGES = "enabled_packages";
    public static final String KEY_PACKAGES_MIGRATED = "packages_expanded_v15";
    /** Settings.System key used by hooked processes as a provider-free config path. */
    public static final String SYSTEM_CONFIG_KEY = "hyperaccent_config_v1";

    /** MIUI / Miuix default blue. */
    public static final int DEFAULT_ACCENT = 0xFF3482FF;

    private ConfigStore() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Creates the preference file on first launch. libxposed's remote preferences
     * need the module's prefs file to exist before a target process can read it.
     */
    public static void ensureDefaults(Context context) {
        SharedPreferences preferences = prefs(context);
        if (!preferences.contains(KEY_ACCENT)) {
            preferences.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putInt(KEY_ACCENT, DEFAULT_ACCENT)
                    .putBoolean(KEY_CONTRAST, true)
                    .putBoolean(KEY_NATIVE, true)
                    .putString(KEY_PACKAGES, ScopePackages.defaultPackageString())
                    .apply();
            return;
        }
        // v1.3 and earlier defaulted native colors to off. Most of the remaining
        // system surfaces (WiFi/Bluetooth lists, Gallery, Phone, Weather, ...) use
        // native accent roles, so enable it once for existing installs. The switch
        // remains available if the user wants pure-Miuix behavior.
        if (!preferences.getBoolean(KEY_NATIVE_MIGRATED, false)) {
            preferences.edit()
                    .putBoolean(KEY_NATIVE, true)
                    .putBoolean(KEY_NATIVE_MIGRATED, true)
                    .apply();
        }
        // v1.4 adds many packages (downloads, recorder, phone, interconnect, ...).
        // Add them to the existing allowlist once so they work out of the box.
        if (!preferences.getBoolean(KEY_PACKAGES_MIGRATED, false)) {
            Set<String> packages = decodePackages(preferences.getString(KEY_PACKAGES, null));
            if (packages.isEmpty()) {
                packages = ScopePackages.defaultEnabled();
            } else {
                packages.addAll(ScopePackages.ALL);
            }
            preferences.edit()
                    .putString(KEY_PACKAGES, encodePackages(packages))
                    .putBoolean(KEY_PACKAGES_MIGRATED, true)
                    .apply();
        }
    }

    public static int readAccent(Context context) {
        int color = prefs(context).getInt(KEY_ACCENT, DEFAULT_ACCENT);
        if (AccentColorMath.alpha(color) == 0) {
            color = AccentColorMath.opaque(color);
        }
        return color;
    }

    /**
     * A null/absent key means "first run", so every curated app is enabled. An empty
     * string means the user explicitly unchecked everything.
     */
    public static Set<String> readPackages(Context context) {
        String raw = prefs(context).getString(KEY_PACKAGES, null);
        if (raw == null) {
            return ScopePackages.defaultEnabled();
        }
        return decodePackages(raw);
    }

    public static void writePackages(Context context, Set<String> packages) {
        prefs(context).edit().putString(KEY_PACKAGES, encodePackages(packages)).apply();
    }

    /**
     * Publishes the config into Settings.System. Reading Settings.System needs no
     * permission in the hooked app, so this bypasses the ContentProvider caller-uid
     * issue on app processes where ActivityThread's op-package is still "android".
     *
     * @return true if the value was written (WRITE_SETTINGS granted).
     */
    public static boolean publishToSystem(
            Context context,
            boolean enabled,
            int accent,
            boolean contrast,
            boolean nativeColors,
            Set<String> packages
    ) {
        if (!Settings.System.canWrite(context)) {
            return false;
        }
        try {
            return Settings.System.putString(
                    context.getContentResolver(), SYSTEM_CONFIG_KEY,
                    buildSystemValue(enabled, accent, contrast, nativeColors, packages));
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Fallback for devices where WRITE_SETTINGS was not granted. Runs the standard
     * settings CLI through root, so all hooked processes can read Settings.System.
     */
    public static boolean publishToSystemViaRoot(
            boolean enabled,
            int accent,
            boolean contrast,
            boolean nativeColors,
            Set<String> packages
    ) {
        String value = buildSystemValue(enabled, accent, contrast, nativeColors, packages);
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                    "su", "-c",
                    "settings put system " + SYSTEM_CONFIG_KEY + " '" + value + "'"
            });
            return process.waitFor() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String buildSystemValue(
            boolean enabled,
            int accent,
            boolean contrast,
            boolean nativeColors,
            Set<String> packages
    ) {
        StringBuilder packageList = new StringBuilder();
        for (String pkg : packages) {
            if (pkg == null || pkg.isEmpty()) {
                continue;
            }
            if (packageList.length() > 0) {
                packageList.append(',');
            }
            packageList.append(pkg);
        }
        return "v1|" + (enabled ? 1 : 0)
                + "|" + (accent | 0xFF000000)
                + "|" + (contrast ? 1 : 0)
                + "|" + (nativeColors ? 1 : 0)
                + "|" + packageList;
    }

    public static String encodePackages(Set<String> packages) {
        StringBuilder builder = new StringBuilder();
        for (String pkg : packages) {
            if (pkg == null || pkg.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(pkg);
        }
        return builder.toString();
    }

    public static Set<String> decodePackages(String raw) {
        Set<String> result = new LinkedHashSet<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (String line : raw.split("\\r?\\n")) {
            String pkg = line.trim();
            if (!pkg.isEmpty()) {
                result.add(pkg);
            }
        }
        return result;
    }
}
