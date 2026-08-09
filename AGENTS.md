# PulseLibs

Kotlin Multiplatform MVI (Model-View-Intent) library.

## Project Structure

```
pulse/              Core MVI container - pure Kotlin + coroutines (JVM, Android, iOS, JS, WasmJS)
pulse-viewmodel/    AndroidX ViewModel integration (JVM, Android, iOS, JS, WasmJS)
pulse-savedstate/   SavedStateHandle integration - auto-persist state (JVM, Android, iOS, JS, WasmJS)
pulse-compose/      Compose Multiplatform extensions (JVM, Android, iOS, JS, WasmJS)
pulse-test/         Testing utilities - TestContainer + assertions (JVM, Android, iOS, JS, WasmJS)
demo/               Desktop demo app (JVM)
```

### Dependency Graph

```
demo → pulse-compose     → pulse
     → pulse-savedstate  → pulse-viewmodel → pulse
     → pulse-test        → pulse
```

`pulse` has zero UI dependencies - only `kotlinx-coroutines-core`.

## Build Commands

```bash
./gradlew build                  # Build all modules
./gradlew :pulse:build           # Build core only
./gradlew :pulse-viewmodel:build # Build viewmodel module
./gradlew :pulse-savedstate:build # Build savedstate module
./gradlew :pulse-compose:build   # Build compose module
./gradlew :pulse-test:build      # Build test utilities
./gradlew :demo:run              # Run desktop demo app
```

## MVI Pattern

- **ContainerContract** - declares `STATE`, `INTENT`, `EFFECT` types (no `initialState`; state is provided by the Container/ViewModel)
- **Container** - core engine: takes `initialState` as constructor parameter; `dispatch(intent)` → `reduce()` → new state; `handleIntent()` for async side-effects; `emitEffect()` for one-shot events; supports `restoredState` for state restoration
- **ContainerHost** - interface exposing `stateFlow`, `effectFlow`, `dispatch`
- **DebouncedDispatcher** - standalone debounce engine: `dispatchDebounced()`, `cancel(key)`, `cancelAll()`, `clearHistory()`; thread-safe, composable with any dispatch function
- **ComponentContract** - lightweight sub-container with its own reducer (no effects)
- **PulseViewModel** - AndroidX ViewModel wrapper around Container
- **PulseSavedStateViewModel** - PulseViewModel + SavedStateHandle auto-persistence (STATE must be `@Serializable`)
- **ComposeExtensions** - `collectAsStateWithLifecycle()`, `collectEffect()`, `collectEffectWithLifecycle()`, `onLifecycleIntent()`, `onCompositionIntent()`
- **TestContainer** - test-friendly Container with `UnconfinedTestDispatcher`

## Screen Pattern (Compose)

```kotlin
@Composable
fun XScreen(viewModel: XViewModel = viewModel { XViewModel() }) {
    val state by viewModel.collectAsStateWithLifecycle()
    viewModel.collectEffect { /* handle one-shot effects */ }
    XContent(state, viewModel::dispatch)
}

@Composable
fun XContent(state: UiState, dispatch: (Intent) -> Unit) {
    // Pure UI - no ViewModel reference
}
```

## SavedState Pattern

```kotlin
@Serializable
data class UiState(val count: Int = 0)

class MyViewModel(savedStateHandle: SavedStateHandle) :
    PulseSavedStateViewModel<UiState, Intent, Effect>(
        containerContract = MyContract,
        initialState = UiState(),
        savedStateHandle = savedStateHandle,
        serializer = UiState.serializer()
    ) {
    override fun reduce(state: UiState, intent: Intent): UiState = ...
}

// In Compose:
viewModel { MyViewModel(createSavedStateHandle()) }
```

## Testing Pattern

```kotlin
MyContract.containerTest(
    initialState = MyContract.UiState(),
    reduce = { state, intent -> /* ... */ }
) {
    dispatch(MyIntent.Increment)
    assertState { it.count == 1 }
}
```

## Conventions

- Kotlin 2.3, KMP
- Tabs for indentation
- Package root: `com.bitsycore.lib.pulse`
- Targets: JVM, Android, iOS, JS, WasmJS
