package com.ywb.focusguard.ui.theme

import android.app.Activity
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

/** 深色模式的核心语义色，使用完整的深色配色方案。 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkMintPrimary,
    secondary = DarkMintSecondary,
    tertiary = DarkMintAccent,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = DarkMintWarning,
    onPrimary = Color(0xFF065F46),
    onSecondary = Color(0xFF1E3A5F),
    onTertiary = Color(0xFF78350F),
    onBackground = Color(0xFFF9FAFB),
    onSurface = Color(0xFFF9FAFB),
    onSurfaceVariant = Color(0xFFD1D5DB)
)

/** 浅色模式的核心语义色，也是当前默认视觉方案。 */
private val LightColorScheme = lightColorScheme(
    primary = MintPrimaryDark,
    secondary = MintSecondary,
    tertiary = MintAccent,
    background = MintBackground,
    surface = MintSurface,
    error = MintWarning,
    onPrimary = Color(0xFF065F46),
    onSecondary = Color(0xFF1E3A5F),
    onTertiary = Color(0xFF78350F)
)

/**
 * FocusGuard 全局 Material 主题入口。
 *
 * @param darkTheme 是否跟随系统启用深色模式。
 * @param dynamicColor 是否在 Android 12+ 使用系统动态配色；默认关闭以保持品牌一致性。
 * @param content 应用页面内容。
 */
@Composable
fun FocusGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 动态配色仅在 Android 12 及以上可用。
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 动态色不可用或被关闭时，回退到项目定义的明暗主题色板。
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
