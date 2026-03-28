package com.bitsycore.demo.colorpicker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ColorPickerContent(
	state: ColorPickerComponent.State,
	dispatch: (ColorPickerComponent.Intent) -> Unit
) {
	Column(
		modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(state.label, style = MaterialTheme.typography.titleMedium)

		Spacer(Modifier.height(12.dp))

		Box(
			modifier = Modifier
				.size(80.dp)
				.clip(RoundedCornerShape(12.dp))
				.background(Color(state.red, state.green, state.blue))
				.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
		)

		AnimatedVisibility (state.showHex) {
			Spacer(Modifier.height(4.dp))
			Text(state.hex, style = MaterialTheme.typography.labelMedium)
		}

		Spacer(Modifier.height(12.dp))

		ColorSlider("R", state.red, Color.Red) { dispatch(ColorPickerComponent.Intent.SetRed(it)) }
		ColorSlider("G", state.green, Color.Green) { dispatch(ColorPickerComponent.Intent.SetGreen(it)) }
		ColorSlider("B", state.blue, Color.Blue) { dispatch(ColorPickerComponent.Intent.SetBlue(it)) }

		Spacer(Modifier.height(8.dp))

		Row(horizontalArrangement = Arrangement.Center) {
			OutlinedButton(onClick = { dispatch(ColorPickerComponent.Intent.Randomize) }) {
				Text("Random")
			}
			Spacer(Modifier.width(8.dp))
			TextButton(onClick = { dispatch(ColorPickerComponent.Intent.ToggleHex) }) {
				AnimatedContent(
					targetState = state.showHex,
					label = "textAnimation"
				) { showHex ->
					Text(if (showHex) "Hide hex" else "Show hex")
				}
			}
		}
	}
}

@Composable
private fun ColorSlider(
	label: String,
	value: Float,
	color: Color,
	onValueChange: (Float) -> Unit
) {
	// Animation state lives in UI only
	val animatable = remember { Animatable(value) }
	val interacting = remember { mutableStateOf(false) }

	// Animate toward the target value only when the user is NOT dragging
	LaunchedEffect(value) {
		if (interacting.value) animatable.snapTo(value)
		else animatable.animateTo(value, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 200f))
	}

	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
	) {
		Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(20.dp))
		Slider(
			value = animatable.value,
			onValueChange = {
				interacting.value = true
				onValueChange(it)
			},
			onValueChangeFinished = {
				interacting.value = false
			},
			modifier = Modifier.weight(1f),
			colors = SliderDefaults.colors(
				thumbColor = color,
				activeTrackColor = color
			)
		)
		Text(
			"${(animatable.value * 255).toInt()}",
			style = MaterialTheme.typography.labelSmall,
			modifier = Modifier.width(30.dp)
		)
	}
}

@Preview
@Composable
private fun ColorPickerContentPreview() {
	Surface(modifier = Modifier.fillMaxWidth()) {
		ColorPickerContent(
			state = ColorPickerComponent.State(
				red = 0.5f,
				green = 0.3f,
				blue = 0.8f,
				label = "Pick a color",
				showHex = true
			),
			dispatch = {}
		)
	}
}
