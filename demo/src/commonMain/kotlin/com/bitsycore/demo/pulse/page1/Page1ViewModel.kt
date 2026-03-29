package com.bitsycore.demo.pulse.page1

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle

class Page1ViewModel(savedStateHandle: SavedStateHandle) : Page1Contract.VM(
	containerContract = Page1Contract,
	savedStateHandle = savedStateHandle,
	serializer = Page1Contract.UiState.serializer()
) {

	override suspend fun handleIntent(intent: Page1Contract.Intent) {
		when (intent) {
			Page1Contract.Intent.Reset -> emitEffect(Page1Contract.Effect.ShowToast("Counter reset!"))

			// Log all lifecycle events
			is Page1Contract.Intent.OnLifecycle -> {
				when(intent.event) {
                    Lifecycle.Event.ON_CREATE -> println("[Page1][Lifecycle] onCreate")
                    Lifecycle.Event.ON_START -> {
						emitEffect(Page1Contract.Effect.ShowToast("onStart"))
						println("[Page1][Lifecycle] onStart")
					}
                    Lifecycle.Event.ON_RESUME -> println("[Page1][Lifecycle] onResume")
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

	fun add() = dispatchCustom {
        reduce {
            copy(count = count + 1)
        }
		handle {
			emitEffect(Page1Contract.Effect.ShowToast("Counter incremented!"))
		}
    }

	override fun onCleared() = println("[Page1][ViewModel] onCleared")
}
