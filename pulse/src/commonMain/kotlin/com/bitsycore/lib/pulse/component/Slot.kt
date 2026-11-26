package com.bitsycore.lib.pulse.component

import com.bitsycore.lib.pulse.ExperimentalPulse
import com.bitsycore.lib.pulse.Reducer

/**
 * Where a child's state lives in its parent's: declared once, it replaces the `copy` line of
 * every reducer branch that hands the child an intent.
 *
 * ```
 * private val PICKER = slot(ColorPicker, { it.picker }) { state, picker -> state.copy(picker = picker) }
 * is Intent.Picker -> PICKER.reduce(state, intent.intent)
 * ```
 */
@ExperimentalPulse
public class Slot<PARENT, STATE, INTENT>(
	private val reducer: Reducer<STATE, INTENT>,
	private val get: (PARENT) -> STATE,
	private val set: (PARENT, STATE) -> PARENT,
) {
	public fun reduce(parent: PARENT, intent: INTENT): PARENT = set(parent, reducer.reduce(get(parent), intent))
}

/**
 * A list of children in the parent's state, each found by its id. An intent for an id that is
 * no longer there changes nothing.
 *
 * ```
 * private val ROWS = listSlot(Row, { it.rows }, { state, rows -> state.copy(rows = rows) }) { it.id }
 * is Intent.Row -> ROWS.reduce(state, intent.id, intent.intent)
 * ```
 */
@ExperimentalPulse
public class ListSlot<PARENT, ID, STATE, INTENT>(
	private val reducer: Reducer<STATE, INTENT>,
	private val get: (PARENT) -> List<STATE>,
	private val set: (PARENT, List<STATE>) -> PARENT,
	private val idOf: (STATE) -> ID,
) {
	public fun reduce(parent: PARENT, id: ID, intent: INTENT): PARENT {
		val vItems = get(parent)
		val vIndex = vItems.indexOfFirst { idOf(it) == id }
		if (vIndex < 0) return parent
		val vReduced = reducer.reduce(vItems[vIndex], intent)
		if (vReduced == vItems[vIndex]) return parent
		return set(parent, vItems.toMutableList().also { it[vIndex] = vReduced })
	}

	/** The same [intent] to every child. */
	public fun reduceAll(parent: PARENT, intent: INTENT): PARENT = set(parent, get(parent).map { reducer.reduce(it, intent) })
}

@ExperimentalPulse
public fun <PARENT, STATE, INTENT> slot(
	reducer: Reducer<STATE, INTENT>,
	get: (PARENT) -> STATE,
	set: (PARENT, STATE) -> PARENT,
): Slot<PARENT, STATE, INTENT> = Slot(reducer, get, set)

@ExperimentalPulse
public fun <PARENT, ID, STATE, INTENT> listSlot(
	reducer: Reducer<STATE, INTENT>,
	get: (PARENT) -> List<STATE>,
	set: (PARENT, List<STATE>) -> PARENT,
	idOf: (STATE) -> ID,
): ListSlot<PARENT, ID, STATE, INTENT> = ListSlot(reducer, get, set, idOf)
