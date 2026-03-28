package com.bitsycore.lib.pulse.container

/**
 * Marker interface that groups the three MVI types for a screen into one object.
 *
 * Each screen contract should be an `object` that:
 *  - Implements `MviContract<STATE, INTENT, EFFECT>`
 *  - Provides `initialState` (removes it from the ViewModel constructor)
 *  - Declares nested `State`, `Intent`, and `Effect`
 *
 * Usage:
 * ```
 * object MyContract : Contract<MyContract.UiState, MyContract.Intent, MyContract.Effect> {
 *     override val initialState = UiState()
 *     data class UiState(...)
 *     sealed interface Intent { ... }
 *     sealed interface Effect { ... }
 * }
 *
 * class MyViewModel : MviViewModel<MyContract.UiState, MyContract.Intent, MyContract.Effect>(MyContract) { ... }
 * ```
 */
abstract class ContainerContract<STATE : Any, INTENT : Any, EFFECT : Any> {
	abstract val initialState: STATE
}