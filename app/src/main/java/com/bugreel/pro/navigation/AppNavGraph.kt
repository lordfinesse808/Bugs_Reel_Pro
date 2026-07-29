package com.bugreel.pro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bugreel.pro.core.domain.model.ReportContext
import com.bugreel.pro.feature.ai.ui.AiAssistResult
import com.bugreel.pro.feature.ai.ui.AiAssistScreen
import com.bugreel.pro.feature.ai.ui.AiAssistViewModel
import com.bugreel.pro.feature.analytics.ui.AnalyticsDashboardScreen
import com.bugreel.pro.feature.analytics.ui.OutcomeFeedbackScreen
import com.bugreel.pro.feature.analytics.ui.OutcomeFeedbackViewModel
import com.bugreel.pro.feature.analytics.ui.PracticeModeScreen
import com.bugreel.pro.feature.capture.ui.CaptureScreen
import com.bugreel.pro.feature.quality.ui.QualityScoreScreen
import com.bugreel.pro.feature.quality.ui.QualityScoreViewModel
import com.bugreel.pro.feature.report.ui.EvidencePickerScreen
import com.bugreel.pro.feature.report.ui.ReportBuilderIntent
import com.bugreel.pro.feature.report.ui.ReportBuilderScreen
import com.bugreel.pro.feature.report.ui.ReportBuilderViewModel
import com.bugreel.pro.feature.report.ui.ReportListScreen

// Keys for handing data between ReportBuilder and AiAssist through Compose Navigation's
// SavedStateHandle "returning a result" pattern (developer.android.com/guide/navigation/
// navigation-programmatic#returning_a_result). Neither ReportContext nor AiAssistResult is
// Parcelable; this works because the hand-off is same-process and short-lived (one screen
// transition), which is the documented trade-off of that pattern -- it isn't restoration-safe
// across process death, acceptable for this bounded interaction.
private const val AI_ASSIST_CONTEXT_KEY = "aiAssistContext"
private const val AI_ASSIST_RESULT_KEY = "aiAssistResult"

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.ReportList.route) {
        composable(Screen.ReportList.route) {
            ReportListScreen(
                onReportClick = { reportId ->
                    navController.navigate(Screen.QualityScore.createRoute(reportId))
                },
                onStartCapture = { navController.navigate(Screen.Capture.route) },
                onPracticeMode = { navController.navigate(Screen.PracticeMode.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.AnalyticsDashboard.route) }
            )
        }

        composable(Screen.AnalyticsDashboard.route) {
            AnalyticsDashboardScreen()
        }

        composable(Screen.PracticeMode.route) {
            PracticeModeScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Capture.route) {
            CaptureScreen(
                onNavigateToReport = { navController.navigate(Screen.ReportBuilder.route) }
            )
        }

        composable(Screen.ReportBuilder.route) { entry ->
            val viewModel: ReportBuilderViewModel = hiltViewModel(entry)

            val aiResult = entry.savedStateHandle.get<AiAssistResult>(AI_ASSIST_RESULT_KEY)
            LaunchedEffect(aiResult) {
                aiResult?.let { result ->
                    viewModel.onIntent(
                        ReportBuilderIntent.AiSuggestionsApplied(
                            title = result.title,
                            summary = result.summary,
                            reproSteps = result.reproSteps,
                            aiOutput = result.aiOutput
                        )
                    )
                    entry.savedStateHandle.remove<AiAssistResult>(AI_ASSIST_RESULT_KEY)
                }
            }

            ReportBuilderScreen(
                viewModel = viewModel,
                onNavigateToEvidencePicker = { navController.navigate(Screen.EvidencePicker.route) },
                onNavigateToAiAssist = { context ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(AI_ASSIST_CONTEXT_KEY, context)
                    navController.navigate(Screen.AiAssist.route)
                },
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

        composable(Screen.AiAssist.route) { entry ->
            val reportContext = navController.previousBackStackEntry
                ?.savedStateHandle?.get<ReportContext>(AI_ASSIST_CONTEXT_KEY)
            val viewModel: AiAssistViewModel = hiltViewModel(entry)
            LaunchedEffect(reportContext) { reportContext?.let(viewModel::initialize) }

            AiAssistScreen(
                viewModel = viewModel,
                onApply = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(AI_ASSIST_RESULT_KEY, result)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QualityScore.route,
            arguments = listOf(navArgument(QualityScoreViewModel.REPORT_ID_ARG) { type = NavType.StringType })
        ) { entry ->
            val reportId = checkNotNull(entry.arguments?.getString(QualityScoreViewModel.REPORT_ID_ARG))
            QualityScoreScreen(
                onDone = { navController.popBackStack(Screen.ReportList.route, false) },
                onReportOutcome = { navController.navigate(Screen.OutcomeFeedback.createRoute(reportId)) }
            )
        }

        composable(
            route = Screen.OutcomeFeedback.route,
            arguments = listOf(navArgument(OutcomeFeedbackViewModel.REPORT_ID_ARG) { type = NavType.StringType })
        ) {
            OutcomeFeedbackScreen(onDone = { navController.popBackStack() })
        }
    }
}
