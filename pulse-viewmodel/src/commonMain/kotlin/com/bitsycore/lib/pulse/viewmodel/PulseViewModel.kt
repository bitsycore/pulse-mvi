package com.bitsycore.lib.pulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitsycore.lib.pulse.container.Container
import com.bitsycore.lib.pulse.container.ContainerContract
import com.bitsycore.lib.pulse.container.ContainerHost
import com.bitsycore.lib.pulse.internal.UntypedIntentBuilderScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

abstract class PulseViewModel<STATE : Any, INTENT : Any, EFFECT : Any>(
	val containerContract: ContainerContract<STATE, INTENT, EFFECT>,
	replayUnconsumed: Int = 4,
	restoredState: STATE? = null
) : ViewModel(), ContainerHost<STATE, INTENT, EFFECT> {

	override val effectFlow: Flow<EFFECT> get() = container.effectFlow
	override val stateFlow: StateFlow<STATE> get() = container.stateFlow

	private val container = object : Container<STATE, INTENT, EFFECT>(containerContract, viewModelScope, replayUnconsumed, restoredState) {
		override suspend fun handleIntent(intent: INTENT) = this@PulseViewModel.handleIntent(intent)
		override fun reduce(state: STATE, intent: INTENT): STATE = this@PulseViewModel.reduce(state, intent)
	}

	override fun dispatch(intent: INTENT) {
		container.dispatch(intent)
	}

	override fun dispatchDebounced(intent: INTENT, delay: Duration, key: String?, skipIfUnchanged: Boolean, shareAcrossTypes: Boolean) =
		container.dispatchDebounced(intent, delay, key, skipIfUnchanged, shareAcrossTypes)

	protected fun dispatch(block: UntypedIntentBuilderScope<STATE, EFFECT>.() -> Unit) = container.dispatch(block)

	protected open fun reduce(state: STATE, intent: INTENT): STATE = containerContract.reduce(state, intent)
	protected open suspend fun handleIntent(intent: INTENT) {}

	protected fun updateState(block: STATE.() -> STATE) = container.updateState(block)
	protected fun emitEffect(effect: EFFECT) = container.emitEffect(effect)
}
