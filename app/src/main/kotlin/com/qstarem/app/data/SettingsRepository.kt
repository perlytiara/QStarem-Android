package com.qstarem.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qstarem_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            homeUrl = prefs[KEY_HOME_URL] ?: AppSettings.DEFAULT_HOME_URL,
            adBlocker = AdBlockerChoice.entries.find { it.name == prefs[KEY_AD_BLOCKER] }
                ?: AdBlockerChoice.UBLOCK,
            pStreamEnabled = prefs[KEY_PSTREAM_ENABLED] ?: true,
            appIconId = prefs[KEY_APP_ICON_ID] ?: AppSettings.DEFAULT_APP_ICON_ID,
        )
    }

    suspend fun updateHomeUrl(url: String) {
        context.dataStore.edit { it[KEY_HOME_URL] = url.trim() }
    }

    suspend fun updateAdBlocker(choice: AdBlockerChoice) {
        context.dataStore.edit { it[KEY_AD_BLOCKER] = choice.name }
    }

    suspend fun updatePStreamEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PSTREAM_ENABLED] = enabled }
    }

    suspend fun updateAppIconId(iconId: Int) {
        context.dataStore.edit {
            it[KEY_APP_ICON_ID] = iconId.coerceIn(
                AppSettings.MIN_APP_ICON_ID,
                AppSettings.MAX_APP_ICON_ID,
            )
        }
    }

    companion object {
        private val KEY_HOME_URL = stringPreferencesKey("home_url")
        private val KEY_AD_BLOCKER = stringPreferencesKey("ad_blocker")
        private val KEY_PSTREAM_ENABLED = booleanPreferencesKey("pstream_enabled")
        private val KEY_APP_ICON_ID = intPreferencesKey("app_icon_id")
    }
}
