package com.bugreel.pro.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidence",
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
data class EvidenceEntity(
    @PrimaryKey val reportId: String,
    val recordingUri: String?,
    val screenshots: List<String>,
    val logText: String?,
    val eventLog: String?
)
