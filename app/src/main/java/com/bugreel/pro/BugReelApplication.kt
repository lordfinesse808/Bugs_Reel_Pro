package com.bugreel.pro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BugReelApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createCaptureNotificationChannel()
    }

    private fun createCaptureNotificationChannel() {
        val channel = NotificationChannel(
            CAPTURE_CHANNEL_ID,
            "Screen Capture",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CAPTURE_CHANNEL_ID = "bugreel_capture"
    }
}
