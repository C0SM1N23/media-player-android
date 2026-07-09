package com.cosmin23.mediaplayer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How the app decides between the light and dark colour schemes. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** Immutable snapshot of the user's app preferences. */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val sortOrder: SortOrder = SortOrder.TITLE
)

/** Single DataStore instance shared by all preference repositories. */
internal val Context.userPreferencesDataStore by preferencesDataStore(name = "user_prefs")

/**
 * Persists app-level preferences (theme, dynamic colour, sort order) with Jetpack DataStore.
 * Replaces the previous in-memory-only dark-mode flag which was lost on every restart.
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.userPreferencesDataStore

    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            themeMode = ThemeMode.fromName(prefs[KEY_THEME_MODE]),
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
            sortOrder = SortOrder.fromName(prefs[KEY_SORT_ORDER])
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setSortOrder(order: SortOrder) {
        dataStore.edit { it[KEY_SORT_ORDER] = order.name }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
    }
}
