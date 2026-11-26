package com.bitsycore.lib.pulse.container

/**
 * Keeps a container's state across its death, wherever that is: a SavedStateHandle, a sealed
 * file, nothing at all. A container without a saver keeps nothing.
 *
 * A saver that is asked when to save (a saved-state registry) implements [bind] and reads the
 * state then; one that must write as the state changes implements [onStateChanged].
 */
public interface StateSaver<STATE : Any> {
	/** The state kept from a previous life, or null to start from the initial state. */
	public fun restore(): STATE?

	/** Called once, at the container's creation, with the way to read its state at any time. */
	public fun bind(current: () -> STATE) {}

	/** Called on every state change, on the thread that made it: keep it cheap. */
	public fun onStateChanged(state: STATE) {}
}
