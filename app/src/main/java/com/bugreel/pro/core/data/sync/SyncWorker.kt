package com.bugreel.pro.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.bugreel.pro.core.data.local.dao.OutcomeFeedbackDao
import com.bugreel.pro.core.data.local.dao.ReportDao
import com.bugreel.pro.core.data.remote.firebase.FirestoreSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.concurrent.TimeUnit

// CLAUDE.md Section 7 / Architecture Rule 2 — the ONLY Firestore write path. Pushes every
// unsynced ReportEntity and every OutcomeFeedbackEntity (Section 23 — THE FLYWHEEL) row.
// Conflict resolution is Room-wins: this worker only ever pushes local -> remote, it never
// pulls during a normal sync pass (Section 7).
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reportDao: ReportDao,
    private val outcomeFeedbackDao: OutcomeFeedbackDao,
    private val firestoreSource: FirestoreSource
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reportsSynced = syncUnsyncedReports()
        val feedbackSynced = syncOutcomeFeedback()
        return when {
            reportsSynced && feedbackSynced -> Result.success()
            runAttemptCount >= MAX_RETRIES -> Result.failure()
            else -> Result.retry()
        }
    }

    private suspend fun syncUnsyncedReports(): Boolean {
        var allSucceeded = true
        reportDao.getUnsyncedReports().forEach { entity ->
            firestoreSource.pushReport(entity.id, SyncMapper.reportEntityToJson(entity))
                .onSuccess { reportDao.markSynced(entity.id, Instant.now()) }
                .onFailure { allSucceeded = false }
        }
        return allSucceeded
    }

    // OutcomeFeedbackEntity has no syncedAt column of its own (Section 23 requires it in
    // the schema starting Sprint 1, not a sync-tracking column) — re-pushing the full set
    // each run is safe because pushOutcomeFeedback's Firestore write is a plain add(), and
    // the flywheel data volume here is one row per reviewed report, not per-event.
    private suspend fun syncOutcomeFeedback(): Boolean {
        var allSucceeded = true
        outcomeFeedbackDao.getAll().first().forEach { entity ->
            firestoreSource.pushOutcomeFeedback(SyncMapper.outcomeFeedbackEntityToJson(entity))
                .onFailure { allSucceeded = false }
        }
        return allSucceeded
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val BACKOFF_DELAY_SECONDS = 30L

        fun buildWorkRequest(): WorkRequest =
            OneTimeWorkRequest.Builder(SyncWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()
    }
}
