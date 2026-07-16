package com.bugreel.pro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bugreel.pro.feature.capture.ui.CaptureScreen

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Capture.route) {
        composable(Screen.Capture.route) {
            CaptureScreen(
                onNavigateToReport = { navController.navigate(Screen.ReportBuilder.route) }
            )
        }
        composable(Screen.ReportBuilder.route) {
            ReportBuilderStub()
        }
    }
}

// Sprint 2 builds the real ReportBuilderScreen (CLAUDE.md Section 15) — this stub only keeps
// CaptureScreen's navigation call valid until then. It intentionally does not receive the
// CaptureResult; wiring that hand-off is Sprint 2 scope.
@Composable
private fun ReportBuilderStub() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Report Builder — coming in Sprint 2",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
