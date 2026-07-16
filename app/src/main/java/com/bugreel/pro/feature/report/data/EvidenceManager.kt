package com.bugreel.pro.feature.report.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.bugreel.pro.core.domain.model.EvidenceBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

// CLAUDE.md Section 15 — attaches and validates evidence for one in-progress report. Unscoped
// (@Inject, no @Singleton): a fresh instance is created per ReportBuilderViewModel, matching
// that ViewModel's own lifetime — evidence from one draft must never leak into another.
class EvidenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var recordingUri: String? = null
    private val screenshotUris = mutableListOf<String>()
    private var logText: String? = null

    fun validateAndAttachRecording(uri: Uri): Result<Uri> = runCatching {
        val mimeType = context.contentResolver.getType(uri)
        require(mimeType == MIME_MP4) { "Recording must be an MP4 file (was $mimeType)" }
        recordingUri = uri.toString()
        uri
    }

    fun validateAndAttachScreenshot(uri: Uri): Result<Uri> = runCatching {
        val mimeType = context.contentResolver.getType(uri)
        require(mimeType == MIME_PNG || mimeType == MIME_JPEG) {
            "Screenshot must be a PNG or JPEG file (was $mimeType)"
        }
        val finalUri = if (fileSizeBytes(uri) > MAX_SCREENSHOT_BYTES) compress(uri) else uri
        screenshotUris += finalUri.toString()
        finalUri
    }

    fun attachLogText(text: String): Result<Unit> = runCatching {
        logText = text
    }

    // eventLog is intentionally left null here: it comes from CaptureRepository.CaptureResult,
    // a separate source this class has no knowledge of, and is merged in by the caller.
    fun getEvidenceBundle(): EvidenceBundle = EvidenceBundle(
        recordingUri = recordingUri,
        screenshots = screenshotUris.toList(),
        logText = logText,
        eventLog = null
    )

    fun clearAll() {
        recordingUri = null
        screenshotUris.clear()
        logText = null
    }

    private fun fileSizeBytes(uri: Uri): Long =
        context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L

    private fun compress(uri: Uri): Uri {
        val bitmap = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: return uri
        val outFile = File(context.cacheDir, "evidence_${screenshotUris.size}_${uri.lastPathSegment}.jpg")
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return Uri.fromFile(outFile)
    }

    companion object {
        private const val MIME_MP4 = "video/mp4"
        private const val MIME_PNG = "image/png"
        private const val MIME_JPEG = "image/jpeg"
        private const val MAX_SCREENSHOT_BYTES = 2L * 1024 * 1024
        private const val JPEG_QUALITY = 80
    }
}
