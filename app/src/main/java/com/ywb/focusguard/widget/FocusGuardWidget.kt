package com.ywb.focusguard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ywb.focusguard.ui.navigation.createQuickStartIntent

/** 桌面上的轻量专注入口；传感器采集仍由应用内开始按钮明确触发。 */
class FocusGuardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { FocusGuardWidgetContent() }
    }
}

/** 系统发现并更新 FocusGuard 小组件的接收器。 */
class FocusGuardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusGuardWidget()
}

@Composable
private fun FocusGuardWidgetContent() {
    val context = LocalContext.current
    val primary = ColorProvider(Color(0xFF009B7A))
    val onPrimary = ColorProvider(Color.White)
    val title = ColorProvider(Color(0xFF0E2A31))
    val secondary = ColorProvider(Color(0xFF60747A))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF8FAF9)))
            .cornerRadius(20.dp)
            .padding(18.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "FocusGuard",
            style = TextStyle(color = title, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "先检查环境，再进入一段安静的专注",
            style = TextStyle(color = secondary)
        )
        Spacer(GlanceModifier.height(14.dp))
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(primary)
                .cornerRadius(12.dp)
                .clickable(actionStartActivity(createQuickStartIntent(context, 25)))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "准备 25 分钟专注",
                style = TextStyle(color = onPrimary, fontWeight = FontWeight.Medium)
            )
        }
    }
}
