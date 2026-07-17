package com.bugreel.pro.feature.report.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

// EvidenceManager's real callers hand it content:// URIs from ActivityResultContracts.GetContent()
// (EvidencePickerScreen), where the document provider reports MIME type reliably. A plain
// android.content.ContentResolver.getType() has no special file:// handling to fall back on
// (confirmed empirically — Robolectric's shadow returns null for any URI with no registered
// provider, matching real AOSP behaviour), so this test provider stands in for that document
// provider rather than using file:// URIs directly.
private const val AUTHORITY = "com.bugreel.pro.test.evidence"

class FakeEvidenceProvider : ContentProvider() {
    private val mimeTypes = mutableMapOf<Uri, String>()
    private val backingFiles = mutableMapOf<Uri, File>()

    fun register(uri: Uri, mimeType: String, file: File) {
        mimeTypes[uri] = mimeType
        backingFiles[uri] = file
    }

    override fun onCreate() = true
    override fun getType(uri: Uri): String? = mimeTypes[uri]
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? =
        backingFiles[uri]?.let { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}

@RunWith(RobolectricTestRunner::class)
class EvidenceManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val evidenceManager = EvidenceManager(context)
    private lateinit var provider: FakeEvidenceProvider

    @Before
    fun setUp() {
        provider = Robolectric.setupContentProvider(FakeEvidenceProvider::class.java, AUTHORITY)
    }

    @Test
    fun `validateAndAttachRecording rejects a non-mp4 file`() {
        val result = evidenceManager.validateAndAttachRecording(registerUri("recording", "text/plain"))

        assertTrue(result.isFailure)
        assertNull(evidenceManager.getEvidenceBundle().recordingUri)
    }

    @Test
    fun `validateAndAttachRecording accepts an mp4 file`() {
        val uri = registerUri("recording", "video/mp4")

        val result = evidenceManager.validateAndAttachRecording(uri)

        assertTrue(result.isSuccess)
        assertEquals(uri.toString(), evidenceManager.getEvidenceBundle().recordingUri)
    }

    @Test
    fun `validateAndAttachScreenshot rejects a non-png-or-jpeg file`() {
        val result = evidenceManager.validateAndAttachScreenshot(registerUri("shot", "text/plain"))

        assertTrue(result.isFailure)
        assertEquals(0, evidenceManager.getEvidenceBundle().screenshots.size)
    }

    @Test
    fun `validateAndAttachScreenshot accepts a png file`() {
        val result = evidenceManager.validateAndAttachScreenshot(registerUri("shot", "image/png"))

        assertTrue(result.isSuccess)
        assertEquals(1, evidenceManager.getEvidenceBundle().screenshots.size)
    }

    @Test
    fun `validateAndAttachScreenshot accepts an oversized file without crashing`() {
        val uri = registerUri("shot", "image/jpeg", content = ByteArray(OVERSIZED_BYTES))

        val result = evidenceManager.validateAndAttachScreenshot(uri)

        assertTrue(result.isSuccess)
        assertEquals(1, evidenceManager.getEvidenceBundle().screenshots.size)
    }

    @Test
    fun `attachLogText stores text and getEvidenceBundle reflects it`() {
        val result = evidenceManager.attachLogText("line one\nline two")

        assertTrue(result.isSuccess)
        assertEquals("line one\nline two", evidenceManager.getEvidenceBundle().logText)
    }

    @Test
    fun `clearAll resets recording, screenshots and log text`() {
        evidenceManager.validateAndAttachRecording(registerUri("recording", "video/mp4"))
        evidenceManager.validateAndAttachScreenshot(registerUri("shot", "image/png"))
        evidenceManager.attachLogText("some log output")

        evidenceManager.clearAll()

        val bundle = evidenceManager.getEvidenceBundle()
        assertNull(bundle.recordingUri)
        assertTrue(bundle.screenshots.isEmpty())
        assertNull(bundle.logText)
    }

    private fun registerUri(name: String, mimeType: String, content: ByteArray = ByteArray(16)): Uri {
        val file = File.createTempFile("evidence", null).apply {
            writeBytes(content)
            deleteOnExit()
        }
        val uri = Uri.parse("content://$AUTHORITY/$name-${System.nanoTime()}")
        provider.register(uri, mimeType, file)
        return uri
    }

    private companion object {
        const val OVERSIZED_BYTES = 3 * 1024 * 1024
    }
}
