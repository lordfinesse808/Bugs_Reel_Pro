package com.bugreel.pro.feature.report.domain

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.feature.report.data.ZipGenerator
import java.io.File
import javax.inject.Inject

class ExportReportUseCase @Inject constructor(
    private val zipGenerator: ZipGenerator
) {
    suspend operator fun invoke(report: BugReport): Result<File> =
        zipGenerator.generateZip(report, report.evidenceBundle)
}
