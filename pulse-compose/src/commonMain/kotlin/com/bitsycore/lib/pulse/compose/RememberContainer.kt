package com.bitsycore.lib.pulse.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import com.bitsycore.lib.pulse.container.Container
import com.bitsycore.lib.pulse.container.ContainerContract
import com.bitsycore.lib.pulse.container.ContainerHost
import com.bitsycore.lib.pulse.container.ContainerScope
import com.bitsycore.lib.pulse.container.IntentPolicy
import com.bitsycore.lib.pulse.container.PolicedIntent
import com.bitsycore.lib.pulse.container.StateChange
import com.bitsycore.lib.pulse.container.StateSaver

/**
 * A container that lives as long as the caller stays in the composition, for where a ViewModel
 * is too much or is not there: a dialog, a widget, a desktop window.
 *
 * Its state does not outlive the composition unless a [saver] keeps it.
 */
@Composable
public fun <STATE : Any, INTENT : Any, EFFECT : Any> rememberContainer(
	contract: ContainerContract<STATE, INTENT, EFFECT>,
	initialState: STATE,
	saver: StateSaver<STATE>? = null,
	policyOf: (INTENT) -> IntentPolicy = { (it as? PolicedIntent)?.policy ?: IntentPolicy.Parallel },
	onError: ((intent: INTENT?, error: Exception) -> Unit)? = null,
	handleIntent: suspend ContainerScope<STATE, INTENT, EFFECT>.(intent: INTENT, change: StateChange<STATE>) -> Unit = { _, _ -> },
): ContainerHost<STATE, INTENT, EFFECT> {
	val vScope = rememberCoroutineScope()
	val vPolicyOf by rememberUpdatedState(policyOf)
	val vOnError by rememberUpdatedState(onError)
	val vHandle by rememberUpdatedState(handleIntent)
	return remember(contract) {
		Container(
			contract = contract,
			initialState = initialState,
			coroutineScope = vScope,
			saver = saver,
			policyOf = { vPolicyOf(it) },
			onError = { vIntent, vError -> vOnError?.invoke(vIntent, vError) ?: throw vError },
			handleIntent = { vIntent, vChange -> vHandle(vIntent, vChange) },
		)
	}
}

/**
 * The three lines every screen starts with: the state collected with the lifecycle, the effects
 * handled while started, and the content given the state and where to send intents.
 *
 * ```
 * PulseScreen(viewModel, onEffect = { when (it) { is Effect.Done -> onDone() } }) { state, dispatch ->
 *     MyContent(state, dispatch)
 * }
 * ```
 */
@Composable
public fun <STATE : Any, INTENT : Any, EFFECT : Any> PulseScreen(
	host: ContainerHost<STATE, INTENT, EFFECT>,
	minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
	onEffect: suspend (effect: EFFECT) -> Unit = {},
	content: @Composable (state: STATE, dispatch: (INTENT) -> Unit) -> Unit,
) {
	val vState by host.collectAsStateWithLifecycle(minActiveState)
	host.collectEffect(minActiveState, onEffect)
	content(vState, remember(host) { host::dispatch })
}
