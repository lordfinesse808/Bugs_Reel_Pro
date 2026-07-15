package com.bugreel.pro.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant

// In-progress, unsaved report. Nested domain objects (DeviceProfile, EvidenceBundle,
// QualityScore, AiOutput) are stored as pre-serialized JSON — no relational FK to reports
// since a draft isn't a report yet.
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val summary: String?,
    val reproSteps: List<String>?,
    val severity: Severity?,
    val frequency: Frequency?,
    val platform: TestPlatform?,
    val deviceProfileJson: String?,
    val evidenceBundleJson: String?,
    val qualityScoreJson: String?,
    val aiOutputJson: String?,
    val createdAt: Instant,
    val updatedAt: Instant?
)
