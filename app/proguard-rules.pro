# Keep the libxposed entry and hook classes.
-keep class com.hyperaccent.miuix.hook.ModernEntry { *; }
-keep class com.hyperaccent.miuix.hook.** { *; }
-keep class com.hyperaccent.miuix.data.** { *; }

# libxposed API is provided by the framework at runtime.
-dontwarn io.github.libxposed.**
