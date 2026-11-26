package com.bitsycore.lib.pulse.container

import com.bitsycore.lib.pulse.Pulse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The MVI engine, with no dependency but coroutines.
 *
 * [dispatch] reduces at once, through the contract's pure reducer, then runs the intent's side
 * effect ([handleIntent]) in a coroutine of [coroutineScope], as [policyOf] says. What a reducer,
 * a side effect or a job throws goes to [onError].
 *
 * @param saver keeps the state across the container's death; null keeps nothing.
 * @param observer sees what this container does, on top of [Pulse.observer].
 * @param tag what an observer is given as the source; the container itself when null.
 */
public abstract class Container<STATE : Any, INTENT : Any, EFFECT : Any>(
	protected val containerContract: ContainerContract<STATE, INTENT, EFFECT>,
	initialState: STATE,
	protected val coroutineScope: CoroutineScope,
	private val saver: StateSaver<STATE>? = null,
	private val observer: PulseObserver? = null,
	tag: Any? = null,
) : ContainerHost<STATE, INTENT, EFFECT>, ContainerScope<STATE, INTENT, EFFECT> {

	private val mTag: Any = tag ?: this
	private val mState = MutableStateFlow(saver?.restore() ?: initialState)
	private val mEffects = Channel<EFFECT>(Channel.UNLIMITED)
	private val mJobs = KeyedJobs(coroutineScope)
	private val mStarted = MutableStateFlow(false)
	private val mSubscribers = MutableStateFlow(0)

	override val stateFlow: StateFlow<STATE> = ObservedStateFlow(mState, ::onSubscribed, ::onUnsubscribed)
	override val effectFlow: Flow<EFFECT> = mEffects.receiveAsFlow()
	override val state: STATE get() = mState.value

	init {
		saver?.bind { mState.value }
	}

	// ==================
	// MARK: Hooks
	// ==================

	protected open fun reduce(state: STATE, intent: INTENT): STATE = containerContract.reduce(state, intent)

	/** The side effect of [intent], after the reducer. Override this one or the two-argument one. */
	protected open suspend fun handleIntent(intent: INTENT) {}

	/** The side effect of [intent], with what the reducer just did of it. */
	protected open suspend fun handleIntent(intent: INTENT, change: StateChange<STATE>): Unit = handleIntent(intent)

	/**
	 * How the side effect of [intent] runs next to the ones already running: what the intent says
	 * of itself when it is a [PolicedIntent], otherwise in parallel. Override to decide here.
	 */
	protected open fun policyOf(intent: INTENT): IntentPolicy = (intent as? PolicedIntent)?.policy ?: IntentPolicy.Parallel

	/**
	 * Runs once, when the container is first used: the first collection of [stateFlow] or the
	 * first [dispatch], never at construction (a subclass's fields would not exist yet).
	 */
	protected open suspend fun onStart() {}

	/**
	 * Runs while someone collects [stateFlow], and is cancelled [inactiveGrace] after the last
	 * collector left: timers, polling, a listener. Started again at the next collector.
	 */
	protected open suspend fun whileActive() {}

	/** How long [whileActive] outlives the last collector, so a rotation does not restart it. */
	protected open val inactiveGrace: Duration get() = 5.seconds

	/**
	 * What a reducer, a side effect or a job threw; [intent] is null when no intent was behind it.
	 * Rethrows by default (fail fast): override to report and keep running.
	 */
	protected open fun onError(intent: INTENT?, error: Exception): Unit = throw error

	// ==================
	// MARK: Intents
	// ==================

	final override fun dispatch(intent: INTENT) {
		ensureStarted()
		val vChange = reduced(intent) ?: return
		val vBody: suspend CoroutineScope.() -> Unit = { guarded(intent) { handleIntent(intent, vChange) } }
		when (val vPolicy = policyOf(intent)) {
			IntentPolicy.Parallel -> coroutineScope.launch(block = vBody)
			is IntentPolicy.DropIfRunning -> mJobs.once(vPolicy.key ?: intent::class, vBody)
			is IntentPolicy.Latest -> mJobs.latest(vPolicy.key ?: intent::class, vBody)
			is IntentPolicy.Queue -> mJobs.queued(vPolicy.key ?: intent::class, vBody)
			is IntentPolicy.Debounce -> mJobs.latest(vPolicy.key ?: intent::class) {
				delay(vPolicy.delay)
				vBody()
			}
		}
	}

	final override fun reduceOnly(intent: INTENT) {
		reduced(intent)
	}

	private fun reduced(intent: INTENT): StateChange<STATE>? {
		val vChange = try {
			mutate { reduce(it, intent) }
		} catch (vError: Exception) {
			report(intent, vError)
			return null
		}
		notify { it.onIntent(mTag, intent, vChange.previous, vChange.current) }
		return vChange
	}

	// ==================
	// MARK: State and effects
	// ==================

	final override fun updateState(name: String?, block: STATE.() -> STATE) {
		val vChange = mutate(block)
		notify { it.onStateUpdate(mTag, name, vChange.previous, vChange.current) }
	}

	final override fun emitEffect(effect: EFFECT) {
		mEffects.trySend(effect)
		notify { it.onEffect(mTag, effect) }
	}

	private inline fun mutate(transform: (STATE) -> STATE): StateChange<STATE> {
		while (true) {
			val vPrevious = mState.value
			val vNext = transform(vPrevious)
			if (mState.compareAndSet(vPrevious, vNext)) {
				if (vPrevious != vNext) saver?.onStateChanged(vNext)
				return StateChange(vPrevious, vNext)
			}
		}
	}

	// ==================
	// MARK: Jobs
	// ==================

	final override fun launchLatest(key: Any, block: suspend CoroutineScope.() -> Unit): Job =
		mJobs.latest(key) { guarded(null) { block() } }

	final override fun launchOnce(key: Any, block: suspend CoroutineScope.() -> Unit): Job? =
		mJobs.once(key) { guarded(null) { block() } }

	final override fun launchQueued(key: Any, block: suspend CoroutineScope.() -> Unit): Job =
		mJobs.queued(key) { guarded(null) { block() } }

	final override fun launchDebounced(key: Any, delay: Duration, block: suspend CoroutineScope.() -> Unit): Job =
		mJobs.latest(key) {
			delay(delay)
			guarded(null) { block() }
		}

	final override fun cancelJob(key: Any): Unit = mJobs.cancel(key)

	final override fun isRunning(key: Any): Boolean = mJobs.isRunning(key)

	// ==================
	// MARK: Internals
	// ==================

	private suspend inline fun guarded(intent: INTENT?, block: () -> Unit) {
		try {
			block()
		} catch (vCancellation: CancellationException) {
			throw vCancellation
		} catch (vError: Exception) {
			report(intent, vError)
		}
	}

	private fun report(intent: INTENT?, error: Exception) {
		notify { it.onError(mTag, intent, error) }
		onError(intent, error)
	}

	private inline fun notify(block: (PulseObserver) -> Unit) {
		observer?.let(block)
		Pulse.observer?.let(block)
	}

	private fun ensureStarted() {
		if (mStarted.compareAndSet(expect = false, update = true)) {
			coroutineScope.launch { guarded(null) { onStart() } }
		}
	}

	private fun onSubscribed() {
		ensureStarted()
		if (mSubscribers.updateAndGet { it + 1 } == 1) {
			mJobs.cancel(GraceKey)
			mJobs.once(ActiveKey) { guarded(null) { whileActive() } }
		}
	}

	private fun onUnsubscribed() {
		// Nothing to stop, no timer: a container without whileActive leaves no coroutine behind.
		if (mSubscribers.updateAndGet { it - 1 } == 0 && mJobs.isRunning(ActiveKey)) {
			mJobs.latest(GraceKey) {
				delay(inactiveGrace)
				if (mSubscribers.value == 0) mJobs.cancel(ActiveKey)
			}
		}
	}

	private object ActiveKey
	private object GraceKey
}

/** A [StateFlow] that says when it gains and loses a collector, with no coroutine watching it. */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class ObservedStateFlow<T>(
	private val source: StateFlow<T>,
	private val onSubscribed: () -> Unit,
	private val onUnsubscribed: () -> Unit,
) : StateFlow<T> {
	override val value: T get() = source.value
	override val replayCache: List<T> get() = source.replayCache

	override suspend fun collect(collector: FlowCollector<T>): Nothing {
		onSubscribed()
		try {
			source.collect(collector)
		} finally {
			onUnsubscribed()
		}
	}
}

/**
 * A container out of lambdas, for where a subclass is too much: a composition-scoped container,
 * a test.
 */
public fun <STATE : Any, INTENT : Any, EFFECT : Any> Container(
	contract: ContainerContract<STATE, INTENT, EFFECT>,
	initialState: STATE,
	coroutineScope: CoroutineScope,
	saver: StateSaver<STATE>? = null,
	observer: PulseObserver? = null,
	policyOf: (INTENT) -> IntentPolicy = { (it as? PolicedIntent)?.policy ?: IntentPolicy.Parallel },
	onError: ((intent: INTENT?, error: Exception) -> Unit)? = null,
	handleIntent: suspend ContainerScope<STATE, INTENT, EFFECT>.(intent: INTENT, change: StateChange<STATE>) -> Unit = { _, _ -> },
): Container<STATE, INTENT, EFFECT> {
	val vHandle = handleIntent
	val vPolicyOf = policyOf
	val vOnError = onError
	return object : Container<STATE, INTENT, EFFECT>(contract, initialState, coroutineScope, saver, observer) {
		override suspend fun handleIntent(intent: INTENT, change: StateChange<STATE>) = vHandle(intent, change)
		override fun policyOf(intent: INTENT): IntentPolicy = vPolicyOf(intent)
		override fun onError(intent: INTENT?, error: Exception) = vOnError?.invoke(intent, error) ?: throw error
	}
}
