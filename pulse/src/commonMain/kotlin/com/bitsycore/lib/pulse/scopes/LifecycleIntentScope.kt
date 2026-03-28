package com.bitsycore.lib.pulse.scopes

class LifecycleIntentScope<INTENT>(private val dispatcher: (INTENT) -> Unit) {

	internal var onCreate: (() -> Unit)? = null
	internal var onStart: (() -> Unit)? = null
	internal var onStop: (() -> Unit)? = null
	internal var onResume: (() -> Unit)? = null
	internal var onPause: (() -> Unit)? = null
	internal var onDestroy: (() -> Unit)? = null

	fun onCreate(block: () -> INTENT) {
		onCreate = { dispatcher(block()) }
	}

	fun onStart(block: () -> INTENT) {
		onStart = { dispatcher(block()) }
	}

	fun onStop(block: () -> INTENT) {
		onStop = { dispatcher(block()) }
	}

	fun onResume(block: () -> INTENT) {
		onResume = { dispatcher(block()) }
	}

	fun onPause(block: () -> INTENT) {
		onPause = { dispatcher(block()) }
	}

	fun onDestroy(block: () -> INTENT) {
		onDestroy = { dispatcher(block()) }
	}

}