package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RecordingEntity
import com.example.service.CaptureSource
import com.example.service.RecordingSettings
import com.example.service.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ScreenNova", appName)
    }

    @Test
    fun `default recording settings are configured`() {
        val settings = RecordingSettings()
        assertEquals(CaptureSource.FULL_SCREEN, settings.captureSource)
        assertEquals(VideoQuality.HD_720P, settings.videoQuality)
        assertTrue(settings.isMicEnabled)
    }

    @Test
    fun `recording entity properties`() {
        val entity = RecordingEntity(
            title = "Test Capture",
            filePath = "/storage/test.mp4",
            durationMs = 5000L,
            width = 1920,
            height = 1080
        )
        assertEquals("Test Capture", entity.title)
        assertEquals(5000L, entity.durationMs)
        assertEquals(1080, entity.height)
    }
}
