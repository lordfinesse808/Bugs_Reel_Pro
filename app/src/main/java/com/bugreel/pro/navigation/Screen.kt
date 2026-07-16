package com.bugreel.pro.navigation

sealed class Screen(val route: String) {
    data object Capture : Screen("capture")
    data object ReportBuilder : Screen("report_builder")
    data object ReportList : Screen("report_list")
    data object EvidencePicker : Screen("evidence_picker")
}
