package com.bugreel.pro.core.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// CLAUDE.md Architecture Rule 2 — the ONLY class permitted to call FirebaseFirestore.
// Writes are scoped to the signed-in user (FirebaseUserSession) — firestore.rules rejects any
// document whose userId isn't the caller's own auth uid.
@Singleton
class FirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userSession: FirebaseUserSession
) {

    suspend fun pushReport(reportId: String, reportJson: String): Result<Unit> = runCatching {
        firestore.collection(REPORTS_COLLECTION)
            .document(reportId)
            .set(mapOf(USER_ID_FIELD to userSession.requireUserId(), DATA_FIELD to reportJson))
            .await()
        Unit
    }

    // CLAUDE.md Section 23 — THE FLYWHEEL. Document ID is the reportId (one report produces
    // exactly one feedback row — OutcomeFeedbackEntity's primary key), so SyncWorker re-pushing
    // the full set on every run overwrites in place instead of duplicating training rows.
    suspend fun pushOutcomeFeedback(reportId: String, feedbackJson: String): Result<Unit> = runCatching {
        firestore.collection(OUTCOME_FEEDBACK_COLLECTION)
            .document(reportId)
            .set(mapOf(USER_ID_FIELD to userSession.requireUserId(), DATA_FIELD to feedbackJson))
            .await()
        Unit
    }

    // CLAUDE.md Section 7 — Firebase reads happen only on first login (pulling cloud history
    // into Room) or an explicit user-triggered sync, never during normal operation.
    suspend fun pullAllReports(userId: String): Result<List<String>> = runCatching {
        firestore.collection(REPORTS_COLLECTION)
            .whereEqualTo(USER_ID_FIELD, userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString(DATA_FIELD) }
    }

    companion object {
        private const val REPORTS_COLLECTION = "reports"
        private const val OUTCOME_FEEDBACK_COLLECTION = "outcome_feedback"
        private const val USER_ID_FIELD = "userId"
        private const val DATA_FIELD = "data"
    }
}
