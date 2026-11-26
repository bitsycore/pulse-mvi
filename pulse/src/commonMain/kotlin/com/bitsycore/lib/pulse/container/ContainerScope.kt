package com.bitsycore.lib.pulse.container

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.time.Duration

/** What a side effect can do with its container. */
public interface ContainerScope<STATE : Any, INTENT : Any, EFFECT : Any> {
	public val state: STATE

	/** Reduces, then runs the intent's side effect. */
	public fun dispatch(intent: INTENT)

	/** Reduces only, no side effect: for a result coming back from async work. */
	public fun reduceOnly(intent: INTENT)

	/** Changes the state outside the reducer; [name] is what an observer sees of it. */
	public fun updateState(name: String? = null, block: STATE.() -> STATE)

	public fun emitEffect(effect: EFFECT)

	/** Runs [block], cancelling the job still running under [key]. */
	public fun launchLatest(key: Any, block: suspend CoroutineScope.() -> Unit): Job

	/** Runs [block] unless a job is still running under [key]; null when it was skipped. */
	public fun launchOnce(key: Any, block: suspend CoroutineScope.() -> Unit): Job?

	/** Runs [block] after the jobs already under [key]. */
	public fun launchQueued(key: Any, block: suspend CoroutineScope.() -> Unit): Job

	/** Like [launchLatest], after [delay] of quiet. */
	public fun launchDebounced(key: Any, delay: Duration, block: suspend CoroutineScope.() -> Unit): Job

	/** Cancels every job under [key], running or waiting. */
	public fun cancelJob(key: Any)

	public fun isRunning(key: Any): Boolean
}
