package com.foodpack.downtime.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * The two things this app needs configured once per phone:
 * the operator's name (so entries are attributed) and the
 * Google Apps Script Web App URL that rows get POSTed to.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val OPERATOR_NAME = stringPreferencesKey("operator_name")
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val REMINDERS_ON = booleanPreferencesKey("reminders_on")
        val HANDOVER_TIME = stringPreferencesKey("handover_reminder_time")
        val BREAKS_TIME = stringPreferencesKey("breaks_reminder_time")
    }

    val operatorName: Flow<String> = context.dataStore.data.map { it[Keys.OPERATOR_NAME] ?: "" }
    val webhookUrl: Flow<String> = context.dataStore.data.map { it[Keys.WEBHOOK_URL] ?: "" }

    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMINDERS_ON] ?: true }
    /** "HH:mm" — default a bit before the 7am handover deadline. */
    val handoverReminderTime: Flow<String> = context.dataStore.data.map { it[Keys.HANDOVER_TIME] ?: "06:45" }
    /** "HH:mm" — default a bit before the 6:30am break-times deadline. */
    val breaksReminderTime: Flow<String> = context.dataStore.data.map { it[Keys.BREAKS_TIME] ?: "06:15" }

    suspend fun setReminders(enabled: Boolean, handoverTime: String, breaksTime: String) {
        context.dataStore.edit {
            it[Keys.REMINDERS_ON] = enabled
            it[Keys.HANDOVER_TIME] = handoverTime
            it[Keys.BREAKS_TIME] = breaksTime
        }
    }

    suspend fun setOperatorName(name: String) {
        context.dataStore.edit { it[Keys.OPERATOR_NAME] = name }
    }

    suspend fun setWebhookUrl(url: String) {
        context.dataStore.edit { it[Keys.WEBHOOK_URL] = url }
    }
}
