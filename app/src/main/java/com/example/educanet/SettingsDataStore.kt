package com.example.educanet

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("educanet_settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val REMEMBER = booleanPreferencesKey("remember_session")
        val DARKMODE = booleanPreferencesKey("dark_mode")
        val AVATAR = stringPreferencesKey("avatar_uri")
    }

    val rememberSession: Flow<Boolean> = context.ds.data.map { it[REMEMBER] ?: true }
    val darkMode: Flow<Boolean> = context.ds.data.map { it[DARKMODE] ?: false }
    val avatarUri: Flow<String?> = context.ds.data.map { it[AVATAR] }

    suspend fun setRememberSession(v: Boolean) = context.ds.edit { it[REMEMBER] = v }
    suspend fun setDarkMode(v: Boolean) = context.ds.edit { it[DARKMODE] = v }
    suspend fun setAvatar(uri: String?) = context.ds.edit { prefs ->
        if (uri == null) prefs.remove(AVATAR) else prefs[AVATAR] = uri
    }
}
