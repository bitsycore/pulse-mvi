package com.bitsycore.lib.pulse.container

/**
 * Sees what a container does, for logging, a journal or a debugger. Every function is called
 * on the thread the event happened on, so it must be quick and must not throw.
 *
 * [source] is the container's tag: the ViewModel when there is one, otherwise the container.
 */
public interface PulseObserver {
	/** An intent went through the reducer. */
	public fun onIntent(source: Any, intent: Any, previous: Any, current: Any) {}

	/** The state was changed outside an intent ([ContainerScope.updateState]), under [name] if it gave one. */
	public fun onStateUpdate(source: Any, name: String?, previous: Any, current: Any) {}

	public fun onEffect(source: Any, effect: Any) {}

	/** A reducer, a side effect or a job threw. [intent] is null when no intent was behind it. */
	public fun onError(source: Any, intent: Any?, error: Exception) {}
}
