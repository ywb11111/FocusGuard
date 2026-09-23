package com.ywb.focusguard.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.EnvironmentStatus
import com.ywb.focusguard.domain.model.FocusConfig
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.LightSample
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseLevel
import com.ywb.focusguard.domain.model.NoiseSample
import com.ywb.focusguard.domain.model.PermissionState
import com.ywb.focusguard.domain.model.TodaySummary
import com.ywb.focusguard.domain.model.UserSettings
import com.ywb.focusguard.ui.screen.ReportsScreen
import com.ywb.focusguard.ui.screen.SessionScreen
import com.ywb.focusguard.ui.screen.SettingsScreen
import com.ywb.focusguard.ui.screen.TodayScreen
import com.ywb.focusguard.ui.state.PeriodSummary
import com.ywb.focusguard.ui.state.ReportPeriod
import com.ywb.focusguard.ui.state.ReportsUiState
import com.ywb.focusguard.ui.state.SessionUiState
import com.ywb.focusguard.ui.state.SettingsUiState
import com.ywb.focusguard.ui.state.TodayUiState
import com.ywb.focusguard.ui.theme.FocusGuardTheme

private val previewEnvironment = EnvironmentSnapshot(
    noise = NoiseSample(1L, 38f, NoiseLevel.QUIET),
    light = LightSample(1L, 285f, LightLevel.COMFORTABLE),
    motion = MotionSample(1L, 0.08f, isSignificantMove = false, isMoving = false),
    status = EnvironmentStatus.FOCUSED
)

private val previewSessions = listOf(
    FocusSession(3L, 1_786_083_600_000L, 1_786_086_300_000L, 2_700_000L, 38f, 52f, 285f, 0, 0, 92, "算法练习"),
    FocusSession(2L, 1_785_997_800_000L, 1_786_000_500_000L, 2_700_000L, 44f, 61f, 240f, 1, 0, 86, "阅读 Compose"),
    FocusSession(1L, 1_785_911_400_000L, 1_785_912_900_000L, 1_500_000L, 51f, 68f, 180f, 2, 0, 78, "课程复习")
)

@PreviewTest
@Preview(name = "Today light", widthDp = 390, heightDp = 844, showBackground = true, backgroundColor = 0xFFF8FAF9)
@Composable
fun TodayScreenshotPreview() {
    FocusGuardTheme(darkTheme = false) {
        Scaffold(bottomBar = { FocusGuardBottomBar("today", {}) }) { padding ->
            Box(Modifier.padding(padding)) {
                TodayScreen(
                    uiState = TodayUiState(
                        isLoading = false,
                        environment = previewEnvironment,
                        todaySummary = TodaySummary(3_000_000L, 84, 0, 2),
                        permissionState = PermissionState(audioGranted = true, notificationGranted = true)
                    ),
                    onStartFocus = {},
                    onOpenSettings = {},
                    onOpenSessionDetail = {}
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Session ready", widthDp = 390, heightDp = 844, showBackground = true, backgroundColor = 0xFFF8FAF9)
@Composable
fun SessionReadyScreenshotPreview() {
    FocusGuardTheme(darkTheme = false) {
        SessionScreen(
            uiState = SessionUiState.Ready(FocusConfig(25), previewEnvironment),
            onStart = {},
            onSelectDuration = {},
            onPause = {},
            onResume = {},
            onFinish = {},
            onReset = {},
            onOpenDetail = {}
        )
    }
}

@PreviewTest
@Preview(name = "Session running", widthDp = 390, heightDp = 844, showBackground = true, backgroundColor = 0xFFF8FAF9)
@Composable
fun SessionRunningScreenshotPreview() {
    FocusGuardTheme(darkTheme = false) {
        SessionScreen(
            uiState = SessionUiState.Running(
                sessionId = 7L,
                elapsedMillis = 480_000L,
                remainingMillis = 1_020_000L,
                noiseSamples = listOf(previewEnvironment.noise),
                lightLevel = LightLevel.COMFORTABLE,
                movementCount = 0
            ),
            onStart = {},
            onSelectDuration = {},
            onPause = {},
            onResume = {},
            onFinish = {},
            onReset = {},
            onOpenDetail = {}
        )
    }
}

@PreviewTest
@Preview(name = "Reports light", widthDp = 390, heightDp = 844, showBackground = true, backgroundColor = 0xFFF8FAF9)
@Composable
fun ReportsScreenshotPreview() {
    FocusGuardTheme(darkTheme = false) {
        Scaffold(bottomBar = { FocusGuardBottomBar("reports", {}) }) { padding ->
            Box(Modifier.padding(padding)) {
                ReportsScreen(
                    uiState = ReportsUiState(
                        period = ReportPeriod.WEEK,
                        sessions = previewSessions,
                        periodSummary = PeriodSummary(6_900_000L, 85, 3, 44f, 235f, 3),
                        trendValues = listOf(78f, 86f, 92f)
                    ),
                    onOpenSessionDetail = {}
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Settings light", widthDp = 390, heightDp = 844, showBackground = true, backgroundColor = 0xFFF8FAF9)
@Composable
fun SettingsScreenshotPreview() {
    FocusGuardTheme(darkTheme = false) {
        Scaffold(bottomBar = { FocusGuardBottomBar("settings", {}) }) { padding ->
            Box(Modifier.padding(padding)) {
                SettingsScreen(
                    uiState = SettingsUiState(
                        settings = UserSettings(backgroundMonitoringEnabled = true, dailyReportEnabled = true, onboardingCompleted = true),
                        permissionState = PermissionState(audioGranted = true, notificationGranted = true)
                    ),
                    onOpenPermissionGuide = {}
                )
            }
        }
    }
}
