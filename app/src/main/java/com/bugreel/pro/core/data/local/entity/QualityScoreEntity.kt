package com.bugreel.pro.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// `coaching` holds a pre-serialized JSON array of CoachingHint — no CoachingHint TypeConverter
// exists (Converters.kt is scoped to Instant/List<String>/enums only), so encoding happens
// at the repository mapping layer, not here.
@Entity(
    tableName = "quality_scores",
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
data class QualityScoreEntity(
    @PrimaryKey val reportId: String,
    val total: Int,
    val reproduction: Int,
    val evidence: Int,
    val description: Int,
    val severity: Int,
    val metadata: Int,
    val coaching: String
)
