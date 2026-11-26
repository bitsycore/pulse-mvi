package com.bitsycore.lib.pulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitsycore.lib.pulse.container.Container
import com.bitsycore.lib.pulse.container.ContainerContract
import com.bitsycore.lib.pulse.container.ContainerHost
import com.bitsycore.lib.pulse.container.IntentPolicy
import com.bitsycore.lib.pulse.container.PulseObserver
import com.bitsycore.lib.pulse.container.StateChange
import com.bitsycore.lib.pulse.container.StateSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A [Container] living in a ViewModel, on its `viewModelScope`.
 *
 * @param saver keeps the state across the ViewModel's death (`savedStateHandle.stateSaver(...)`,
 *   or one of your own); null keeps nothing.
 * @param observer sees what this ViewModel does, on top of `Pulse.observer`.
 */
public abstract class PulseViewModel<STATE : Any, INTENT : Any, EFFECT : Any>(
	private val containerContract: ContainerContract<STATE, INTENT, EFFECT>,
	initialState: STATE,
	saver: StateSaver<STATE>? = null,
	observer: PulseObserver? = null,
) : ViewModel(), ContainerHost<STATE, INTENT, EFFECT> {

	private val mContainer = object : Container<STATE, INTENT, EFFECT>(
		containerContract, initialState, viewModelScope, saver, observer, tag = this,
	) {
		override fun reduce(state: STATE, intent: INTENT): STATE = this@PulseViewModel.reduce(state, intent)
		override suspend fun handleIntent(intent: INTENT, change: StateChange<STATE>) = this@PulseViewModel.handleIntent(intent, change)
		override fun policyOf(intent: INTENT): IntentPolicy = this@PulseViewModel.policyOf(intent)
		override suspend fun onStart() = this@PulseViewModel.onStart()
		override suspend fun whileActive() = this@PulseViewModel.whileActive()
		override val inactiveGrace: Duration get() = this@PulseViewModel.inactiveGrace
		override fun onError(intent: INTENT?, error: Exception) = this@PulseViewModel.onError(intent, error)
	}

	final override val stateFlow: StateFlow<STATE> get() = mContainer.stateFlow
	final override val effectFlow: Flow<EFFECT> get() = mContainer.effectFlow
	final override val state: STATE get() = mContainer.state
	final override fun dispatch(intent: INTENT): Unit = mContainer.dispatch(intent)

	// ==================
	// MARK: Hooks
	// ==================

	protected open fun reduce(state: STATE, intent: INTENT): STATE = containerContract.reduce(state, intent)

	/** The side effect of [intent], after the reducer. Override this one or the two-argument one. */
	protected open suspend fun handleIntent(intent: INTENT) {}

	/** The side effect of [intent], with what the reducer just did of it. */
	protected open suspend fun handleIntent(intent: INTENT, change: StateChange<STATE>): Unit = handleIntent(intent)

	/** How the side effect of [intent] runs next to the ones already running. */
	protected open fun policyOf(intent: INTENT): IntentPolicy = IntentPolicy.Parallel

	/** Runs once, at the first collection of [stateFlow] or the first [dispatch], never at construction. */
	protected open suspend fun onStart() {}

	/** Runs while a screen collects [stateFlow], cancelled [inactiveGrace] after the last one left. */
	protected open suspend fun whileActive() {}

	protected open val inactiveGrace: Duration get() = 5.seconds

	/**
	 * What a reducer, a side effect or a job threw; [intent] is null when no intent was behind it.
	 * Rethrows by default (fail fast): override to report and keep running.
	 */
	protected open fun onError(intent: INTENT?, error: Exception): Unit = throw error

	// ==================
	// MARK: Tools
	// ==================

	/** Reduces only, no side effect: for a result coming back from async work. */
	protected fun reduceOnly(intent: INTENT): Unit = mContainer.reduceOnly(intent)

	/** Changes the state outside the reducer; [name] is what an observer sees of it. */
	protected fun updateState(name: String? = null, block: STATE.() -> STATE): Unit = mContainer.updateState(name, block)

	protected fun emitEffect(effect: EFFECT): Unit = mContainer.emitEffect(effect)

	/** Runs [block], cancelling the job still running under [key]. */
	protected fun launchLatest(key: Any, block: suspend CoroutineScope.() -> Unit): Job = mContainer.launchLatest(key, block)

	/** Runs [block] unless a job is still running under [key]; null when it was skipped. */
	protected fun launchOnce(key: Any, block: suspend CoroutineScope.() -> Unit): Job? = mContainer.launchOnce(key, block)

	/** Runs [block] after the jobs already under [key]. */
	protected fun launchQueued(key: Any, block: suspend CoroutineScope.() -> Unit): Job = mContainer.launchQueued(key, block)

	/** Like [launchLatest], after [delay] of quiet. */
	protected fun launchDebounced(key: Any, delay: Duration, block: suspend CoroutineScope.() -> Unit): Job =
		mContainer.launchDebounced(key, delay, block)

	/** Cancels every job under [key], running or waiting. */
	protected fun cancelJob(key: Any): Unit = mContainer.cancelJob(key)

	protected fun isRunning(key: Any): Boolean = mContainer.isRunning(key)
}
