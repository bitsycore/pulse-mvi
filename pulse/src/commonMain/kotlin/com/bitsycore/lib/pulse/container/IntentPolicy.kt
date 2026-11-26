package com.bitsycore.lib.pulse.container

import kotlin.time.Duration

/**
 * How the side effect of an intent runs next to the ones already running.
 *
 * The reducer always runs, at once, for every dispatch: a policy only decides on the side effect.
 * Policies that share a [key] share one lane; a null key means "this intent's class".
 */
public sealed interface IntentPolicy {
	public val key: Any?

	/** Every dispatch gets its own coroutine, in no particular order. */
	public data object Parallel : IntentPolicy {
		override val key: Any? get() = null
	}

	/** Skipped while a previous one is still running: a double tap prints once. */
	public data class DropIfRunning(override val key: Any? = null) : IntentPolicy

	/** Cancels the previous one: only the last ask lands. */
	public data class Latest(override val key: Any? = null) : IntentPolicy

	/** Runs after the previous ones, in dispatch order. */
	public data class Queue(override val key: Any? = null) : IntentPolicy

	/** Like [Latest], and waits [delay] of quiet before running. */
	public data class Debounce(val delay: Duration, override val key: Any? = null) : IntentPolicy
}
