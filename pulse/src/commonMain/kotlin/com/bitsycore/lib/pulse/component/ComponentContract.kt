package com.bitsycore.lib.pulse.component

abstract class ComponentContract<STATE : Any, INTENT : Any> {
	abstract val initialState: STATE
	abstract fun reduce(state: STATE, intent: INTENT): STATE
}