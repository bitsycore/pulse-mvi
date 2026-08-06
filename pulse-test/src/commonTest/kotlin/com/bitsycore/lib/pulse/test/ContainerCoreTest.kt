package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.container.Container
import com.bitsycore.lib.pulse.container.ContainerContract
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests targeting the real [Container] implementation directly
 * (not [TestContainer], which has its own effect/reduce plumbing).
 */
class ContainerCoreTest {

	private object CoreContract : ContainerContract<CoreContract.UiState, CoreContract.Intent, CoreContract.Effect>() {
		data class UiState(val count: Int = 0)

		sealed interface Intent {
			data object EmitOne : Intent
			data object Boom : Intent
		}

		sealed interface Effect {
			data class Ping(val tag: String) : Effect
		}
	}

	private class CoreContainer(
		coroutineScope: CoroutineScope,
		private val onErrorCallback: (Throwable) -> Unit = { throw it }
	) : Container<CoreContract.UiState, CoreContract.Intent, CoreContract.Effect>(
		containerContract = CoreContract,
		initialState = CoreContract.UiState(),
		coroutineScope = coroutineScope
	) {
		override suspend fun handleIntent(intent: CoreContract.Intent) {
			when (intent) {
				CoreContract.Intent.EmitOne -> emitEffect(CoreContract.Effect.Ping("one"))
				CoreContract.Intent.Boom -> throw IllegalStateException("boom")
			}
		}

		override fun onError(intent: CoreContract.Intent, error: Throwable) = onErrorCallback(error)
	}

	// Regression: effects emitted while nobody collects must be buffered, not dropped.
	@Test
	fun effectEmittedWithoutCollectorIsBuffered() = runTest {
		val container = CoreContainer(backgroundScope)

		container.dispatch(CoreContract.Intent.EmitOne)

		// Collector subscribes only AFTER the effect was emitted.
		val effect = container.effectFlow.first()
		assertEquals(CoreContract.Effect.Ping("one"), effect)
	}

	// Regression: an exception in handleIntent must be routed to onError instead of
	// killing the scope when the container overrides it.
	@Test
	fun handleIntentExceptionIsRoutedToOnError() = runTest {
		val vCaught = CompletableDeferred<Throwable>()
		val container = CoreContainer(backgroundScope, onErrorCallback = { vCaught.complete(it) })

		container.dispatch(CoreContract.Intent.Boom)
		assertEquals("boom", vCaught.await().message)

		// The container must still be fully alive after the error.
		container.dispatch(CoreContract.Intent.EmitOne)
		assertEquals(CoreContract.Effect.Ping("one"), container.effectFlow.first())
	}
}
