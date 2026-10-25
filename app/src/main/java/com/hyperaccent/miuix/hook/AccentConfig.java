package com.hyperaccent.miuix.hook;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable snapshot of the module configuration used by a target process. */
public final class AccentConfig {
    public static final int DEFAULT_ACCENT = 0xFF3482FF;
    /**
     * Fail closed: if the provider cannot be read, do not recolor system processes
     * unexpectedly.
     */
    public static final AccentConfig DEFAULT = new AccentConfig(
            false, DEFAULT_ACCENT, true, false, Collections.<String>emptySet());

    public final boolean enabled;
    public final int accent;
    public final boolean contrastText;
    public final boolean nativeColors;
    private final Set<String> enabledPackages;

    public AccentConfig(boolean enabled, int accent, boolean contrastText) {
        this(enabled, accent, contrastText, false, ScopePackages.defaultEnabled());
    }

    public AccentConfig(boolean enabled, int accent, boolean contrastText, Set<String> packages) {
        this(enabled, accent, contrastText, false, packages);
    }

    public AccentConfig(
            boolean enabled,
            int accent,
            boolean contrastText,
            boolean nativeColors,
            Set<String> packages
    ) {
        this.enabled = enabled;
        // Accent colors are always opaque; MIUI applies transparency through derived
        // resources and that alpha is preserved by resolve().
        this.accent = AccentColorMath.opaque(accent);
        this.contrastText = contrastText;
        this.nativeColors = nativeColors;
        this.enabledPackages = packages == null
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(packages));
    }

    public boolean isPackageEnabled(String packageName) {
        if (!enabled || packageName == null) {
            return false;
        }
        if (enabledPackages.contains(packageName)) {
            return true;
        }
        // HyperOS Gallery's detail UI is hosted by this plugin package. It is not
        // launchable, so it does not appear in the checklist; inherit the Gallery
        // toggle instead of leaving it disabled forever.
        return "com.hyper.gallery.plugin".equals(packageName)
                && enabledPackages.contains("com.miui.gallery");
    }

    public int enabledPackageCount() {
        return enabledPackages.size();
    }

    /**
     * @return replacement ARGB color, or {@code null} when this resource is not an
     *         accent resource and must be left untouched.
     */
    public Integer resolve(String resourceName, int originalColor, boolean hasOriginalColor) {
        if (!enabled) {
            return null;
        }

        boolean nativeAccent = nativeColors && AccentNameMatcher.isNativeAccent(resourceName);
        boolean nativeOnAccent = nativeColors && AccentNameMatcher.isNativeOnAccent(resourceName);
        boolean nativeContainer = nativeColors && AccentNameMatcher.isNativeContainer(resourceName);
        if (!nativeAccent && !nativeOnAccent && !nativeContainer
                && AccentNameMatcher.isExcluded(resourceName)) {
            return null;
        }

        if (nativeContainer) {
            boolean fifty = resourceName != null && resourceName.endsWith("_50");
            return AccentColorMath.withAlpha(accent, fifty ? 0x14 : 0x26);
        }
        if (AccentNameMatcher.isConnectedBackground(resourceName)) {
            boolean connected = resourceName != null
                    && resourceName.startsWith("miuix_preference_connect_bg_connected_color");
            int alpha = connected
                    ? 0xFF
                    : (hasOriginalColor ? AccentColorMath.alpha(originalColor) : 0x1A);
            return AccentColorMath.withAlpha(accent, alpha);
        }

        if (contrastText) {
            boolean onAccent = AccentNameMatcher.isOnAccent(resourceName) || nativeOnAccent;
            if (onAccent) {
                int alpha = hasOriginalColor ? AccentColorMath.alpha(originalColor) : 0xFF;
                if (alpha == 0) {
                    return null;
                }
                return AccentColorMath.withAlpha(AccentColorMath.contrastOn(accent), alpha);
            }
        }

        boolean namedAccent = AccentNameMatcher.isAccent(resourceName) || nativeAccent;
        boolean stockAccentValue = hasOriginalColor
                && AccentColorMath.isStockMiuixAccent(originalColor);
        if (!namedAccent && !stockAccentValue) {
            return null;
        }

        int alpha;
        if (hasOriginalColor) {
            alpha = AccentColorMath.alpha(originalColor);
            // A deliberately transparent resource should stay transparent.
            if (alpha == 0) {
                return null;
            }
        } else {
            alpha = AccentColorMath.defaultAlphaForName(resourceName);
        }
        return AccentColorMath.withAlpha(accent, alpha);
    }
}
