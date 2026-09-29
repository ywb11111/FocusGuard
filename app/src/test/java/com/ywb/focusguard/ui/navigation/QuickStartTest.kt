package com.ywb.focusguard.ui.navigation

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuickStartTest {
    @Test
    fun `合法的桌面入口能解析预设时长`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("focusguard://session?duration=45"))

        assertEquals(45, intent.quickStartDurationMinutes())
    }

    @Test
    fun `外部协议和异常时长不会触发专注入口`() {
        val externalIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://session?duration=25"))
        val invalidDuration = Intent(Intent.ACTION_VIEW, Uri.parse("focusguard://session?duration=999"))

        assertNull(externalIntent.quickStartDurationMinutes())
        assertNull(invalidDuration.quickStartDurationMinutes())
    }
}
