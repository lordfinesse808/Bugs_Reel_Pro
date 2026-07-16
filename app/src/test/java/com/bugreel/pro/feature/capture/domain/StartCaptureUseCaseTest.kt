package com.bugreel.pro.feature.capture.domain

import android.content.Intent
import com.bugreel.pro.core.util.PermissionManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StartCaptureUseCaseTest {

    @Test
    fun `returns failure when capture is already active`() = runTest {
        val captureRepository = mockk<CaptureRepository> {
            every { isCaptureActive() } returns flowOf(true)
        }
        val permissionManager = mockk<PermissionManager>()
        val useCase = StartCaptureUseCase(captureRepository, permissionManager)

        val result = useCase(resultCode = -1, projectionData = mockk<Intent>())

        assertTrue(result.isFailure)
    }
}
