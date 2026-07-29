package com.bugreel.pro.navigation

sealed class Screen(val route: String) {
    data object Capture : Screen("capture")
    data object ReportBuilder : Screen("report_builder")
    data object ReportList : Screen("report_list")
    data object EvidencePicker : Screen("evidence_picker")
    data object AiAssist : Screen("ai_assist")
    data object QualityScore : Screen("quality_score/{reportId}") {
        fun createRoute(reportId: String) = "quality_score/$reportId"
    }
    data object AnalyticsDashboard : Screen("analytics_dashboard")
    data object PracticeMode : Screen("practice_mode")
    data object OutcomeFeedback : Screen("outcome_feedback/{reportId}") {
        fun createRoute(reportId: String) = "outcome_feedback/$reportId"
    }
}
