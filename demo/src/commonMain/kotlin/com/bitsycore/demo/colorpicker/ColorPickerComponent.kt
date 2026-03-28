package com.bitsycore.demo.colorpicker

import com.bitsycore.lib.pulse.component.ComponentContract
import kotlinx.serialization.Serializable

object ColorPickerComponent : ComponentContract<ColorPickerComponent.State, ColorPickerComponent.Intent>() {

	override val initialState = State()

	@Serializable
	data class State(
		val red: Float = 0.5f,
		val green: Float = 0.3f,
		val blue: Float = 0.8f,
		val label: String = "Pick a color",
		val showHex: Boolean = true,
	) {
		val hex: String
			get() = "#%02X%02X%02X".format(
				(red * 255).toInt(),
				(green * 255).toInt(),
				(blue * 255).toInt()
			)
	}

	sealed interface Intent {
		data class SetRed(val value: Float) : Intent
		data class SetGreen(val value: Float) : Intent
		data class SetBlue(val value: Float) : Intent
		data object Randomize : Intent
		data object ToggleHex : Intent
	}

	override fun reduce(state: State, intent: Intent): State = when (intent) {
		is Intent.SetRed -> state.copy(red = intent.value)
		is Intent.SetGreen -> state.copy(green = intent.value)
		is Intent.SetBlue -> state.copy(blue = intent.value)
		Intent.Randomize -> state.copy(
			red = Math.random().toFloat(),
			green = Math.random().toFloat(),
			blue = Math.random().toFloat()
		)
		Intent.ToggleHex -> state.copy(showHex = !state.showHex)
	}
}
