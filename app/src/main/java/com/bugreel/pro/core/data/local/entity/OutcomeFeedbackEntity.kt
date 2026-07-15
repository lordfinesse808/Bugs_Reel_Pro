package com.bugreel.pro.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// CLAUDE.md Section 1 / Section 23 — THE FLYWHEEL. Present in the Room schema from version 1.
@Entity(
    tableName = "outcome_feedback",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reportId")]
)
data class OutcomeFeedbackEntity(
    @PrimaryKey val reportId: String,
    val platformOutcome: String,
    val rejectionReason: String?,
    val reportedByTesterAt: Long,
    val preAiQualityScore: Int,
    val postAiQualityScore: Int,
    val testerOverrodeSuggestion: Boolean,
    val aiSuggestionsAccepted: String,
    val aiSuggestionsRejected: String
)
