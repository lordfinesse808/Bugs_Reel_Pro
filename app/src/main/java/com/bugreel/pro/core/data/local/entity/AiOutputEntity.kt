package com.bugreel.pro.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_outputs",
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
data class AiOutputEntity(
    @PrimaryKey val reportId: String,
    val generatedTitle: String?,
    val generatedSummary: String?,
    val formattedReproSteps: List<String>,
    val safetyPassed: Boolean,
    val blockedReason: String?
)
