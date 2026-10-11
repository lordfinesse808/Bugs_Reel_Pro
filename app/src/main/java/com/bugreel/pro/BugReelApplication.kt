package com.bugreel.pro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bugreel.pro.core.domain.repository.ConsentRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Configuration.Provider lets WorkManager's default on-demand initializer pick up
// HiltWorkerFactory automatically — no manifest changes needed to wire @HiltWorker classes
// like SyncWorker (core/data/sync).
@HiltAndroidApp
class BugReelApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var consentRepository: ConsentRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createCaptureNotificationChannel()
        enableCrashReportingOnceConsented()
    }

    // Launch Readiness Gate (CLAUDE.md Section 18) measures crash-free rate in Crashlytics, but
    // collection is disabled in the manifest until the tester has accepted the consent screen.
    private fun enableCrashReportingOnceConsented() {
        appScope.launch {
            consentRepository.consentState
                .map { it.consentGiven }
                .distinctUntilChanged()
                .collect { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(it) }
        }
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
