package com.bugreel.pro.feature.report.data

import android.content.Context
import android.net.Uri
import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.EvidenceBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// CLAUDE.md Section 15 — fixed ZIP structure: recording.mp4, screenshots/screenshot_N.png,
// log.txt, report.json. Platform-specific formatting is PlatformTemplateRepository's job, not
// this class's — report.json is the same shape for every platform.
class ZipGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @Serializable
    private data class ReportJson(
        val id: String,
        val title: String,
        val summary: String,
        val reproSteps: List<String>,
        val severity: String,
        val frequency: String,
        val platform: String,
        val device: DeviceJson,
        val createdAt: String
    )

    @Serializable
    private data class DeviceJson(
        val brand: String,
        val model: String,
        val osVersion: String,
        val screenResolution: String
    )

    fun generateZip(report: BugReport, evidence: EvidenceBundle): Result<File> = runCatching {
        val outFile = File(context.cacheDir, "report_${report.id}.zip")
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            evidence.recordingUri?.let { writeUriEntry(zip, Uri.parse(it), "recording.mp4") }
            evidence.screenshots.forEachIndexed { index, uriString ->
                writeUriEntry(zip, Uri.parse(uriString), "screenshots/screenshot_$index.png")
            }
            writeTextEntry(zip, "log.txt", evidence.logText.orEmpty())
            writeTextEntry(zip, "report.json", reportJson(report))
        }
        outFile
    }

    private fun writeUriEntry(zip: ZipOutputStream, uri: Uri, entryName: String) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            zip.putNextEntry(ZipEntry(entryName))
            input.copyTo(zip)
            zip.closeEntry()
        }
    }

    private fun writeTextEntry(zip: ZipOutputStream, entryName: String, text: String) {
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(text.toByteArray())
        zip.closeEntry()
    }

    private fun reportJson(report: BugReport): String = Json.encodeToString(
        ReportJson(
            id = report.id,
            title = report.title,
            summary = report.summary,
            reproSteps = report.reproSteps,
            severity = report.severity.name,
            frequency = report.frequency.name,
            platform = report.platform.name,
            device = DeviceJson(
                brand = report.deviceProfile.brand,
                model = report.deviceProfile.model,
                osVersion = report.deviceProfile.osVersion,
                screenResolution = report.deviceProfile.screenResolution
            ),
            createdAt = report.createdAt.toString()
        )
    )
}
