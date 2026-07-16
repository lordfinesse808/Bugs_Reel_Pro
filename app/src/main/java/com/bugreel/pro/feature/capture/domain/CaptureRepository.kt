package com.bugreel.pro.feature.capture.domain

import android.content.Intent
import com.bugreel.pro.feature.capture.service.BugReelEvent
import java.io.File
import kotlinx.coroutines.flow.Flow

// A raw android.media.projection.MediaProjection cannot be handed to this repository:
// CaptureService runs in a separate :capture process (CLAUDE.md Section 8), and MediaProjection
// is not Parcelable — it cannot cross a process boundary. Only the consent token the system
// handed back from the capture-permission Activity result (resultCode + data Intent) survives
// the trip; CaptureService reconstructs its own process-local MediaProjection from that token
// via MediaProjectionManager.getMediaProjection(resultCode, data).
interface CaptureRepository {

    suspend fun startCapture(resultCode: Int, projectionData: Intent): Result<Unit>

    suspend fun stopCapture(): Result<CaptureResult>

    fun isCaptureActive(): Flow<Boolean>

    data class CaptureResult(
        val recordingFile: File?,
        val eventLog: List<BugReelEvent>
    )
}
