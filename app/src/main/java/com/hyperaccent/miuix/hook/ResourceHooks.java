package com.hyperaccent.miuix.hook;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.TypedValue;

import com.hyperaccent.miuix.BuildConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface.Chain;
import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedModule;

/**
 * Replaces MIUI / Miuix accent colors at the resource layer.
 *
 * <p>Why several hooks?
 * <ul>
 *   <li>{@code MiuiResourcesImpl.getThemeInt} is the path MIUI uses to apply
 *       theme_values.xml colors, including typed-array inflation.</li>
 *   <li>{@code Resources.getColor/ColorStateList} covers direct resource lookups.</li>
 *   <li>{@code TypedArray.getColor/ColorStateList} covers
 *       {@code ?attr/colorAccent} style resolution.</li>
 *   <li>{@code Resources.Theme.resolveAttribute} covers callers that read
 *       {@link TypedValue#data} directly.</li>
 * </ul>
 */
final class ResourceHooks {
    private static final String TAG = "HyperAccent";
    private static final int MAX_LOG_ENTRIES = 80;

    private static final Map<Class<?>, Method> ENTRY_NAME_METHODS = new ConcurrentHashMap<>();
    private static final Map<Object, android.util.SparseArray<String>> ENTRY_NAME_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<String, Boolean> LOGGED = new ConcurrentHashMap<>();
    private static final AtomicInteger LOG_COUNT = new AtomicInteger();

    private static Field sTypedArrayResources;

    private final XposedModule module;
    private final RuntimeConfig runtimeConfig;
    private final String packageName;

    ResourceHooks(XposedModule module, RuntimeConfig runtimeConfig, String packageName) {
        this.module = module;
        this.runtimeConfig = runtimeConfig;
        this.packageName = packageName;
    }

    void install(ClassLoader classLoader) {
        int hooks = 0;

        try {
            Class<?> typedArray = findClass("android.content.res.TypedArray", classLoader);
            try {
                sTypedArrayResources = findField(typedArray, "mResources");
                sTypedArrayResources.setAccessible(true);
            } catch (Throwable ignored) {
                sTypedArrayResources = null;
            }
            hooks += hookTypedArray(typedArray);
        } catch (Throwable t) {
            logError("TypedArray hooks failed", t);
        }

        try {
            Class<?> resources = findClass("android.content.res.Resources", classLoader);
            hooks += hookResources(resources);
        } catch (Throwable t) {
            logError("Resources hooks failed", t);
        }

        try {
            Class<?> theme = findClass("android.content.res.Resources$Theme", classLoader);
            hooks += hookTheme(theme);
        } catch (Throwable t) {
            logError("Theme hook failed", t);
        }

        // Application.attach() is the earliest point where the app Context exists.
        // Load the provider config here so the first UI resource lookup already has
        // the custom color instead of waiting for a retry.
        try {
            Class<?> application = findClass("android.app.Application", classLoader);
            Method attach = findMethod(application, "attach", Context.class);
            if (attach != null) {
                hook(attach, chain -> {
                    Object result = chain.proceed();
                    runtimeConfig.get();
                    return result;
                });
                hooks++;
            }
            Method onCreate = findMethod(application, "onCreate");
            if (onCreate != null) {
                hook(onCreate, chain -> {
                    Object result = chain.proceed();
                    runtimeConfig.get();
                    return result;
                });
                hooks++;
            }
        } catch (Throwable t) {
            logError("Application.attach hook failed", t);
        }

        // By Activity.onCreate the ActivityThread op-package is the target app, so
        // ContentResolver.query carries the correct calling package.
        try {
            Method activityOnCreate = findMethod(Activity.class, "onCreate", android.os.Bundle.class);
            if (activityOnCreate != null) {
                hook(activityOnCreate, chain -> {
                    Object result = chain.proceed();
                    runtimeConfig.get();
                    return result;
                });
                hooks++;
            }
        } catch (Throwable t) {
            logError("Activity.onCreate hook failed", t);
        }

        try {
            Class<?> miuiImpl = findClass("android.content.res.MiuiResourcesImpl", classLoader);
            Method getThemeInt = findMethod(miuiImpl, "getThemeInt", int.class);
            if (getThemeInt != null) {
                hook(getThemeInt, chain -> {
                    int id = (Integer) chain.getArg(0);
                    Object result = chain.proceed();
                    String name = resourceName(chain.getThisObject(), id);
                    Integer replacement = resolveColor(
                            name,
                            result instanceof Integer ? (Integer) result : 0,
                            result instanceof Integer
                    );
                    if (replacement != null) {
                        logOverride(name, replacement);
                        return replacement;
                    }
                    return result;
                });
                hooks++;
            }
        } catch (Throwable ignored) {
            // MiuiResourcesImpl is absent on AOSP-like builds; other hooks still work.
        }

        logInfo("installed " + hooks + " resource hook(s) for " + packageName);
    }

    private AccentConfig activeConfig() {
        AccentConfig config = runtimeConfig.get();
        return config.isPackageEnabled(packageName) ? config : null;
    }

    private Integer resolveColor(String name, int color, boolean hasColor) {
        AccentConfig config = activeConfig();
        return config == null ? null : config.resolve(name, color, hasColor);
    }

    private int hookTheme(Class<?> themeClass) {
        int count = 0;
        for (Method method : themeClass.getDeclaredMethods()) {
            if (!method.getName().equals("resolveAttribute") || method.getParameterTypes().length != 3) {
                continue;
            }
            hook(method, chain -> {
                Object result = chain.proceed();
                if (!Boolean.TRUE.equals(result)) {
                    return result;
                }
                Object valueObject = chain.getArg(1);
                if (!(valueObject instanceof TypedValue)) {
                    return result;
                }
                TypedValue value = (TypedValue) valueObject;
                if (value.type < TypedValue.TYPE_FIRST_COLOR_INT
                        || value.type > TypedValue.TYPE_LAST_COLOR_INT) {
                    return result;
                }
                String name = null;
                if (value.resourceId != 0) {
                    Resources.Theme theme = (Resources.Theme) chain.getThisObject();
                    name = resourceName(theme.getResources(), value.resourceId);
                }
                Integer replacement = resolveColor(name, value.data, true);
                if (replacement != null) {
                    value.data = replacement;
                    logOverride(name, replacement);
                }
                return result;
            });
            count++;
        }
        return count;
    }

    private int hookResources(Class<?> resources) {
        int count = 0;
        for (Method method : resources.getDeclaredMethods()) {
            if (!method.getName().equals("getColor") && !method.getName().equals("getColorStateList")) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0 || parameterTypes[0] != int.class) {
                continue;
            }
            hook(method, chain -> {
                int id = (Integer) chain.getArg(0);
                Object result = chain.proceed();
                if (result == null) {
                    return null;
                }
                String name = resourceName(chain.getThisObject(), id);
                if (result instanceof Integer) {
                    Integer replacement = resolveColor(name, (Integer) result, true);
                    if (replacement != null) {
                        logOverride(name, replacement);
                        return replacement;
                    }
                } else if (result instanceof ColorStateList) {
                    ColorStateList mapped = mapColorStateList((ColorStateList) result, name);
                    if (mapped != null) {
                        logOverride(name, mapped.getDefaultColor());
                        return mapped;
                    }
                }
                return result;
            });
            count++;
        }
        return count;
    }

    private int hookTypedArray(Class<?> typedArray) {
        int count = 0;
        for (Method method : typedArray.getDeclaredMethods()) {
            if (!method.getName().equals("getColor") && !method.getName().equals("getColorStateList")) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0 || parameterTypes[0] != int.class) {
                continue;
            }
            hook(method, chain -> {
                TypedArray array = (TypedArray) chain.getThisObject();
                int index = (Integer) chain.getArg(0);
                Object result = chain.proceed();
                if (result == null) {
                    return null;
                }
                int id = array.getResourceId(index, 0);
                String name = null;
                if (id != 0) {
                    Object resources = typedArrayResources(array);
                    if (resources != null) {
                        name = resourceName(resources, id);
                    }
                }
                if (result instanceof Integer) {
                    Integer replacement = resolveColor(name, (Integer) result, true);
                    if (replacement != null) {
                        logOverride(name, replacement);
                        return replacement;
                    }
                } else if (result instanceof ColorStateList) {
                    ColorStateList mapped = mapColorStateList((ColorStateList) result, name);
                    if (mapped != null) {
                        logOverride(name, mapped.getDefaultColor());
                        return mapped;
                    }
                }
                return result;
            });
            count++;
        }
        return count;
    }

    private void hook(Method method, io.github.libxposed.api.XposedInterface.Hooker hooker) {
        try {
            module.hook(method)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(hooker);
        } catch (Throwable t) {
            logError("failed to hook " + method, t);
        }
    }

    private ColorStateList mapColorStateList(ColorStateList original, String name) {
        if (original == null) {
            return null;
        }
        AccentConfig config = activeConfig();
        if (config == null) {
            return null;
        }
        int defaultColor = original.getDefaultColor();
        Integer mappedDefault = config.resolve(name, defaultColor, true);

        // getColors()/getStates() are public @hide and were removed from the public
        // android.jar in API 37. Reflection works in LSPosed's runtime; if it is
        // blocked, fall back to a single-color state list so the accent still applies.
        int[] colors = readIntArray(original, "getColors");
        int[][] states = readStates(original, "getStates");
        if (colors == null || colors.length == 0 || states == null) {
            return mappedDefault == null ? null : ColorStateList.valueOf(mappedDefault);
        }
        int[] replacement = new int[colors.length];
        boolean changed = false;
        for (int i = 0; i < colors.length; i++) {
            Integer color = config.resolve(name, colors[i], true);
            if (color == null) {
                replacement[i] = colors[i];
            } else {
                replacement[i] = color;
                changed = true;
            }
        }
        if (!changed) {
            return null;
        }
        if (states.length != replacement.length) {
            return ColorStateList.valueOf(replacement[0]);
        }
        try {
            return new ColorStateList(states, replacement);
        } catch (Throwable t) {
            return ColorStateList.valueOf(replacement[0]);
        }
    }

    private static int[] readIntArray(ColorStateList list, String methodName) {
        try {
            Method method = list.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object value = method.invoke(list);
            return value instanceof int[] ? (int[]) value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int[][] readStates(ColorStateList list, String methodName) {
        try {
            Method method = list.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object value = method.invoke(list);
            return value instanceof int[][] ? (int[][]) value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object typedArrayResources(TypedArray array) {
        if (sTypedArrayResources == null) {
            return null;
        }
        try {
            return sTypedArrayResources.get(array);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String resourceName(Object resources, int id) {
        if (resources == null || id == 0) {
            return null;
        }
        android.util.SparseArray<String> cache;
        synchronized (ENTRY_NAME_CACHE) {
            cache = ENTRY_NAME_CACHE.get(resources);
            if (cache == null) {
                cache = new android.util.SparseArray<>();
                ENTRY_NAME_CACHE.put(resources, cache);
            }
        }
        synchronized (cache) {
            String cached = cache.get(id);
            if (cached != null) {
                return cached.isEmpty() ? null : cached;
            }
            String name = null;
            try {
                Method method = ENTRY_NAME_METHODS.get(resources.getClass());
                if (method == null) {
                    method = resources.getClass().getMethod("getResourceEntryName", int.class);
                    method.setAccessible(true);
                    ENTRY_NAME_METHODS.put(resources.getClass(), method);
                }
                name = (String) method.invoke(resources, id);
            } catch (Throwable ignored) {
                name = null;
            }
            cache.put(id, name == null ? "" : name);
            return name;
        }
    }

    private static Class<?> findClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
        if (classLoader != null) {
            return Class.forName(name, false, classLoader);
        }
        return Class.forName(name);
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... parameters) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private void logOverride(String name, int color) {
        if (!BuildConfig.DEBUG || name == null) {
            return;
        }
        if (LOG_COUNT.get() >= MAX_LOG_ENTRIES || LOGGED.put(name, Boolean.TRUE) != null) {
            return;
        }
        LOG_COUNT.incrementAndGet();
        logInfo("accent " + name + " -> #" + String.format("%08X", color));
    }

    private void logInfo(String message) {
        module.log(Log.INFO, TAG, "[" + packageName + "] " + message);
    }

    private void logError(String message, Throwable t) {
        module.log(Log.ERROR, TAG, "[" + packageName + "] " + message, t);
    }
}
