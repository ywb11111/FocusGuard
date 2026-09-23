package com.ywb.focusguard.ui.viewmodel

import android.Manifest
import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

/** 覆盖通知权限在 Android 13 前后的系统差异，防止旧系统被误判为缺少权限。 */
@RunWith(RobolectricTestRunner::class)
class PermissionManagerTest {

    @Test
    @Config(sdk = [32])
    fun `notification permission is implicitly granted before Android 13`() {
        val context = RuntimeEnvironment.getApplication() as Application
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertTrue(PermissionManager(context).isNotificationPermissionGranted())
    }

    @Test
    @Config(sdk = [33])
    fun `notification permission reflects runtime grant on Android 13`() {
        val context = RuntimeEnvironment.getApplication() as Application
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val permissionManager = PermissionManager(context)

        assertFalse(permissionManager.isNotificationPermissionGranted())

        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(permissionManager.isNotificationPermissionGranted())
    }
}
