package com.foodpack.downtime.data

import android.content.Context
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
    }

    val operatorName: Flow<String> = context.dataStore.data.map { it[Keys.OPERATOR_NAME] ?: "" }
    val webhookUrl: Flow<String> = context.dataStore.data.map { it[Keys.WEBHOOK_URL] ?: "" }

    suspend fun setOperatorName(name: String) {
        context.dataStore.edit { it[Keys.OPERATOR_NAME] = name }
    }

    suspend fun setWebhookUrl(url: String) {
        context.dataStore.edit { it[Keys.WEBHOOK_URL] = url }
    }
}
