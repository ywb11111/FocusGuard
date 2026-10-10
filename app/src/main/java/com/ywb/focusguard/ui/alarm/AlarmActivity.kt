package com.ywb.focusguard.ui.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.service.AlarmRingService
import com.ywb.focusguard.service.AlarmStateHolder
import com.ywb.focusguard.ui.theme.FocusDarkBackground
import com.ywb.focusguard.ui.theme.FocusDarkInk
import com.ywb.focusguard.ui.theme.FocusDarkInkMuted
import com.ywb.focusguard.ui.theme.FocusGuardTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 闹钟响铃页：可以显示在锁屏之上，并在熄屏时点亮屏幕。
 *
 * 由 AlarmRingService 的全屏通知拉起。页面本身不控制铃声，只把用户操作转发给服务；
 * 通过观察 AlarmStateHolder，在响铃于别处停止时（通知按钮、超时）自动关闭自己。
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        setContent {
            FocusGuardTheme(darkTheme = true) {
                val ringing by AlarmStateHolder.ringing.collectAsStateWithLifecycle()
                // 不在响铃（已被关闭，或进程重启后 Activity 被恢复）就没有存在的意义
                LaunchedEffect(ringing) { if (ringing == null) finish() }
                AlarmScreen(
                    smartEarly = ringing?.smartEarly ?: false,
                    onSnooze = {
                        AlarmRingService.snooze(this)
                        finish()
                    },
                    onDismiss = {
                        AlarmRingService.dismiss(this)
                        finish()
                    }
                )
            }
        }
    }

    /**
     * 允许显示在锁屏上并点亮屏幕。Android 8.1 起改用 Activity 方法，旧版本使用窗口标志。
     * 只放行这一个页面，不会解除整台手机的锁屏。
     */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

/** 响铃页内容：当前时间 + 两个大按钮，刚睡醒也容易点中。 */
@Composable
private fun AlarmScreen(
    smartEarly: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusDarkBackground)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.Alarm, contentDescription = null, tint = FocusDarkInkMuted, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now)),
            style = MaterialTheme.typography.displayLarge,
            color = FocusDarkInk
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (smartEarly) "检测到你正处于浅睡，现在起床会更清醒" else "起床时间到了",
            style = MaterialTheme.typography.titleMedium,
            color = FocusDarkInkMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(56.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("起床，结束睡眠记录", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("贪睡 9 分钟", color = FocusDarkInk)
        }
    }
}
