package com.bitsycore.lib.pulse.utility

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/**
 * Standalone debounced intent dispatcher.
 *
 * Holds all debounce state (pending jobs and last dispatched intents) and can be
 * connected to any regular dispatch function. Typically used alongside a [Container]
 * or inside a ViewModel.
 *
 * Thread-safe: safe to call [dispatchDebounced] from any thread or coroutine context.
 *
 * @param coroutineScope The scope used to launch debounce delay jobs.
 * @param dispatch The underlying dispatch function to invoke after the debounce window.
 */
class DebouncedDispatcher<INTENT : Any>(
	private val coroutineScope: CoroutineScope,
	private val dispatch: (INTENT) -> Unit
) {
	private val mutex = Mutex()
	private val debounceJobs = mutableMapOf<Any, Job>()
	private val lastIntents = mutableMapOf<Any, INTENT>()

	/**
	 * Dispatches an [intent] with debouncing, delaying execution until [delay]
	 * has elapsed without another call for the same debounce key.
	 *
	 * ### Debounce key resolution
	 *
	 * | `shareAcrossTypes` | `key`    | Resulting key              | Scope                       |
	 * |--------------------|----------|----------------------------|-----------------------------|
	 * | `true`             | non-null | `key`                      | All intents sharing that key|
	 * | `true`             | `null`   | `Unit`                     | **All** debounced intents   |
	 * | `false`            | non-null | `intent::class to key`     | Same type + same key        |
	 * | `false` (default)  | `null`   | `intent::class`            | Same intent type            |
	 *
	 * @param intent The intent to dispatch after the debounce window.
	 * @param delay The debounce window in [Duration]. Negative values are clamped to zero.
	 * @param key Optional string to further partition the debounce scope.
	 * @param skipIfUnchanged When `true`, the intent is silently dropped if it equals
	 *   the last successfully dispatched intent for the same debounce key.
	 * @param shareAcrossTypes When `true`, the intent's class is excluded from the key.
	 */
	fun dispatchDebounced(
		intent: INTENT,
		delay: Duration,
		key: String? = null,
		skipIfUnchanged: Boolean = false,
		shareAcrossTypes: Boolean = false
	) {
		coroutineScope.launch {
			val clampedDelay = if (delay.isNegative()) Duration.ZERO else delay

			val debounceKey: Any = when {
				shareAcrossTypes && key != null -> key
				shareAcrossTypes -> Unit
				key != null -> intent::class to key
				else -> intent::class
			}

			// Skip-check, cancellation of the previous job and registration of the new
			// one happen under a single lock acquisition, so two concurrent calls can
			// never interleave between those steps.
			mutex.withLock {
				if (skipIfUnchanged && lastIntents[debounceKey] == intent) return@launch

				debounceJobs[debounceKey]?.cancel()
				val job = launch {
					delay(clampedDelay)
					// Dispatch only if this job is still the registered owner of the key.
					// A newer call may have cancelled us between the end of the delay and
					// this point — cancellation alone cannot interrupt non-suspending code,
					// so the ownership check is what prevents a stale dispatch.
					val vIsOwner = mutex.withLock {
						if (debounceJobs[debounceKey] === coroutineContext[Job]) {
							lastIntents[debounceKey] = intent
							debounceJobs.remove(debounceKey)
							true
						} else {
							false
						}
					}
					if (vIsOwner) dispatch(intent)
				}
				debounceJobs[debounceKey] = job
			}
		}
	}

	/**
	 * Cancels a pending debounced dispatch identified by its [key].
	 *
	 * This cancels any debounce entry whose key matches the given [key] string,
	 * regardless of whether it was registered with `shareAcrossTypes` or not.
	 * The dispatch history for matching keys is also cleared, so subsequent
	 * calls with `skipIfUnchanged = true` will not skip.
	 *
	 * @param key The debounce key string to cancel.
	 */
	fun cancel(key: String) {
		coroutineScope.launch {
			mutex.withLock {
				val matchesKey: (Any) -> Boolean = { debounceKey ->
					debounceKey == key || (debounceKey is Pair<*, *> && debounceKey.second == key)
				}
				debounceJobs.keys.filter(matchesKey).forEach { k ->
					debounceJobs.remove(k)?.cancel()
				}
				lastIntents.keys.filter(matchesKey).forEach { k ->
					lastIntents.remove(k)
				}
			}
		}
	}

	/**
	 * Cancels all pending debounced dispatches and clears dispatch history.
	 *
	 * Does **not** affect already-dispatched intents. Clears the `skipIfUnchanged`
	 * history so that subsequent dispatches are not skipped.
	 */
	fun cancelAll() {
		coroutineScope.launch {
			mutex.withLock {
				debounceJobs.values.forEach { it.cancel() }
				debounceJobs.clear()
				lastIntents.clear()
			}
		}
	}

	/**
	 * Clears the dispatch history used by `skipIfUnchanged` without cancelling
	 * any pending debounced dispatches.
	 *
	 * After calling this, the next [dispatchDebounced] with `skipIfUnchanged = true`
	 * will dispatch even if the intent matches a previously dispatched one.
	 */
	fun clearHistory() {
		coroutineScope.launch {
			mutex.withLock {
				lastIntents.clear()
			}
		}
	}
}
