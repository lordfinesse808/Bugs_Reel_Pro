package com.bugreel.pro.feature.capture.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.bugreel.pro.core.util.OemCompat
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// CLAUDE.md Section 8 — android:process=":capture" isolates in-progress recordings from a
// main-process crash, so a plain same-process Binder cannot be used to talk to this service
// (a BinderProxy cast to a custom Binder subclass across processes throws ClassCastException).
// Clients (CaptureRepositoryImpl) communicate over a Messenger protocol instead; MSG_* /
// KEY_* below are that protocol's contract. Nothing capture-related runs on the main thread:
// recording work happens on [serviceScope], cancelled in onDestroy.
@AndroidEntryPoint
class CaptureService : Service() {

    @Inject
    lateinit var eventLogger: BugReelEventLogger

    @Inject
    lateinit var bubbleOverlayManager: BubbleOverlayManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var mediaProjectionHandler: MediaProjectionHandler? = null

    // The last client to successfully start a capture — used to push a stop notification
    // back if the bubble/notification (not the app UI) is what actually stopped recording.
    private var clientMessenger: Messenger? = null

    private val messenger = Messenger(IncomingHandler())

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
                performStopCapture(clientMessenger)
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
            BubbleOverlayManager.ACTION_STOP_CAPTURE -> performStopCapture(clientMessenger)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        serviceScope.cancel()
        mediaProjectionHandler?.stopRecording()
        bubbleOverlayManager.hide()
        super.onDestroy()
    }

    private fun requestCapturePermission() {
        // The :capture process cannot launch the MediaProjection consent dialog itself —
        // only an Activity can. Broadcast a request so the app's foreground UI can start the
        // consent flow and hand the resulting (resultCode, data) token back via MSG_START_CAPTURE.
        sendBroadcast(Intent(ACTION_REQUEST_MEDIA_PROJECTION).setPackage(packageName))
    }

    private fun performStartCapture(resultCode: Int, projectionData: Intent, replyTo: Messenger?) {
        if (mediaProjectionHandler?.isRecording() == true) {
            replyTo?.send(Message.obtain(null, MSG_CAPTURE_START_FAILED))
            return
        }

        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        val projection = projectionManager.getMediaProjection(resultCode, projectionData)
        val handler = MediaProjectionHandler(projection, serviceScope)
        val outputFile = File(filesDir, "capture_${System.currentTimeMillis()}.mp4")

        if (handler.startRecording(outputFile)) {
            mediaProjectionHandler = handler
            clientMessenger = replyTo
            eventLogger.logEvent(BugReelEvent.RecordingStarted())
            bubbleOverlayManager.updateCaptureState(isCapturing = true)
            replyTo?.send(Message.obtain(null, MSG_CAPTURE_STARTED))
        } else {
            replyTo?.send(Message.obtain(null, MSG_CAPTURE_START_FAILED))
        }
    }

    private fun performStopCapture(replyTo: Messenger?) {
        val handler = mediaProjectionHandler
        val file = handler?.stopRecording()
        mediaProjectionHandler = null

        if (handler == null || file == null) {
            replyTo?.send(Message.obtain(null, MSG_CAPTURE_STOP_FAILED))
            return
        }

        eventLogger.logEvent(BugReelEvent.RecordingStopped())
        bubbleOverlayManager.updateCaptureState(isCapturing = false)

        val eventsJson = Json.encodeToString(eventLogger.getEvents())
        eventLogger.clearEvents()
        clientMessenger = null

        val reply = Message.obtain(null, MSG_CAPTURE_STOPPED).apply {
            data = Bundle().apply {
                putString(KEY_RECORDING_PATH, file.absolutePath)
                putString(KEY_EVENT_LOG_JSON, eventsJson)
            }
        }
        replyTo?.send(reply)
    }

    private fun replyIsCapturing(replyTo: Messenger?) {
        val reply = Message.obtain(null, MSG_CAPTURE_STATE).apply {
            data = Bundle().apply {
                putBoolean(KEY_IS_CAPTURING, mediaProjectionHandler?.isRecording() == true)
            }
        }
        replyTo?.send(reply)
    }

    private fun Bundle.getProjectionIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(KEY_PROJECTION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(KEY_PROJECTION_DATA)
        }

    // Lint's HandlerLeak check assumes an Activity-scoped Handler; this one is scoped to
    // CaptureService itself, which lives exactly as long as the Handler does.
    @Suppress("HandlerLeak")
    private inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_START_CAPTURE -> {
                    val resultCode = msg.data.getInt(KEY_RESULT_CODE)
                    val projectionData = msg.data.getProjectionIntent()
                    if (projectionData == null) {
                        msg.replyTo?.send(Message.obtain(null, MSG_CAPTURE_START_FAILED))
                    } else {
                        performStartCapture(resultCode, projectionData, msg.replyTo)
                    }
                }
                MSG_STOP_CAPTURE -> performStopCapture(msg.replyTo)
                MSG_IS_CAPTURING -> replyIsCapturing(msg.replyTo)
                else -> super.handleMessage(msg)
            }
        }
    }

    companion object {
        const val ACTION_REQUEST_MEDIA_PROJECTION = "com.bugreel.pro.action.REQUEST_MEDIA_PROJECTION"

        // Messenger protocol — client to service.
        const val MSG_START_CAPTURE = 1
        const val MSG_STOP_CAPTURE = 2
        const val MSG_IS_CAPTURING = 3

        // Messenger protocol — service to client.
        const val MSG_CAPTURE_STARTED = 101
        const val MSG_CAPTURE_START_FAILED = 102
        const val MSG_CAPTURE_STOPPED = 103
        const val MSG_CAPTURE_STOP_FAILED = 104
        const val MSG_CAPTURE_STATE = 105

        const val KEY_RESULT_CODE = "result_code"
        const val KEY_PROJECTION_DATA = "projection_data"
        const val KEY_RECORDING_PATH = "recording_path"
        const val KEY_EVENT_LOG_JSON = "event_log_json"
        const val KEY_IS_CAPTURING = "is_capturing"
    }
}
