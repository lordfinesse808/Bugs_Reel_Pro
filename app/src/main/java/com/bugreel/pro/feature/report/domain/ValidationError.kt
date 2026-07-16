package com.bugreel.pro.feature.report.domain

// CLAUDE.md Sprint 2 Session 2 — BuildReportUseCase reports one of these per invalid field
// rather than a single Boolean, so ReportBuilderScreen can show inline, per-field errors.
data class ValidationError(val field: String, val message: String)
