package com.ongarand.gymtracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ongarand.gymtracker.AppViewModel

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabItem("home", "Beranda", Icons.Default.Home),
    TabItem("workout", "Latihan", Icons.Default.FitnessCenter),
    TabItem("library", "Pustaka", Icons.Default.VideoLibrary),
    TabItem("progress", "Progres", Icons.Default.BarChart),
    TabItem("body", "Tubuh", Icons.Default.MonitorWeight)
)

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snack = remember { SnackbarHostState() }
    var videoUri by remember { mutableStateOf<String?>(null) }
    val summary by vm.summary.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.events.collect { snack.showSnackbar(it) }
    }

    fun goTab(r: String) {
        nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (TABS.any { it.route == route }) {
                NavigationBar {
                    TABS.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { goTab(t.route) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(vm, onStart = { goTab("workout") }, onSettings = { nav.navigate("settings") })
            }
            composable("workout") { WorkoutScreen(vm, onPlay = { videoUri = it }) }
            composable("library") {
                LibraryScreen(vm, onPlay = { videoUri = it }, onStarted = { goTab("workout") })
            }
            composable("progress") { ProgressScreen(vm) }
            composable("body") { BodyScreen(vm) }
            composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }

    videoUri?.let { VideoDialog(it) { videoUri = null } }

    summary?.let { text ->
        AlertDialog(
            onDismissRequest = { vm.dismissSummary() },
            title = { Text("Latihan selesai 🎉") },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { vm.dismissSummary() }) { Text("Mantap!") } }
        )
    }
}
