package com.bitsycore.lib.pulse.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import com.bitsycore.lib.pulse.container.StateSaver

private const val VALUE = "value"

/**
 * Keeps a ViewModel's state in its [SavedStateHandle], as the text [encode] makes of it. The
 * state is encoded only when the platform asks for the saved state, not at every change.
 *
 * [encode] and [decode] are yours: JSON, a subset of the state, or a sealed form of it
 * (`{ seal(Json.encodeToString(it)) }`). A state that cannot be decoded is given to
 * [onDecodeError] and the ViewModel starts from its initial state.
 *
 * ```
 * class MyViewModel(handle: SavedStateHandle) : PulseViewModel<UiState, Intent, Effect>(
 *     MyContract, UiState(),
 *     saver = handle.stateSaver({ Json.encodeToString(it) }, { Json.decodeFromString(it) }),
 * )
 * ```
 */
public fun <STATE : Any> SavedStateHandle.stateSaver(
	encode: (STATE) -> String,
	decode: (String) -> STATE,
	key: String = "pulse.state",
	onDecodeError: (Exception) -> Unit = {},
): StateSaver<STATE> = object : StateSaver<STATE> {

	override fun restore(): STATE? {
		val vSaved = get<SavedState>(key) ?: return null
		return try {
			vSaved.read { getStringOrNull(VALUE) }?.let(decode)
		} catch (vError: Exception) {
			onDecodeError(vError)
			null
		}
	}

	override fun bind(current: () -> STATE) {
		setSavedStateProvider(key) { savedState { putString(VALUE, encode(current())) } }
	}
}
