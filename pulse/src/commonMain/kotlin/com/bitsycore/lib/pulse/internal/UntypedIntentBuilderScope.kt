package com.bitsycore.lib.pulse.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * DSL scope available inside [com.bitsycore.lib.pulse.container.Container.dispatch] blocks.
 *
 * [reduce] and [handle] are declarative — only the **last** definition of each
 * is executed, and both run at the end of the block (reduce first, then handle).
 *
 * ```kotlin
 * fun add(number: Int) = dispatch {
 *     reduce { copy(total = total + number) }
 *     handle {
 *         effect(CalculatorSideEffect.Toast("Added $number!"))
 *     }
 * }
 * ```
 */
@PulseDsl
class UntypedIntentBuilderScope<STATE : Any, EFFECT : Any> internal constructor(
	private val stateFlow: MutableStateFlow<STATE>,
	private val coroutineScope: CoroutineScope,
	emitEffectDelegate: suspend (EFFECT) -> Unit,
) {

	class EffectEmitterScope<EFFECT : Any> internal constructor(
		private val emitEffectDelegate: suspend (EFFECT) -> Unit,
	) {
		suspend fun effect(effect: EFFECT) = emitEffectDelegate(effect)
	}

	private val effectEmitterScope = EffectEmitterScope(emitEffectDelegate)
	private var reducer: (STATE.() -> STATE)? = null
	private var handler: (suspend EffectEmitterScope<EFFECT>.() -> Unit)? = null

	/** Declares a state reducer. Only the last call wins. */
	fun reduce(reducer: STATE.() -> STATE) {
		this.reducer = reducer
	}

	/** Declares a side-effect handler. Only the last call wins. */
	fun handle(block: suspend EffectEmitterScope<EFFECT>.() -> Unit) {
		this.handler = block
	}

	internal fun build() {
		reducer?.let { r -> stateFlow.value = stateFlow.value.r() }
		handler?.let { h -> coroutineScope.launch { effectEmitterScope.h() } }
	}
}
