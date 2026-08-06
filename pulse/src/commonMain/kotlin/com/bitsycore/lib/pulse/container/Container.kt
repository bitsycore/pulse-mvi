package com.bitsycore.lib.pulse.container

import com.bitsycore.lib.pulse.internal.ExperimentalPulse
import com.bitsycore.lib.pulse.internal.UntypedIntentBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Core MVI engine.
 *
 * @param STATE  Immutable UI state type.
 * @param INTENT  Intent (user action or lifecycle event) type.
 * @param EFFECT  One-time Effect (navigation, toasts, etc.) type.
 *
 * Data flow:
 *   UI → dispatch(Intent) → reduce() → new State → UI recomposes
 *                        ↘ handleIntent() → async work → emitEffect() → Screen reacts
 */
abstract class Container<STATE : Any, INTENT : Any, EFFECT : Any>(
	protected val containerContract: ContainerContract<STATE, INTENT, EFFECT>,
	initialState: STATE,
	restoredState: STATE? = null,
	protected val coroutineScope: CoroutineScope,
) : ContainerHost<STATE, INTENT, EFFECT> {

	private val stateMutableFlow = MutableStateFlow(restoredState ?: initialState)
	override val stateFlow: StateFlow<STATE> = stateMutableFlow.asStateFlow()

	// Channel-backed so effects emitted while no collector is subscribed (e.g. during
	// a configuration change) are buffered and delivered on resubscription instead of
	// being silently dropped. Effects are consumed by a single collector.
	private val effectChannel = Channel<EFFECT>(Channel.UNLIMITED)
	override val effectFlow: Flow<EFFECT> = effectChannel.receiveAsFlow()

	/** Entry point for all UI-originated actions. Thread-safe. */
	override fun dispatch(intent: INTENT) {
		stateMutableFlow.update { reduce(it, intent) }
		coroutineScope.launch {
			try {
				handleIntent(intent)
			} catch (vCancellation: CancellationException) {
				throw vCancellation
			} catch (vError: Throwable) {
				onError(intent, vError)
			}
		}
	}

	@ExperimentalPulse
	fun dispatchCustom(block: UntypedIntentBuilder<STATE>.() -> Unit) {
		UntypedIntentBuilder(
			stateMutableFlow,
			coroutineScope
		).apply(block).build()
	}

	/** Pure, synchronous state reducer. Override to handle state transitions. */
	protected open fun reduce(state: STATE, intent: INTENT): STATE = containerContract.reduce(state, intent)

	/** Long operation handler. Override to perform async work (network, NFC, etc.). */
	protected open suspend fun handleIntent(intent: INTENT) {}

	/**
	 * Called when [handleIntent] throws (except [CancellationException]).
	 * Default rethrows, preserving fail-fast behaviour — override to report the
	 * error and keep the container alive (e.g. emit an error state or effect).
	 */
	protected open fun onError(intent: INTENT, error: Throwable): Unit = throw error

	/** Emits a one-time effect to the screen. Thread-safe, never drops the effect. */
	fun emitEffect(effect: EFFECT) {
		effectChannel.trySend(effect)
	}

	/** Convenience for updating state outside of the reducer (e.g., inside callbacks). */
	fun updateState(block: STATE.() -> STATE) = stateMutableFlow.update(block)
}