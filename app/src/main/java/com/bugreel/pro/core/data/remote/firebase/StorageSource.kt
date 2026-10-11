package com.bugreel.pro.core.data.remote.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// CLAUDE.md Architecture Rule 2 — the ONLY class permitted to call FirebaseStorage.
// Every path sits under users/{uid}/ so storage.rules can check ownership from the path alone.
@Singleton
class StorageSource @Inject constructor(
    private val storage: FirebaseStorage,
    private val userSession: FirebaseUserSession
) {

    suspend fun uploadRecording(uri: Uri, reportId: String): Result<String> = runCatching {
        val ref = reportRef(reportId).child(RECORDING_FILENAME)
        ref.putFile(uri, storageMetadata { contentType = MP4_CONTENT_TYPE }).await()
        ref.downloadUrl.await().toString()
    }

    suspend fun uploadScreenshot(uri: Uri, reportId: String, index: Int): Result<String> =
        runCatching {
            val ref = reportRef(reportId).child("$SCREENSHOTS_DIR/screenshot_$index.png")
            ref.putFile(uri, storageMetadata { contentType = PNG_CONTENT_TYPE }).await()
            ref.downloadUrl.await().toString()
        }

    // listAll() only lists a folder's direct children, so nested screenshots/ contents need
    // an explicit recursive walk or they'd be silently left behind.
    suspend fun deleteReportFiles(reportId: String): Result<Unit> = runCatching {
        deleteRecursively(reportRef(reportId))
    }

    private suspend fun reportRef(reportId: String): StorageReference =
        storage.reference.child("$USERS_PATH/${userSession.requireUserId()}/$REPORTS_PATH/$reportId")

    private suspend fun deleteRecursively(ref: StorageReference) {
        val listing = ref.listAll().await()
        listing.items.forEach { it.delete().await() }
        listing.prefixes.forEach { deleteRecursively(it) }
    }

    companion object {
        private const val USERS_PATH = "users"
        private const val REPORTS_PATH = "reports"
        private const val RECORDING_FILENAME = "recording.mp4"
        private const val SCREENSHOTS_DIR = "screenshots"

        // storage.rules only accepts these exact content types; set explicitly rather than
        // relying on putFile() inferring one from the Uri.
        private const val MP4_CONTENT_TYPE = "video/mp4"
        private const val PNG_CONTENT_TYPE = "image/png"
    }
}
