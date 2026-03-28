package com.bitsycore.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bitsycore.demo.page1.Page1Screen
import com.bitsycore.demo.page2.Page2Screen

sealed interface Route {
	data object Page1 : Route
	data object Page2 : Route
}

private val tabs = listOf(
	Route.Page1 to "Page 1",
	Route.Page2 to "Page 2",
)

@Composable
fun AppNavHost() {
	val backStack: SnapshotStateList<Route> = listOf<Route>(Route.Page1).toMutableStateList()

	Column(Modifier.fillMaxSize()) {
		NavDisplay(
			backStack = backStack,
			onBack = { backStack.removeLastOrNull() },
			modifier = Modifier.weight(1f).fillMaxWidth(),
			entryDecorators = listOf(
				rememberSaveableStateHolderNavEntryDecorator(),
				rememberViewModelStoreNavEntryDecorator()
			),
			entryProvider = { key ->
				when (key) {
					is Route.Page1 -> NavEntry(key) { Page1Screen() }
					is Route.Page2 -> NavEntry(key) { Page2Screen() }
				}
			}
		)

		NavigationBar {
			tabs.forEach { (route, label) ->
				NavigationBarItem(
					selected = backStack.lastOrNull() == route,
					onClick = {
						// If already on this tab, do nothing
						// Otherwise remove any existing instance and push on top
						if (backStack.lastOrNull() != route) {
							backStack.remove(route)
							backStack.add(route)
						}
					},
					icon = {},
					label = { Text(label) }
				)
			}
		}
	}
}
