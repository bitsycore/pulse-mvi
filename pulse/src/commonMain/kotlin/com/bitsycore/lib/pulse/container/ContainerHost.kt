package com.bitsycore.lib.pulse.container

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** What a screen sees of a container: its state, its one-shot effects, and where intents go. */
public interface ContainerHost<STATE : Any, INTENT : Any, EFFECT : Any> {
	public val stateFlow: StateFlow<STATE>

	/**
	 * One-shot effects, each delivered exactly once and kept while nobody collects.
	 * Meant for a single collector: two collectors would share the effects between them.
	 */
	public val effectFlow: Flow<EFFECT>

	public val state: STATE get() = stateFlow.value

	public fun dispatch(intent: INTENT)
}
