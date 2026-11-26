package com.bitsycore.lib.pulse

/**
 * A pure state transition: the same [state] and [intent] always give the same result.
 *
 * It is the one shape shared by a screen's contract and by a component, which is what lets a
 * component sit inside a contract, inside another component, or be used alone.
 */
public fun interface Reducer<STATE, INTENT> {
	public fun reduce(state: STATE, intent: INTENT): STATE
}
