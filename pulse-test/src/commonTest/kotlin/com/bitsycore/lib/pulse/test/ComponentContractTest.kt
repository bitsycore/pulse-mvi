package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.component.ComponentContract
import kotlin.test.Test
import kotlin.test.assertEquals

// ── Test Component ─────────────────────────────────────────────────────────────

private object ColorComponent : ComponentContract<ColorComponent.State, ColorComponent.Intent>() {
	override val initialState = State()

	data class State(
		val red: Float = 0f,
		val green: Float = 0f,
		val blue: Float = 0f,
	)

	sealed interface Intent {
		data class SetRed(val value: Float) : Intent
		data class SetGreen(val value: Float) : Intent
		data class SetBlue(val value: Float) : Intent
		data object Reset : Intent
	}

	override fun reduce(state: State, intent: Intent): State = when (intent) {
		is Intent.SetRed -> state.copy(red = intent.value.coerceIn(0f, 1f))
		is Intent.SetGreen -> state.copy(green = intent.value.coerceIn(0f, 1f))
		is Intent.SetBlue -> state.copy(blue = intent.value.coerceIn(0f, 1f))
		Intent.Reset -> initialState
	}
}

// ── Tests ──────────────────────────────────────────────────────────────────────

class ComponentContractTest {

	@Test
	fun initialStateIsDefault() {
		assertEquals(ColorComponent.State(), ColorComponent.initialState)
	}

	@Test
	fun reduceSetsSingleChannel() {
		val state = ColorComponent.reduce(ColorComponent.initialState, ColorComponent.Intent.SetRed(0.5f))
		assertEquals(0.5f, state.red)
		assertEquals(0f, state.green)
		assertEquals(0f, state.blue)
	}

	@Test
	fun reducePreservesOtherChannels() {
		var state = ColorComponent.initialState
		state = ColorComponent.reduce(state, ColorComponent.Intent.SetRed(0.1f))
		state = ColorComponent.reduce(state, ColorComponent.Intent.SetGreen(0.2f))
		state = ColorComponent.reduce(state, ColorComponent.Intent.SetBlue(0.3f))

		assertEquals(ColorComponent.State(red = 0.1f, green = 0.2f, blue = 0.3f), state)
	}

	@Test
	fun reduceClampsValues() {
		val overMax = ColorComponent.reduce(ColorComponent.initialState, ColorComponent.Intent.SetRed(1.5f))
		assertEquals(1f, overMax.red)

		val underMin = ColorComponent.reduce(ColorComponent.initialState, ColorComponent.Intent.SetGreen(-0.5f))
		assertEquals(0f, underMin.green)
	}

	@Test
	fun resetReturnsToInitial() {
		var state = ColorComponent.initialState
		state = ColorComponent.reduce(state, ColorComponent.Intent.SetRed(0.8f))
		state = ColorComponent.reduce(state, ColorComponent.Intent.SetGreen(0.6f))
		state = ColorComponent.reduce(state, ColorComponent.Intent.SetBlue(0.4f))

		state = ColorComponent.reduce(state, ColorComponent.Intent.Reset)
		assertEquals(ColorComponent.initialState, state)
	}

	@Test
	fun componentEmbeddedInParentState() {
		// Demonstrates that a ComponentContract's state can be embedded as a field
		// in a parent container's state and reduced independently
		data class ParentState(
			val title: String = "",
			val color: ColorComponent.State = ColorComponent.initialState,
		)

		val parent = ParentState(title = "test")
		val updatedColor = ColorComponent.reduce(parent.color, ColorComponent.Intent.SetRed(1f))
		val updatedParent = parent.copy(color = updatedColor)

		assertEquals("test", updatedParent.title)
		assertEquals(1f, updatedParent.color.red)
		assertEquals(0f, updatedParent.color.green)
	}
}
