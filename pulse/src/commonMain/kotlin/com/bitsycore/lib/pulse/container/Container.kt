package com.bitsycore.lib.pulse.container

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

/**
 * Base class for MVI ViewModels.
 *
 * @param STATE  Immutable UI state type.
 * @param INTENT  Intent (user action or lifecycle event) type.
 * @param EFFECT  One-time Effect (navigation, toasts, etc.) type.
 *
 * The [containerContract] provides [ContainerContract.initialState], making the contract the single
 * source of truth for State, Intent, Effect, and the starting state value.
 *
 * Data flow:
 *   UI → dispatch(Intent) → reduce() → new State → UI recomposes
 *                        ↘ handleIntent() → async work → emitEffect() → Screen reacts
 */
abstract class Container<STATE : Any, INTENT : Any, EFFECT : Any>(
	containerContract: ContainerContract<STATE, INTENT, EFFECT>,
	val coroutineScope: CoroutineScope,
	replayUnconsumed: Int = 4,
	restoredState: STATE? = null
) : ContainerHost<STATE, INTENT, EFFECT> {

	private val stateMutableFlow = MutableStateFlow(restoredState ?: containerContract.initialState)
	override val stateFlow: StateFlow<STATE> = stateMutableFlow.asStateFlow()

	private val effectMutableFlow = MutableSharedFlow<OneTimeConsumable<EFFECT>>(replay = replayUnconsumed, extraBufferCapacity = 8)
	override val effectFlow: Flow<EFFECT> = effectMutableFlow.mapNotNull { it.consume() }

	/** Entry point for all UI-originated actions. Thread-safe. */
	override fun dispatch(intent: INTENT) {
		stateMutableFlow.update { reduce(it, intent) }
		coroutineScope.launch { handleIntent(intent) }
	}

	private val debounceJobs = mutableMapOf<Any, Job>()
	private val lastIntents = mutableMapOf<Any, INTENT>()

	/**
	 * Dispatches an [intent] with debouncing, delaying execution until [delay]
	 * milliseconds have elapsed without another call for the same debounce key.
	 *
	 * Useful for rate-limiting rapid user input such as search fields, sliders,
	 * or any repeated action where only the latest value matters.
	 *
	 * ### Debounce key resolution
	 *
	 * The debounce key determines which calls cancel each other. It is resolved
	 * as follows (first match wins):
	 *
	 * | `ignoreTypeForKey` | `key`    | Resulting key              | Scope                       |
	 * |--------------------|----------|----------------------------|-----------------------------|
	 * | `true`             | non-null | `key`                      | All intents sharing that key|
	 * | `true`             | `null`   | `Unit`                     | **All** debounced intents   |
	 * | `false`            | non-null | `intent::class to key`     | Same type + same key        |
	 * | `false` (default)  | `null`   | `intent::class`            | Same intent type            |
	 *
	 * @param intent The intent to dispatch after the debounce window.
	 * @param key Optional string to further partition the debounce scope
	 *   within (or across) intent types. For example, passing a field name
	 *   lets multiple fields debounce independently.
	 * @param delay The debounce window in [Duration].
	 * @param skipIfUnchanged When `true`, the intent is silently dropped
	 *   if it is [structurally equal][Any.equals] to the last dispatched
	 *   intent for the same debounce key. Requires the intent to implement
	 *   a meaningful [equals] (e.g., be a `data class`).
	 * @param shareAcrossTypes When `true`, the intent's class is excluded
	 *   from the debounce key, allowing different intent types to share a
	 *   single debounce slot. Use with caution: if combined with a `null`
	 *   [key], **every** debounced intent shares one slot.
	 *
	 * @see dispatch For immediate, non-debounced dispatch.
	 */
	override fun dispatchDebounced(
		intent: INTENT,
		delay: Duration,
		key: String?,
		skipIfUnchanged: Boolean,
		shareAcrossTypes: Boolean
	) {
		val debounceKey: Any = when {
			shareAcrossTypes && key != null -> key
			shareAcrossTypes -> Unit
			key != null -> intent::class to key
			else -> intent::class
		}
		coroutineScope.launch {
			val last = lastIntents[debounceKey]
			if (skipIfUnchanged && last == intent) return@launch

			debounceJobs[debounceKey]?.cancel()
			debounceJobs[debounceKey] = launch {
				delay(delay)
				lastIntents[debounceKey] = intent
				dispatch(intent)
			}.also { job ->
				job.invokeOnCompletion { debounceJobs.remove(debounceKey) }
			}
		}
	}

	/** Pure, synchronous state reducer. Override to handle state transitions. */
	protected open fun reduce(state: STATE, intent: INTENT): STATE = state

	/** Long operation handler. Override to perform async work (network, NFC, etc.). */
	protected open suspend fun handleIntent(intent: INTENT) {}

	/** Emits a one-time effect to the screen. Thread-safe. Replay-safe via [OneTimeConsumable]. */
	fun emitEffect(effect: EFFECT) {
		coroutineScope.launch { effectMutableFlow.emit(OneTimeConsumable(effect)) }
	}

	/** Convenience for updating state outside of the reducer (e.g., inside callbacks). */
	fun updateState(block: STATE.() -> STATE) = stateMutableFlow.update(block)
}

/**
 * A thread-safe wrapper that allows a value to be consumed exactly once.
 *
 * Used internally to wrap effects in a replay-capable [kotlinx.coroutines.flow.SharedFlow] so that
 * late collectors (e.g. after config change) receive the last emitted effect,
 * but it is never delivered twice.
 */
@OptIn(ExperimentalAtomicApi::class)
private class OneTimeConsumable<out T>(private val value: T) {
	private val consumed = AtomicBoolean(false)

	/**
	 * Returns the wrapped value on first call, `null` on all subsequent calls.
	 */
	fun consume(): T? =
		if (consumed.compareAndSet(expectedValue = false, newValue = true)) value else null
}