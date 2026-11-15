package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vibeDataStore by preferencesDataStore(name = "vibe_prefs")

class ActiveProfileStore(private val context: Context) {
    private val keyDarkTheme = booleanPreferencesKey("dark_theme")

    private fun activeProfileKey(uid: String) = stringPreferencesKey("active_profile_$uid")
    private fun loggedOutProfilesKey(uid: String) = stringSetPreferencesKey("logged_out_profiles_$uid")

    fun observeActiveProfileId(uid: String): Flow<String?> =
        context.vibeDataStore.data.map { prefs -> prefs[activeProfileKey(uid)] }

    suspend fun saveActiveProfileId(uid: String, profileId: String) {
        context.vibeDataStore.edit { prefs ->
            prefs[activeProfileKey(uid)] = profileId
        }
    }

    fun observeLocallyLoggedOutProfileIds(uid: String): Flow<Set<String>> =
        context.vibeDataStore.data.map { prefs -> prefs[loggedOutProfilesKey(uid)] ?: emptySet() }

    suspend fun markProfileLoggedOut(uid: String, profileId: String, loggedOut: Boolean) {
        context.vibeDataStore.edit { prefs ->
            val current = prefs[loggedOutProfilesKey(uid)] ?: emptySet()
            prefs[loggedOutProfilesKey(uid)] = if (loggedOut) current + profileId else current - profileId
        }
    }

    fun observeDarkTheme(): Flow<Boolean> =
        context.vibeDataStore.data.map { prefs -> prefs[keyDarkTheme] ?: true }

    suspend fun saveDarkTheme(isDark: Boolean) {
        context.vibeDataStore.edit { prefs ->
            prefs[keyDarkTheme] = isDark
        }
    }
}
