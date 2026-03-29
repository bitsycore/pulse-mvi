package com.bitsycore.demo.pulse.page2

import androidx.lifecycle.Lifecycle
import com.bitsycore.lib.pulse.container.ContainerContract
import com.bitsycore.lib.pulse.viewmodel.PulseViewModel

object Page2Contract : ContainerContract<Page2Contract.UiState, Page2Contract.Intent, Page2Contract.Effect>() {

	override val initialState = UiState()

	typealias VM = PulseViewModel<UiState, Intent, Effect>

	data class UiState(
		val count: Int = 0,
		val message: String = "Hello from Page 2",
	)

	sealed interface Intent {

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
