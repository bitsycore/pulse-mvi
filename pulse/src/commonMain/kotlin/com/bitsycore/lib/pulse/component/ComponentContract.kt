package com.bitsycore.lib.pulse.component

import com.bitsycore.lib.pulse.Reducer

/**
 * A reusable piece of state with its own intents and pure reducer, and nothing else: no effects,
 * no coroutines. Whoever holds it (a screen's contract, another component, a composable) keeps
 * its state, hands it its intents, and does the async work its intents ask for.
 *
 * Embedding it takes a field in the parent's state, a wrapping intent, and one line in the
 * parent's reducer:
 * ```
 * data class UiState(val picker: ColorPicker.State = ColorPicker.initialState)
 * sealed interface Intent { data class Picker(val intent: ColorPicker.Intent) : Intent }
 * is Intent.Picker -> state.copy(picker = ColorPicker.reduce(state.picker, intent.intent))
 * ```
 * and the parent reacts to its child by matching `intent.intent` there, or in its side effects.
 */
public abstract class ComponentContract<STATE : Any, INTENT : Any>(public val initialState: STATE) : Reducer<STATE, INTENT> {
	abstract override fun reduce(state: STATE, intent: INTENT): STATE
}
