package com.hyperaccent.miuix.hook;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.hyperaccent.miuix.data.ConfigProvider;
import com.hyperaccent.miuix.data.ConfigStore;

import java.lang.reflect.Method;
import java.util.Set;

import io.github.libxposed.api.XposedModule;

/**
 * Loads the config for one hooked process.
 *
 * <p>Config loading is lazy: it runs on the first resource lookup, after the target
 * Application is attached. That avoids using the system context (package "android")
 * for a query whose caller UID belongs to another app, which Android rejects with
 * "calling package android does not match caller's uid".
 */
public final class RuntimeConfig {
    private static final String TAG = "HyperAccent";
    private static final long RETRY_DELAY_MS = 500L;

    private final XposedModule module;
    private final String packageName;
    private volatile AccentConfig config;
    private volatile long nextRetryAt;
    private boolean loadFailureLogged;

    public RuntimeConfig(XposedModule module, String packageName) {
        this.module = module;
        this.packageName = packageName;
    }

    public AccentConfig get() {
        AccentConfig current = config;
        if (current != null) {
            return current;
        }
        if (System.currentTimeMillis() < nextRetryAt) {
            return AccentConfig.DEFAULT;
        }
        synchronized (this) {
            current = config;
            if (current != null) {
                return current;
            }
            current = load();
            if (current != null) {
                config = current;
                return current;
            }
            nextRetryAt = System.currentTimeMillis() + RETRY_DELAY_MS;
            return AccentConfig.DEFAULT;
        }
    }

    private AccentConfig load() {
        AccentConfig systemConfig = loadFromSystemSettings();
        if (systemConfig != null) {
            logLoaded("Settings.System", systemConfig);
            return systemConfig;
        }

        AccentConfig providerConfig = loadFromProvider();
        if (providerConfig != null) {
            logLoaded("provider", providerConfig);
            return providerConfig;
        }

        AccentConfig remoteConfig = loadFromRemotePreferences();
        if (remoteConfig != null) {
            logLoaded("remote preferences", remoteConfig);
            return remoteConfig;
        }

        if (!loadFailureLogged) {
            loadFailureLogged = true;
            module.log(Log.WARN, TAG, "config unavailable for " + packageName
                    + "; will retry, accent replacement disabled for now");
        }
        return null;
    }

    private void logLoaded(String source, AccentConfig loaded) {
        module.log(Log.INFO, TAG, "config loaded from " + source
                + ": enabled=" + loaded.enabled
                + " accent=#" + String.format("%08X", loaded.accent)
                + " contrast=" + loaded.contrastText
                + " native=" + loaded.nativeColors
                + " packages=" + loaded.enabledPackageCount());
    }

    private AccentConfig loadFromSystemSettings() {
        Context context = currentApplication();
        if (context == null) {
            context = packageContext();
        }
        if (context == null) {
            return null;
        }
        try {
            String raw = Settings.System.getString(
                    context.getContentResolver(), ConfigStore.SYSTEM_CONFIG_KEY);
            if (raw == null) {
                return null;
            }
            String[] parts = raw.split("\\|", 6);
            if (parts.length != 6 || !"v1".equals(parts[0])) {
                return null;
            }
            boolean enabled = "1".equals(parts[1]);
            int accent = Integer.parseInt(parts[2]);
            boolean contrast = "1".equals(parts[3]);
            boolean nativeColors = "1".equals(parts[4]);
            Set<String> packages = new java.util.LinkedHashSet<>();
            if (!parts[5].isEmpty()) {
                for (String pkg : parts[5].split(",")) {
                    if (!pkg.isEmpty()) {
                        packages.add(pkg);
                    }
                }
            }
            return new AccentConfig(enabled, accent, contrast, nativeColors, packages);
        } catch (Throwable t) {
            return null;
        }
    }

    private AccentConfig loadFromProvider() {
        Context context = currentApplication();
        if (context != null && !packageName.equals(context.getPackageName())) {
            try {
                context = context.createPackageContext(packageName, 0);
            } catch (Throwable ignored) {
                // Fall through to the system-context helper below.
                context = null;
            }
        }
        if (context == null) {
            context = packageContext();
        }
        if (context == null) {
            return null;
        }
        Cursor cursor = null;
        try {
            Uri uri = ConfigProvider.CONFIG_URI;
            cursor = context.getContentResolver().query(uri, new String[]{
                    ConfigStore.KEY_ENABLED,
                    ConfigStore.KEY_ACCENT,
                    ConfigStore.KEY_CONTRAST,
                    ConfigStore.KEY_NATIVE,
                    ConfigStore.KEY_PACKAGES
            }, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            boolean enabled = cursor.getInt(0) != 0;
            int accent = cursor.getInt(1);
            boolean contrast = cursor.getInt(2) != 0;
            boolean nativeColors = cursor.getInt(3) != 0;
            String rawPackages = cursor.getString(4);
            Set<String> packages = rawPackages == null
                    ? ScopePackages.defaultEnabled()
                    : ConfigStore.decodePackages(rawPackages);
            return new AccentConfig(enabled, accent, contrast, nativeColors, packages);
        } catch (Throwable t) {
            module.log(Log.WARN, TAG, "provider config read failed for " + packageName
                    + " (context=" + context.getPackageName() + "): " + t);
            return null;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private AccentConfig loadFromRemotePreferences() {
        try {
            SharedPreferences prefs = module.getRemotePreferences(ConfigStore.PREFS);
            if (prefs == null || !prefs.contains(ConfigStore.KEY_ACCENT)) {
                return null;
            }
            boolean enabled = prefs.getBoolean(ConfigStore.KEY_ENABLED, true);
            int accent = prefs.getInt(ConfigStore.KEY_ACCENT, ConfigStore.DEFAULT_ACCENT);
            boolean contrast = prefs.getBoolean(ConfigStore.KEY_CONTRAST, true);
            boolean nativeColors = prefs.getBoolean(ConfigStore.KEY_NATIVE, true);
            String rawPackages = prefs.getString(ConfigStore.KEY_PACKAGES, null);
            Set<String> packages = rawPackages == null
                    ? ScopePackages.defaultEnabled()
                    : ConfigStore.decodePackages(rawPackages);
            return new AccentConfig(enabled, accent, contrast, nativeColors, packages);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Context currentApplication() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object activityThread = currentActivityThread.invoke(null);
            if (activityThread == null) {
                return null;
            }
            Method getApplication = activityThreadClass.getDeclaredMethod("getApplication");
            getApplication.setAccessible(true);
            Object application = getApplication.invoke(activityThread);
            return application instanceof Context ? (Context) application : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private Context packageContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object activityThread = currentActivityThread.invoke(null);
            if (activityThread == null) {
                return null;
            }
            Method getSystemContext = activityThread.getClass().getDeclaredMethod("getSystemContext");
            getSystemContext.setAccessible(true);
            Object systemContext = getSystemContext.invoke(activityThread);
            if (!(systemContext instanceof Context)) {
                return null;
            }
            if (ScopePackages.ANDROID.equals(packageName)) {
                return (Context) systemContext;
            }
            return ((Context) systemContext).createPackageContext(packageName, 0);
        } catch (Throwable t) {
            return null;
        }
    }
}
