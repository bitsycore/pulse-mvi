package com.bitsycore.demo.pulse.page1

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitsycore.demo.pulse.colorpicker.ColorPickerContent
import com.bitsycore.lib.pulse.compose.collectAsState
import com.bitsycore.lib.pulse.compose.collectEffect
import com.bitsycore.lib.pulse.compose.onCompositionIntent
import com.bitsycore.lib.pulse.compose.onLifecycleIntent

@Composable
fun Page1Screen(
	snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
	viewModel: Page1ViewModel = viewModel { Page1ViewModel(createSavedStateHandle()) }
) {
	val state by viewModel.collectAsState()

	viewModel.onLifecycleIntent {
		// Prefer Intent without lifecycle related name but for demo, simplify it
		onCreate { Page1Contract.Intent.OnLifecycle(Lifecycle.Event.ON_CREATE) }
		onStart { Page1Contract.Intent.OnLifecycle(Lifecycle.Event.ON_START) }
		onResume { Page1Contract.Intent.OnLifecycle(Lifecycle.Event.ON_RESUME) }
		onPause { Page1Contract.Intent.OnLifecycle(Lifecycle.Event.ON_PAUSE) }
		onStop { Page1Contract.Intent.OnLifecycle(Lifecycle.Event.ON_STOP) }
		onDestroy { Page1Contract.Intent.OnLifecycle(Lifecycle.Event.ON_DESTROY) }
	}

	viewModel.onCompositionIntent {
		onEnter { Page1Contract.Intent.OnScreenEntered }
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

	Page1Content(
		state = state,
		dispatch = viewModel::dispatch
	)
}

@Composable
private fun Page1Content(
	state: Page1Contract.UiState,
	dispatch: (Page1Contract.Intent) -> Unit
) {
	Column(
		modifier = Modifier.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.verticalScroll(rememberScrollState())
			.padding(vertical = 32.dp, horizontal = 8.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		AnimatedContent(
			targetState = state.count,
			transitionSpec = {
				val goingUp = targetState > initialState
				(slideInVertically { if (goingUp) -it else it/2 } + fadeIn())
					.togetherWith(slideOutVertically { if (goingUp) it/2 else -it } + fadeOut())
					.using(SizeTransform(clip = false))
			},
			label = "counterAnimation"
		) { count ->
			Text(
				text = "$count",
				style = MaterialTheme.typography.displayLarge,
				modifier = Modifier.fillMaxWidth(),
				textAlign = TextAlign.Center
			)
		}

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