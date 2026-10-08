package com.foodpack.downtime.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.foodpack.downtime.data.DowntimeRepository
import com.foodpack.downtime.ui.screens.HistoryScreen
import com.foodpack.downtime.ui.screens.HomeScreen
import com.foodpack.downtime.ui.screens.LineSpeedScreen
import com.foodpack.downtime.ui.screens.LogEntryScreen
import com.foodpack.downtime.ui.screens.NoteFolderScreen
import com.foodpack.downtime.ui.screens.NotesScreen
import com.foodpack.downtime.ui.screens.SettingsScreen
import com.foodpack.downtime.ui.screens.SleeveCalculatorScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val LOG = "log"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val LINE_SPEED = "line_speed"
    const val SLEEVE_CALC = "sleeve_calc"
    const val NOTES = "notes"
    const val NOTE_FOLDER = "note_folder/{folder}"

    fun noteFolder(folder: String) = "note_folder/" + URLEncoder.encode(folder, "UTF-8")
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
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenLineSpeed = { navController.navigate(Routes.LINE_SPEED) },
                onOpenSleeveCalc = { navController.navigate(Routes.SLEEVE_CALC) },
                onOpenNotes = { navController.navigate(Routes.NOTES) }
            )
        }
        composable(Routes.LINE_SPEED) {
            LineSpeedScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SLEEVE_CALC) {
            SleeveCalculatorScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTES) {
            NotesScreen(
                onBack = { navController.popBackStack() },
                onOpenFolder = { folder -> navController.navigate(Routes.noteFolder(folder)) }
            )
        }
        composable(
            route = Routes.NOTE_FOLDER,
            arguments = listOf(navArgument("folder") { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("folder") ?: ""
            val folder = URLDecoder.decode(encoded, "UTF-8")
            NoteFolderScreen(
                folder = folder,
                repository = repository,
                onBack = { navController.popBackStack() }
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
