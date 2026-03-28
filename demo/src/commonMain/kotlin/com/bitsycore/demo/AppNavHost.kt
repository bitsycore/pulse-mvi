package com.bitsycore.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bitsycore.demo.page1.Page1Screen
import com.bitsycore.demo.page2.Page2Screen

enum class Route(val label: String) {
	Page1("Page 1"),
	Page2("Page 2"),
}

@Composable
fun AppNavHost() {
	var currentRoute by remember { mutableStateOf(Route.Page1) }

	Column(Modifier.fillMaxSize()) {
		// Content area — switches page, triggering composition enter/exit
		when (currentRoute) {
			Route.Page1 -> Page1Screen(modifier = Modifier.weight(1f).fillMaxWidth())
			Route.Page2 -> Page2Screen(modifier = Modifier.weight(1f).fillMaxWidth())
		}

		// Bottom navigation bar
		NavigationBar {
			Route.entries.forEach { route ->
				NavigationBarItem(
					selected = currentRoute == route,
					onClick = { currentRoute = route },
					icon = {},
					label = { Text(route.label) }
				)
			}
		}
	}
}
