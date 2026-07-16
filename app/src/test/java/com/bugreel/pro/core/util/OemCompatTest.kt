package com.bugreel.pro.core.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OemCompatTest {

    @Test
    fun `requiresManualBatteryOptimizationWhitelist returns true for xiaomi regardless of case`() {
        assertTrue(OemCompat.requiresManualBatteryOptimizationWhitelist("Xiaomi"))
        assertTrue(OemCompat.requiresManualBatteryOptimizationWhitelist("XIAOMI"))
        assertTrue(OemCompat.requiresManualBatteryOptimizationWhitelist("xiaomi"))
    }

    @Test
    fun `needsPiPFallback returns false by default`() {
        assertFalse(OemCompat.needsPiPFallback())
    }

    @Test
    fun `all methods callable without throwing`() {
        val context = mockk<Context>()
        every { context.packageName } returns "com.bugreel.pro"

        // deviceManufacturer() reads Build.MANUFACTURER directly; it needs a real device or
        // Robolectric shadow to resolve to a non-null value (covered by DeviceProfilerTest).
        OemCompat.requiresManualBatteryOptimizationWhitelist("Google")
        OemCompat.foregroundServiceSurvivesBatterySaver("Google")
        OemCompat.mediaProjectionReprompts("Google")
        OemCompat.needsPiPFallback()
        OemCompat.overlayPermissionGrantIntent(context)
    }
}
