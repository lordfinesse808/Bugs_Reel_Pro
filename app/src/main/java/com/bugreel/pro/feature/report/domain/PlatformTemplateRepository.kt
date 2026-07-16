package com.bugreel.pro.feature.report.domain

import com.bugreel.pro.core.domain.model.enums.TestPlatform

interface PlatformTemplateRepository {
    fun getTemplate(platform: TestPlatform): ReportTemplate

    data class ReportTemplate(
        val requiredFields: List<String>,
        val optionalFields: List<String>,
        val formatInstructions: String
    )
}
