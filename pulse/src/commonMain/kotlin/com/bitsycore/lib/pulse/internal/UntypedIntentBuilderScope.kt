package com.bitsycore.lib.pulse.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * DSL scope available inside [com.bitsycore.lib.pulse.container.Container.dispatchCustom] blocks.
 *
 * [reduce] and [handle] are declarative — only the **last** definition of each
 * is executed, and both run at the end of the block (reduce first, then handle).
 *
 * ```kotlin
 * fun add(number: Int) = dispatch {
 *     reduce { copy(total = total + number) }
 *     handle {
 *         emitEffect(CalculatorSideEffect.Toast("Added $number!"))
 *     }
 * }
 * ```
 */
@PulseDsl
class UntypedIntentBuilderScope<STATE : Any> internal constructor(
	private val stateFlow: MutableStateFlow<STATE>,
	private val coroutineScope: CoroutineScope,
) {
	class HandlerScope internal constructor()

	private val handlerScope = HandlerScope()
	private var reducer: (STATE.() -> STATE)? = null
	private var handler: (suspend HandlerScope.() -> Unit)? = null

	/** Declares a state reducer. Only the last call wins. */
	fun reduce(reducer: STATE.() -> STATE) {
		this.reducer = reducer
	}

	/** Declares a side-effect handler. Only the last call wins. */
	fun handle(block: suspend HandlerScope.() -> Unit) {
		this.handler = block
	}

	internal fun build() {
		reducer?.let { r -> stateFlow.value = stateFlow.value.r() }
		handler?.let { h -> coroutineScope.launch { handlerScope.h() } }
	}
}
