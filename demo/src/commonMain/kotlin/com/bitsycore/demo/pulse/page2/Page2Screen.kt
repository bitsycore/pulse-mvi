package com.bitsycore.demo.pulse.page2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitsycore.lib.pulse.compose.collectAsState
import com.bitsycore.lib.pulse.compose.collectEffect
import com.bitsycore.lib.pulse.compose.onCompositionIntent
import com.bitsycore.lib.pulse.compose.onLifecycleIntent

@Composable
fun Page2Screen(
	snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
	viewModel: Page2ViewModel = viewModel { Page2ViewModel() }
) {
	val state by viewModel.collectAsState()

	viewModel.onLifecycleIntent {
		// Prefer Intent without lifecycle related name but for demo, simplify it
		onCreate { Page2Contract.Intent.OnLifecycle(Lifecycle.Event.ON_CREATE) }
		onStart { Page2Contract.Intent.OnLifecycle(Lifecycle.Event.ON_START) }
		onResume { Page2Contract.Intent.OnLifecycle(Lifecycle.Event.ON_RESUME) }
		onPause { Page2Contract.Intent.OnLifecycle(Lifecycle.Event.ON_PAUSE) }
		onStop { Page2Contract.Intent.OnLifecycle(Lifecycle.Event.ON_STOP) }
		onDestroy { Page2Contract.Intent.OnLifecycle(Lifecycle.Event.ON_DESTROY) }
	}

	viewModel.onCompositionIntent {
		onEnter { Page2Contract.Intent.OnScreenEntered }
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

	Page2Content(state, viewModel::dispatch)
}

@Composable
private fun Page2Content(state: Page2Contract.UiState, dispatch: (Page2Contract.Intent) -> Unit) {
	Column(
		modifier = Modifier.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.verticalScroll(rememberScrollState())
			.padding(vertical = 32.dp, horizontal = 8.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(
			text = state.message,
			style = MaterialTheme.typography.headlineMedium
		)
	}
}
