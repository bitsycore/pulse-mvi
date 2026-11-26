package com.bitsycore.lib.pulse.test

import androidx.lifecycle.SavedStateHandle
import com.bitsycore.lib.pulse.container.ContainerContract
import com.bitsycore.lib.pulse.container.IntentPolicy
import com.bitsycore.lib.pulse.container.StateSaver
import com.bitsycore.lib.pulse.viewmodel.PulseViewModel
import com.bitsycore.lib.pulse.viewmodel.stateSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private object Screen : ContainerContract<Screen.State, Screen.Intent, Screen.Effect>() {
	data class State(val count: Int = 0, val loaded: Boolean = false, val printing: Boolean = false)
	sealed interface Intent {
		data object Add : Intent
		data object Print : Intent
		data object Boom : Intent
		data object Loaded : Intent
	}

	sealed interface Effect {
		data object Printed : Effect
		data class Failed(val what: String?) : Effect
	}

	override fun reduce(state: State, intent: Intent): State = when (intent) {
		Intent.Add -> state.copy(count = state.count + 1)
		Intent.Print -> state.copy(printing = true)
		Intent.Loaded -> state.copy(loaded = true)
		Intent.Boom -> state
	}
}

private class ScreenViewModel(saver: StateSaver<Screen.State>? = null) :
	PulseViewModel<Screen.State, Screen.Intent, Screen.Effect>(Screen, Screen.State(), saver) {

	override fun policyOf(intent: Screen.Intent) = when (intent) {
		Screen.Intent.Print -> IntentPolicy.DropIfRunning()
		else -> IntentPolicy.Parallel
	}

	override suspend fun onStart() = reduceOnly(Screen.Intent.Loaded)

	override suspend fun handleIntent(intent: Screen.Intent) {
		when (intent) {
			Screen.Intent.Print -> {
				delay(1.seconds)
				updateState("printed") { copy(printing = false) }
				emitEffect(Screen.Effect.Printed)
			}
			Screen.Intent.Boom -> error("boom")
			else -> Unit
		}
	}

	override fun onError(intent: Screen.Intent?, error: Exception) = emitEffect(Screen.Effect.Failed(error.message))
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

	private fun viewModelTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
		Dispatchers.setMain(StandardTestDispatcher(testScheduler))
		try {
			block()
		} finally {
			Dispatchers.resetMain()
		}
	}

	@Test
	fun aViewModelOverridesEveryHook() = viewModelTest {
		ScreenViewModel().test(this) {
			expectState { it.loaded }

			// A double tap prints once.
			dispatch(Screen.Intent.Print)
			dispatch(Screen.Intent.Print)
			expectState { it.printing }
			advanceBy(2.seconds)
			expectEffect<Screen.Effect.Printed>()
			expectNoEffects()
			expectState { !it.printing }

			// A failure is the ViewModel's to handle, and it lives on.
			dispatch(Screen.Intent.Boom)
			assertEquals("boom", expectEffect<Screen.Effect.Failed>().what)
			dispatch(Screen.Intent.Add)
			expectState { it.count == 1 }
		}
	}

	@Test
	fun theStateSurvivesThroughASavedStateHandle() = viewModelTest {
		fun SavedStateHandle.saver(onError: (Exception) -> Unit = {}) = stateSaver<Screen.State>(
			encode = { it.count.toString() },
			decode = { Screen.State(count = it.toInt()) },
			onDecodeError = onError,
		)

		val vHandle = SavedStateHandle()
		ScreenViewModel(vHandle.saver()).test(this) {
			dispatch(Screen.Intent.Add)
			dispatch(Screen.Intent.Add)
			expectState { it.count == 2 }
		}

		// What the platform would keep, given to the next life.
		val vKept = vHandle.savedStateProvider().saveState()
		val vRestored = SavedStateHandle.createHandle(vKept, null)
		assertEquals(2, ScreenViewModel(vRestored.saver()).state.count)

		// A fresh handle starts from the initial state.
		assertEquals(0, ScreenViewModel(SavedStateHandle().saver()).state.count)
	}
}
