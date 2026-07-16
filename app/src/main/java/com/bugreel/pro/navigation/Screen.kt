package com.bugreel.pro.navigation

sealed class Screen(val route: String) {
    data object Capture : Screen("capture")
    data object ReportBuilder : Screen("report_builder")
}
