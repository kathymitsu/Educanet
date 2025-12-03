package com.example.educanet.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension de DataStore para el Context
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPrefs {

    private val KEY_NAME = stringPreferencesKey("name")
    private val KEY_ROLE = stringPreferencesKey("role")

    data class Profile(
        val name: String = "",
        val role: String = ""
    )

    /** Flow con el perfil actual guardado (nombre y rol). */
    fun profileFlow(ctx: Context): Flow<Profile> =
        ctx.dataStore.data.map { prefs ->
            Profile(
                name = prefs[KEY_NAME] ?: "",
                role = prefs[KEY_ROLE] ?: ""
            )
        }

    /** Guarda nombre y rol del usuario logueado. */
    suspend fun saveProfile(ctx: Context, name: String, role: String) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_NAME] = name
            prefs[KEY_ROLE] = role
        }
    }

    /** Limpia el perfil, usar al cerrar sesión. */
    suspend fun clearProfile(ctx: Context) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_NAME] = ""
            prefs[KEY_ROLE] = ""
        }
    }
}
