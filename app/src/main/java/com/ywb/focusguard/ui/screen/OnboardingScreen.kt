package com.ywb.focusguard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ywb.focusguard.ui.theme.FocusTealSoft

private data class OnboardingPage(
    val icon: ImageVector,
    val eyebrow: String,
    val title: String,
    val body: String
)

/**
 * 三步首次引导：先说明价值，再解释数据，最后说明隐私。
 *
 * 权限不会在引导中一次性弹出，而是在首页真正使用监测能力时按需申请。
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit = {}) {
    val pages = listOf(
        OnboardingPage(
            Icons.Outlined.Sensors,
            "环境预检",
            "开始前，先看看此刻是否适合专注",
            "FocusGuard 会结合噪声、光照和手机移动状态，给出简单、可行动的环境建议。"
        ),
        OnboardingPage(
            Icons.Outlined.Insights,
            "专注分析",
            "结束后，不只得到一个分数",
            "每次记录都会解释哪些环境因素影响了你，并逐渐形成最适合你的专注规律。"
        ),
        OnboardingPage(
            Icons.Outlined.PrivacyTip,
            "本地与隐私",
            "声音被分析，但不会被录下来",
            "麦克风仅计算相对音量，原始音频不会保存或上传；专注数据默认只保存在当前设备。"
        )
    )
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FocusGuard", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onComplete) { Text("跳过") }
        }
        LinearProgressIndicator(
            progress = { (pageIndex + 1) / pages.size.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Surface(shape = CircleShape, color = FocusTealSoft, modifier = Modifier.size(112.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(50.dp))
            }
        }
        Spacer(Modifier.height(30.dp))
        Text(page.eyebrow, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Text(page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(
            page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1.2f))
        Button(
            onClick = {
                if (pageIndex == pages.lastIndex) onComplete() else pageIndex += 1
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (pageIndex == pages.lastIndex) "开始使用" else "继续")
        }
        if (pageIndex > 0) {
            TextButton(onClick = { pageIndex -= 1 }) { Text("返回上一步") }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}
