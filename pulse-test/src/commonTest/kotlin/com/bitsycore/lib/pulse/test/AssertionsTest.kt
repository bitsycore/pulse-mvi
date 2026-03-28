package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.container.ContainerContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// ── Minimal Contract for Assertion Tests ───────────────────────────────────────

private object SimpleContract : ContainerContract<SimpleContract.UiState, SimpleContract.Intent, SimpleContract.Effect>() {
	override val initialState = UiState()

	data class UiState(val value: Int = 0)

	sealed interface Intent {
		data object Up : Intent
	}

	sealed interface Effect {
		data class Notify(val msg: String) : Effect
	}
}

// ── Tests ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class AssertionsTest {

	@Test
	fun assertStateWithExpectedValuePasses() = SimpleContract.containerTest(
		reduce = { state, intent ->
			when (intent) {
				SimpleContract.Intent.Up -> state.copy(value = state.value + 1)
			}
		}
	) {
		dispatch(SimpleContract.Intent.Up)
		assertState(SimpleContract.UiState(value = 1))
	}

	@Test
	fun assertStateWithExpectedValueFails() = SimpleContract.containerTest {
		assertFailsWith<AssertionError> {
			assertState(SimpleContract.UiState(value = 999))
		}
	}

	@Test
	fun assertStateWithPredicatePasses() = SimpleContract.containerTest(
		reduce = { state, intent ->
			when (intent) {
				SimpleContract.Intent.Up -> state.copy(value = state.value + 1)
			}
		}
	) {
		dispatch(SimpleContract.Intent.Up)
		dispatch(SimpleContract.Intent.Up)
		assertState { it.value > 0 }
	}

	@Test
	fun assertStateWithPredicateFails() = SimpleContract.containerTest {
		assertFailsWith<AssertionError> {
			assertState("value should be positive") { it.value > 0 }
		}
	}

	@Test
	fun containerTestDslProvidesWorkingContainer() = SimpleContract.containerTest(
		reduce = { state, intent ->
			when (intent) {
				SimpleContract.Intent.Up -> state.copy(value = state.value + 1)
			}
		},
		handleIntent = { intent ->
			when (intent) {
				SimpleContract.Intent.Up -> emitEffect(SimpleContract.Effect.Notify("up!"))
			}
		}
	) {
		// State
		dispatch(SimpleContract.Intent.Up)
		assertState { it.value == 1 }

		// Effect
		val effect = awaitEffect {
			dispatch(SimpleContract.Intent.Up)
		}
		assertEquals(SimpleContract.Effect.Notify("up!"), effect)
		assertState { it.value == 2 }
	}

	@Test
	fun awaitEffectReturnsCorrectEffect() = SimpleContract.containerTest(
		handleIntent = { intent ->
			when (intent) {
				SimpleContract.Intent.Up -> emitEffect(SimpleContract.Effect.Notify("hello"))
			}
		}
	) {
		val effect = awaitEffect { dispatch(SimpleContract.Intent.Up) }
		assertTrue(effect is SimpleContract.Effect.Notify)
		assertEquals("hello", effect.msg)
	}
}
