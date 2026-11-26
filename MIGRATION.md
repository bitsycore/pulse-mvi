# Migrating from 0.3 to 0.4

0.4 is a breaking release. What changes in your code, most common first.

## Nothing to do

- `handleIntent(intent)`, `reduce`, `emitEffect`, `updateState { }`, `dispatch`, `collectAsStateWithLifecycle` keep working as they are.

## Behaviour that changed

| | 0.3 | 0.4 |
|---|---|---|
| `collectEffect { }` | collected whatever the lifecycle | collects while STARTED; an effect emitted below waits for the return. `collectEffectAlways { }` is the old behaviour. |
| a reducer that throws | thrown from `dispatch` | goes to `onError` first, which rethrows by default |
| `containerTest`, `TestContainer` | reduced with `{ state, _ -> state }` unless given a reducer | reduce through the contract's own reducer |
| `onCompositionIntent { onEnter }` | dispatched after the first frame | dispatched when the caller enters the composition |

## Signatures that changed

| 0.3 | 0.4 |
|---|---|
| `onError(intent: INTENT, error: Throwable)` (Container only) | `onError(intent: INTENT?, error: Exception)`, on `PulseViewModel` too. `intent` is null for a job, `onStart` or `whileActive`. Errors that are not exceptions (`OutOfMemoryError`) are no longer caught. |
| `Container(contract, initialState, restoredState, coroutineScope)` | `Container(contract, initialState, coroutineScope, saver, observer, tag)` |
| `PulseViewModel(contract, initialState, restoredState)` | `PulseViewModel(contract, initialState, saver, observer)` |
| `com.bitsycore.lib.pulse.internal.ExperimentalPulse` | `com.bitsycore.lib.pulse.ExperimentalPulse` |

## Removed

| 0.3 | Instead |
|---|---|
| `dispatchCustom { reduce { }; handle { } }` | An intent. For a result coming back from async work, `reduceOnly(intent)`, or `updateState("name") { }`. |
| `pulse-savedstate`, `PulseSavedStateViewModel` | `PulseViewModel(..., saver = savedStateHandle.stateSaver(encode, decode))`, in `pulse-viewmodel`. Remove the `pulse-savedstate` dependency. |
| `restoredState` | A `StateSaver` whose `restore()` returns it. |
| `collectEffectWithLifecycle` (deprecated) | `collectEffect`, now lifecycle-aware. |
| `DebouncedDispatcher` (deprecated) | `IntentPolicy.Debounce(delay)` in `policyOf`, or `launchDebounced(key, delay) { }`. |

## Worth adopting

| If you have | Use |
|---|---|
| a flag or a set guarding against double taps | `policyOf(intent) = IntentPolicy.DropIfRunning()` or `launchOnce(key) { }` |
| `Job?` fields and `?.cancel()` | `IntentPolicy.Latest()` or `launchLatest(key) { }`, `cancelJob(key)` |
| `init { viewModelScope.launch { load() } }` | `override suspend fun onStart()` |
| a timer started and stopped from lifecycle intents | `override suspend fun whileActive()` |
| a try/catch around every side effect | `override fun onError(intent, error)` |
| a "Loaded" intent dispatched back from a side effect | `reduceOnly(Intent.Loaded(...))` |
| logging in every ViewModel | `Pulse.observer` |
| tests collecting `effectFlow` by hand | `viewModel.test(this) { expectEffect<...>() }` |
| a preview holding its state in a `remember` | `rememberReducer(contract, initialState)` |
