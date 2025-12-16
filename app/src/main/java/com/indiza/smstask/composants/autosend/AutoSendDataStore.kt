package com.indiza.smstask.composants.autosend

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings_datastore")

class AutoSendDataStore(private val context: Context) {

    companion object {
        private val AUTO_SEND_KEY = booleanPreferencesKey("auto_send_enabled")
    }

    val autoSendEnabled: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[AUTO_SEND_KEY] ?: false
        }

    suspend fun setAutoSend(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_SEND_KEY] = enabled
        }
    }
}
