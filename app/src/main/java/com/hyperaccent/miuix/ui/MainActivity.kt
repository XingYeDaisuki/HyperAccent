package com.hyperaccent.miuix.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperaccent.miuix.data.ConfigStore
import com.hyperaccent.miuix.hook.AccentColorMath
import com.hyperaccent.miuix.hook.ScopePackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.ColorSpace
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.SwitchColors
import top.yukonga.miuix.kmp.basic.SwitchDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.window.WindowDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConfigStore.ensureDefaults(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        // HyperOS otherwise paints a translucent scrim behind the gesture pill.
        window.isNavigationBarContrastEnforced = false
        window.navigationBarColor = AndroidColor.TRANSPARENT
        window.statusBarColor = AndroidColor.TRANSPARENT
        setContent {
            MiuixTheme(controller = remember { ThemeController(ColorSchemeMode.System) }) {
                SettingsScreen()
            }
        }
    }
}

private data class AccentPreset(val name: String, val color: Color)

private data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

private data class RestartTarget(val packageName: String, val label: String)

private val PRESETS = listOf(
    AccentPreset("经典蓝", Color(0xFF3482FF)),
    AccentPreset("青碧", Color(0xFF00BCD4)),
    AccentPreset("薄荷", Color(0xFF36D167)),
    AccentPreset("紫罗兰", Color(0xFF7C4DFF)),
    AccentPreset("樱粉", Color(0xFFE91E63)),
    AccentPreset("落日橙", Color(0xFFFF6D00)),
    AccentPreset("琥珀", Color(0xFFFFC107)),
    AccentPreset("赤红", Color(0xFFF44336)),
)

private val RESTART_TARGETS = listOf(
    RestartTarget("com.android.systemui", "系统界面"),
    RestartTarget("com.miui.home", "系统桌面"),
    RestartTarget("com.android.settings", "设置 / WiFi / 蓝牙"),
    RestartTarget("com.android.fileexplorer", "文件管理"),
    RestartTarget("com.miui.securitycenter", "安全中心"),
    RestartTarget("com.miui.gallery", "相册"),
    RestartTarget("com.hyper.gallery.plugin", "相册插件"),
    RestartTarget("com.miui.weather2", "天气"),
    RestartTarget("com.android.deskclock", "时钟"),
    RestartTarget("com.miui.calculator", "计算器"),
    RestartTarget("com.android.phone", "电话服务"),
    RestartTarget("com.android.incallui", "来电界面"),
    RestartTarget("com.xiaomi.phone", "电话(小米)"),
    RestartTarget("com.milink.service", "设备互联"),
    RestartTarget("com.miui.findmy", "查找设备"),
    RestartTarget("com.android.providers.downloads.ui", "下载管理"),
    RestartTarget("com.android.soundrecorder", "录音机"),
)

@Composable
private fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { ConfigStore.prefs(context) }

    var enabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(ConfigStore.KEY_ENABLED, true))
    }
    var contrastText by rememberSaveable {
        mutableStateOf(prefs.getBoolean(ConfigStore.KEY_CONTRAST, true))
    }
    var nativeColors by rememberSaveable {
        mutableStateOf(prefs.getBoolean(ConfigStore.KEY_NATIVE, true))
    }
    var accent by remember {
        mutableStateOf(Color(ConfigStore.readAccent(context)))
    }
    var hex by remember {
        mutableStateOf(accent.toArgb().toHexRgb())
    }
    var enabledPackages by remember {
        mutableStateOf(ConfigStore.readPackages(context))
    }
    val installedApps = remember { loadInstalledApps(context) }
    var status by remember { mutableStateOf<String?>(null) }
    var showAppList by rememberSaveable { mutableStateOf(false) }
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var restartSelection by remember {
        mutableStateOf(RESTART_TARGETS.map { it.packageName }.toSet())
    }
    val scope = rememberCoroutineScope()

    fun persist(
        newEnabled: Boolean = enabled,
        newAccent: Color = accent,
        newContrast: Boolean = contrastText,
        newNative: Boolean = nativeColors,
        newPackages: Set<String> = enabledPackages,
    ) {
        ConfigStore.prefs(context).edit()
            .putBoolean(ConfigStore.KEY_ENABLED, newEnabled)
            .putInt(ConfigStore.KEY_ACCENT, newAccent.toArgb() or 0xFF000000.toInt())
            .putBoolean(ConfigStore.KEY_CONTRAST, newContrast)
            .putBoolean(ConfigStore.KEY_NATIVE, newNative)
            .putString(ConfigStore.KEY_PACKAGES, ConfigStore.encodePackages(newPackages))
            .apply()
        val published = ConfigStore.publishToSystem(
            context,
            newEnabled,
            newAccent.toArgb() or 0xFF000000.toInt(),
            newContrast,
            newNative,
            newPackages,
        )
        if (!published) {
            status = "部分应用需要“写入系统设置”权限，请点下方授权后再应用"
        }
    }

    fun setAccent(color: Color, updateHex: Boolean = true) {
        accent = Color(color.toArgb() or 0xFF000000.toInt())
        if (updateHex) hex = accent.toArgb().toHexRgb()
        persist(newAccent = accent)
    }

    fun togglePackage(packageName: String, checked: Boolean) {
        val next = if (checked) enabledPackages + packageName else enabledPackages - packageName
        enabledPackages = next
        persist(newPackages = next)
    }

    fun publishConfiguration() {
        val currentAccent = accent.toArgb() or 0xFF000000.toInt()
        val currentPackages = enabledPackages
        val currentEnabled = enabled
        val currentContrast = contrastText
        val currentNative = nativeColors
        scope.launch {
            val synced = withContext(Dispatchers.IO) {
                ConfigStore.publishToSystem(
                    context, currentEnabled, currentAccent, currentContrast, currentNative, currentPackages
                ) || ConfigStore.publishToSystemViaRoot(
                    currentEnabled, currentAccent, currentContrast, currentNative, currentPackages
                )
            }
            status = if (synced) {
                "配置已同步，重启应用后生效"
            } else {
                "同步失败：请在设置中授予“修改系统设置”或允许 Root"
            }
        }
    }

    BackHandler(enabled = showAppList) {
        showAppList = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Header(accent)
            Spacer(Modifier.height(12.dp))

            if (!Settings.System.canWrite(context)) {
                Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                    Text(
                        text = "还需要“修改系统设置”权限",
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "天气、时钟、计算器等应用要通过系统设置读取配置，否则可能继续显示默认蓝。",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        text = "去授权",
                        onClick = {
                            try {
                                context.startActivity(Intent(
                                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                ))
                            } catch (t: Throwable) {
                                status = "无法打开授权页面，请在系统设置中手动授予"
                            }
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            SmallTitle(text = "开关")
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                SwitchRow(
                    title = "启用强调色替换",
                    summary = "仅改写 HyperOS 强调色资源",
                    checked = enabled,
                    accent = accent,
                    onCheckedChange = {
                        enabled = it
                        persist(newEnabled = it)
                    },
                )
                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    title = "同时修改原生取色",
                    summary = "WiFi/蓝牙列表、相册、电话、天气等大量界面需要开启",
                    checked = nativeColors,
                    accent = accent,
                    onCheckedChange = {
                        nativeColors = it
                        persist(newNative = it)
                    },
                )
                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    title = "自动计算按钮文字颜色",
                    summary = "浅色强调色自动改用黑色文字",
                    checked = contrastText,
                    accent = accent,
                    onCheckedChange = {
                        contrastText = it
                        persist(newContrast = it)
                    },
                )
            }

            Spacer(Modifier.height(10.dp))
            SmallTitle(text = "强调色")
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                ColorPicker(
                    color = accent,
                    onColorChanged = { setAccent(it) },
                    colorSpace = ColorSpace.OKLAB,
                    showPreview = false
                )
                Spacer(Modifier.height(14.dp))
                TextField(
                    value = hex,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                        if (filtered.length <= 8) {
                            hex = filtered
                            parseHex(filtered)?.let { setAccent(Color(it), updateHex = false) }
                        }
                    },
                    leadingIcon = {
                        Text(
                            "#",
                            modifier = Modifier.padding(start = 14.dp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        text = "应用当前颜色",
                        onClick = {
                            setAccent(accent)
                            publishConfiguration()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(
                            color = accent,
                            textColor = accent.contentColor()
                        )
                    )
                    TextButton(
                        text = "恢复默认蓝",
                        onClick = {
                            setAccent(Color(ConfigStore.DEFAULT_ACCENT))
                            status = "已恢复 HyperOS 默认蓝"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SmallTitle(text = "预设")
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                PresetGrid(
                    selected = accent,
                    onSelect = {
                        setAccent(it)
                        status = "已选择${PRESETS.firstOrNull { p -> p.color == it }?.name ?: "自定义色"}"
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            SmallTitle(text = "生效应用")
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showAppList = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "管理生效应用",
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "已勾选 ${installedApps.count { enabledPackages.contains(it.packageName) }} / ${installedApps.size} 个",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                    Text(
                        text = "›",
                        fontSize = 28.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "系统桌面、文件管理、日历等都在二级列表中，可从系统读取图标并单独开关。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }

            Spacer(Modifier.height(10.dp))
            SmallTitle(text = "应用与重启")
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                Text(
                    text = "资源颜色会被系统缓存。修改颜色、原生取色开关或应用列表后，需要重启对应进程。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(Modifier.height(12.dp))
                TextButton(
                    text = if (Settings.System.canWrite(context)) {
                        "同步配置到系统设置"
                    } else {
                        "授予“修改系统设置”权限"
                    },
                    onClick = {
                        if (Settings.System.canWrite(context)) {
                            persist()
                            publishConfiguration()
                        } else {
                            try {
                                context.startActivity(Intent(
                                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                ))
                                status = "授权后返回，再点一次此按钮同步"
                            } catch (t: Throwable) {
                                status = "无法打开授权页面，请在系统设置中授予“修改系统设置”"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    text = "重启生效作用域",
                    onClick = { showRestartDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(
                        color = accent,
                        textColor = accent.contentColor()
                    )
                )
                status?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    AnimatedVisibility(
        visible = showAppList,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        AppListPage(
            apps = installedApps,
            enabledPackages = enabledPackages,
            accent = accent,
            onToggle = ::togglePackage,
            onSelectAll = {
                val next = installedApps.map { it.packageName }.toSet()
                enabledPackages = next
                persist(newPackages = next)
            },
            onClear = {
                enabledPackages = emptySet()
                persist(newPackages = emptySet())
            },
            onBack = { showAppList = false },
        )
    }

    if (showRestartDialog) {
        RestartDialog(
            accent = accent,
            selected = restartSelection,
            onSelectionChange = { restartSelection = it },
            onDismiss = { showRestartDialog = false },
            onConfirm = {
                val targets = RESTART_TARGETS.filter { restartSelection.contains(it.packageName) }
                if (targets.isEmpty()) {
                    status = "没有选择要重启的应用"
                    showRestartDialog = false
                } else {
                    showRestartDialog = false
                    val currentAccent = accent.toArgb() or 0xFF000000.toInt()
                    val currentPackages = enabledPackages
                    val currentEnabled = enabled
                    val currentContrast = contrastText
                    val currentNative = nativeColors
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val synced = ConfigStore.publishToSystem(
                                context, currentEnabled, currentAccent, currentContrast,
                                currentNative, currentPackages
                            ) || ConfigStore.publishToSystemViaRoot(
                                currentEnabled, currentAccent, currentContrast,
                                currentNative, currentPackages
                            )
                            val restartResult = targets.joinToString("\n") { target ->
                                "${target.label}：${restartProcess(target.packageName)}"
                            }
                            (if (synced) "配置已同步\n" else "配置同步失败（检查设置权限/Root）\n") + restartResult
                        }
                        status = result
                    }
                }
            },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = accentSwitchColors(accent)
        )
    }
}

@Composable
private fun accentSwitchColors(accent: Color): SwitchColors {
    val uncheckedTrack = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    return SwitchDefaults.switchColors(
        checkedThumbColor = Color.White,
        uncheckedThumbColor = Color.White,
        disabledCheckedThumbColor = Color.White.copy(alpha = 0.6f),
        disabledUncheckedThumbColor = Color.White.copy(alpha = 0.6f),
        checkedTrackColor = accent,
        uncheckedTrackColor = uncheckedTrack,
        disabledCheckedTrackColor = accent.copy(alpha = 0.3f),
        disabledUncheckedTrackColor = uncheckedTrack.copy(alpha = 0.6f),
    )
}

@Composable
private fun RestartDialog(
    accent: Color,
    selected: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    WindowDialog(
        show = true,
        title = "重启作用域",
        summary = "勾选需要重启的应用，新的颜色和列表才会加载。",
        onDismissRequest = onDismiss,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                RESTART_TARGETS.forEach { target ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = if (selected.contains(target.packageName)) {
                                selected - target.packageName
                            } else {
                                selected + target.packageName
                            }
                            onSelectionChange(next)
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = target.label,
                        style = MiuixTheme.textStyles.body1,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = selected.contains(target.packageName),
                        onCheckedChange = { checked ->
                            val next = if (checked) {
                                selected + target.packageName
                            } else {
                                selected - target.packageName
                            }
                            onSelectionChange(next)
                        },
                        colors = accentSwitchColors(accent)
                    )
                }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "重启选中",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(
                        color = accent,
                        textColor = accent.contentColor()
                    )
                )
            }
        }
    }
}

@Composable
private fun AppListPage(
    apps: List<InstalledApp>,
    enabledPackages: Set<String>,
    accent: Color,
    onToggle: (String, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        fontSize = 30.sp,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "生效应用",
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 28.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                        Text(
                            text = "勾选需要改色的应用。图标来自系统 PackageManager；修改后需重启对应应用。",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "已勾选 ${apps.count { enabledPackages.contains(it.packageName) }} / ${apps.size} 个",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                text = "全选",
                                onClick = onSelectAll,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary(
                                    color = accent,
                                    textColor = accent.contentColor()
                                )
                            )
                            TextButton(
                                text = "全不选",
                                onClick = onClear,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                items(apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        checked = enabledPackages.contains(app.packageName),
                        accent = accent,
                        onCheckedChange = { onToggle(app.packageName, it) }
                    )
                }
            }
        }
    }
}

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val coreLabels = mapOf(
        "android" to "系统框架",
        "com.android.systemui" to "系统界面",
        "miui.systemui.plugin" to "控制中心插件",
        "com.android.settings" to "设置",
        "com.miui.securitycenter" to "安全中心",
        "com.android.thememanager" to "主题",
        "com.miui.home" to "系统桌面"
    )
    val result = mutableListOf<InstalledApp>()
    for (packageName in ScopePackages.ALL) {
        try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            result += InstalledApp(
                packageName = packageName,
                label = coreLabels[packageName] ?: packageManager.getApplicationLabel(info).toString(),
                icon = try {
                    packageManager.getApplicationIcon(info).toImageBitmap()
                } catch (_: Throwable) {
                    null
                }
            )
        } catch (_: PackageManager.NameNotFoundException) {
            // Not installed on this device/region.
        }
    }
    if (result.isEmpty()) {
        // Package visibility can be restricted on some builds. Keep the checklist
        // usable with package names rather than showing an empty page.
        return ScopePackages.ALL.map { InstalledApp(it, it, null) }
    }
    return result.sortedWith(
        compareBy(
            { if (ScopePackages.CORE.contains(it.packageName)) 0 else 1 },
            { it.label }
        )
    )
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val oldBounds = bounds
    setBounds(0, 0, size, size)
    draw(canvas)
    bounds = oldBounds
    return bitmap.asImageBitmap()
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 7.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1).ifEmpty { "?" },
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = app.packageName,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = accentSwitchColors(accent)
        )
    }
}

@Composable
private fun Header(accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent)
                .border(3.dp, MiuixTheme.colorScheme.surfaceContainer, CircleShape)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = "HyperAccent",
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "HyperOS 系统强调色",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

@Composable
private fun PresetGrid(selected: Color, onSelect: (Color) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PRESETS.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { preset ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(preset.color) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(preset.color)
                                .then(
                                    if (preset.color == selected) {
                                        Modifier.border(3.dp, MiuixTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier.border(1.dp, MiuixTheme.colorScheme.outline, CircleShape)
                                    }
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = preset.name,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InfoLine(title: String, summary: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(74.dp)
        )
        Text(
            text = summary,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

private fun Int.toHexRgb(): String = String.format("%06X", this and 0xFFFFFF)

private fun Color.contentColor(): Color =
    if (AccentColorMath.relativeLuminance(toArgb()) > 0.45) Color.Black else Color.White

private fun parseHex(input: String): Int? {
    val value = input.trim().removePrefix("#")
    return when (value.length) {
        6 -> value.toLongOrNull(16)?.toInt()?.let { it or 0xFF000000.toInt() }
        8 -> value.toLongOrNull(16)?.toInt()
        else -> null
    }
}

private fun restartProcess(packageName: String): String {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -f $packageName"))
        val exit = process.waitFor()
        if (exit == 0) {
            "已重启"
        } else {
            "失败（su 返回 $exit）"
        }
    } catch (t: Throwable) {
        "失败（无法获取 Root）"
    }
}
