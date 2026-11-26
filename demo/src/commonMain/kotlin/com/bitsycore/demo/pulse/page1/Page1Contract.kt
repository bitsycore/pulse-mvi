package com.bitsycore.demo.pulse.page1

import androidx.lifecycle.Lifecycle
import com.bitsycore.demo.pulse.colorpicker.ColorPickerComponent
import com.bitsycore.demo.pulse.palette.PaletteComponent
import com.bitsycore.lib.pulse.container.ContainerContract
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

object Page1Contract : ContainerContract<Page1Contract.UiState, Page1Contract.Intent, Page1Contract.Effect>() {

	@Serializable
	data class UiState(
		val count: Int = 0,
		val colorPicker: ColorPickerComponent.State = ColorPickerComponent.initialState,
		@Transient val palette: PaletteComponent.State = PaletteComponent.initialState,
	)

	override fun reduce(state: UiState, intent: Intent): UiState = when (intent) {
		Intent.Increment -> state.copy(count = state.count + 1)
		Intent.Decrement -> state.copy(count = state.count - 1)
		Intent.Reset -> state.copy(count = 0)
		is Intent.ColorPicker -> state.copy(
			colorPicker = ColorPickerComponent.reduce(state.colorPicker, intent.intent)
		)
		is Intent.Palette -> state.copy(palette = PaletteComponent.reduce(state.palette, intent.intent))
		// Composition: randomize color when entering the screen
		Intent.OnScreenEntered -> state.copy(
			colorPicker = ColorPickerComponent.reduce(state.colorPicker, ColorPickerComponent.Intent.Randomize)
		)
		else -> state
	}

	sealed interface Intent {
		data object Increment : Intent
		data object Decrement : Intent
		data object Reset : Intent
		data class ColorPicker(val intent: ColorPickerComponent.Intent) : Intent
		data class Palette(val intent: PaletteComponent.Intent) : Intent

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
