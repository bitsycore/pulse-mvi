package com.bitsycore.demo.page1

import com.bitsycore.demo.colorpicker.ColorPickerComponent
import com.bitsycore.lib.pulse.container.ContainerContract
import com.bitsycore.lib.pulse.savedstate.PulseSavedStateViewModel
import kotlinx.serialization.Serializable

object Page1Contract : ContainerContract<Page1Contract.UiState, Page1Contract.Intent, Page1Contract.Effect>() {

	override val initialState = UiState()

	typealias VM = PulseSavedStateViewModel<UiState, Intent, Effect>

	@Serializable
	data class UiState(
		val count: Int = 0,
		val colorPicker: ColorPickerComponent.State = ColorPickerComponent.initialState,
	)

	sealed interface Intent {
		data object Increment : Intent
		data object Decrement : Intent
		data object Reset : Intent
		data class ColorPicker(val intent: ColorPickerComponent.Intent) : Intent

		// Lifecycle-driven
		data object OnCreated : Intent
		data object OnStarted : Intent
		data object OnResumed : Intent
		data object OnPaused : Intent
		data object OnStopped : Intent
		data object OnDestroyed : Intent

		// Composition-driven
		data object OnScreenEntered : Intent
		data object OnScreenExited : Intent
	}

	sealed interface Effect {
		data class ShowToast(val message: String) : Effect
	}
}
