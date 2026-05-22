package com.callguardian.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.callguardian.app.ui.screens.LogScreen
import com.callguardian.app.ui.screens.ProtectionScreen
import com.callguardian.app.ui.screens.RulesScreen
import com.callguardian.app.ui.screens.SettingsScreen
import com.callguardian.app.ui.screens.StatsScreen

private data class Destination(
    val route: String,
    val label: String,
    val icon: @Composable (Boolean) -> Unit,
)

@Composable
fun CallGuardianNavHost() {
    val navController = rememberNavController()
    val useRail = LocalConfiguration.current.screenWidthDp >= 700
    val destinations = listOf(
        Destination("protection", "Protezione") { Icon(Icons.Default.Home, null) },
        Destination("rules", "Regole") { Icon(Icons.AutoMirrored.Filled.List, null) },
        Destination("logs", "Registro") { Icon(Icons.Default.History, null) },
        Destination("stats", "Statistiche") { Icon(Icons.Default.BarChart, null) },
        Destination("settings", "Opzioni") { Icon(Icons.Default.Settings, null) },
    )
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val navigateToDestination: (Destination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
        }
    }

    if (useRail) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.40f)),
            ) {
                destinations.forEach { destination ->
                    val selected = current?.hierarchy?.any { it.route == destination.route } == true
                    NavigationRailItem(
                        selected = selected,
                        onClick = { navigateToDestination(destination) },
                        icon = { destination.icon(selected) },
                        label = { Text(destination.label) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        ),
                    )
                }
            }
            AppNavHostContent(
                modifier = Modifier.weight(1f),
                navController = navController,
                destinations = destinations,
                currentRoute = current?.route,
                onNavigate = navigateToDestination,
            )
        }
    } else {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                tonalElevation = 0.dp,
            ) {
                destinations.forEach { destination ->
                    val selected = current?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToDestination(destination) },
                        icon = { destination.icon(selected) },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        ),
                    )
                }
            }
            }
        ) { padding ->
            AppNavHostContent(
                modifier = Modifier.padding(padding),
                navController = navController,
                destinations = destinations,
                currentRoute = current?.route,
                onNavigate = navigateToDestination,
            )
        }
    }
}

@Composable
private fun AppNavHostContent(
    navController: androidx.navigation.NavHostController,
    destinations: List<Destination>,
    currentRoute: String?,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 120.dp.toPx() }

    NavHost(
        navController = navController,
        startDestination = "protection",
        modifier = modifier
            .fillMaxSize()
            .gestureNavigation(
                currentRoute = currentRoute,
                destinations = destinations,
                swipeThresholdPx = swipeThresholdPx,
                onNavigate = onNavigate,
            )
            .padding(horizontal = 0.dp),
    ) {
        composable("protection") { ProtectionScreen() }
        composable("rules") { RulesScreen() }
        composable("logs") { LogScreen() }
        composable("stats") { StatsScreen() }
        composable("settings") { SettingsScreen() }
    }
}

private fun Modifier.gestureNavigation(
    currentRoute: String?,
    destinations: List<Destination>,
    swipeThresholdPx: Float,
    onNavigate: (Destination) -> Unit,
): Modifier = pointerInput(currentRoute, destinations, swipeThresholdPx) {
    detectPageSwipe(
        currentRoute = currentRoute,
        destinations = destinations,
        swipeThresholdPx = swipeThresholdPx,
        onNavigate = onNavigate,
    )
}

private suspend fun PointerInputScope.detectPageSwipe(
    currentRoute: String?,
    destinations: List<Destination>,
    swipeThresholdPx: Float,
    onNavigate: (Destination) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalX = 0f
        var totalY = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break

            val delta = change.positionChange()
            totalX += delta.x
            totalY += delta.y

            val isHorizontalSwipe = kotlin.math.abs(totalX) > swipeThresholdPx &&
                kotlin.math.abs(totalX) > kotlin.math.abs(totalY) * 1.35f
            if (isHorizontalSwipe) {
                val currentIndex = destinations.indexOfFirst { it.route == currentRoute }
                val targetIndex = when {
                    totalX < 0f -> currentIndex + 1
                    totalX > 0f -> currentIndex - 1
                    else -> currentIndex
                }
                destinations.getOrNull(targetIndex)?.let(onNavigate)
                change.consume()
                break
            }
        }
    }
}
