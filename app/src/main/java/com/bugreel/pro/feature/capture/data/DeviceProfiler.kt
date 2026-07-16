package com.bugreel.pro.feature.capture.data

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.util.OemCompat
import javax.inject.Inject
import javax.inject.Singleton

// CLAUDE.md Section 14 — read-only device metadata, no special permissions required.
@Singleton
class DeviceProfiler @Inject constructor() {

    fun getDeviceProfile(context: Context): DeviceProfile = DeviceProfile(
        brand = OemCompat.deviceManufacturer(),
        model = Build.MODEL,
        osVersion = Build.VERSION.RELEASE,
        apiLevel = Build.VERSION.SDK_INT,
        ramMb = readRamMb(context),
        screenResolution = readScreenResolution(context),
        networkType = readNetworkType(context)
    )

    private fun readRamMb(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / BYTES_PER_MB
    }

    private fun readScreenResolution(context: Context): String {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            "${bounds.width()}x${bounds.height()}"
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }
            "${metrics.widthPixels}x${metrics.heightPixels}"
        }
    }

    private fun readNetworkType(context: Context): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?: return "NONE"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    private companion object {
        const val BYTES_PER_MB = 1024 * 1024
    }
}
