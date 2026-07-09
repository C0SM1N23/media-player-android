package com.cosmin23.mediaplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists favourite track ids with DataStore so they survive process death and restarts.
 * Replaces the previous in-memory `Set<String>` in the ViewModel and the empty Room stubs.
 *
 * Ids are stored as strings (DataStore only supports `Set<String>`) and mapped back to `Long`.
 */
class FavoritesRepository(context: Context) {

    private val dataStore = context.applicationContext.userPreferencesDataStore

    val favoriteIds: Flow<Set<Long>> = dataStore.data.map { prefs ->
        prefs[KEY_FAVORITES]?.mapNotNullTo(HashSet()) { it.toLongOrNull() } ?: emptySet()
    }

    suspend fun toggle(id: Long) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            val key = id.toString()
            if (!current.add(key)) current.remove(key)
            prefs[KEY_FAVORITES] = current
        }
    }

    private companion object {
        val KEY_FAVORITES = stringSetPreferencesKey("favorite_ids")
    }
}
