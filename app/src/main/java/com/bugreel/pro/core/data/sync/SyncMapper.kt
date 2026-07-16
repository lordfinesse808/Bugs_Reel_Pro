package com.bugreel.pro.core.data.sync

import com.bugreel.pro.core.data.local.entity.OutcomeFeedbackEntity
import com.bugreel.pro.core.data.local.entity.ReportEntity
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

// CLAUDE.md Section 15 — Entity <-> Firestore document mapping. Room entities stay
// Room-only (Section 6); these private @Serializable DTOs are the wire format so
// kotlinx.serialization never needs a contextual serializer for java.time.Instant.
object SyncMapper {

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ReportDto(
        val id: String,
        val title: String,
        val summary: String,
        val reproSteps: List<String>,
        val severity: String,
        val frequency: String,
        val platform: String,
        val status: String,
        val createdAt: Long,
        val updatedAt: Long,
        val syncedAt: Long?
    )

    @Serializable
    private data class OutcomeFeedbackDto(
        val reportId: String,
        val platformOutcome: String,
        val rejectionReason: String?,
        val reportedByTesterAt: Long,
        val preAiQualityScore: Int,
        val postAiQualityScore: Int,
        val testerOverrodeSuggestion: Boolean,
        val aiSuggestionsAccepted: String,
        val aiSuggestionsRejected: String
    )

    fun reportEntityToJson(entity: ReportEntity): String = jsonFormat.encodeToString(
        ReportDto(
            id = entity.id,
            title = entity.title,
            summary = entity.summary,
            reproSteps = entity.reproSteps,
            severity = entity.severity.name,
            frequency = entity.frequency.name,
            platform = entity.platform.name,
            status = entity.status.name,
            createdAt = entity.createdAt.toEpochMilli(),
            updatedAt = entity.updatedAt.toEpochMilli(),
            syncedAt = entity.syncedAt?.toEpochMilli()
        )
    )

    // Returns null on malformed input rather than throwing — a corrupt or partial pull
    // shouldn't crash the sync pass; the caller just skips that one document.
    fun jsonToReportEntity(json: String): ReportEntity? = runCatching {
        val dto = jsonFormat.decodeFromString<ReportDto>(json)
        ReportEntity(
            id = dto.id,
            title = dto.title,
            summary = dto.summary,
            reproSteps = dto.reproSteps,
            severity = Severity.valueOf(dto.severity),
            frequency = Frequency.valueOf(dto.frequency),
            platform = TestPlatform.valueOf(dto.platform),
            status = ReportStatus.valueOf(dto.status),
            createdAt = Instant.ofEpochMilli(dto.createdAt),
            updatedAt = Instant.ofEpochMilli(dto.updatedAt),
            syncedAt = dto.syncedAt?.let(Instant::ofEpochMilli)
        )
    }.getOrNull()

    // CLAUDE.md Section 23 — THE FLYWHEEL.
    fun outcomeFeedbackEntityToJson(entity: OutcomeFeedbackEntity): String =
        jsonFormat.encodeToString(
            OutcomeFeedbackDto(
                reportId = entity.reportId,
                platformOutcome = entity.platformOutcome,
                rejectionReason = entity.rejectionReason,
                reportedByTesterAt = entity.reportedByTesterAt,
                preAiQualityScore = entity.preAiQualityScore,
                postAiQualityScore = entity.postAiQualityScore,
                testerOverrodeSuggestion = entity.testerOverrodeSuggestion,
                aiSuggestionsAccepted = entity.aiSuggestionsAccepted,
                aiSuggestionsRejected = entity.aiSuggestionsRejected
            )
        )
}
