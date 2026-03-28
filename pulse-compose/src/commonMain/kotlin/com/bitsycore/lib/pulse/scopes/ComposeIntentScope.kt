package com.bitsycore.lib.pulse.scopes

class ComposeIntentScope<INTENT>(
	private val dispatcher: (INTENT) -> Unit
) {
	internal var onEnter: (() -> Unit)? = null
	internal var onExit: (() -> Unit)? = null
	internal var onRecompose: (() -> Unit)? = null

	fun onEnter(block: () -> INTENT) {
		onEnter = { dispatcher(block()) }
	}

	fun onExit(block: () -> INTENT) {
		onExit = { dispatcher(block()) }
	}

	fun onRecompose(block: () -> INTENT) {
		onRecompose = { dispatcher(block()) }
	}
}
