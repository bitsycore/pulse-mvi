package com.bitsycore.lib.pulse.container

import com.bitsycore.lib.pulse.Reducer

/**
 * Groups the three MVI types of a screen into a single object, with its pure [reduce].
 *
 * ```
 * object MyContract : ContainerContract<MyContract.UiState, MyContract.Intent, MyContract.Effect>() {
 *     data class UiState(...)
 *     sealed interface Intent { ... }
 *     sealed interface Effect { ... }
 *     override fun reduce(state: UiState, intent: Intent): UiState = when (intent) { ... }
 * }
 * ```
 */
public abstract class ContainerContract<STATE : Any, INTENT : Any, EFFECT : Any> : Reducer<STATE, INTENT> {
	override fun reduce(state: STATE, intent: INTENT): STATE = state
}
