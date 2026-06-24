package dev.mpa.client.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("mpa_profiles")

/**
 * Хранилище профилей на базе DataStore.
 * Зеркало electron/main/store.ts — те же операции, те же концепции.
 */
class ProfileRepository(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val PROFILES = stringPreferencesKey("profiles")
        val ACTIVE_ID = stringPreferencesKey("active_profile_id")
    }

    val profilesFlow: Flow<List<ServerProfile>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.PROFILES] ?: return@map emptyList()
        val type = object : TypeToken<List<ServerProfile>>() {}.type
        try {
            gson.fromJson<List<ServerProfile>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val activeProfileIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_ID]
    }

    suspend fun getProfiles(): List<ServerProfile> {
        var result: List<ServerProfile> = emptyList()
        context.dataStore.data.collect { prefs ->
            val json = prefs[Keys.PROFILES] ?: return@collect
            val type = object : TypeToken<List<ServerProfile>>() {}.type
            result = try {
                gson.fromJson<List<ServerProfile>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            return@collect
        }
        return result
    }

    suspend fun addProfile(profile: ServerProfile): ServerProfile {
        context.dataStore.edit { prefs ->
            val current = parseProfiles(prefs[Keys.PROFILES])
            val updated = current + profile
            prefs[Keys.PROFILES] = gson.toJson(updated)
            // Если профилей не было - ставим первый активным
            if (current.isEmpty()) {
                prefs[Keys.ACTIVE_ID] = profile.id
            }
        }
        return profile
    }

    suspend fun removeProfile(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseProfiles(prefs[Keys.PROFILES])
            val updated = current.filter { it.id != id }
            prefs[Keys.PROFILES] = gson.toJson(updated)
            if (prefs[Keys.ACTIVE_ID] == id) {
                prefs[Keys.ACTIVE_ID] = updated.firstOrNull()?.id ?: ""
            }
        }
    }

    suspend fun updateProfile(id: String, update: ServerProfile) {
        context.dataStore.edit { prefs ->
            val current = parseProfiles(prefs[Keys.PROFILES])
            val updated = current.map { if (it.id == id) update else it }
            prefs[Keys.PROFILES] = gson.toJson(updated)
        }
    }

    suspend fun setActiveProfileId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_ID] = id
        }
    }

    suspend fun getActiveProfileId(): String? {
        var result: String? = null
        context.dataStore.data.collect { prefs ->
            result = prefs[Keys.ACTIVE_ID]?.takeIf { it.isNotEmpty() }
            return@collect
        }
        return result
    }

    private fun parseProfiles(json: String?): List<ServerProfile> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<ServerProfile>>() {}.type
        return try {
            gson.fromJson<List<ServerProfile>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
