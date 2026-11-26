package com.bitsycore.lib.pulse.test

import com.bitsycore.lib.pulse.ExperimentalPulse
import com.bitsycore.lib.pulse.component.ComponentContract
import com.bitsycore.lib.pulse.component.listSlot
import com.bitsycore.lib.pulse.component.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

private object Counter : ComponentContract<Counter.State, Counter.Intent>(State()) {
	data class State(val id: Int = 0, val count: Int = 0)
	sealed interface Intent {
		data object Add : Intent
		data object Reset : Intent
	}

	override fun reduce(state: State, intent: Intent): State = when (intent) {
		Intent.Add -> state.copy(count = state.count + 1)
		Intent.Reset -> state.copy(count = 0)
	}
}

/** A component holding components: one counter, and a list of them. */
@OptIn(ExperimentalPulse::class)
private object Board : ComponentContract<Board.State, Board.Intent>(State()) {
	data class State(val main: Counter.State = Counter.initialState, val rows: List<Counter.State> = emptyList())
	sealed interface Intent {
		data class Main(val intent: Counter.Intent) : Intent
		data class Row(val id: Int, val intent: Counter.Intent) : Intent
		data object ResetRows : Intent
	}

	private val MAIN = slot(Counter, State::main) { vState, vMain -> vState.copy(main = vMain) }
	private val ROWS = listSlot(Counter, State::rows, { vState, vRows -> vState.copy(rows = vRows) }) { it.id }

	override fun reduce(state: State, intent: Intent): State = when (intent) {
		is Intent.Main -> MAIN.reduce(state, intent.intent)
		is Intent.Row -> ROWS.reduce(state, intent.id, intent.intent)
		Intent.ResetRows -> ROWS.reduceAll(state, Counter.Intent.Reset)
	}
}

class SlotTest {

	@Test
	fun aSlotReducesTheChildInPlace() {
		val vState = Board.reduce(Board.initialState, Board.Intent.Main(Counter.Intent.Add))
		assertEquals(1, vState.main.count)
	}

	@Test
	fun aListSlotFindsItsChildById() {
		val vStart = Board.State(rows = listOf(Counter.State(id = 1), Counter.State(id = 2)))
		val vState = Board.reduce(vStart, Board.Intent.Row(2, Counter.Intent.Add))
		assertEquals(listOf(0, 1), vState.rows.map { it.count })

		// A child that is gone, or one that did not change, leaves the parent as it was.
		assertSame(vState, Board.reduce(vState, Board.Intent.Row(9, Counter.Intent.Add)))
		assertSame(vStart, Board.reduce(vStart, Board.Intent.Row(1, Counter.Intent.Reset)))

		assertEquals(listOf(0, 0), Board.reduce(vState, Board.Intent.ResetRows).rows.map { it.count })
	}
}
