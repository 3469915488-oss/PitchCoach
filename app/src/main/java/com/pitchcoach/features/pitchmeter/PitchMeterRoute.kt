package com.pitchcoach.features.pitchmeter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@SuppressLint("MissingPermission")
@Composable
fun PitchMeterRoute(
    viewModel: PitchMeterViewModel,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.startMetering()
        }
    }

    PitchMeterScreen(
        uiState = uiState,
        history = historyState,
        hasAudioPermission = hasAudioPermission,
        onStart = {
            if (hasAudioPermission) {
                viewModel.startMetering()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onPauseToggle = viewModel::togglePause,
        onSave = viewModel::stopMetering,
        onDeleteSession = viewModel::deleteSession,
        onTogglePlayback = viewModel::togglePlayback,
        onModeSelected = viewModel::selectPracticeMode,
        onGuitarTuningSelected = viewModel::selectGuitarTuning,
        onGuitarStringSelected = viewModel::selectGuitarString,
        onToggleReferenceTone = viewModel::toggleReferenceTone,
    )
}
