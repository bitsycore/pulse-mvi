package com.bitsycore.lib.pulse.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@Composable
fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.collectAsState(
	lifecycleState: Lifecycle.State = Lifecycle.State.STARTED
) = stateFlow.collectAsStateWithLifecycle(minActiveState = lifecycleState)

@Suppress("ComposableNaming")
@Composable
fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.collectEffect(
	lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
	sideEffect: (suspend (sideEffect: EFFECT) -> Unit)
) {
	val lifecycleOwner = LocalLifecycleOwner.current
	val callback by rememberUpdatedState(newValue = sideEffect)
	LaunchedEffect(lifecycleOwner) {
		lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
			effectFlow.collect { callback(it) }
		}
	}
}

@Suppress("ComposableNaming")
@Composable
fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.onLifecycleIntent(
	lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
	mapper: LifecycleIntentScope<INTENT>.() -> Unit
) {
	val scope = remember {
		LifecycleIntentScope<INTENT> { dispatch(it) }
	}

	// rebuild handlers safely
	scope.onCreate = null
	scope.onStart = null
	scope.onResume = null
	scope.onPause = null
	scope.onStop = null
	scope.onDestroy = null
	scope.mapper()

	val callback by rememberUpdatedState(
		LifecycleEventObserver { _, event ->
			when (event) {
				Lifecycle.Event.ON_CREATE -> scope.onCreate?.invoke()
				Lifecycle.Event.ON_START -> scope.onStart?.invoke()
				Lifecycle.Event.ON_RESUME -> scope.onResume?.invoke()
				Lifecycle.Event.ON_PAUSE -> scope.onPause?.invoke()
				Lifecycle.Event.ON_STOP -> scope.onStop?.invoke()
				Lifecycle.Event.ON_DESTROY -> scope.onDestroy?.invoke()
				else -> {}
			}
		}
	)

	DisposableEffect(lifecycleOwner) {
		lifecycleOwner.lifecycle.addObserver(callback)
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(callback)
		}
	}
}

@Suppress("ComposableNaming")
@Composable
fun <STATE : Any, INTENT : Any, EFFECT : Any> ContainerHost<STATE, INTENT, EFFECT>.onCompositionIntent(
	key1: Any? = Unit,
	vararg keys: Any?,
	mapper: ComposeIntentScope<INTENT>.() -> Unit
) {
	val scope = remember(key1, *keys) {
		ComposeIntentScope<INTENT> { dispatch(it) }
	}

	scope.onEnter = null
	scope.onExit = null
	scope.mapper()

	// Capture into stable refs so the effects always exist
	val enterRef = rememberUpdatedState(scope.onEnter)
	val exitRef = rememberUpdatedState(scope.onExit)

	// Always composed — unconditional
	LaunchedEffect(key1, *keys) {
		enterRef.value?.invoke()
	}

	DisposableEffect(key1, *keys) {
		onDispose { exitRef.value?.invoke() }
	}
}
