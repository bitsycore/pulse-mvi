package com.bitsycore.lib.pulse.scopes

@PulseScopeDsl
public class ComposeIntentScope<INTENT>(private val dispatcher: (INTENT) -> Unit) {
	internal var onEnter: (() -> Unit)? = null
	internal var onExit: (() -> Unit)? = null

	public fun onEnter(block: () -> INTENT) {
		onEnter = { dispatcher(block()) }
	}

	public fun onExit(block: () -> INTENT) {
		onExit = { dispatcher(block()) }
	}
}
