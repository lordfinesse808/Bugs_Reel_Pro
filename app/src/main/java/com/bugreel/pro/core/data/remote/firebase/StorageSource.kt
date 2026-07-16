package com.bugreel.pro.core.data.remote.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// CLAUDE.md Architecture Rule 2 — the ONLY class permitted to call FirebaseStorage.
@Singleton
class StorageSource @Inject constructor(
    private val storage: FirebaseStorage
) {

    suspend fun uploadRecording(uri: Uri, reportId: String): Result<String> = runCatching {
        val ref = storage.reference.child("$REPORTS_PATH/$reportId/$RECORDING_FILENAME")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    suspend fun uploadScreenshot(uri: Uri, reportId: String, index: Int): Result<String> =
        runCatching {
            val ref = storage.reference.child(
                "$REPORTS_PATH/$reportId/$SCREENSHOTS_DIR/screenshot_$index.png"
            )
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }

    // listAll() only lists a folder's direct children, so nested screenshots/ contents need
    // an explicit recursive walk or they'd be silently left behind.
    suspend fun deleteReportFiles(reportId: String): Result<Unit> = runCatching {
        deleteRecursively(storage.reference.child("$REPORTS_PATH/$reportId"))
    }

    private suspend fun deleteRecursively(ref: StorageReference) {
        val listing = ref.listAll().await()
        listing.items.forEach { it.delete().await() }
        listing.prefixes.forEach { deleteRecursively(it) }
    }

    companion object {
        private const val REPORTS_PATH = "reports"
        private const val RECORDING_FILENAME = "recording.mp4"
        private const val SCREENSHOTS_DIR = "screenshots"
    }
}
