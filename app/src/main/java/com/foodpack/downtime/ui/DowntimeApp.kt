package com.foodpack.downtime.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.foodpack.downtime.data.DowntimeRepository
import com.foodpack.downtime.ui.screens.BreaksScreen
import com.foodpack.downtime.ui.screens.EditEntryScreen
import com.foodpack.downtime.ui.screens.EveningHandoverScreen
import com.foodpack.downtime.ui.screens.HandoverHistoryScreen
import com.foodpack.downtime.ui.screens.HandoverScreen
import com.foodpack.downtime.ui.screens.HistoryScreen
import com.foodpack.downtime.ui.screens.HomeScreen
import com.foodpack.downtime.ui.screens.LineSpeedScreen
import com.foodpack.downtime.ui.screens.LogEntryScreen
import com.foodpack.downtime.ui.screens.MorningHandoverScreen
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
    const val HANDOVER = "handover"
    const val BREAKS = "breaks"
    const val HANDOVER_MORNING = "handover_morning"
    const val HANDOVER_EVENING = "handover_evening"
    const val HANDOVER_HISTORY = "handover_history"
    const val HANDOVER_MORNING_EDIT = "handover_morning_edit/{entryId}"
    const val HANDOVER_EVENING_EDIT = "handover_evening_edit/{entryId}"
    const val EDIT_ENTRY = "edit_entry/{entryId}"

    fun noteFolder(folder: String) = "note_folder/" + URLEncoder.encode(folder, "UTF-8")
    fun editEntry(entryId: Long) = "edit_entry/$entryId"
    fun editMorningHandover(entryId: Long) = "handover_morning_edit/$entryId"
    fun editEveningHandover(entryId: Long) = "handover_evening_edit/$entryId"
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
                onOpenNotes = { navController.navigate(Routes.NOTES) },
                onOpenHandover = { navController.navigate(Routes.HANDOVER) },
                onOpenBreaks = { navController.navigate(Routes.BREAKS) }
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
        composable(Routes.BREAKS) {
            BreaksScreen(repository = repository, onBack = { navController.popBackStack() })
        }
        composable(Routes.HANDOVER) {
            HandoverScreen(
                onBack = { navController.popBackStack() },
                onOpenMorning = { navController.navigate(Routes.HANDOVER_MORNING) },
                onOpenEvening = { navController.navigate(Routes.HANDOVER_EVENING) },
                onOpenHistory = { navController.navigate(Routes.HANDOVER_HISTORY) }
            )
        }
        composable(Routes.HANDOVER_MORNING) {
            MorningHandoverScreen(
                repository = repository,
                onDone = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
        composable(Routes.HANDOVER_EVENING) {
            EveningHandoverScreen(
                repository = repository,
                onDone = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
        composable(Routes.HANDOVER_HISTORY) {
            HandoverHistoryScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onEditMorning = { id -> navController.navigate(Routes.editMorningHandover(id)) },
                onEditEvening = { id -> navController.navigate(Routes.editEveningHandover(id)) }
            )
        }
        composable(
            route = Routes.HANDOVER_MORNING_EDIT,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: 0L
            MorningHandoverScreen(
                repository = repository,
                entryId = entryId,
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.HANDOVER_EVENING_EDIT,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: 0L
            EveningHandoverScreen(
                repository = repository,
                entryId = entryId,
                onDone = { navController.popBackStack() }
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
                onBack = { navController.popBackStack() },
                onEditEntry = { id -> navController.navigate(Routes.editEntry(id)) }
            )
        }
        composable(
            route = Routes.EDIT_ENTRY,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: 0L
            EditEntryScreen(
                repository = repository,
                entryId = entryId,
                onDone = { navController.popBackStack() }
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
