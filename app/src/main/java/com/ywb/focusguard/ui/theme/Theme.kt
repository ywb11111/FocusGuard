package com.ywb.focusguard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** 深色主题语义色；页面只读取语义，不直接判断当前主题。 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF35D6B2),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFF9EF2DB),
    secondary = DarkMintSecondary,
    onSecondary = Color(0xFF082E5C),
    tertiary = DarkMintAccent,
    error = DarkMintWarning,
    background = FocusDarkBackground,
    onBackground = FocusDarkInk,
    surface = FocusDarkSurface,
    onSurface = FocusDarkInk,
    surfaceVariant = FocusDarkSurfaceMuted,
    onSurfaceVariant = FocusDarkInkMuted,
    outline = FocusDarkOutline,
    outlineVariant = Color(0xFF24363B)
)

/** 浅色主题语义色，与选定的“理性数据”视觉方案保持一致。 */
private val LightColorScheme = lightColorScheme(
    primary = FocusTealStrong,
    onPrimary = Color.White,
    primaryContainer = FocusTealSoft,
    onPrimaryContainer = Color(0xFF00513F),
    secondary = FocusBlue,
    onSecondary = Color.White,
    secondaryContainer = FocusBlueSoft,
    onSecondaryContainer = Color(0xFF173D70),
    tertiary = FocusAmber,
    error = FocusRed,
    errorContainer = FocusRedSoft,
    background = FocusBackground,
    onBackground = FocusInk,
    surface = FocusSurface,
    onSurface = FocusInk,
    surfaceVariant = FocusSurfaceMuted,
    onSurfaceVariant = FocusInkMuted,
    outline = FocusOutline,
    outlineVariant = Color(0xFFE8EFEC)
)

/**
 * FocusGuard 全局主题入口。
 *
 * 默认关闭动态取色，避免 Android 12+ 的壁纸颜色破坏传感器状态的固定语义。
 */
@Composable
fun FocusGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colors, typography = Typography, content = content)
}
