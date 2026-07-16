package com.bugreel.pro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Configuration.Provider lets WorkManager's default on-demand initializer pick up
// HiltWorkerFactory automatically — no manifest changes needed to wire @HiltWorker classes
// like SyncWorker (core/data/sync).
@HiltAndroidApp
class BugReelApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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
