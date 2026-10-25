# HyperAccent

HyperAccent 是一个面向 **小米澎湃 OS（HyperOS）** 的 LSPosed 模块，用来替换系统和系统应用中的强调色：按钮、开关、进度条、选中态、音量条、悬浮按钮等。

模块只处理**通过 Android 资源表取色**的界面，不会修改壁纸、主题包、图标包、字体，也不会向系统分区推送文件。

## 功能

- **完整取色器**：8 个预设色，也支持任意 HEX 颜色。
- **保留透明度**：禁用态、浅色叠层、半透明背景会保留原 alpha，不会全部变成实心色。
- **自动文字对比色**：浅色强调色（黄色、薄荷色等）会自动把强调色上的文字/图标切成黑色。
- **同时修改原生取色**：可选覆盖 `system_accent*`、`materialColorPrimary`、`settingslib_accent*` 等原生强调角色；默认开启，关闭后只改 Miuix 资源。
- **应用级开关**：二级页面从系统读取图标，可逐个勾选生效应用，支持全选/全不选。
- **重启作用域**：像 HyperCeiler 一样用对话框选择要重启的进程，改色后不用手动找应用。
- **配置同步**：支持 `Settings.System` 和只读 ContentProvider 两条通道，避免目标进程读不到模块设置。

## 支持环境

| 项目 | 要求 |
| --- | --- |
| 系统 | HyperOS 1/2/3/4（Android 14–17）；MIUI 14 理论可用 |
| 框架 | LSPosed 2.1.0+，支持 libxposed API 102 |
| Root | 安装/启用 LSPosed 需要；重启系统进程也需要 |
| 构建 | JDK 21、Android SDK Platform 37、Build-Tools 37.0.0 |

模块使用 **静态作用域**，内置 60+ 个常用系统应用，包括系统框架、系统界面、控制中心插件、设置、安全中心、主题、桌面、文件管理、日历、联系人、电话、短信、时钟、相机、相册、音乐、视频、天气、笔记、录音、计算器、应用商店等。

LSPosed 2.1.0+ 通常会自动应用 `scope.list`；如果你的 LSPosed 分支不识别静态作用域，需要在 LSPosed 中手动勾选对应应用。

## 安装与使用

1. 从 [GitHub Releases](https://github.com/XingYeDaisuki/HyperAccent/releases) 下载并安装 `HyperAccent-1.5.2.apk`。
2. 在 LSPosed 中启用模块，确认作用域包含“系统界面”“系统桌面”“设置”等应用。
3. **重启手机**。首次启用必须重启，之后改色也需要重启对应进程。
4. 打开 HyperAccent：
   - 选择颜色，或输入 HEX；
   - 在“生效应用”里确认要改色的应用；
   - 点“应用当前颜色”。
5. 如果不想逐个重启，点“重启生效作用域”，勾选目标后一键重启。

> 系统桌面、文件管理、电话等进程在模块启用前就已经启动，必须重启进程后才会加载 Hook。普通 `force-stop` 对 `com.android.phone` 这类 persistent 进程无效，最稳妥的方式是重启手机。

### 修改系统设置权限

配置的主通道是 `Settings.System`。首次使用时，按页面提示授予“修改系统设置”权限，然后点“同步配置到系统设置”。

目标进程读取配置的顺序：

```
Settings.System (hyperaccent_config_v1)
        ↓
只读 ContentProvider
        ↓
libxposed Remote Preferences
```

读取失败时会按“默认禁用”处理，并在后续取色时每 500ms 重试，避免配置晚到导致永久使用默认蓝。

## 支持范围

会匹配并替换的 Miuix 资源包括：

```
miuix_default_color_primary_*
miuix_appcompat_accent_*
miuix_appcompat_primary_color_*
miuix_appcompat_floating_primary_color_*
miuix_appcompat_*primary*（排除 text / on_primary / surface / container）
miuix_color_blue_*_level1/2/3
miuix_color_blue_*_primary_*
colorAccent / textColorAccent / colorControlActivated
theme_color / miui_theme_color
```

开启“同时修改原生取色”后，还会覆盖：

```
system_accent1/2/3_{200,300,500,600,700}
material_dynamic_primary*
materialColorPrimary / Secondary / Tertiary
settingslib_accent_primary_device_default
settingslib_colorAccentPrimary
settingslib_dialog_accent
```

以及对应的 on-accent 文字色，例如 `materialColorOnPrimary`、`system_on_primary*`、`settingslib_text_color_on_accent_device_default`。

模块不写死资源 ID，而是按**资源 entry name** 匹配，再在资源解析层返回替换色；同时会把解析后的颜色与 Miuix 默认蓝色核心色板比对，覆盖“引用了派生资源、名字看不出来”的情况。

### 不会修改

```
system_accent*              # Android Monet（未开启原生开关时）
material*                   # Material / Material You（未开启原生开关时）
settingslib_*               # SettingsLib 原生强调色（未开启原生开关时）
sud_* / suw_*               # 设置向导
status_bar_battery_*        # 电池语义色
battery_indicator_*         # 电池语义色
banner_accent_attention_*   # 警告级别颜色
preference_fallback_accent_color
sc_shield_accent*
ringtone_auth_accent
```

## 已知限制

- **只覆盖资源型颜色。** Compose 里直接写死的 `Color(0xFF...)`、Java 里 `Color.parseColor()` 常量、图片里的像素都不会被重绘。
- **Flutter 页面不支持。** 例如相册的设置页：相册 APK 是 `android:hasCode="false"`，界面由 `libapp.so` + `flutter_assets` 渲染，不经过 `Resources.getColor` / `TypedArray`，LSPosed 没有 Java 入口可 Hook。这页的开关和图标会保持原色，不是作用域没勾选。
- **电话页的自绘图标不处理。** `com.android.phone` 的移动数据开关、余额等资源色会跟随模块；SIM 卡自绘芯片属于图标/绘制层，保持原样。
- **“原生取色”不是完整 Material 调色板。** 只处理系统原生强调主色阶，深色/浅色容器仍是系统原来的色调。
- **作用域是静态的。** 列表外的第三方应用需要修改 `ScopePackages` 和 `scope.list` 后重新构建。
- **持久化进程需要重启手机。** `com.android.phone` 等 persistent 进程不能被普通 `force-stop` 杀掉。

## 构建

```bash
# JDK 21 + Android SDK Platform 37 + Build-Tools 37.0.0
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

Windows：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

产物：

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

`keystore/debug.keystore` 是公开的调试签名（password 均为 `android`，alias 为 `androiddebugkey`），只适合本地安装/测试。正式发布请替换成自己的签名配置。

### 构建环境

- AGP `9.4.1` + Gradle `9.8.2` + Kotlin `2.4.21`。
- Miuix `0.9.4` 由 Kotlin 2.4 / Compose 1.12 构建，因此 `gradle.properties` 中设置：

  ```properties
  android.builtInKotlin=false
  android.newDsl=false
  ```

- libxposed API：

  ```kotlin
  compileOnly("io.github.libxposed:api:102.0.0")
  ```

- 模块元数据：

  ```
  app/src/main/resources/META-INF/xposed/module.prop
  app/src/main/resources/META-INF/xposed/java_init.list
  app/src/main/resources/META-INF/xposed/scope.list
  ```

## 工作原理

### Hook 链

```
MiuiResourcesImpl.getThemeInt(int id)
        ├─ TypedArray 加载时的颜色覆盖
        └─ Resources.getValue 单值覆盖

Resources.getColor(int[, Theme])
Resources.getColorStateList(int[, Theme])

TypedArray.getColor(int, int)
TypedArray.getColorStateList(int[, Theme])

Resources.Theme.resolveAttribute(int, TypedValue, boolean)
```

每条路径都会解析 `id -> entryName`，再用 `AccentNameMatcher` 判断是否属于强调色角色；命中后替换颜色，但保留原 alpha。`ColorStateList` 会保留 state 数组，避免 pressed/disabled 状态丢失。

### 配置传递

Hook 进程通过两个通道读取模块设置：

- **Settings.System**：`hyperaccent_config_v1`，主通道；
- **只读 Provider**：`content://com.hyperaccent.miuix.prefs/config`，只暴露 `enabled / accent_color / contrast_text / native_colors / enabled_packages`，不提供写入。

模块内部通过 `Application.attach/onCreate`、`Activity.onCreate` 等时机懒加载配置，并在首次取色失败时重试。

### 为什么必须重启

资源颜色在加载 / TypedArray 解析时会被缓存，已经创建的 View 也持有旧颜色。修改配置后：

- 重启 `com.android.systemui`：状态栏、控制中心、音量条；
- 重启 `com.android.settings`：设置页；
- 重启 `com.miui.home`：桌面、文件夹弹窗；
- 重启 `com.android.fileexplorer`：文件管理；
- 改到 `android`（system_server）相关对话框时，最稳妥是重启手机。

## 排查

### 看日志

LSPosed → 日志，过滤 `HyperAccent`。Debug 版会输出：

```
[HyperAccent] module loaded in com.miui.home (api=102)
[HyperAccent] config loaded from settings: enabled=true accent=#FFFF7496 contrast=true native=true packages=51
[HyperAccent] [com.miui.home] installed resource hook(s) for com.miui.home
[HyperAccent] [com.miui.home] accent miuix_default_color_primary_light -> #FFFF7496
```

### 桌面/设置不生效

1. 确认 LSPosed 2.1.0+ 已启用模块；
2. 确认“生效应用”里对应应用已勾选；
3. 点“重启生效作用域”重启对应进程，或重启手机；
4. 看日志里是否出现 `installed resource hook(s)`；
5. 如果完全没有 `[HyperAccent]` 行，检查 LSPosed 是否把模块注入了该进程，以及静态作用域是否生效。

### 相册设置页不变色

这是 Flutter 页面，属于已知限制，不在资源 Hook 的能力范围内。具体原因见“已知限制”。

### 原生 Material You 没变

如果“同时修改原生取色”已关闭，原生资源会被刻意跳过；打开开关并重启对应进程后再试。模块不会重建完整 Material 调色板。

## 代码结构

```
app/src/main/
├── java/com/hyperaccent/miuix/
│   ├── hook/
│   │   ├── ModernEntry.java        # libxposed API 102 入口
│   │   ├── ResourceHooks.java      # 资源 Hook 链
│   │   ├── AccentNameMatcher.java  # 资源名匹配
│   │   ├── AccentColorMath.java    # alpha / 对比色
│   │   ├── AccentConfig.java       # 配置快照 + 应用白名单
│   │   ├── RuntimeConfig.java      # Settings/Provider/Remote Prefs 读取
│   │   └── ScopePackages.java      # 静态作用域 / 默认应用列表
│   ├── data/
│   │   ├── ConfigStore.java        # 本地配置与同步
│   │   └── ConfigProvider.java     # 只读配置桥
│   └── ui/MainActivity.kt          # 主页面 / 应用二级页 / 重启对话框
└── resources/META-INF/xposed/       # module.prop / scope.list / java_init.list
```

单元测试覆盖名称匹配、原生资源排除/开关、透明度、对比色、应用白名单：

```bash
./gradlew :app:testDebugUnitTest
```

## 致谢

- [Miuix](https://github.com/compose-miuix-ui/miuix)：设置界面组件库。
- [libxposed](https://github.com/libxposed/api)：现代 Xposed API。
- [LSPosed](https://github.com/LSPosed/LSPosed)：Xposed 框架。

## License

[MIT](LICENSE)
