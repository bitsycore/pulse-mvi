package com.bitsycore.lib.pulse.scopes

class LifecycleIntentScope<INTENT>(private val dispatcher: ((INTENT) -> Unit)) {

	internal var onCreate: (() -> Unit)? = null
	internal var onStart: (() -> Unit)? = null
	internal var onStop: (() -> Unit)? = null
	internal var onResume: (() -> Unit)? = null
	internal var onPause: (() -> Unit)? = null
	internal var onDestroy: (() -> Unit)? = null

	@PulseScopeDsl
	fun onCreate(block: () -> INTENT) {
		onCreate = { dispatcher(block()) }
	}

	@PulseScopeDsl
	fun onStart(block: () -> INTENT) {
		onStart = { dispatcher(block()) }
	}

	@PulseScopeDsl
	fun onStop(block: () -> INTENT) {
		onStop = { dispatcher(block()) }
	}

	@PulseScopeDsl
	fun onResume(block: () -> INTENT) {
		onResume = { dispatcher(block()) }
	}

	@PulseScopeDsl
	fun onPause(block: () -> INTENT) {
		onPause = { dispatcher(block()) }
	}

	@PulseScopeDsl
	fun onDestroy(block: () -> INTENT) {
		onDestroy = { dispatcher(block()) }
	}

}
