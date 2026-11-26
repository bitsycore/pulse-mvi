package com.bitsycore.lib.pulse.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bitsycore.lib.pulse.Reducer
import com.bitsycore.lib.pulse.component.ComponentContract

/**
 * A state and its reducer held by the composition: `val (state, dispatch) = rememberReducer(...)`.
 */
@Stable
public class ReducerState<STATE, INTENT> internal constructor(
	private val reducer: Reducer<STATE, INTENT>,
	initialState: STATE,
) {
	public var state: STATE by mutableStateOf(initialState)
		private set

	public val dispatch: (INTENT) -> Unit = { state = reducer.reduce(state, it) }

	public operator fun component1(): STATE = state
	public operator fun component2(): (INTENT) -> Unit = dispatch
}

/**
 * Runs a pure [reducer] in the composition, with no container behind it: a component used on
 * its own, or a `@Preview` that reacts to its buttons.
 *
 * ```
 * @Preview @Composable
 * private fun MyPreview() {
 *     val (state, dispatch) = rememberReducer(MyContract, MyContract.UiState())
 *     MyContent(state, dispatch)
 * }
 * ```
 */
@Composable
public fun <STATE, INTENT> rememberReducer(
	reducer: Reducer<STATE, INTENT>,
	initialState: STATE,
): ReducerState<STATE, INTENT> = remember(reducer) { ReducerState(reducer, initialState) }

/** A component on its own, from its initial state. */
@Composable
public fun <STATE : Any, INTENT : Any> rememberReducer(
	component: ComponentContract<STATE, INTENT>,
): ReducerState<STATE, INTENT> = rememberReducer(component, component.initialState)
