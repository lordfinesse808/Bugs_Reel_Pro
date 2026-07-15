package com.bugreel.pro.core.domain.model

import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant

// In-progress, unsaved report. Mirrors BugReport with every field nullable except id/createdAt.
data class BugReportDraft(
    val id: String,
    val title: String?,
    val summary: String?,
    val reproSteps: List<String>?,
    val severity: Severity?,
    val frequency: Frequency?,
    val deviceProfile: DeviceProfile?,
    val evidenceBundle: EvidenceBundle?,
    val qualityScore: QualityScore?,
    val aiOutput: AiOutput?,
    val platform: TestPlatform?,
    val createdAt: Instant,
    val updatedAt: Instant?
)
