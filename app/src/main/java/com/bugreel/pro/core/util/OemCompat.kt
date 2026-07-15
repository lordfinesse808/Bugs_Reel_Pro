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

    fun requiresManualBatteryOptimizationWhitelist(): Boolean =
        Build.MANUFACTURER.lowercase() in BATTERY_WHITELIST_MANUFACTURERS

    fun overlayPermissionGrantIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

    fun foregroundServiceSurvivesBatterySaver(): Boolean =
        Build.MANUFACTURER.lowercase() !in BATTERY_SAVER_KILLS_SERVICE_MANUFACTURERS

    fun mediaProjectionReprompts(): Boolean =
        Build.MANUFACTURER.lowercase() in MEDIA_PROJECTION_REPROMPT_MANUFACTURERS

    // TODO: Update when Play Console review lands (CLAUDE.md Section 13, Track 2).
    fun needsPiPFallback(): Boolean = false
}
