package com.bitsycore.demo.pulse

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bitsycore.demo.pulse.page1.Page1Screen
import com.bitsycore.demo.pulse.page2.Page2Screen
import org.jetbrains.compose.resources.vectorResource
import pulsemvi.demo.generated.resources.Res
import pulsemvi.demo.generated.resources.ic_pulse

private const val DURATION = 500

private val forwardTransition: ContentTransform = ContentTransform(
	slideInHorizontally(tween(DURATION)) { it }
			+ scaleIn(tween(DURATION), initialScale = 0.85f),
	scaleOut(tween(DURATION), targetScale = 0.5f)
			+ fadeOut(tween(DURATION)))

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

	val backStack: SnapshotStateList<Route> = retain { listOf<Route>(Route.Page1).toMutableStateList() }
	val snackbarHostState = remember { SnackbarHostState() }

	Column(
        Modifier.fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
	) {


		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
		) {
			var textHeight by remember { mutableIntStateOf(0) }
			Image(
				imageVector = vectorResource(Res.drawable.ic_pulse),
				contentDescription = null,
				contentScale = ContentScale.FillHeight,
				modifier = Modifier
					.heightIn(64.dp)
					.height(with(LocalDensity.current) { textHeight.toDp() })
			)
			Spacer(Modifier.width(16.dp))
			Text(
				"Pulse Demo",
				style = MaterialTheme.typography.headlineMedium,
				color = Color.White,
				modifier = Modifier
					.fillMaxWidth()
					.onGloballyPositioned { textHeight = it.size.height }
			)
		}

		Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
			NavDisplay(
				backStack = backStack,
				onBack = { backStack.removeLastOrNull() },
				modifier = Modifier.fillMaxSize(),
				transitionSpec = { forwardTransition },
				popTransitionSpec = { popTransition },
				predictivePopTransitionSpec = { popTransition },
				entryDecorators = listOf(
					rememberSaveableStateHolderNavEntryDecorator(),
					rememberViewModelStoreNavEntryDecorator()
				),
				entryProvider = { key ->
					when (key) {
						is Route.Page1 -> NavEntry(key) { Page1Screen(snackbarHostState = snackbarHostState) }
						is Route.Page2 -> NavEntry(key) { Page2Screen(snackbarHostState = snackbarHostState) }
					}
				}
			)
			SnackbarHost(
				hostState = snackbarHostState,
				modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
			)
		}

		NavigationBar(
			modifier = Modifier.fillMaxWidth(),
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
