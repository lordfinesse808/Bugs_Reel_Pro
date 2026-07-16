package com.bugreel.pro.feature.capture.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric provides real (shadowed) ActivityManager/WindowManager/ConnectivityManager
// behaviour that DeviceProfiler needs — a plain unit test can't exercise this. Robolectric has
// no JUnit5 extension, so this file runs as a JUnit4 test through the Vintage engine
// registered alongside Jupiter (JUnit5) in build.gradle.kts, per de.mannodermaus/android-junit5.
@RunWith(RobolectricTestRunner::class)
class DeviceProfilerTest {

    private val deviceProfiler = DeviceProfiler()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `getDeviceProfile returns non-null DeviceProfile`() {
        val profile = deviceProfiler.getDeviceProfile(context)

        assertNotNull(profile)
    }

    @Test
    fun `brand and model are non-empty strings`() {
        val profile = deviceProfiler.getDeviceProfile(context)

        assertTrue(profile.brand.isNotEmpty())
        assertTrue(profile.model.isNotEmpty())
    }
}
