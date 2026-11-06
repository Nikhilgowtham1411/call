package com.example.call.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.call.data.model.AppSettings
import com.example.call.data.model.TextScale
import com.example.call.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "call_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val textScale = stringPreferencesKey("text_scale")
        val showPhotos = booleanPreferencesKey("show_photos")
        val pinned = stringSetPreferencesKey("pinned_contacts")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.LIGHT,
                textScale = prefs[Keys.textScale]?.let { runCatching { TextScale.valueOf(it) }.getOrNull() }
                    ?: TextScale.LARGE,
                showPhotos = prefs[Keys.showPhotos] ?: true
            )
        }

    /** Lookup keys of contacts the user pinned to the top. */
    val pinned: Flow<Set<String>> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { it[Keys.pinned] ?: emptySet() }

    suspend fun togglePin(lookupKey: String) {
        if (lookupKey.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.pinned] ?: emptySet()
            prefs[Keys.pinned] = if (lookupKey in current) current - lookupKey else current + lookupKey
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setTextScale(scale: TextScale) {
        context.dataStore.edit { it[Keys.textScale] = scale.name }
    }

    suspend fun setShowPhotos(show: Boolean) {
        context.dataStore.edit { it[Keys.showPhotos] = show }
    }
}
