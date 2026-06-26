package dev.mpa.client.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("mpa_profiles")

class ProfileRepository(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val PROFILES  = stringPreferencesKey("profiles")
        val ACTIVE_ID = stringPreferencesKey("active_profile_id")
    }

    val profilesFlow: Flow<List<ServerProfile>> = context.dataStore.data.map { prefs ->
        parseProfiles(prefs[Keys.PROFILES])
    }

    val activeProfileIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_ID]?.takeIf { it.isNotEmpty() }
    }

    suspend fun addProfile(profile: ServerProfile): ServerProfile {
        val uniqueProfile = context.dataStore.edit { prefs ->
            val current = parseProfiles(prefs[Keys.PROFILES])
            // Дедупликация имён: если имя уже занято - добавляем (1), (2) и т.д.
            val finalName = deduplicateName(profile.name, current.map { it.name })
            val toAdd = profile.copy(name = finalName)
            val updated = current + toAdd
            prefs[Keys.PROFILES] = gson.toJson(updated)
            if (current.isEmpty()) prefs[Keys.ACTIVE_ID] = toAdd.id
        }
        // Возвращаем добавленный профиль с финальным именем
        return profilesFlow.first().last()
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
            prefs[Keys.PROFILES] = gson.toJson(current.map { if (it.id == id) update else it })
        }
    }

    suspend fun setActiveProfileId(id: String) {
        context.dataStore.edit { prefs -> prefs[Keys.ACTIVE_ID] = id }
    }

    suspend fun getProfiles(): List<ServerProfile> =
        profilesFlow.first()

    suspend fun getActiveProfileId(): String? =
        activeProfileIdFlow.first()

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Если имя уже существует в списке — добавляет (1), (2) и т.д.
     * "Finland MPA" → "Finland MPA (1)" → "Finland MPA (2)"
     */
    private fun deduplicateName(name: String, existingNames: List<String>): String {
        if (name !in existingNames) return name
        var counter = 1
        while ("$name ($counter)" in existingNames) counter++
        return "$name ($counter)"
    }

    private fun parseProfiles(json: String?): List<ServerProfile> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<ServerProfile>>() {}.type
        return try { gson.fromJson(json, type) ?: emptyList() } catch (_: Exception) { emptyList() }
    }
}
