package com.pitchcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pitchcoach.design.theme.PitchCoachTheme
import com.pitchcoach.features.pitchmeter.PitchMeterRoute
import com.pitchcoach.features.pitchmeter.PitchMeterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PitchCoachTheme {
                val context = LocalContext.current
                val viewModel: PitchMeterViewModel = viewModel(
                    factory = PitchMeterViewModel.factory(context.applicationContext),
                )
                PitchMeterRoute(viewModel = viewModel)
            }
        }
    }
}
