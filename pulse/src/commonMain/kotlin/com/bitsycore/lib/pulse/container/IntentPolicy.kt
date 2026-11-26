package com.bitsycore.lib.pulse.container

import kotlin.time.Duration

/**
 * How the side effect of an intent runs next to the ones already running.
 *
 * The reducer always runs, at once, for every dispatch: a policy only decides on the side effect.
 * Policies that share a [key] share one lane; a null key means "this intent's class".
 */
public sealed interface IntentPolicy : PolicedIntent {
	public val key: Any?

	/** A policy is its own: what lets an intent say `PolicedIntent by IntentPolicy.Latest()`. */
	override val policy: IntentPolicy get() = this

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

/**
 * An intent that says its own policy, where it is declared, instead of in `policyOf`:
 * ```
 * sealed interface Intent {
 *     data object Print : Intent, PolicedIntent by IntentPolicy.DropIfRunning()
 *     data class Search(val query: String) : Intent, PolicedIntent by IntentPolicy.Debounce(300.milliseconds)
 *     data object Refresh : Intent   // Parallel
 * }
 * ```
 * or, written out, `override val policy = IntentPolicy.DropIfRunning()`.
 *
 * It is the default only: a container's `policyOf` has the final word.
 */
public interface PolicedIntent {
	public val policy: IntentPolicy
}
