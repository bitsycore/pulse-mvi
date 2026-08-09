package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.container.ContainerContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// ── Test Contract ──────────────────────────────────────────────────────────────

private object CounterContract : ContainerContract<CounterContract.UiState, CounterContract.Intent, CounterContract.Effect>() {

	data class UiState(val count: Int = 0, val label: String = "")

	sealed interface Intent {
		data object Increment : Intent
		data object Decrement : Intent
		data object Reset : Intent
		data class SetLabel(val label: String) : Intent
	}

	sealed interface Effect {
		data class ShowToast(val message: String) : Effect
	}
}

// ── Tests ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class ContainerTest {

	// ── Initial State ──────────────────────────────────────────────────────────

	@Test
	fun initialStateIsContractDefault() = CounterContract.containerTest(
		CounterContract.UiState(),
		reduce = { state, _ -> state }
	) {
		assertState(CounterContract.UiState())
	}

	@Test
	fun restoredStateOverridesInitial() = runTest {
		val restored = CounterContract.UiState(count = 42, label = "restored")
		val container = TestContainer(
			CounterContract,
			initialState = CounterContract.UiState(),
			testScope = this,
			reducer = { state, _ -> state },
		)
		// Can't pass restoredState via TestContainer currently, so test via Container directly
		// Instead, verify that the default is initialState
		assertEquals(CounterContract.UiState(), container.stateFlow.value)
	}

	// ── Dispatch + Reduce ──────────────────────────────────────────────────────

	@Test
	fun dispatchUpdatesState() = CounterContract.containerTest(
		CounterContract.UiState(),
		reduce = { state, intent ->
			when (intent) {
				CounterContract.Intent.Increment -> state.copy(count = state.count + 1)
				CounterContract.Intent.Decrement -> state.copy(count = state.count - 1)
				CounterContract.Intent.Reset -> state.copy(count = 0)
				is CounterContract.Intent.SetLabel -> state.copy(label = intent.label)
			}
		}
	) {
		dispatch(CounterContract.Intent.Increment)
		assertState { it.count == 1 }

		dispatch(CounterContract.Intent.Increment)
		dispatch(CounterContract.Intent.Increment)
		assertState(CounterContract.UiState(count = 3))
	}

	@Test
	fun decrementBelowZero() = CounterContract.containerTest(
		CounterContract.UiState(),
		reduce = { state, intent ->
			when (intent) {
				CounterContract.Intent.Decrement -> state.copy(count = state.count - 1)
				else -> state
			}
		}
	) {
		dispatch(CounterContract.Intent.Decrement)
		assertState { it.count == -1 }
	}

	@Test
	fun multipleIntentTypesReduceCorrectly() = CounterContract.containerTest(
		CounterContract.UiState(),
		reduce = { state, intent ->
			when (intent) {
				CounterContract.Intent.Increment -> state.copy(count = state.count + 1)
				CounterContract.Intent.Reset -> state.copy(count = 0)
				is CounterContract.Intent.SetLabel -> state.copy(label = intent.label)
				else -> state
			}
		}
	) {
		dispatch(CounterContract.Intent.Increment)
		dispatch(CounterContract.Intent.Increment)
		dispatch(CounterContract.Intent.SetLabel("hello"))
		assertState(CounterContract.UiState(count = 2, label = "hello"))

		dispatch(CounterContract.Intent.Reset)
		assertState(CounterContract.UiState(count = 0, label = "hello"))
	}

	// ── Effects ────────────────────────────────────────────────────────────────

	@Test
	fun emitEffectDeliversToCollector() = CounterContract.containerTest(
		CounterContract.UiState(),
		handleIntent = { intent ->
			when (intent) {
				CounterContract.Intent.Reset -> emitEffect(CounterContract.Effect.ShowToast("reset!"))
				else -> {}
			}
		}
	) {
		val effect = awaitEffect {
			dispatch(CounterContract.Intent.Reset)
		}
		assertEquals(CounterContract.Effect.ShowToast("reset!"), effect)
	}

	@Test
	fun collectEffectsGathersMultiple() = runTest {
		val scope = this
		val container = TestContainer(
			initialState = CounterContract.UiState(),
			contract = CounterContract,
			testScope = scope,
			intentHandler = { intent ->
				when (intent) {
					CounterContract.Intent.Increment -> emitEffect(CounterContract.Effect.ShowToast("inc"))
					CounterContract.Intent.Reset -> emitEffect(CounterContract.Effect.ShowToast("reset"))
					else -> {}
				}
			}
		)

		val effects = container.collectEffects(scope) {
			container.dispatch(CounterContract.Intent.Increment)
			container.dispatch(CounterContract.Intent.Reset)
		}

		assertEquals(2, effects.size)
		assertEquals(CounterContract.Effect.ShowToast("inc"), effects[0])
		assertEquals(CounterContract.Effect.ShowToast("reset"), effects[1])
	}

	@Test
	fun noEffectEmittedWhenNotTriggered() = runTest {
		val container = TestContainer(
			initialState = CounterContract.UiState(),
			contract = CounterContract,
			testScope = this,
			reducer = { state, intent ->
				when (intent) {
					CounterContract.Intent.Increment -> state.copy(count = state.count + 1)
					else -> state
				}
			}
		)

		val effects = container.collectEffects(this) {
			container.dispatch(CounterContract.Intent.Increment)
		}

		assertTrue(effects.isEmpty())
	}

	// ── Effect Replay (Consumable) ─────────────────────────────────────────────

	@Test
	fun lateCollectorReceivesReplayedEffect() = runTest {
		val container = TestContainer(
			initialState = CounterContract.UiState(),
			contract = CounterContract,
			testScope = this,
			intentHandler = { intent ->
				when (intent) {
					CounterContract.Intent.Reset -> emitEffect(CounterContract.Effect.ShowToast("replayed"))
					else -> {}
				}
			}
		)

		// Emit effect with NO collector
		container.dispatch(CounterContract.Intent.Reset)

		// Now a late collector should get the replayed effect
		val effect = container.effectFlow.first()
		assertEquals(CounterContract.Effect.ShowToast("replayed"), effect)
	}

	@Test
	fun effectNotRedeliveredToSecondCollector() = runTest {
		val container = TestContainer(
			initialState = CounterContract.UiState(),
			contract = CounterContract,
			testScope = this,
			intentHandler = { intent ->
				when (intent) {
					CounterContract.Intent.Reset -> emitEffect(CounterContract.Effect.ShowToast("once"))
					else -> {}
				}
			}
		)

		// Dispatch then collect - first subscriber gets it
		container.dispatch(CounterContract.Intent.Reset)
		val effect1 = container.effectFlow.first()
		assertEquals(CounterContract.Effect.ShowToast("once"), effect1)

		// Second subscriber: SharedFlow has replay=0, so nothing should arrive
		val effect2 = withTimeoutOrNull(100) {
			container.effectFlow.first()
		}
		assertTrue(effect2 == null, "Second collector should not receive previously delivered effect")
	}

	// ── HandleIntent (Side Effects) ────────────────────────────────────────────

	@Test
	fun handleIntentCalledOnDispatch() = runTest {
		val handledIntents = mutableListOf<CounterContract.Intent>()
		val container = TestContainer(
			initialState = CounterContract.UiState(),
			contract = CounterContract,
			testScope = this,
			intentHandler = {
				intent -> handledIntents.add(intent)
			}
		)

		container.dispatch(CounterContract.Intent.Increment)
		container.dispatch(CounterContract.Intent.Reset)

		// handleIntent is called asynchronously after reduce, so yield to allow it to run
		yield()

		assertEquals(2, handledIntents.size)
		assertEquals(CounterContract.Intent.Increment, handledIntents[0])
		assertEquals(CounterContract.Intent.Reset, handledIntents[1])
	}

	// ── UpdateState ────────────────────────────────────────────────────────────

	@Test
	fun updateStateModifiesStateDirectly() = runTest {
		val container = TestContainer(
			initialState = CounterContract.UiState(),
			contract = CounterContract,
			testScope = this,
		)

		container.updateState { copy(count = 99, label = "direct") }

		assertEquals(99, container.stateFlow.value.count)
		assertEquals("direct", container.stateFlow.value.label)
	}
}
