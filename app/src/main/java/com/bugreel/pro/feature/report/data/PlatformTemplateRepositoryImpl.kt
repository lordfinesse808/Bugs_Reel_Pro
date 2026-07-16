package com.bugreel.pro.feature.report.data

import com.bugreel.pro.core.domain.model.enums.TestPlatform
import com.bugreel.pro.feature.report.domain.PlatformTemplateRepository
import com.bugreel.pro.feature.report.domain.PlatformTemplateRepository.ReportTemplate
import javax.inject.Inject

// CLAUDE.md Section 15 — platform templates are data-driven (a map), never if/when branches,
// so adding a platform is a data change here, not a new code path elsewhere.
class PlatformTemplateRepositoryImpl @Inject constructor() : PlatformTemplateRepository {

    override fun getTemplate(platform: TestPlatform): ReportTemplate =
        TEMPLATES[platform] ?: DEFAULT_TEMPLATE

    companion object {
        private val TEMPLATES: Map<TestPlatform, ReportTemplate> = mapOf(
            TestPlatform.UTEST to ReportTemplate(
                requiredFields = listOf(
                    "title", "environment", "severity", "frequency", "reproSteps",
                    "expectedResult", "actualResult"
                ),
                optionalFields = emptyList(),
                formatInstructions = "uTest expects numbered repro steps and separate " +
                    "expected/actual result fields."
            ),
            TestPlatform.TESTLIO to ReportTemplate(
                requiredFields = listOf("title", "severity", "steps", "expectedVsActual", "deviceInfo"),
                optionalFields = emptyList(),
                formatInstructions = "Testlio combines expected and actual behaviour into a " +
                    "single expectedVsActual field."
            ),
            TestPlatform.TEST_IO to ReportTemplate(
                requiredFields = listOf(
                    "bugTitle", "severity", "stepsToReproduce", "actualBehavior", "expectedBehavior"
                ),
                optionalFields = emptyList(),
                formatInstructions = "Test.io calls the title field bugTitle and splits actual/" +
                    "expected behaviour into two fields."
            )
        )

        private val DEFAULT_TEMPLATE = ReportTemplate(
            requiredFields = listOf("title", "severity", "reproSteps", "expectedResult", "actualResult"),
            optionalFields = emptyList(),
            formatInstructions = "Generic bug report format."
        )
    }
}
