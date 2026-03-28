package com.bitsycore.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
	Window(
		onCloseRequest = ::exitApplication,
		title = "Pulse Demo",
		state = rememberWindowState(width = 500.dp, height = 800.dp)
	) {
		MaterialTheme {
			AppNavHost()
		}
	}
}
