package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.Pulse
import com.bitsycore.lib.pulse.container.Container
import com.bitsycore.lib.pulse.container.ContainerContract
import com.bitsycore.lib.pulse.container.IntentPolicy
import com.bitsycore.lib.pulse.container.PulseObserver
import com.bitsycore.lib.pulse.container.StateChange
import com.bitsycore.lib.pulse.container.StateSaver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private object Engine : ContainerContract<Engine.State, Engine.Intent, Engine.Effect>() {
	data class State(val busy: Boolean = false, val query: String = "", val result: String = "", val count: Int = 0)

	sealed interface Intent {
		data object Submit : Intent
		data class Search(val query: String) : Intent
		data class Loaded(val result: String) : Intent
		data object Add : Intent
		data object Boom : Intent
		data object BadReduce : Intent
	}

	sealed interface Effect {
		data class Done(val tag: String) : Effect
	}

	override fun reduce(state: State, intent: Intent): State = when (intent) {
		Intent.Submit -> state.copy(busy = true)
		is Intent.Search -> state.copy(query = intent.query)
		is Intent.Loaded -> state.copy(result = intent.result)
		Intent.Add -> state.copy(count = state.count + 1)
		Intent.Boom -> state
		Intent.BadReduce -> error("bad reduce")
	}
}

/** A container whose side effects are scripted by the test. */
private class EngineContainer(
	scope: CoroutineScope,
	saver: StateSaver<Engine.State>? = null,
	observer: PulseObserver? = null,
	private val policy: (Engine.Intent) -> IntentPolicy = { IntentPolicy.Parallel },
	private val grace: Duration = 5.seconds,
	private val start: suspend EngineContainer.() -> Unit = {},
	private val active: suspend EngineContainer.() -> Unit = {},
	private val error: ((Engine.Intent?, Exception) -> Unit)? = null,
	private val handle: suspend EngineContainer.(Engine.Intent, StateChange<Engine.State>) -> Unit = { _, _ -> },
) : Container<Engine.State, Engine.Intent, Engine.Effect>(Engine, Engine.State(), scope, saver, observer) {
	override suspend fun handleIntent(intent: Engine.Intent, change: StateChange<Engine.State>) = handle(intent, change)
	override fun policyOf(intent: Engine.Intent) = policy(intent)
	override suspend fun onStart() = start()
	override suspend fun whileActive() = active()
	override val inactiveGrace: Duration get() = grace
	override fun onError(intent: Engine.Intent?, error: Exception) = this.error?.invoke(intent, error) ?: throw error
}

@OptIn(ExperimentalCoroutinesApi::class)
class EngineTest {

	// ==================
	// MARK: Reduce and side effect
	// ==================

	@Test
	fun sideEffectSeesWhatTheReducerDid() = runTest {
		val vChanges = mutableListOf<StateChange<Engine.State>>()
		val vContainer = EngineContainer(backgroundScope) { _, vChange -> vChanges += vChange }

		vContainer.dispatch(Engine.Intent.Submit)
		vContainer.dispatch(Engine.Intent.Submit)
		runCurrent()

		// The first tap set busy, the second found it set: a guard the reducer alone cannot give.
		assertEquals(listOf(false, true), vChanges.map { it.previous.busy })
		assertEquals(listOf(true, false), vChanges.map { it.changed })
		assertTrue(vChanges.all { it.current.busy })
	}

	@Test
	fun reduceOnlyRunsNoSideEffect() = runTest {
		var vHandled = 0
		val vContainer = EngineContainer(backgroundScope) { _, _ -> vHandled++ }

		vContainer.reduceOnly(Engine.Intent.Loaded("x"))
		runCurrent()

		assertEquals("x", vContainer.state.result)
		assertEquals(0, vHandled)
	}

	// ==================
	// MARK: Policies
	// ==================

	@Test
	fun dropIfRunningSkipsTheSecondTap() = runTest {
		var vRuns = 0
		val vGate = CompletableDeferred<Unit>()
		val vContainer = EngineContainer(backgroundScope, policy = { IntentPolicy.DropIfRunning() }) { _, _ ->
			vRuns++
			vGate.await()
		}

		vContainer.dispatch(Engine.Intent.Submit)
		vContainer.dispatch(Engine.Intent.Submit)
		runCurrent()
		assertEquals(1, vRuns)

		// Once the first is over, the next one runs.
		vGate.complete(Unit)
		runCurrent()
		vContainer.dispatch(Engine.Intent.Submit)
		runCurrent()
		assertEquals(2, vRuns)
	}

	@Test
	fun latestCancelsThePreviousLoad() = runTest {
		val vContainer = EngineContainer(backgroundScope, policy = { IntentPolicy.Latest() }) { vIntent, _ ->
			vIntent as Engine.Intent.Search
			// The first ask is the slow one: without the policy it would land last.
			delay(if (vIntent.query == "a") 500.milliseconds else 100.milliseconds)
			reduceOnly(Engine.Intent.Loaded(vIntent.query))
		}

		vContainer.dispatch(Engine.Intent.Search("a"))
		vContainer.dispatch(Engine.Intent.Search("ab"))
		advanceTimeBy(1.seconds)

		assertEquals("ab", vContainer.state.result)
	}

	@Test
	fun queueRunsInDispatchOrder() = runTest {
		val vOrder = mutableListOf<String>()
		val vContainer = EngineContainer(backgroundScope, policy = { IntentPolicy.Queue("lane") }) { vIntent, _ ->
			vIntent as Engine.Intent.Search
			delay(if (vIntent.query == "first") 500.milliseconds else 10.milliseconds)
			vOrder += vIntent.query
		}

		vContainer.dispatch(Engine.Intent.Search("first"))
		vContainer.dispatch(Engine.Intent.Search("second"))
		vContainer.dispatch(Engine.Intent.Search("third"))
		advanceTimeBy(1.seconds)

		assertEquals(listOf("first", "second", "third"), vOrder)
	}

	@Test
	fun debounceReducesAtOnceAndRunsAfterTheQuiet() = runTest {
		val vSearched = mutableListOf<String>()
		val vContainer = EngineContainer(backgroundScope, policy = { IntentPolicy.Debounce(300.milliseconds) }) { vIntent, _ ->
			vSearched += (vIntent as Engine.Intent.Search).query
		}

		vContainer.dispatch(Engine.Intent.Search("a"))
		advanceTimeBy(200.milliseconds)
		vContainer.dispatch(Engine.Intent.Search("ab"))
		assertEquals("ab", vContainer.state.query)
		advanceTimeBy(200.milliseconds)
		runCurrent()
		assertTrue(vSearched.isEmpty())

		advanceTimeBy(200.milliseconds)
		runCurrent()
		assertEquals(listOf("ab"), vSearched)
	}

	@Test
	fun policiesKeepIntentTypesApart() = runTest {
		val vRan = mutableListOf<Engine.Intent>()
		val vContainer = EngineContainer(backgroundScope, policy = { IntentPolicy.DropIfRunning() }) { vIntent, _ ->
			vRan += vIntent
			awaitCancellation()
		}

		vContainer.dispatch(Engine.Intent.Submit)
		vContainer.dispatch(Engine.Intent.Add)
		vContainer.dispatch(Engine.Intent.Add)
		runCurrent()

		assertEquals(listOf(Engine.Intent.Submit, Engine.Intent.Add), vRan)
	}

	// ==================
	// MARK: Keyed jobs
	// ==================

	@Test
	fun keyedJobs() = runTest {
		val vContainer = EngineContainer(backgroundScope)
		val vLog = mutableListOf<String>()

		// latest: the timer restarted is the only one that fires
		vContainer.launchLatest("idle") { delay(1.seconds); vLog += "idle 1" }
		vContainer.launchLatest("idle") { delay(1.seconds); vLog += "idle 2" }
		assertTrue(vContainer.isRunning("idle"))
		advanceTimeBy(2.seconds)
		assertEquals(listOf("idle 2"), vLog)
		assertFalse(vContainer.isRunning("idle"))

		// once: null while the first still runs
		val vFirst = vContainer.launchOnce("print") { delay(1.seconds) }
		assertTrue(vFirst != null)
		assertNull(vContainer.launchOnce("print") { vLog += "second print" })
		advanceTimeBy(2.seconds)
		assertTrue(vContainer.launchOnce("print") {} != null)

		// cancelJob: running and waiting alike
		vLog.clear()
		vContainer.launchQueued("lane") { delay(1.seconds); vLog += "one" }
		vContainer.launchQueued("lane") { vLog += "two" }
		runCurrent()
		vContainer.cancelJob("lane")
		advanceTimeBy(2.seconds)
		assertTrue(vLog.isEmpty())

		// debounced
		vContainer.launchDebounced("save", 300.milliseconds) { vLog += "saved 1" }
		vContainer.launchDebounced("save", 300.milliseconds) { vLog += "saved 2" }
		advanceTimeBy(1.seconds)
		assertEquals(listOf("saved 2"), vLog)
	}

	// ==================
	// MARK: Start and activity
	// ==================

	@Test
	fun onStartRunsOnceAtTheFirstUseNotAtConstruction() = runTest {
		var vStarts = 0
		val vContainer = EngineContainer(backgroundScope, start = { vStarts++ })
		runCurrent()
		assertEquals(0, vStarts)

		val vCollector = backgroundScope.launch { vContainer.stateFlow.collect {} }
		runCurrent()
		vContainer.dispatch(Engine.Intent.Add)
		backgroundScope.launch { vContainer.stateFlow.collect {} }
		runCurrent()
		assertEquals(1, vStarts)
		vCollector.cancel()
	}

	@Test
	fun onStartRunsAtTheFirstDispatchToo() = runTest {
		var vStarts = 0
		val vContainer = EngineContainer(backgroundScope, start = { vStarts++ })

		vContainer.dispatch(Engine.Intent.Add)
		runCurrent()

		assertEquals(1, vStarts)
	}

	@Test
	fun whileActiveFollowsTheCollectors() = runTest {
		var vTicks = 0
		var vRuns = 0
		val vContainer = EngineContainer(backgroundScope, grace = 5.seconds, active = {
			vRuns++
			while (true) {
				delay(1.seconds)
				vTicks++
			}
		})
		runCurrent()
		assertEquals(0, vRuns)

		val vFirst = backgroundScope.launch { vContainer.stateFlow.collect {} }
		advanceTimeBy(3500.milliseconds)
		assertEquals(3, vTicks)

		// Gone and back within the grace (a rotation): not restarted.
		vFirst.cancel()
		advanceTimeBy(2.seconds)
		val vSecond = backgroundScope.launch { vContainer.stateFlow.collect {} }
		advanceTimeBy(10.seconds)
		assertEquals(1, vRuns)

		// Gone for good: stopped after the grace.
		vSecond.cancel()
		advanceTimeBy(6.seconds)
		val vAtStop = vTicks
		advanceTimeBy(10.seconds)
		assertEquals(vAtStop, vTicks)

		// Back later: started again.
		backgroundScope.launch { vContainer.stateFlow.collect {} }
		runCurrent()
		assertEquals(2, vRuns)
	}

	// ==================
	// MARK: Errors
	// ==================

	@Test
	fun everyFailureGoesToOnError() = runTest {
		val vErrors = mutableListOf<Pair<Engine.Intent?, String?>>()
		val vContainer = EngineContainer(
			backgroundScope,
			start = { error("start") },
			error = { vIntent, vError -> vErrors += vIntent to vError.message },
		) { vIntent, _ -> if (vIntent == Engine.Intent.Boom) error("side effect") }

		vContainer.dispatch(Engine.Intent.Boom)
		vContainer.dispatch(Engine.Intent.BadReduce)
		vContainer.launchLatest("job") { error("job") }
		runCurrent()

		assertEquals(
			setOf(null to "start", Engine.Intent.Boom to "side effect", Engine.Intent.BadReduce to "bad reduce", null to "job"),
			vErrors.toSet(),
		)
		// Still alive.
		vContainer.dispatch(Engine.Intent.Add)
		assertEquals(1, vContainer.state.count)
	}

	@Test
	fun aThrowingReducerFailsFastByDefault() = runTest {
		val vContainer = EngineContainer(backgroundScope)
		assertFailsWith<IllegalStateException> { vContainer.dispatch(Engine.Intent.BadReduce) }
	}

	// ==================
	// MARK: Observer and saver
	// ==================

	@Test
	fun theObserverSeesEverything() = runTest {
		val vSeen = mutableListOf<String>()
		val vObserver = object : PulseObserver {
			override fun onIntent(source: Any, intent: Any, previous: Any, current: Any) { vSeen += "intent $intent" }
			override fun onStateUpdate(source: Any, name: String?, previous: Any, current: Any) { vSeen += "update $name" }
			override fun onEffect(source: Any, effect: Any) { vSeen += "effect $effect" }
			override fun onError(source: Any, intent: Any?, error: Exception) { vSeen += "error ${error.message}" }
		}
		val vGlobal = mutableListOf<Any>()
		Pulse.observer = object : PulseObserver {
			override fun onIntent(source: Any, intent: Any, previous: Any, current: Any) { vGlobal += source }
		}
		try {
			val vContainer = EngineContainer(backgroundScope, observer = vObserver, error = { _, _ -> }) { vIntent, _ ->
				if (vIntent == Engine.Intent.Boom) error("boom")
				emitEffect(Engine.Effect.Done("x"))
			}
			vContainer.dispatch(Engine.Intent.Add)
			vContainer.updateState("picture loaded") { copy(result = "p") }
			vContainer.dispatch(Engine.Intent.Boom)
			runCurrent()

			assertEquals(
				listOf("intent Add", "update picture loaded", "intent Boom", "effect Done(tag=x)", "error boom"),
				vSeen,
			)
			assertEquals(listOf<Any>(vContainer, vContainer), vGlobal)
		} finally {
			Pulse.observer = null
		}
	}

	@Test
	fun theSaverRestoresAndIsToldOfChanges() = runTest {
		val vSaved = mutableListOf<Engine.State>()
		var vRead: (() -> Engine.State)? = null
		val vSaver = object : StateSaver<Engine.State> {
			override fun restore() = Engine.State(count = 41)
			override fun bind(current: () -> Engine.State) { vRead = current }
			override fun onStateChanged(state: Engine.State) { vSaved += state }
		}
		val vContainer = EngineContainer(backgroundScope, saver = vSaver)
		assertEquals(41, vContainer.state.count)

		vContainer.dispatch(Engine.Intent.Add)
		vContainer.dispatch(Engine.Intent.Boom) // no change, nothing to save

		assertEquals(listOf(Engine.State(count = 42)), vSaved)
		assertEquals(42, vRead?.invoke()?.count)
	}

	// ==================
	// MARK: Test kit
	// ==================

	@Test
	fun containerTestReducesThroughTheContract() = Engine.containerTest(Engine.State()) {
		dispatch(Engine.Intent.Add)
		assertState { it.count == 1 }
	}

	@Test
	fun hostTesterDrivesARealHost() = runTest {
		val vContainer = EngineContainer(
			backgroundScope,
			policy = { if (it is Engine.Intent.Search) IntentPolicy.Debounce(300.milliseconds) else IntentPolicy.Parallel },
			start = { reduceOnly(Engine.Intent.Loaded("started")) },
		) { vIntent, _ ->
			when (vIntent) {
				is Engine.Intent.Search -> emitEffect(Engine.Effect.Done(vIntent.query))
				Engine.Intent.Submit -> emitEffect(Engine.Effect.Done("submit"))
				else -> Unit
			}
		}

		vContainer.test(this) {
			expectState { it.result == "started" }
			dispatch(Engine.Intent.Submit)
			assertEquals("submit", expectEffect<Engine.Effect.Done>().tag)
			expectNoEffects()

			dispatch(Engine.Intent.Search("a"))
			expectState { it.query == "a" }
			expectNoEffects()
			advanceBy(300.milliseconds)
			expectEffect(Engine.Effect.Done("a"))
			assertEquals(Engine.State(), states.first())
		}
	}

	@Test
	fun lambdaContainer() = runTest {
		val vContainer = Container(Engine, Engine.State(), backgroundScope) { vIntent, vChange ->
			if (vIntent == Engine.Intent.Submit && vChange.changed) emitEffect(Engine.Effect.Done("first"))
		}
		vContainer.dispatch(Engine.Intent.Submit)
		vContainer.dispatch(Engine.Intent.Submit)
		runCurrent()
		assertEquals(Engine.Effect.Done("first"), vContainer.effectFlow.first())
		vContainer.test(this) { expectNoEffects() }
	}
}
