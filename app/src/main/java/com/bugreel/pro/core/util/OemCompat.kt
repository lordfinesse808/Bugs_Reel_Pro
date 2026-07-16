package com.bugreel.pro.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

// CLAUDE.md Section 8 / Architecture Rule 3 — every Build.MANUFACTURER / Build.BRAND / MIUI
// check lives exclusively here. Nothing outside this file may branch on OEM identity.
object OemCompat {

    private val BATTERY_WHITELIST_MANUFACTURERS = listOf("xiaomi", "oppo", "vivo", "huawei")
    private val BATTERY_SAVER_KILLS_SERVICE_MANUFACTURERS = listOf("xiaomi", "oppo")
    private val MEDIA_PROJECTION_REPROMPT_MANUFACTURERS = listOf("samsung", "xiaomi")

    // manufacturer defaults to the real device value; tests inject an explicit one instead of
    // mutating the final Build.MANUFACTURER field (unreliable on JDK 17+) or requiring Robolectric.
    fun requiresManualBatteryOptimizationWhitelist(manufacturer: String = Build.MANUFACTURER): Boolean =
        manufacturer.lowercase() in BATTERY_WHITELIST_MANUFACTURERS

    fun overlayPermissionGrantIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

    fun foregroundServiceSurvivesBatterySaver(manufacturer: String = Build.MANUFACTURER): Boolean =
        manufacturer.lowercase() !in BATTERY_SAVER_KILLS_SERVICE_MANUFACTURERS

    fun mediaProjectionReprompts(manufacturer: String = Build.MANUFACTURER): Boolean =
        manufacturer.lowercase() in MEDIA_PROJECTION_REPROMPT_MANUFACTURERS

    // Architecture Rule 3 requires every Build.MANUFACTURER read to live here — even a plain,
    // non-branching read for device-profile reporting (DeviceProfiler) — so route it through
    // this passthrough rather than reading Build.MANUFACTURER at the call site.
    fun deviceManufacturer(): String = Build.MANUFACTURER

    // TODO: Update when Play Console review lands (CLAUDE.md Section 13, Track 2).
    fun needsPiPFallback(): Boolean = false
}
