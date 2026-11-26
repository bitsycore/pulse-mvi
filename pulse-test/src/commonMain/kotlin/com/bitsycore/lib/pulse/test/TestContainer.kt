package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.container.Container
import com.bitsycore.lib.pulse.container.ContainerContract
import com.bitsycore.lib.pulse.container.IntentPolicy
import com.bitsycore.lib.pulse.container.PolicedIntent
import com.bitsycore.lib.pulse.container.PulseObserver
import com.bitsycore.lib.pulse.container.StateSaver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * A [Container] out of lambdas, for a test. It reduces through the contract's own reducer unless
 * given another, and runs on an unconfined test dispatcher unless given a scope.
 *
 * To test a real ViewModel or container, use [test] on it instead.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class TestContainer<STATE : Any, INTENT : Any, EFFECT : Any>(
	contract: ContainerContract<STATE, INTENT, EFFECT>,
	initialState: STATE,
	testScope: TestScope = TestScope(UnconfinedTestDispatcher()),
	private val reducer: (STATE, INTENT) -> STATE = contract::reduce,
	saver: StateSaver<STATE>? = null,
	observer: PulseObserver? = null,
	private val policy: (INTENT) -> IntentPolicy = { (it as? PolicedIntent)?.policy ?: IntentPolicy.Parallel },
	private val errorHandler: ((intent: INTENT?, error: Exception) -> Unit)? = null,
	private val intentHandler: suspend TestContainer<STATE, INTENT, EFFECT>.(INTENT) -> Unit = {},
) : Container<STATE, INTENT, EFFECT>(contract, initialState, testScope, saver, observer) {

	override fun reduce(state: STATE, intent: INTENT): STATE = reducer(state, intent)
	override suspend fun handleIntent(intent: INTENT): Unit = intentHandler(intent)
	override fun policyOf(intent: INTENT): IntentPolicy = policy(intent)
	override fun onError(intent: INTENT?, error: Exception): Unit = errorHandler?.invoke(intent, error) ?: throw error
}
