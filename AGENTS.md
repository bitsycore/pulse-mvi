# PulseLibs

Kotlin Multiplatform MVI (Model-View-Intent) library.

## Project Structure

```
pulse/              Core MVI container: pure Kotlin + coroutines (JVM, Android, iOS, JS, WasmJS)
pulse-viewmodel/    AndroidX ViewModel integration, SavedStateHandle saver (JVM, Android, iOS, JS, WasmJS)
pulse-compose/      Compose Multiplatform extensions (JVM, Android, iOS, JS, WasmJS)
pulse-test/         Test kit, and the library's own tests in its commonTest (JVM, Android, iOS, JS, WasmJS)
demo/               Demo app, shared code (JVM desktop, Android library)
demoAndroid/        Demo app, Android shell
```

### Dependency Graph

```
demo → pulse-compose   → pulse
     → pulse-viewmodel → pulse
pulse-test → pulse     (its tests also use pulse-viewmodel)
```

`pulse` depends on `kotlinx-coroutines-core` only: no atomicfu, no UI, no serialization. Keep it so.

## Build Commands

```bash
./gradlew :pulse-test:jvmTest    # Run the tests (what CI runs)
./gradlew assemble               # Build every module, every target (needs ANDROID_HOME or local.properties)
./gradlew :demo:run              # Run desktop demo app
```

`pulse.demo.use` in `gradle.properties` says what the demo builds against: `local` (the modules of
this checkout), `snapshot` or `release` (published artifacts).

## Where things are

- `pulse/.../Reducer.kt`: the pure `(state, intent) -> state` shape shared by contracts and components
- `pulse/.../container/`: `Container` (the engine), `ContainerContract`, `ContainerHost` (what a
  screen sees), `ContainerScope` (what a side effect can do), `IntentPolicy`, `StateChange`,
  `PulseObserver`, `StateSaver`, `KeyedJobs` (internal, lock-free)
- `pulse/.../component/`: `ComponentContract` (state-only), `Slot` / `ListSlot` (experimental)
- `pulse-viewmodel/`: `PulseViewModel` (forwards every hook of its private container),
  `SavedStateHandle.stateSaver`
- `pulse-compose/`: `collectEffect`, `rememberReducer`, `rememberContainer`, `PulseScreen`,
  lifecycle and composition intents
- `pulse-test/`: `ContainerHost.test { }` (`HostTester`), `TestContainer`, `containerTest`

README.md is the user documentation, MIGRATION.md the 0.3 → 0.4 guide: keep both true in the same
change as the code.

## Rules of the engine

- `dispatch` reduces at once (compare-and-set, so a reducer may run twice under contention: it
  must be pure), then runs the side effect as `policyOf` says.
- A hook is never called at construction: a subclass's fields do not exist yet.
- Whatever a reducer, a side effect or a job throws goes to `onError`; only `Exception`s, and
  never a `CancellationException`.
- No coroutine is left running by a container that does nothing: tests on a `TestScope` must end.
- A new hook on `Container` is forwarded by `PulseViewModel`, and covered in `EngineTest` and
  `ViewModelTest`.

## Conventions

- Kotlin 2.3, KMP, `explicitApi()` in the library modules
- Tabs for indentation
- Locals prefixed `v`, private properties declared in a body prefixed `m`, constants in
  SCREAMING_SNAKE_CASE; doc comments are KDoc
- Never the em dash
- Package root: `com.bitsycore.lib.pulse`
- Targets: JVM, Android, iOS, JS, WasmJS
