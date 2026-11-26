package com.bitsycore.demo.pulse.palette

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bitsycore.demo.pulse.colorpicker.ColorPickerContent
import com.bitsycore.lib.pulse.compose.rememberReducer

@Composable
fun PaletteContent(
	state: PaletteComponent.State,
	dispatch: (PaletteComponent.Intent) -> Unit,
) {
	Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
		state.swatches.forEach { vSwatch ->
			key(vSwatch.id) {
				ColorPickerContent(vSwatch.picker) { dispatch(PaletteComponent.Intent.Picker(vSwatch.id, it)) }
				TextButton(onClick = { dispatch(PaletteComponent.Intent.Remove(vSwatch.id)) }) { Text("Remove") }
			}
		}
		Button(onClick = { dispatch(PaletteComponent.Intent.Add) }) { Text("Add a colour") }
	}
}

/** The component alone, held by the composition: no screen, no ViewModel. */
@Preview
@Composable
private fun PalettePreview() {
	Surface(Modifier.fillMaxWidth()) {
		val (vState, vDispatch) = rememberReducer(PaletteComponent)
		PaletteContent(vState, vDispatch)
	}
}
