package com.hyperaccent.miuix.hook;

import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;

/**
 * libxposed API 102 entry point.
 *
 * <p>Hooks are installed lazily per managed package. The config is deliberately not
 * read in onPackageReady: at that point ActivityThread#getApplication() can still be
 * null, which would make a ContentResolver query from the system context fail with a
 * caller-uid mismatch. RuntimeConfig retries on the first actual resource lookup,
 * after the Application is attached.
 */
public final class ModernEntry extends XposedModule {
    private static final String TAG = "HyperAccent";

    private boolean systemServerHooked;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "module loaded in " + param.getProcessName()
                + " (api=" + getApiVersion() + ")");
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!param.isFirstPackage()) {
            return;
        }
        String pkg = param.getPackageName();
        if (!ScopePackages.isManaged(pkg)) {
            return;
        }
        RuntimeConfig runtimeConfig = new RuntimeConfig(this, pkg);
        new ResourceHooks(this, runtimeConfig, pkg).install(param.getClassLoader());
        if (ScopePackages.ANDROID.equals(pkg)) {
            systemServerHooked = true;
        }
    }

    @Override
    public void onSystemServerStarting(SystemServerStartingParam param) {
        if (systemServerHooked) {
            return;
        }
        systemServerHooked = true;
        RuntimeConfig runtimeConfig = new RuntimeConfig(this, ScopePackages.ANDROID);
        new ResourceHooks(this, runtimeConfig, ScopePackages.ANDROID).install(param.getClassLoader());
    }
}
