@file:Suppress("DEPRECATION")

package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.utility.DebouncedDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)

class DebouncedDispatcherTest {

	@Test
	fun debouncedDispatchOnlyFiresLast() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("a", 100.milliseconds)
		dispatcher.dispatchDebounced("b", 100.milliseconds)
		dispatcher.dispatchDebounced("c", 100.milliseconds)

		advanceTimeBy(150.milliseconds)

		assertEquals(listOf("c"), dispatched)
	}

	@Test
	fun debouncedWithDifferentKeysFireIndependently() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("inc", 100.milliseconds, key = "a")
		dispatcher.dispatchDebounced("label", 100.milliseconds, key = "b")

		advanceTimeBy(150.milliseconds)

		assertEquals(listOf("inc", "label"), dispatched)
	}

	@Test
	fun debouncedSkipIfUnchanged() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("same", 100.milliseconds, skipIfUnchanged = true)
		advanceTimeBy(150.milliseconds)

		dispatcher.dispatchDebounced("same", 100.milliseconds, skipIfUnchanged = true)
		advanceTimeBy(150.milliseconds)

		assertEquals(1, dispatched.size, "skipIfUnchanged should prevent duplicate dispatch")
	}

	@Test
	fun debouncedShareAcrossTypes() = runTest {
		val dispatched = mutableListOf<Any>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced(1, 100.milliseconds, shareAcrossTypes = true)
		dispatcher.dispatchDebounced("wins", 100.milliseconds, shareAcrossTypes = true)

		advanceTimeBy(150.milliseconds)

		assertEquals(listOf<Any>("wins"), dispatched)
	}

	@Test
	fun negativeDelayHandled() = runTest {
		val dispatcher = DebouncedDispatcher<String>(this) {}
		dispatcher.dispatchDebounced("x", (-100).milliseconds)
	}

	@Test
	fun zeroDelayHandled() = runTest {
		val dispatcher = DebouncedDispatcher<String>(this) {}
		dispatcher.dispatchDebounced("x", 0.milliseconds)
	}

	@Test
	fun cancelAllCancelsPendingDispatches() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("a", 100.milliseconds, key = "x")
		dispatcher.dispatchDebounced("b", 100.milliseconds, key = "y")
		runCurrent()

		dispatcher.cancelAll()
		runCurrent()

		advanceTimeBy(150.milliseconds)

		assertEquals(emptyList(), dispatched, "cancelAll should prevent all pending dispatches")
	}

	@Test
	fun cancelAllClearsSkipHistory() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("same", 100.milliseconds, skipIfUnchanged = true)
		advanceTimeBy(150.milliseconds)
		assertEquals(1, dispatched.size)

		dispatcher.cancelAll()
		runCurrent()

		dispatcher.dispatchDebounced("same", 100.milliseconds, skipIfUnchanged = true)
		advanceTimeBy(150.milliseconds)

		assertEquals(2, dispatched.size, "cancelAll should clear history so skipIfUnchanged re-dispatches")
	}

	@Test
	fun cancelBySingleKey() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("search", 100.milliseconds, key = "search")
		dispatcher.dispatchDebounced("filter", 100.milliseconds, key = "filter")
		runCurrent()

		dispatcher.cancel("search")
		runCurrent()

		advanceTimeBy(150.milliseconds)

		assertEquals(listOf("filter"), dispatched, "cancel(key) should only cancel the targeted key")
	}

	@Test
	fun cancelByKeyClearsHistoryForThatKey() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("query", 100.milliseconds, key = "search", skipIfUnchanged = true)
		advanceTimeBy(150.milliseconds)
		assertEquals(1, dispatched.size)

		dispatcher.cancel("search")
		runCurrent()

		dispatcher.dispatchDebounced("query", 100.milliseconds, key = "search", skipIfUnchanged = true)
		advanceTimeBy(150.milliseconds)

		assertEquals(2, dispatched.size, "cancel(key) should clear history so skipIfUnchanged re-dispatches")
	}

	@Test
	fun cancelByKeyWithShareAcrossTypes() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("value", 100.milliseconds, key = "shared", shareAcrossTypes = true)
		runCurrent()

		dispatcher.cancel("shared")
		runCurrent()

		advanceTimeBy(150.milliseconds)

		assertEquals(emptyList(), dispatched, "cancel(key) should work with shareAcrossTypes keys")
	}

	@Test
	fun clearHistoryResetsSkipIfUnchanged() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("same", 100.milliseconds, skipIfUnchanged = true)
		advanceTimeBy(150.milliseconds)
		assertEquals(1, dispatched.size)

		dispatcher.clearHistory()
		runCurrent()

		dispatcher.dispatchDebounced("same", 100.milliseconds, skipIfUnchanged = true)
		advanceTimeBy(150.milliseconds)

		assertEquals(2, dispatched.size, "clearHistory should allow previously-skipped intents to dispatch")
	}

	@Test
	fun clearHistoryDoesNotCancelPending() = runTest {
		val dispatched = mutableListOf<String>()
		val dispatcher = DebouncedDispatcher(this, dispatched::add)

		dispatcher.dispatchDebounced("pending", 100.milliseconds)
		runCurrent()

		dispatcher.clearHistory()
		runCurrent()

		advanceTimeBy(150.milliseconds)

		assertEquals(listOf("pending"), dispatched, "clearHistory should not cancel pending dispatches")
	}
}
