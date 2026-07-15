package com.bugreel.pro.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val reproSteps: List<String>,
    val severity: Severity,
    val frequency: Frequency,
    val platform: TestPlatform,
    val status: ReportStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncedAt: Instant?
)
