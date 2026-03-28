package com.bitsycore.demo.page1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.bitsycore.demo.colorpicker.ColorPickerContent
import com.bitsycore.lib.pulse.compose.collectAsState
import com.bitsycore.lib.pulse.compose.collectEffect
import com.bitsycore.lib.pulse.compose.onCompositionIntent
import com.bitsycore.lib.pulse.compose.onLifecycleIntent

@Composable
fun Page1Screen(
	modifier: Modifier = Modifier,
	viewModel: Page1ViewModel = viewModel { Page1ViewModel() }
) {
	val state by viewModel.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }

	// Lifecycle intents — fires on each Android/desktop lifecycle transition
	viewModel.onLifecycleIntent {
		onCreate { Page1Contract.Intent.OnCreated }
		onStart { Page1Contract.Intent.OnStarted }
		onResume { Page1Contract.Intent.OnResumed }
		onPause { Page1Contract.Intent.OnPaused }
		onStop { Page1Contract.Intent.OnStopped }
		onDestroy { Page1Contract.Intent.OnDestroyed }
	}

	// Composition intents — fires when this composable enters/recomposes/exits the tree
	viewModel.onCompositionIntent {
		onEnter { Page1Contract.Intent.OnScreenEntered }
		onRecompose { Page1Contract.Intent.OnScreenRecomposed }
		onExit { Page1Contract.Intent.OnScreenExited }
	}

	viewModel.collectEffect { effect ->
		when (effect) {
			is Page1Contract.Effect.ShowToast -> {
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
		MainContent(
			state = state,
			dispatch = viewModel::dispatch
		)
		SnackbarHost(
			hostState = snackbarHostState,
			modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
		)
	}
}

@Composable
private fun MainContent(
	state: Page1Contract.UiState,
	dispatch: (Page1Contract.Intent) -> Unit
) {
	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(
			text = "${state.count}",
			style = MaterialTheme.typography.displayLarge
		)

		Spacer(Modifier.height(24.dp))

		Row(horizontalArrangement = Arrangement.Center) {
			Button(onClick = { dispatch(Page1Contract.Intent.Decrement) }) {
				Text("-")
			}

			Spacer(Modifier.width(16.dp))

			OutlinedButton(onClick = { dispatch(Page1Contract.Intent.Reset) }) {
				Text("Reset")
			}

			Spacer(Modifier.width(16.dp))

			Button(onClick = { dispatch(Page1Contract.Intent.Increment) }) {
				Text("+")
			}
		}

		Spacer(Modifier.height(24.dp))
		HorizontalDivider()
		Spacer(Modifier.height(16.dp))

		ColorPickerContent(
			state = state.colorPicker,
			dispatch = { dispatch(Page1Contract.Intent.ColorPicker(it)) }
		)
	}
}