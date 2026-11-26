package com.bitsycore.demo.pulse.palette

import com.bitsycore.demo.pulse.colorpicker.ColorPickerComponent
import com.bitsycore.lib.pulse.ExperimentalPulse
import com.bitsycore.lib.pulse.component.ComponentContract
import com.bitsycore.lib.pulse.component.listSlot

/** A component made of components: a list of colour pickers, each found by its id. */
@OptIn(ExperimentalPulse::class)
object PaletteComponent : ComponentContract<PaletteComponent.State, PaletteComponent.Intent>(State()) {

	data class Swatch(val id: Int, val picker: ColorPickerComponent.State = ColorPickerComponent.initialState)

	data class State(val swatches: List<Swatch> = emptyList(), val nextId: Int = 1)

	sealed interface Intent {
		data object Add : Intent
		data class Remove(val id: Int) : Intent
		data class Picker(val id: Int, val intent: ColorPickerComponent.Intent) : Intent
	}

	private val PICKERS = listSlot(
		reducer = { vSwatch: Swatch, vIntent: ColorPickerComponent.Intent ->
			vSwatch.copy(picker = ColorPickerComponent.reduce(vSwatch.picker, vIntent))
		},
		get = State::swatches,
		set = { vState, vSwatches -> vState.copy(swatches = vSwatches) },
		idOf = Swatch::id,
	)

	override fun reduce(state: State, intent: Intent): State = when (intent) {
		Intent.Add -> state.copy(
			swatches = state.swatches + Swatch(state.nextId, ColorPickerComponent.initialState.copy(label = "Colour ${state.nextId}")),
			nextId = state.nextId + 1,
		)
		is Intent.Remove -> state.copy(swatches = state.swatches.filter { it.id != intent.id })
		is Intent.Picker -> PICKERS.reduce(state, intent.id, intent.intent)
	}
}
