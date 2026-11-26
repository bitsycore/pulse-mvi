package com.bitsycore.demo.pulse.page1

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import com.bitsycore.lib.pulse.viewmodel.PulseViewModel
import com.bitsycore.lib.pulse.viewmodel.stateSaver
import kotlinx.serialization.json.Json

class Page1ViewModel(savedStateHandle: SavedStateHandle) :
	PulseViewModel<Page1Contract.UiState, Page1Contract.Intent, Page1Contract.Effect>(
		containerContract = Page1Contract,
		initialState = Page1Contract.UiState(),
		// The state survives the process through the SavedStateHandle; how it is written is ours.
		saver = savedStateHandle.stateSaver(
			encode = { Json.encodeToString(Page1Contract.UiState.serializer(), it) },
			decode = { Json.decodeFromString(Page1Contract.UiState.serializer(), it) },
			onDecodeError = { println("[Page1][SavedState] could not be read: $it") },
		),
	) {

	// A failing side effect is reported, and the screen lives on.
	override fun onError(intent: Page1Contract.Intent?, error: Exception) =
		emitEffect(Page1Contract.Effect.ShowToast("Failed: ${error.message}"))


	override suspend fun handleIntent(intent: Page1Contract.Intent) {
		when (intent) {
			Page1Contract.Intent.Reset -> emitEffect(Page1Contract.Effect.ShowToast("Counter reset!"))
			// Log all lifecycle events
			is Page1Contract.Intent.OnLifecycle -> {
				when(intent.event) {
                    Lifecycle.Event.ON_CREATE -> {
						emitEffect(Page1Contract.Effect.ShowToast("onCreate"))
						println("[Page1][Lifecycle] onCreate")
					}
                    Lifecycle.Event.ON_START -> {
						emitEffect(Page1Contract.Effect.ShowToast("onStart"))
						println("[Page1][Lifecycle] onStart")
					}
                    Lifecycle.Event.ON_RESUME -> {
						emitEffect(Page1Contract.Effect.ShowToast("onResume"))
						println("[Page1][Lifecycle] onResume")
					}
                    Lifecycle.Event.ON_PAUSE -> println("[Page1][Lifecycle] onPause")
                    Lifecycle.Event.ON_STOP -> println("[Page1][Lifecycle] onStop")
                    Lifecycle.Event.ON_DESTROY -> println("[Page1][Lifecycle] onDestroy")
                    else -> {}
                }

            }

			// Log all composition events
			Page1Contract.Intent.OnScreenEntered -> println("[Page1][Composition] onEnter")
			Page1Contract.Intent.OnScreenExited -> println("[Page1][Composition] onExit")

			else -> {}
		}
	}

	override fun onCleared() = println("[Page1][ViewModel] onCleared")
}
