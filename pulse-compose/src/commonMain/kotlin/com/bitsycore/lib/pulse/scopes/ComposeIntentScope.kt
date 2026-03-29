package com.bitsycore.lib.pulse.scopes

class ComposeIntentScope<INTENT>(
	private val dispatcher: (INTENT) -> Unit
) {
	internal var onEnter: (() -> Unit)? = null
	internal var onExit: (() -> Unit)? = null

	@PulseScopeDsl
	fun onEnter(block: () -> INTENT) {
		onEnter = { dispatcher(block()) }
	}

	@PulseScopeDsl
	fun onExit(block: () -> INTENT) {
		onExit = { dispatcher(block()) }
	}
}
