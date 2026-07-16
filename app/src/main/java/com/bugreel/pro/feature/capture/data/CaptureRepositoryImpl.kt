package com.bugreel.pro.feature.capture.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.bugreel.pro.feature.capture.domain.CaptureRepository
import com.bugreel.pro.feature.capture.service.BugReelEvent
import com.bugreel.pro.feature.capture.service.CaptureService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// Talks to CaptureService (running in the separate :capture process, CLAUDE.md Section 8) over
// the Messenger protocol it exposes — see CaptureService's MSG_*/KEY_* companion constants.
// Only one start and one stop call is expected in flight at a time (StartCaptureUseCase and
// StopCaptureUseCase both check isCaptureActive() first), so a single pending-continuation slot
// per call is sufficient for Sprint 1.
@Singleton
class CaptureRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CaptureRepository {

    private val _isCaptureActive = MutableStateFlow(false)

    private var serviceMessenger: Messenger? = null
    private val pendingBindActions = mutableListOf<() -> Unit>()

    private var pendingStart: Continuation<Result<Unit>>? = null
    private var pendingStop: Continuation<Result<CaptureRepository.CaptureResult>>? = null

    private val replyMessenger = Messenger(
        Handler(Looper.getMainLooper()) { message ->
            handleReply(message)
            true
        }
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            serviceMessenger = binder?.let { Messenger(it) }
            val actions = pendingBindActions.toList()
            pendingBindActions.clear()
            actions.forEach { it.invoke() }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceMessenger = null
            _isCaptureActive.value = false
        }
    }

    init {
        ensureBound { sendToService(Message.obtain(null, CaptureService.MSG_IS_CAPTURING)) }
    }

    override suspend fun startCapture(resultCode: Int, projectionData: Intent): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            pendingStart = continuation
            continuation.invokeOnCancellation { pendingStart = null }
            ensureBound {
                val message = Message.obtain(null, CaptureService.MSG_START_CAPTURE)
                message.data = Bundle().apply {
                    putInt(CaptureService.KEY_RESULT_CODE, resultCode)
                    putParcelable(CaptureService.KEY_PROJECTION_DATA, projectionData)
                }
                sendToService(message)
            }
        }

    override suspend fun stopCapture(): Result<CaptureRepository.CaptureResult> =
        suspendCancellableCoroutine { continuation ->
            pendingStop = continuation
            continuation.invokeOnCancellation { pendingStop = null }
            ensureBound { sendToService(Message.obtain(null, CaptureService.MSG_STOP_CAPTURE)) }
        }

    override fun isCaptureActive(): Flow<Boolean> = _isCaptureActive.asStateFlow()

    private fun handleReply(message: Message) {
        when (message.what) {
            CaptureService.MSG_CAPTURE_STARTED -> {
                _isCaptureActive.value = true
                pendingStart?.resume(Result.success(Unit))
                pendingStart = null
            }
            CaptureService.MSG_CAPTURE_START_FAILED -> {
                pendingStart?.resume(Result.failure(IllegalStateException("Failed to start capture")))
                pendingStart = null
            }
            CaptureService.MSG_CAPTURE_STOPPED -> {
                _isCaptureActive.value = false
                val path = message.data.getString(CaptureService.KEY_RECORDING_PATH)
                val eventsJson = message.data.getString(CaptureService.KEY_EVENT_LOG_JSON)
                val events = eventsJson
                    ?.let { Json.decodeFromString<List<BugReelEvent>>(it) }
                    ?: emptyList()
                val result = CaptureRepository.CaptureResult(
                    recordingFile = path?.let(::File),
                    eventLog = events
                )
                pendingStop?.resume(Result.success(result))
                pendingStop = null
            }
            CaptureService.MSG_CAPTURE_STOP_FAILED -> {
                pendingStop?.resume(Result.failure(IllegalStateException("Failed to stop capture")))
                pendingStop = null
            }
            CaptureService.MSG_CAPTURE_STATE ->
                _isCaptureActive.value = message.data.getBoolean(CaptureService.KEY_IS_CAPTURING)
        }
    }

    private fun ensureBound(action: () -> Unit) {
        if (serviceMessenger != null) {
            action()
            return
        }
        pendingBindActions.add(action)
        val intent = Intent(context, CaptureService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun sendToService(message: Message) {
        message.replyTo = replyMessenger
        serviceMessenger?.send(message)
    }
}
