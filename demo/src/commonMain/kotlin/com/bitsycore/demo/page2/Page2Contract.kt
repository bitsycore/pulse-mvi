package com.bitsycore.demo.page2

import com.bitsycore.lib.pulse.container.ContainerContract
import com.bitsycore.lib.pulse.viewmodel.PulseViewModel

object Page2Contract : ContainerContract<Page2Contract.UiState, Page2Contract.Intent, Page2Contract.Effect>() {

	override val initialState = UiState()

	typealias VM = PulseViewModel<UiState, Intent, Effect>

	data class UiState(
		val message: String = "Hello from Page 2",
	)

	sealed interface Intent {
		// Lifecycle-driven
		data object OnCreated : Intent
		data object OnStarted : Intent
		data object OnResumed : Intent
		data object OnPaused : Intent
		data object OnStopped : Intent
		data object OnDestroyed : Intent

		// Composition-driven
		data object OnScreenEntered : Intent
		data object OnScreenRecomposed : Intent
		data object OnScreenExited : Intent
	}

	sealed interface Effect {
		data class ShowToast(val message: String) : Effect
	}
}
