package com.bitsycore.demo

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bitsycore.demo.page1.Page1Screen
import com.bitsycore.demo.page2.Page2Screen

private const val DURATION = 500

private val forwardTransition: ContentTransform = ContentTransform(
	slideInHorizontally(tween(DURATION)) { it }
			+ scaleIn(tween(DURATION), initialScale = 0.85f),
	scaleOut(tween(DURATION), targetScale = 0.5f)
)

private val popTransition: ContentTransform = ContentTransform(
	scaleIn(tween(DURATION), initialScale = 0.85f)
			+ fadeIn(tween(DURATION / 2)),
	slideOutHorizontally(tween(DURATION)) { it }
			+ scaleOut(tween(DURATION), targetScale = 0.5f)
			+ fadeOut(tween(DURATION))
)

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

	Column(Modifier.fillMaxSize().background(Color.Black)) {
		NavDisplay(
			backStack = backStack,
			onBack = { backStack.removeLastOrNull() },
			modifier = Modifier.weight(1f).fillMaxWidth(),
			transitionSpec = { forwardTransition },
			popTransitionSpec = { popTransition },
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

		NavigationBar(
			containerColor = MaterialTheme.colorScheme.primaryContainer,
			contentColor = MaterialTheme.colorScheme.onPrimaryContainer
		) {
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
					label = { Text(label) },
					colors = NavigationBarItemDefaults.colors(
						selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
						unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
						selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
						unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
						indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
					)
				)
			}
		}
	}
}
