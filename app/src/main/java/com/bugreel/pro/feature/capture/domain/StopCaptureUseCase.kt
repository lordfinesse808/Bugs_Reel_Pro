package com.bugreel.pro.feature.capture.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.first

class StopCaptureUseCase @Inject constructor(
    private val captureRepository: CaptureRepository
) {

    suspend operator fun invoke(): Result<CaptureRepository.CaptureResult> {
        if (!captureRepository.isCaptureActive().first()) {
            return Result.failure(IllegalStateException("No capture is active"))
        }
        return captureRepository.stopCapture()
    }
}
