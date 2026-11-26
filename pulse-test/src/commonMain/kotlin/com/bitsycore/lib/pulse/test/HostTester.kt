package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.container.ContainerHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration

/**
 * Drives a real host (a ViewModel, a container) in a test. Every `expect…` first runs what is
 * due now, never the clock: timers and debounces move only with [advanceBy].
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class HostTester<STATE : Any, INTENT : Any, EFFECT : Any> @PublishedApi internal constructor(
	private val host: ContainerHost<STATE, INTENT, EFFECT>,
	private val scope: TestScope,
) {
	private val mEffects = ArrayDeque<EFFECT>()
	private val mStates = mutableListOf<STATE>()

	/** The state, once what is due now has run. */
	public val state: STATE
		get() {
			settle()
			return host.state
		}

	/** Every state the host went through since the test began, the first one included. */
	public val states: List<STATE>
		get() {
			settle()
			return mStates.toList()
		}

	public fun dispatch(intent: INTENT): Unit = host.dispatch(intent)

	/** Runs what is due now, without moving the clock. */
	public fun settle(): Unit = scope.testScheduler.runCurrent()

	/** Moves the clock by [duration] and runs what became due. */
	public fun advanceBy(duration: Duration) {
		scope.testScheduler.advanceTimeBy(duration)
		scope.testScheduler.runCurrent()
	}

	public fun expectState(expected: STATE): Unit = assertEquals(expected, state)

	public fun expectState(message: String? = null, predicate: (STATE) -> Boolean) {
		val vState = state
		assertTrue(predicate(vState), message ?: "Unexpected state: $vState")
	}

	/** The next effect not yet looked at, whatever it is. */
	public fun nextEffect(): EFFECT {
		settle()
		return mEffects.removeFirstOrNull() ?: fail("Expected an effect, none was emitted")
	}

	/** The next effect, which must be a [T]. */
	public inline fun <reified T : EFFECT> expectEffect(): T {
		val vEffect = nextEffect()
		return vEffect as? T ?: fail("Expected an effect of type ${T::class.simpleName}, got $vEffect")
	}

	public fun expectEffect(expected: EFFECT): Unit = assertEquals(expected, nextEffect())

	public fun expectNoEffects() {
		settle()
		assertTrue(mEffects.isEmpty(), "Expected no effect, got $mEffects")
	}

	@PublishedApi
	internal suspend fun run(block: suspend HostTester<STATE, INTENT, EFFECT>.() -> Unit) {
		val vDispatcher = UnconfinedTestDispatcher(scope.testScheduler)
		val vStates = scope.backgroundScope.launch(vDispatcher) { host.stateFlow.collect { mStates += it } }
		val vEffects = scope.backgroundScope.launch(vDispatcher) { host.effectFlow.collect { mEffects += it } }
		try {
			block()
		} finally {
			vStates.cancelAndJoin()
			vEffects.cancelAndJoin()
		}
	}
}

/**
 * Tests a real host from inside `runTest`:
 * ```
 * @Test fun reset() = runTest {
 *     MyViewModel().test(this) {
 *         dispatch(Intent.Reset)
 *         expectState { it.count == 0 }
 *         expectEffect<Effect.ShowToast>()
 *         expectNoEffects()
 *     }
 * }
 * ```
 * The host is collected for the length of the block, as a screen would: its start hook runs.
 * A ViewModel needs `Dispatchers.setMain` on a dispatcher of [testScope]'s scheduler.
 */
public suspend fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.test(
	testScope: TestScope,
	block: suspend HostTester<STATE, INTENT, EFFECT>.() -> Unit,
): Unit = HostTester(this, testScope).run(block)
