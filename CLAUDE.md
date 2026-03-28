# PulseLibs

Kotlin Multiplatform MVI (Model-View-Intent) library.

## Project Structure

```
pulse/              Core MVI container — pure Kotlin + coroutines (all KMP targets)
pulse-viewmodel/    AndroidX ViewModel integration (Android, JVM, iOS)
pulse-compose/      Compose Multiplatform extensions (Android, JVM, iOS)
demo/               Desktop demo app (JVM)
```

### Dependency Graph

```
demo → pulse-compose → pulse
     → pulse-viewmodel → pulse
```

`pulse` has zero UI dependencies — only `kotlinx-coroutines-core`.

## Build Commands

```bash
./gradlew build                  # Build all modules
./gradlew :pulse:build           # Build core only
./gradlew :pulse-viewmodel:build # Build viewmodel module
./gradlew :pulse-compose:build   # Build compose module
./gradlew :demo:run              # Run desktop demo app
```

## MVI Pattern

- **ContainerContract** — declares `STATE`, `INTENT`, `EFFECT` types + `initialState`
- **Container** — core engine: `dispatch(intent)` → `reduce()` → new state; `handleIntent()` for async side-effects; `emitEffect()` for one-shot events
- **ContainerHost** — interface exposing `stateFlow`, `effectFlow`, `dispatch`, `dispatchDebounced`
- **ComponentContract** — lightweight sub-container with its own reducer (no effects)
- **PulseViewModel** — AndroidX ViewModel wrapper around Container
- **ComposeExtensions** — `collectAsState()`, `collectEffect()`, `onLifecycleIntent()`, `onCompositionIntent()`

## Screen Pattern (Compose)

```kotlin
@Composable
fun XScreen(viewModel: XViewModel = viewModel { XViewModel() }) {
    val state by viewModel.collectAsState()
    viewModel.collectEffect { /* handle one-shot effects */ }
    XContent(state, viewModel::dispatch)
}

@Composable
fun XContent(state: UiState, dispatch: (Intent) -> Unit) {
    // Pure UI — no ViewModel reference
}
```

## Conventions

- Kotlin 2.3, KMP
- Tabs for indentation
- Experimental features enabled: context parameters, explicit backing fields
- Package root: `com.bitsycore.lib.pulse`
- Targets: pulse supports all major KMP targets (JVM, Android, iOS, macOS, Linux, Windows, watchOS, tvOS)
