package com.bugreel.pro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bugreel.pro.feature.capture.ui.CaptureScreen
import com.bugreel.pro.feature.quality.ui.QualityScoreScreen
import com.bugreel.pro.feature.quality.ui.QualityScoreViewModel
import com.bugreel.pro.feature.report.ui.EvidencePickerScreen
import com.bugreel.pro.feature.report.ui.ReportBuilderScreen
import com.bugreel.pro.feature.report.ui.ReportBuilderViewModel
import com.bugreel.pro.feature.report.ui.ReportListScreen

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.ReportList.route) {
        composable(Screen.ReportList.route) {
            ReportListScreen(
                onReportClick = { reportId ->
                    navController.navigate(Screen.QualityScore.createRoute(reportId))
                },
                onStartCapture = { navController.navigate(Screen.Capture.route) },
                // Practice Mode is Sprint 5 scope (CLAUDE.md Section 18) — no screen to
                // navigate to yet.
                onPracticeMode = {}
            )
        }

        composable(Screen.Capture.route) {
            CaptureScreen(
                onNavigateToReport = { navController.navigate(Screen.ReportBuilder.route) }
            )
        }

        composable(Screen.ReportBuilder.route) { entry ->
            val viewModel: ReportBuilderViewModel = hiltViewModel(entry)
            ReportBuilderScreen(
                viewModel = viewModel,
                onNavigateToEvidencePicker = { navController.navigate(Screen.EvidencePicker.route) },
                onSubmitted = { reportId ->
                    navController.navigate(Screen.QualityScore.createRoute(reportId)) {
                        popUpTo(Screen.ReportList.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EvidencePicker.route) { entry ->
            // Shares the ReportBuilder destination's ViewModel instance so evidence picked
            // here lands back in the same in-progress draft (CLAUDE.md Sprint 2 Session 2).
            val parentEntry = remember(entry) { navController.getBackStackEntry(Screen.ReportBuilder.route) }
            val viewModel: ReportBuilderViewModel = hiltViewModel(parentEntry)
            EvidencePickerScreen(
                viewModel = viewModel,
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QualityScore.route,
            arguments = listOf(navArgument(QualityScoreViewModel.REPORT_ID_ARG) { type = NavType.StringType })
        ) {
            QualityScoreScreen(
                onDone = { navController.popBackStack(Screen.ReportList.route, false) }
            )
        }
    }
}
