package com.bitsycore.lib.pulse.container

/**
 * What the reducer did with one intent: the state it was given and the state it returned,
 * captured atomically. Lets a side effect tell "I set busy" from "busy was already set".
 */
public class StateChange<out STATE : Any>(
	public val previous: STATE,
	public val current: STATE,
) {
	public val changed: Boolean get() = previous != current

	public operator fun component1(): STATE = previous
	public operator fun component2(): STATE = current

	override fun toString(): String = "StateChange(previous=$previous, current=$current)"
}
