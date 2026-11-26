package com.bitsycore.lib.pulse.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bitsycore.lib.pulse.container.ContainerHost
import com.bitsycore.lib.pulse.scopes.ComposeIntentScope
import com.bitsycore.lib.pulse.scopes.LifecycleIntentScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Where effects are collected: the main thread, without a dispatch. An effect handed to a
 * collector that is cancelled before a dispatched resume would be lost; resumed at once, it
 * cannot be. Falls back to the caller's context where there is no main dispatcher.
 */
private val EFFECT_CONTEXT: CoroutineContext = try {
	Dispatchers.Main.immediate
} catch (_: NotImplementedError) {
	EmptyCoroutineContext
} catch (_: IllegalStateException) {
	EmptyCoroutineContext
}

@Composable
public fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.collectAsStateWithLifecycle(
	minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
): State<STATE> = stateFlow.collectAsStateWithLifecycle(minActiveState = minActiveState)

/**
 * Handles the one-shot effects while the lifecycle is at least [minActiveState]. An effect
 * emitted below it (the app in the background, the screen under another one) waits and is
 * handled at the return.
 */
@Suppress("ComposableNaming")
@Composable
public fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.collectEffect(
	minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
	onEffect: suspend (effect: EFFECT) -> Unit,
) {
	val vLifecycleOwner = LocalLifecycleOwner.current
	val vOnEffect by rememberUpdatedState(onEffect)
	LaunchedEffect(this, vLifecycleOwner, minActiveState) {
		vLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
			withContext(EFFECT_CONTEXT) {
				effectFlow.collect { vOnEffect(it) }
			}
		}
	}
}

/** Handles the one-shot effects whatever the lifecycle, for as long as the caller is composed. */
@Suppress("ComposableNaming")
@Composable
public fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.collectEffectAlways(
	onEffect: suspend (effect: EFFECT) -> Unit,
) {
	val vOnEffect by rememberUpdatedState(onEffect)
	LaunchedEffect(this) {
		withContext(EFFECT_CONTEXT) {
			effectFlow.collect { vOnEffect(it) }
		}
	}
}

@Deprecated("collectEffect is lifecycle-aware", ReplaceWith("collectEffect(lifecycleState, sideEffect)"))
@Suppress("ComposableNaming")
@Composable
public fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.collectEffectWithLifecycle(
	lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
	sideEffect: suspend (sideEffect: EFFECT) -> Unit,
): Unit = collectEffect(lifecycleState, sideEffect)

/**
 * Dispatches an intent at the lifecycle events the block names. Most of the time a ViewModel's
 * `onStart` and `whileActive` say the same thing without the screen being involved.
 */
@Suppress("ComposableNaming")
@Composable
public fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.onLifecycleIntent(
	lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
	mapper: LifecycleIntentScope<INTENT>.() -> Unit,
) {
	// Built anew at each composition and read through a State: nothing remembered is written
	// during composition.
	val vScope by rememberUpdatedState(LifecycleIntentScope(::dispatch).apply(mapper))
	DisposableEffect(this, lifecycleOwner) {
		val vObserver = LifecycleEventObserver { _, vEvent -> vScope.on(vEvent) }
		lifecycleOwner.lifecycle.addObserver(vObserver)
		onDispose { lifecycleOwner.lifecycle.removeObserver(vObserver) }
	}
}

/** Dispatches an intent when the caller enters the composition, and when it leaves it. */
@Suppress("ComposableNaming")
@Composable
public fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.onCompositionIntent(
	key1: Any? = Unit,
	vararg keys: Any?,
	mapper: ComposeIntentScope<INTENT>.() -> Unit,
) {
	val vScope by rememberUpdatedState(ComposeIntentScope(::dispatch).apply(mapper))
	DisposableEffect(this, key1, *keys) {
		vScope.onEnter?.invoke()
		onDispose { vScope.onExit?.invoke() }
	}
}
