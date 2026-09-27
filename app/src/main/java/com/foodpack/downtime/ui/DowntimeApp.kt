package com.foodpack.downtime.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.foodpack.downtime.data.DowntimeRepository
import com.foodpack.downtime.ui.screens.HistoryScreen
import com.foodpack.downtime.ui.screens.HomeScreen
import com.foodpack.downtime.ui.screens.LogEntryScreen
import com.foodpack.downtime.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val LOG = "log"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun DowntimeApp(repository: DowntimeRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = repository,
                onLogNew = { navController.navigate(Routes.LOG) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.LOG) {
            LogEntryScreen(
                repository = repository,
                onDone = { navController.popBackStack() }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
