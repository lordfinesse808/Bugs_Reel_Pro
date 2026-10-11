package com.bugreel.pro.core.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.bugreel.pro.core.data.local.dao.OutcomeFeedbackDao
import com.bugreel.pro.core.data.local.dao.ReportDao
import com.bugreel.pro.core.data.local.entity.OutcomeFeedbackEntity
import com.bugreel.pro.core.data.remote.firebase.FirestoreSource
import com.bugreel.pro.core.domain.model.ConsentState
import com.bugreel.pro.core.domain.repository.ConsentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// CLAUDE.md Section 13 Track 3 — AI training opt-in is a separate data path: outcome feedback
// (the flywheel) only leaves the device for testers who opted in. Firebase is mocked (Section 12).
class SyncWorkerTest {

    private val reportDao = mockk<ReportDao> {
        coEvery { getUnsyncedReports() } returns emptyList()
    }
    private val outcomeFeedbackDao = mockk<OutcomeFeedbackDao> {
        every { getAll() } returns flowOf(listOf(feedback("r1"), feedback("r2")))
    }
    private val firestoreSource = mockk<FirestoreSource> {
        coEvery { pushOutcomeFeedback(any(), any()) } returns Result.success(Unit)
    }

    private fun feedback(reportId: String) = OutcomeFeedbackEntity(
        reportId = reportId,
        platformOutcome = "PENDING",
        rejectionReason = null,
        reportedByTesterAt = 0L,
        preAiQualityScore = 40,
        postAiQualityScore = 80,
        testerOverrodeSuggestion = false,
        aiSuggestionsAccepted = "",
        aiSuggestionsRejected = ""
    )

    private fun worker(aiTrainingOptIn: Boolean): SyncWorker {
        val consentRepository = mockk<ConsentRepository> {
            every { consentState } returns flowOf(ConsentState(consentGiven = true, aiTrainingOptIn = aiTrainingOptIn))
        }
        return SyncWorker(
            mockk<Context>(relaxed = true),
            mockk<WorkerParameters>(relaxed = true),
            reportDao,
            outcomeFeedbackDao,
            firestoreSource,
            consentRepository
        )
    }

    @Test
    fun `outcome feedback is not uploaded when tester did not opt in to AI training`() = runTest {
        val result = worker(aiTrainingOptIn = false).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { firestoreSource.pushOutcomeFeedback(any(), any()) }
    }

    @Test
    fun `outcome feedback is uploaded keyed by reportId when tester opted in`() = runTest {
        val result = worker(aiTrainingOptIn = true).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { firestoreSource.pushOutcomeFeedback("r1", any()) }
        coVerify(exactly = 1) { firestoreSource.pushOutcomeFeedback("r2", any()) }
    }

    @Test
    fun `a failed upload asks WorkManager to retry`() = runTest {
        coEvery { firestoreSource.pushOutcomeFeedback("r2", any()) } returns Result.failure(RuntimeException("offline"))

        val result = worker(aiTrainingOptIn = true).doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
