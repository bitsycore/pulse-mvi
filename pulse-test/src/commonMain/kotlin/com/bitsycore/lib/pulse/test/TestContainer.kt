package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.container.Container
import com.bitsycore.lib.pulse.container.ContainerContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * A test-friendly [Container] that runs on [UnconfinedTestDispatcher],
 * making all dispatches execute synchronously in tests.
 *
 * Usage:
 * ```kotlin
 * val container = TestContainer(MyContract) { state, intent ->
 *     when (intent) { ... }
 * }
 * container.dispatch(MyIntent.Increment)
 * assertEquals(1, container.stateFlow.value.count)
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestContainer<STATE : Any, INTENT : Any, EFFECT : Any>(
	contract: ContainerContract<STATE, INTENT, EFFECT>,
	private val testScope: TestScope = TestScope(UnconfinedTestDispatcher()),
	private val reducer: (STATE, INTENT) -> STATE = { state, _ -> state },
	private val intentHandler: suspend TestContainer<STATE, INTENT, EFFECT>.(INTENT) -> Unit = {}
) : Container<STATE, INTENT, EFFECT>(contract, testScope, restoredState = null) {

	override fun reduce(state: STATE, intent: INTENT): STATE = reducer(state, intent)

	override suspend fun handleIntent(intent: INTENT) = intentHandler(intent)
}
