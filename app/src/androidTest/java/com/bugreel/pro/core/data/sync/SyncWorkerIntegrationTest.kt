package com.bugreel.pro.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import com.bugreel.pro.core.data.local.dao.OutcomeFeedbackDao
import com.bugreel.pro.core.data.local.dao.ReportDao
import com.bugreel.pro.core.data.remote.firebase.FirestoreSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// CLAUDE.md Section 12 — Firebase is always mocked, never hit for real. A plain
// androidx.work.WorkerFactory constructs SyncWorker directly with mocked DAOs/FirestoreSource,
// sidestepping the full Hilt graph (which would otherwise require a real FirebaseFirestore).
@RunWith(AndroidJUnit4::class)
class SyncWorkerIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val reportDao = mockk<ReportDao>(relaxed = true) {
            coEvery { getUnsyncedReports() } returns emptyList()
        }
        val outcomeFeedbackDao = mockk<OutcomeFeedbackDao>(relaxed = true) {
            coEvery { getAll() } returns flowOf(emptyList())
        }
        val firestoreSource = mockk<FirestoreSource>(relaxed = true)

        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker =
                SyncWorker(appContext, workerParameters, reportDao, outcomeFeedbackDao, firestoreSource)
        }
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(workerFactory)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun `sync_work_request_declares_a_CONNECTED_network_constraint`() {
        val request = SyncWorker.buildWorkRequest()

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
    }

    // CLAUDE.md Section 15 — "fires when network reconnects (WorkManager test API)."
    // TestDriver.setAllConstraintsMet simulates the CONNECTED constraint being satisfied
    // without needing a real network state change on the test device/emulator. Uses the
    // Flow-based WorkManager API rather than the ListenableFuture one to stay off the
    // Guava classpath entirely.
    @Test
    fun `sync_work_runs_and_succeeds_once_its_network_constraint_is_met`() = runTest {
        val workManager = WorkManager.getInstance(context)
        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
        val request = SyncWorker.buildWorkRequest()

        workManager.enqueue(request)
        testDriver.setAllConstraintsMet(request.id)

        workManager.getWorkInfoByIdFlow(request.id).test {
            var info = awaitItem()
            while (info != null && !info.state.isFinished) {
                info = awaitItem()
            }
            assertEquals(WorkInfo.State.SUCCEEDED, info?.state)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
