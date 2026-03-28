package com.bitsycore.demo.page2

import com.bitsycore.demo.page2.Page2Contract.Intent
import com.bitsycore.demo.page2.Page2Contract.UiState

class Page2ViewModel : Page2Contract.VM(Page2Contract) {

	override fun reduce(state: UiState, intent: Intent): UiState = state

	override suspend fun handleIntent(intent: Intent) {
		when (intent) {
			// Log all lifecycle events
			Intent.OnCreated -> println("[Page2][Lifecycle] onCreate")
			Intent.OnStarted -> println("[Page2][Lifecycle] onStart")
			Intent.OnResumed -> println("[Page2][Lifecycle] onResume")
			Intent.OnPaused -> println("[Page2][Lifecycle] onPause")
			Intent.OnStopped -> println("[Page2][Lifecycle] onStop")
			Intent.OnDestroyed -> println("[Page2][Lifecycle] onDestroy")

			// Log all composition events
			Intent.OnScreenEntered -> println("[Page2][Composition] onEnter")
			Intent.OnScreenRecomposed -> println("[Page2][Composition] onRecompose")
			Intent.OnScreenExited -> println("[Page2][Composition] onExit")
		}
	}
}
