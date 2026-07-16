package com.bugreel.pro.feature.capture.domain

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StopCaptureUseCaseTest {

    @Test
    fun `returns failure when no capture was active`() = runTest {
        val captureRepository = mockk<CaptureRepository> {
            every { isCaptureActive() } returns flowOf(false)
        }
        val useCase = StopCaptureUseCase(captureRepository)

        val result = useCase()

        assertTrue(result.isFailure)
    }
}
