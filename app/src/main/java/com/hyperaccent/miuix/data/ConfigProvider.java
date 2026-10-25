package com.hyperaccent.miuix.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hyperaccent.miuix.BuildConfig;
import com.hyperaccent.miuix.hook.ScopePackages;

/**
 * Read-only bridge for the hooked processes.
 *
 * <p>libxposed's remote preferences are stored in the LSPosed database and are not
 * automatically populated by ordinary {@code SharedPreferences} writes from the
 * module UI. A tiny exported provider is the reliable way to share the handful of
 * settings values without adding the libxposed service dependency.
 */
public final class ConfigProvider extends ContentProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".prefs";
    public static final Uri CONFIG_URI = Uri.parse("content://" + AUTHORITY + "/config");

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder
    ) {
        if (getContext() == null) {
            return null;
        }
        MatrixCursor cursor = new MatrixCursor(new String[]{
                ConfigStore.KEY_ENABLED,
                ConfigStore.KEY_ACCENT,
                ConfigStore.KEY_CONTRAST,
                ConfigStore.KEY_NATIVE,
                ConfigStore.KEY_PACKAGES
        });
        ConfigStore.ensureDefaults(getContext());
        android.content.SharedPreferences prefs = ConfigStore.prefs(getContext());
        int enabled = prefs.getBoolean(ConfigStore.KEY_ENABLED, true) ? 1 : 0;
        int accent = ConfigStore.readAccent(getContext());
        int contrast = prefs.getBoolean(ConfigStore.KEY_CONTRAST, true) ? 1 : 0;
        int nativeColors = prefs.getBoolean(ConfigStore.KEY_NATIVE, true) ? 1 : 0;
        String packages = prefs.getString(ConfigStore.KEY_PACKAGES,
                ScopePackages.defaultPackageString());
        cursor.addRow(new Object[]{enabled, accent, contrast, nativeColors, packages});
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "vnd.android.cursor.item/hyperaccent-config";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(
            @NonNull Uri uri,
            @Nullable ContentValues values,
            @Nullable String selection,
            @Nullable String[] selectionArgs
    ) {
        return 0;
    }
}
