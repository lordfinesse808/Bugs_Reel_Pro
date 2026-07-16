package com.bugreel.pro.feature.capture.domain

import android.content.Intent
import com.bugreel.pro.core.util.PermissionManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class StartCaptureUseCase @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val permissionManager: PermissionManager
) {

    suspend operator fun invoke(resultCode: Int, projectionData: Intent): Result<Unit> {
        if (captureRepository.isCaptureActive().first()) {
            return Result.failure(IllegalStateException("Capture is already active"))
        }
        if (!permissionManager.isMediaProjectionPermissionGranted()) {
            return Result.failure(SecurityException("MediaProjection permission was not granted"))
        }
        return captureRepository.startCapture(resultCode, projectionData)
    }
}
