package com.bugreel.pro.core.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// CLAUDE.md Architecture Rule 2 — the ONLY class permitted to call FirebaseFirestore.
// FirebaseAuth is injected (rather than added to every method signature) so writes are
// scoped to the signed-in user without every caller having to thread a userId through.
@Singleton
class FirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ANONYMOUS_USER_ID

    suspend fun pushReport(reportId: String, reportJson: String): Result<Unit> = runCatching {
        firestore.collection(REPORTS_COLLECTION)
            .document(reportId)
            .set(mapOf(USER_ID_FIELD to currentUserId, DATA_FIELD to reportJson))
            .await()
        Unit
    }

    // CLAUDE.md Section 23 — THE FLYWHEEL. Uses an auto-generated document ID: the entity's
    // reportId is already embedded in feedbackJson via SyncMapper, and one report can only
    // ever produce one feedback row (OutcomeFeedbackEntity's primary key is reportId).
    suspend fun pushOutcomeFeedback(feedbackJson: String): Result<Unit> = runCatching {
        firestore.collection(OUTCOME_FEEDBACK_COLLECTION)
            .add(mapOf(USER_ID_FIELD to currentUserId, DATA_FIELD to feedbackJson))
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
        private const val ANONYMOUS_USER_ID = "anonymous"
    }
}
