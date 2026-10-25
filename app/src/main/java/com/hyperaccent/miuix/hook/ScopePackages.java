package com.hyperaccent.miuix.hook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Package allowlist shared by the hook entry and the settings UI.
 *
 * <p>The module declares a static LSPosed scope with these packages, so the in-app
 * checklist immediately controls which of them are actually recolored. This avoids
 * requiring users to hunt through LSPosed's scope picker for File Manager, Calendar,
 * the launcher, etc.
 */
public final class ScopePackages {
    private ScopePackages() {
    }

    public static final String ANDROID = "android";

    public static final List<String> CORE = Collections.unmodifiableList(Arrays.asList(
            ANDROID,
            "com.android.systemui",
            "miui.systemui.plugin",
            "com.android.settings",
            "com.miui.securitycenter",
            "com.android.thememanager",
            "com.miui.home"
    ));

    public static final List<String> OPTIONAL = Collections.unmodifiableList(Arrays.asList(
            // Files / personal information
            "com.android.fileexplorer",
            "com.android.documentsui",
            "com.android.calendar",
            "com.android.contacts",
            "com.android.dialer",
            "com.android.incallui",
            "com.android.mms",
            "com.android.email",
            "com.android.deskclock",
            "com.android.quicksearchbox",
            "com.android.phone",
            "com.xiaomi.phone",
            // Media / tools
            "com.android.camera",
            "com.android.camera2",
            "com.android.gallery3d",
            "com.miui.gallery",
            "com.hyper.gallery.plugin",
            "com.miui.player",
            "com.miui.videoplayer",
            "com.miui.mediaeditor",
            "com.miui.weather2",
            "com.miui.notes",
            "com.miui.soundrecorder",
            "com.android.providers.downloads.ui",
            "com.android.providers.downloads",
            "com.miui.phone",
            "com.miui.screenrecorder",
            "com.miui.calculator",
            "com.miui.fm",
            // System apps
            "com.android.updater",
            "com.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.miui.securityadd",
            "com.miui.bugreport",
            "com.miui.cloudservice",
            "com.miui.cloudbackup",
            "com.miui.aod",
            "com.miui.personalassistant",
            "com.miui.virtualsim",
            "com.miui.tsmclient",
            "com.miui.voiceassist",
            "com.miui.accessibility",
            "com.miui.securitycore",
            "com.miui.cleanmaster",
            "com.miui.contentextension",
            "com.miui.contentcatcher",
            "com.miui.compass",
            "com.miui.miservice",
            "com.miui.screenshot",
            "com.miui.nextpay",
            "com.miui.notification",
            "com.android.soundrecorder",
            // Interconnect / find / smart home
            "com.milink.service",
            "com.miui.findmy",
            "com.xiaomi.smarthome",
            // Xiaomi services
            "com.xiaomi.market",
            "com.xiaomi.scanner",
            "com.xiaomi.mipicks",
            "com.xiaomi.gamecenter"
    ));

    public static final List<String> ALL;

    static {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        all.addAll(CORE);
        all.addAll(OPTIONAL);
        ALL = Collections.unmodifiableList(new ArrayList<>(all));
    }

    public static boolean isManaged(String packageName) {
        return ALL.contains(packageName);
    }

    public static Set<String> defaultEnabled() {
        return new LinkedHashSet<>(ALL);
    }

    public static String defaultPackageString() {
        StringBuilder builder = new StringBuilder();
        for (String pkg : ALL) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(pkg);
        }
        return builder.toString();
    }
}
