package com.bitsycore.demo.page2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitsycore.lib.pulse.compose.collectAsState
import com.bitsycore.lib.pulse.compose.collectEffect
import com.bitsycore.lib.pulse.compose.onCompositionIntent
import com.bitsycore.lib.pulse.compose.onLifecycleIntent

@Composable
fun Page2Screen(
	modifier: Modifier = Modifier,
	viewModel: Page2ViewModel = viewModel { Page2ViewModel() }
) {
	val state by viewModel.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }

	viewModel.onLifecycleIntent {
		onCreate { Page2Contract.Intent.OnCreated }
		onStart { Page2Contract.Intent.OnStarted }
		onResume { Page2Contract.Intent.OnResumed }
		onPause { Page2Contract.Intent.OnPaused }
		onStop { Page2Contract.Intent.OnStopped }
		onDestroy { Page2Contract.Intent.OnDestroyed }
	}

	viewModel.onCompositionIntent {
		onEnter { Page2Contract.Intent.OnScreenEntered }
		onRecompose { Page2Contract.Intent.OnScreenRecomposed }
		onExit { Page2Contract.Intent.OnScreenExited }
	}

	viewModel.collectEffect { effect ->
		when (effect) {
			is Page2Contract.Effect.ShowToast -> {
				snackbarHostState.currentSnackbarData?.dismiss()
				snackbarHostState.showSnackbar(
					message = effect.message,
					duration = SnackbarDuration.Short,
					withDismissAction = true
				)
			}
		}
	}

	Box(modifier.fillMaxSize()) {
		Column(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = state.message,
				style = MaterialTheme.typography.headlineMedium
			)
		}
		SnackbarHost(
			hostState = snackbarHostState,
			modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
		)
	}
}
