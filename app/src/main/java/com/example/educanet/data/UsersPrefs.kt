package com.example.educanet.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val DS_NAME = "user_prefs"
val Context.dataStore by preferencesDataStore(name = DS_NAME)

object UserPrefs {

    private val KEY_NAME = stringPreferencesKey("user_name")
    private val KEY_ROLE = stringPreferencesKey("user_role")
    private val KEY_CLASSES_CACHE = stringPreferencesKey("classes_cache") // JSON liviano

    data class Profile(val name: String = "", val role: String = "")

    fun profileFlow(ctx: Context): Flow<Profile> =
        ctx.dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { p ->
                Profile(
                    name = p[KEY_NAME] ?: "",
                    role = p[KEY_ROLE] ?: ""
                )
            }

    suspend fun saveProfile(ctx: Context, name: String, role: String) {
        ctx.dataStore.edit { p ->
            p[KEY_NAME] = name
            p[KEY_ROLE] = role
        }
    }

    fun classesCacheFlow(ctx: Context): Flow<String> =
        ctx.dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { it[KEY_CLASSES_CACHE] ?: "" }

    suspend fun saveClassesCache(ctx: Context, json: String) {
        ctx.dataStore.edit { p -> p[KEY_CLASSES_CACHE] = json }
    }
}
