package com.ywb.focusguard.ui.theme

import androidx.compose.ui.graphics.Color

/** FocusGuard 1.0 的品牌主色：克制的青绿色，用于主要操作和良好状态。 */
val FocusTeal = Color(0xFF10B981)
val FocusTealStrong = Color(0xFF008F72)
val FocusTealSoft = Color(0xFFDDF8EF)
val FocusBlue = Color(0xFF3B82F6)
val FocusBlueSoft = Color(0xFFE8F1FF)
val FocusAmber = Color(0xFFF59E0B)
val FocusAmberSoft = Color(0xFFFFF4D6)
val FocusRed = Color(0xFFE85D65)
val FocusRedSoft = Color(0xFFFFE9EA)

/** 浅色主题使用暖白底色和深墨色文字，保持数据界面的清晰与安静。 */
val FocusBackground = Color(0xFFF8FAF9)
val FocusSurface = Color(0xFFFFFFFF)
val FocusSurfaceMuted = Color(0xFFF0F5F3)
val FocusInk = Color(0xFF0D2530)
val FocusInkMuted = Color(0xFF657681)
val FocusOutline = Color(0xFFDCE6E2)

/** 深色主题避免纯黑，并降低高饱和颜色在夜间造成的刺激。 */
val FocusDarkBackground = Color(0xFF0C1518)
val FocusDarkSurface = Color(0xFF142126)
val FocusDarkSurfaceMuted = Color(0xFF1B2B30)
val FocusDarkInk = Color(0xFFEAF5F1)
val FocusDarkInkMuted = Color(0xFFAABAB5)
val FocusDarkOutline = Color(0xFF304248)

// 兼容旧组件命名，逐步迁移时仍保持同一套品牌语义。
val MintPrimary = FocusTeal
val MintPrimaryDark = FocusTealStrong
val MintSecondary = FocusBlue
val MintAccent = FocusAmber
val MintWarning = FocusRed
val MintBackground = FocusBackground
val MintSurface = FocusSurface
val DarkMintPrimary = FocusTeal
val DarkMintPrimaryDark = Color(0xFF35D6B2)
val DarkMintSecondary = Color(0xFF70A8FF)
val DarkMintAccent = Color(0xFFFFC45B)
val DarkMintWarning = Color(0xFFFF858B)
val DarkBackground = FocusDarkBackground
val DarkSurface = FocusDarkSurface
val DarkSurfaceVariant = FocusDarkSurfaceMuted
