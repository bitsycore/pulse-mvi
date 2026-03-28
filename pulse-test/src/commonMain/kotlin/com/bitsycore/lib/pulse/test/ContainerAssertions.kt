package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.container.ContainerHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Asserts the current state matches the [expected] value.
 */
fun <STATE : Any> ContainerHost<STATE, *, *>.assertState(expected: STATE) {
	assertEquals(expected, stateFlow.value)
}

/**
 * Asserts the current state satisfies the given [predicate].
 */
fun <STATE : Any> ContainerHost<STATE, *, *>.assertState(
	message: String? = null,
	predicate: (STATE) -> Boolean
) {
	assertTrue(predicate(stateFlow.value), message ?: "State assertion failed: ${stateFlow.value}")
}

/**
 * Collects effects emitted during [block] and returns them as a list.
 *
 * Usage:
 * ```kotlin
 * val effects = container.collectEffects {
 *     container.dispatch(MyIntent.Reset)
 * }
 * assertEquals(1, effects.size)
 * ```
 */
suspend fun <EFFECT : Any> ContainerHost<*, *, EFFECT>.collectEffects(
	scope: TestScope,
	block: suspend () -> Unit
): List<EFFECT> {
	val effects = mutableListOf<EFFECT>()
	val job = scope.launch {
		effectFlow.toList(effects)
	}
	block()
	@OptIn(ExperimentalCoroutinesApi::class)
	scope.advanceUntilIdle() // flush all pending coroutines (including nested launches from emitEffect)
	job.cancel()
	return effects
}

/**
 * Collects the first effect emitted during [block].
 *
 * Usage:
 * ```kotlin
 * val effect = container.awaitEffect {
 *     container.dispatch(MyIntent.Reset)
 * }
 * ```
 */
suspend fun <EFFECT : Any> ContainerHost<*, *, EFFECT>.awaitEffect(
	block: suspend () -> Unit
): EFFECT {
	var result: EFFECT? = null
	kotlinx.coroutines.coroutineScope {
		val job = launch {
			result = effectFlow.first()
		}
		block()
		job.join()
	}
	return result!!
}

/**
 * Convenience to run a test against a [TestContainer].
 *
 * Usage:
 * ```kotlin
 * MyContract.containerTest(
 *     reduce = { state, intent -> ... },
 *     handleIntent = { intent -> ... }
 * ) {
 *     dispatch(MyIntent.Increment)
 *     assertState { it.count == 1 }
 * }
 * ```
 */
fun <STATE : Any, INTENT : Any, EFFECT : Any> com.bitsycore.lib.pulse.container.ContainerContract<STATE, INTENT, EFFECT>.containerTest(
	reduce: (STATE, INTENT) -> STATE = { state, _ -> state },
	handleIntent: suspend TestContainer<STATE, INTENT, EFFECT>.(INTENT) -> Unit = {},
	block: suspend TestContainer<STATE, INTENT, EFFECT>.() -> Unit
) = runTest {
	val container = TestContainer(
		contract = this@containerTest,
		testScope = this,
		reducer = reduce,
		intentHandler = handleIntent
	)
	container.block()
}
