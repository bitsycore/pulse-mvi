package com.bitsycore.demo.pulse.page2

import androidx.lifecycle.Lifecycle
import com.bitsycore.lib.pulse.container.ContainerContract

object Page2Contract : ContainerContract<Page2Contract.UiState, Page2Contract.Intent, Page2Contract.Effect>() {

	data class UiState(
		val count: Int = 0,
		val message: String = "Hello from Page 2",
	)

	override fun reduce(state: UiState, intent: Intent): UiState = when (intent) {
		Intent.Tick -> state.copy(count = state.count + 1)
		else -> state
	}

	sealed interface Intent {

		// Sent by the ViewModel itself, every second while the screen is shown
		data object Tick : Intent

		// Lifecycle-driven
		// Prefer Intent without lifecycle related name but for demo, simplify it
		data class OnLifecycle(val event: Lifecycle.Event) : Intent

		// Composition-driven
		data object OnScreenEntered : Intent
		data object OnScreenExited : Intent
	}

	sealed interface Effect {
		data class ShowToast(val message: String) : Effect
	}
}
