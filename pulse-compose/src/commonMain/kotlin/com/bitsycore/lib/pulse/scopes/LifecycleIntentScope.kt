package com.bitsycore.lib.pulse.scopes

import androidx.lifecycle.Lifecycle

@PulseScopeDsl
public class LifecycleIntentScope<INTENT>(private val dispatcher: (INTENT) -> Unit) {

	private val mHandlers = mutableMapOf<Lifecycle.Event, () -> INTENT>()

	public fun onCreate(block: () -> INTENT) { mHandlers[Lifecycle.Event.ON_CREATE] = block }
	public fun onStart(block: () -> INTENT) { mHandlers[Lifecycle.Event.ON_START] = block }
	public fun onResume(block: () -> INTENT) { mHandlers[Lifecycle.Event.ON_RESUME] = block }
	public fun onPause(block: () -> INTENT) { mHandlers[Lifecycle.Event.ON_PAUSE] = block }
	public fun onStop(block: () -> INTENT) { mHandlers[Lifecycle.Event.ON_STOP] = block }
	public fun onDestroy(block: () -> INTENT) { mHandlers[Lifecycle.Event.ON_DESTROY] = block }

	internal fun on(event: Lifecycle.Event) {
		mHandlers[event]?.let { dispatcher(it()) }
	}
}
