package com.bitsycore.demo.pulse.page2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitsycore.lib.pulse.compose.collectAsState
import com.bitsycore.lib.pulse.compose.collectEffect
import com.bitsycore.lib.pulse.compose.onCompositionIntent
import com.bitsycore.lib.pulse.compose.onLifecycleIntent
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

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


	LaunchedEffect(Unit) {
		while (true) {
			delay(1.seconds)
			viewModel.tick()
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
		Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
			AnimatedContent(
				targetState = state.count,
				transitionSpec = {
					val goingUp = targetState > initialState
					ContentTransform(
						slideInVertically { if (goingUp) -it / 2 else it } + fadeIn(),
						slideOutVertically { if (goingUp) it else -it / 2 } + fadeOut(),
						sizeTransform = SizeTransform(clip = false)
					)
				},
				label = "secondsAnimation"
			) { count ->
				Text(
					text = "$count",
					style = MaterialTheme.typography.headlineMedium,
					textAlign = TextAlign.Center
				)
			}
			Text(
				text = " seconds",
				style = MaterialTheme.typography.headlineMedium,
				textAlign = TextAlign.Center
			)
		}
	}
}

@Preview
@Composable
private fun Page2Preview() {
	var state by remember { mutableStateOf(Page2Contract.UiState()) }
	Page2Content(
		state = state,
		dispatch = { state = Page2Contract.reduce(state, it) }
	)
}