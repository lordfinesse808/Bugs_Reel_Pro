package com.bugreel.pro.feature.capture.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugreel.pro.feature.capture.domain.CaptureRepository

@Composable
fun CaptureScreen(
    onNavigateToReport: (CaptureRepository.CaptureResult) -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onIntent(CaptureIntent.MediaProjectionResult(result.resultCode, result.data))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Capture still works without it; only the status notification is hidden. */ }
    LaunchedEffect(Unit) { viewModel.requestNotificationPermission(notificationPermissionLauncher) }

    // Re-checked on every resume so the card disappears after the tester returns from the
    // system "Display over other apps" page.
    var overlayGranted by remember { mutableStateOf(viewModel.isOverlayPermissionGranted(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayGranted = viewModel.isOverlayPermissionGranted(context)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CaptureUiEvent.NavigateToReport -> onNavigateToReport(event.result)
                CaptureUiEvent.ShowPermissionDenied ->
                    snackbarHostState.showSnackbar("Screen recording permission was denied.")
                CaptureUiEvent.ShowOemSetup ->
                    snackbarHostState.showSnackbar(
                        "Your device may need extra setup to keep recording reliably."
                    )
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is CaptureUiState.Idle -> IdleContent(
                    showOemSetupCard = state.showOemSetupCard,
                    showOverlayCard = !overlayGranted,
                    onAllowOverlay = { viewModel.requestOverlayPermission(context) },
                    onStartCapture = {
                        viewModel.onIntent(CaptureIntent.RequestPermission)
                        viewModel.requestMediaProjection(context as Activity, launcher)
                    }
                )
                CaptureUiState.RequestingPermission -> RequestingPermissionContent()
                CaptureUiState.Capturing -> CapturingContent(
                    onStopCapture = { viewModel.onIntent(CaptureIntent.StopCapture) }
                )
                is CaptureUiState.CaptureComplete -> CaptureCompleteContent()
            }
        }
    }
}

@Composable
private fun IdleContent(
    showOemSetupCard: Boolean,
    showOverlayCard: Boolean,
    onAllowOverlay: () -> Unit,
    onStartCapture: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Capture bugs like a pro", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "BugReel records your screen and every tap so your bug reports come with " +
                "the evidence platforms accept — no more back-and-forth over rejected reports.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (showOemSetupCard) {
            OemSetupCard()
        }
        if (showOverlayCard) {
            OverlayPermissionCard(onAllowOverlay)
        }
        Button(onClick = onStartCapture) {
            Text("Start Capture")
        }
    }
}

@Composable
private fun OverlayPermissionCard(onAllowOverlay: () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Floating capture button", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Allow BugReel to display over other apps so you can stop a recording " +
                    "from inside the app you're testing.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onAllowOverlay) {
                Text("Allow floating button")
            }
        }
    }
}

@Composable
private fun OemSetupCard() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("One-time device setup", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Your device manufacturer may stop BugReel's recording in the " +
                    "background. Whitelist BugReel Pro in your battery settings to keep " +
                    "captures reliable.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RequestingPermissionContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text("Waiting for screen recording permission…")
    }
}

@Composable
private fun CapturingContent(onStopCapture: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PulsingRecordingDot()
        Text("Recording in progress", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onStopCapture) {
            Text("Stop Capture")
        }
    }
}

@Composable
private fun CaptureCompleteContent() {
    Text("Capture complete — building your report…")
}

@Composable
private fun PulsingRecordingDot() {
    val transition = rememberInfiniteTransition(label = "recording-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording-pulse-alpha"
    )
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Color.Red.copy(alpha = alpha))
    )
}
