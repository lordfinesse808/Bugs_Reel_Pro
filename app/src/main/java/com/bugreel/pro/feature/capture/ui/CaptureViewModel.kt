package com.bugreel.pro.feature.capture.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.core.util.OemCompat
import com.bugreel.pro.core.util.PermissionManager
import com.bugreel.pro.feature.capture.domain.CaptureRepository
import com.bugreel.pro.feature.capture.domain.StartCaptureUseCase
import com.bugreel.pro.feature.capture.domain.StopCaptureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CaptureUiState {
    data class Idle(val showOemSetupCard: Boolean) : CaptureUiState()
    data object RequestingPermission : CaptureUiState()
    data object Capturing : CaptureUiState()
    data class CaptureComplete(val result: CaptureRepository.CaptureResult) : CaptureUiState()
}

sealed class CaptureUiEvent {
    data class NavigateToReport(val result: CaptureRepository.CaptureResult) : CaptureUiEvent()
    data object ShowPermissionDenied : CaptureUiEvent()
    data object ShowOemSetup : CaptureUiEvent()
}

sealed class CaptureIntent {
    data object RequestPermission : CaptureIntent()
    data class MediaProjectionResult(val resultCode: Int, val data: Intent?) : CaptureIntent()
    data object StopCapture : CaptureIntent()
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val startCaptureUseCase: StartCaptureUseCase,
    private val stopCaptureUseCase: StopCaptureUseCase,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(idleState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CaptureUiEvent>()
    val events: SharedFlow<CaptureUiEvent> = _events.asSharedFlow()

    init {
        if (OemCompat.requiresManualBatteryOptimizationWhitelist()) {
            viewModelScope.launch { _events.emit(CaptureUiEvent.ShowOemSetup) }
        }
    }

    fun onIntent(intent: CaptureIntent) {
        when (intent) {
            CaptureIntent.RequestPermission -> _uiState.value = CaptureUiState.RequestingPermission
            is CaptureIntent.MediaProjectionResult ->
                handleMediaProjectionResult(intent.resultCode, intent.data)
            CaptureIntent.StopCapture -> stopCapture()
        }
    }

    // Screens call the ViewModel rather than PermissionManager directly (CLAUDE.md
    // Anti-Pattern 3); the ActivityResultLauncher itself must still be created in the
    // Composable via rememberLauncherForActivityResult — that part is a Compose API
    // constraint, not a permission-request call.
    fun requestMediaProjection(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        permissionManager.requestMediaProjection(activity, launcher)
    }

    private fun handleMediaProjectionResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            permissionManager.mediaProjectionGranted = false
            _uiState.value = idleState()
            viewModelScope.launch { _events.emit(CaptureUiEvent.ShowPermissionDenied) }
            return
        }

        permissionManager.mediaProjectionGranted = true
        viewModelScope.launch {
            startCaptureUseCase(resultCode, data).fold(
                onSuccess = { _uiState.value = CaptureUiState.Capturing },
                onFailure = {
                    _uiState.value = idleState()
                    _events.emit(CaptureUiEvent.ShowPermissionDenied)
                }
            )
        }
    }

    private fun stopCapture() {
        viewModelScope.launch {
            stopCaptureUseCase().fold(
                onSuccess = { result ->
                    _uiState.value = CaptureUiState.CaptureComplete(result)
                    _events.emit(CaptureUiEvent.NavigateToReport(result))
                },
                onFailure = { _uiState.value = idleState() }
            )
        }
    }

    private fun idleState() =
        CaptureUiState.Idle(showOemSetupCard = OemCompat.requiresManualBatteryOptimizationWhitelist())
}
