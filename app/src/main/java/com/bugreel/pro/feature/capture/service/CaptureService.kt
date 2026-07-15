package com.bugreel.pro.feature.capture.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.bugreel.pro.core.util.OemCompat
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// CLAUDE.md Section 8 — android:process=":capture" is declared on this service in the
// manifest. Nothing capture-related runs on the main thread: all recording work happens on
// [serviceScope], which is tied to this service's lifecycle (cancelled in onDestroy).
@AndroidEntryPoint
class CaptureService : Service() {

    @Inject
    lateinit var eventLogger: BugReelEventLogger

    @Inject
    lateinit var bubbleOverlayManager: BubbleOverlayManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val binder = LocalBinder()

    @Volatile
    private var mediaProjectionHandler: MediaProjectionHandler? = null

    override fun onCreate() {
        super.onCreate()

        if (OemCompat.requiresManualBatteryOptimizationWhitelist()) {
            // TODO: surface an in-app prompt guiding the tester to manually whitelist
            // BugReel Pro — this manufacturer kills foreground services otherwise.
        }

        bubbleOverlayManager.listener = object : BubbleOverlayManager.ActionListener {
            override fun onCaptureTapped() {
                if (mediaProjectionHandler?.isRecording() != true) {
                    requestCapturePermission()
                }
            }

            override fun onStopRequested() {
                stopCaptureInternal()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = bubbleOverlayManager.buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                BubbleOverlayManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(BubbleOverlayManager.NOTIFICATION_ID, notification)
        }
        bubbleOverlayManager.show()

        when (intent?.action) {
            BubbleOverlayManager.ACTION_START_CAPTURE -> requestCapturePermission()
            BubbleOverlayManager.ACTION_STOP_CAPTURE -> stopCaptureInternal()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        serviceScope.cancel()
        mediaProjectionHandler?.stopRecording()
        bubbleOverlayManager.hide()
        super.onDestroy()
    }

    private fun requestCapturePermission() {
        // The :capture process cannot launch the MediaProjection consent dialog itself —
        // only an Activity can. Broadcast a request so the app's foreground UI can start the
        // consent flow and hand the resulting MediaProjection back via LocalBinder.startCapture.
        sendBroadcast(Intent(ACTION_REQUEST_MEDIA_PROJECTION).setPackage(packageName))
    }

    private fun stopCaptureInternal(): CaptureResult? {
        val handler = mediaProjectionHandler ?: return null
        val file = handler.stopRecording() ?: return null
        mediaProjectionHandler = null
        eventLogger.logEvent(BugReelEvent.RecordingStopped())
        bubbleOverlayManager.updateCaptureState(isCapturing = false)
        return CaptureResult(recordingFile = file, events = eventLogger.getEvents())
    }

    inner class LocalBinder : Binder() {

        fun startCapture(mediaProjection: MediaProjection) {
            serviceScope.launch {
                val handler = MediaProjectionHandler(mediaProjection, serviceScope)
                val outputFile = File(filesDir, "capture_${System.currentTimeMillis()}.mp4")
                if (handler.startRecording(outputFile)) {
                    mediaProjectionHandler = handler
                    eventLogger.logEvent(BugReelEvent.RecordingStarted())
                    bubbleOverlayManager.updateCaptureState(isCapturing = true)
                }
            }
        }

        fun stopCapture(): CaptureResult? = stopCaptureInternal()

        fun isCapturing(): Boolean = mediaProjectionHandler?.isRecording() == true
    }

    companion object {
        const val ACTION_REQUEST_MEDIA_PROJECTION = "com.bugreel.pro.action.REQUEST_MEDIA_PROJECTION"
    }
}

data class CaptureResult(
    val recordingFile: File,
    val events: List<BugReelEvent>
)
