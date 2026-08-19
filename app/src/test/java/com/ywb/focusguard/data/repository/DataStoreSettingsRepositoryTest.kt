package com.ywb.focusguard.data.repository

import com.ywb.focusguard.domain.model.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * DataStoreSettingsRepository 单元测试。
 *
 * 验证：
 * - 默认值正确
 * - 各 update 方法正确持久化
 * - 多次读取结果一致
 */
@RunWith(RobolectricTestRunner::class)
class DataStoreSettingsRepositoryTest {

    private lateinit var repository: DataStoreSettingsRepository

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        repository = DataStoreSettingsRepository(context)
    }

    @Test
    fun `initial settings should have default values`() = runTest {
        val settings = repository.settings.first()
        assertEquals(25, settings.defaultFocusMinutes)
        assertEquals(65f, settings.noiseThresholdDb)
        assertEquals(100f, settings.comfortableLightMinLux)
        assertEquals(500f, settings.comfortableLightMaxLux)
        assertEquals(false, settings.backgroundMonitoringEnabled)
        assertEquals(true, settings.dailyReportEnabled)
    }

    @Test
    fun `updateNoiseThreshold should persist the value`() = runTest {
        repository.updateNoiseThreshold(75f)
        val settings = repository.settings.first()
        assertEquals(75f, settings.noiseThresholdDb)
    }

    @Test
    fun `updateLightRange should persist both min and max`() = runTest {
        repository.updateLightRange(200f, 800f)
        val settings = repository.settings.first()
        assertEquals(200f, settings.comfortableLightMinLux)
        assertEquals(800f, settings.comfortableLightMaxLux)
    }

    @Test
    fun `updateDefaultFocusMinutes should persist the value`() = runTest {
        repository.updateDefaultFocusMinutes(45)
        val settings = repository.settings.first()
        assertEquals(45, settings.defaultFocusMinutes)
    }

    @Test
    fun `settings should emit updated values via Flow`() = runTest {
        repository.updateNoiseThreshold(80f)
        val latestNoise = repository.settings.first().noiseThresholdDb
        assertEquals(80f, latestNoise)
    }

    @Test
    fun `multiple updates should all persist`() = runTest {
        repository.updateNoiseThreshold(70f)
        repository.updateLightRange(150f, 600f)
        repository.updateDefaultFocusMinutes(30)
        val settings = repository.settings.first()
        assertEquals(70f, settings.noiseThresholdDb)
        assertEquals(150f, settings.comfortableLightMinLux)
        assertEquals(600f, settings.comfortableLightMaxLux)
        assertEquals(30, settings.defaultFocusMinutes)
    }
}
