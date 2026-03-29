package com.bitsycore.lib.pulse.scopes

@PulseScopeDsl
class ComposeIntentScope<INTENT>(
	private val dispatcher: (INTENT) -> Unit
) {
	internal var onEnter: (() -> Unit)? = null
	internal var onExit: (() -> Unit)? = null

	fun onEnter(block: () -> INTENT) {
		onEnter = { dispatcher(block()) }
	}

	fun onExit(block: () -> INTENT) {
		onExit = { dispatcher(block()) }
	}
}
