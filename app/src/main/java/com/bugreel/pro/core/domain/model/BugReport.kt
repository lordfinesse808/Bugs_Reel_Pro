package com.bugreel.pro.core.domain.model

import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant

data class BugReport(
    val id: String,
    val title: String,
    val summary: String,
    val reproSteps: List<String>,
    val severity: Severity,
    val frequency: Frequency,
    val deviceProfile: DeviceProfile,
    val evidenceBundle: EvidenceBundle,
    val qualityScore: QualityScore?,
    val aiOutput: AiOutput?,
    val platform: TestPlatform,
    val status: ReportStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncedAt: Instant?,
    val outcomeFeedback: OutcomeFeedback?
)
