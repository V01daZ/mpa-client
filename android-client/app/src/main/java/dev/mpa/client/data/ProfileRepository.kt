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
        context.dataStore.edit { prefs ->
            val current = parseProfiles(prefs[Keys.PROFILES])
            val finalName = deduplicateName(profile.name, current.map { it.name })
            val toAdd = profile.copy(name = finalName)
            val updated = current + toAdd
            prefs[Keys.PROFILES] = gson.toJson(updated)
            if (current.isEmpty()) prefs[Keys.ACTIVE_ID] = toAdd.id
        }
        return profilesFlow.first().last()
    }

    suspend fun addProfiles(profiles: List<ServerProfile>) {
        context.dataStore.edit { prefs ->
            val current = parseProfiles(prefs[Keys.PROFILES])
            val updated = current.toMutableList()
            
            profiles.forEach { profile ->
                // Ищем существующий профиль по URL источника и либо по имени, либо по адресу/порту
                // (если провайдер сменил имя сервера, но адрес остался тот же, или наоборот)
                val existingIndex = updated.indexOfFirst { 
                    it.sourceUrl == profile.sourceUrl && (it.name == profile.name || (it.address == profile.address && it.port == profile.port))
                }
                
                if (existingIndex != -1) {
                    val existing = updated[existingIndex]
                    // Обновляем данные, сохраняя ID и пользовательское имя группы
                    updated[existingIndex] = profile.copy(
                        id = existing.id,
                        groupName = existing.groupName ?: profile.groupName
                    )
                } else {
                    // Это действительно новый сервер в подписке
                    val finalName = deduplicateName(profile.name, updated.map { it.name })
                    updated.add(profile.copy(name = finalName))
                }
            }
            
            prefs[Keys.PROFILES] = gson.toJson(updated)
            if (current.isEmpty() && updated.isNotEmpty()) {
                prefs[Keys.ACTIVE_ID] = updated.first().id
            }
        }
    }

    suspend fun updateGroupName(sourceUrl: String, newGroupName: String) {
        context.dataStore.edit { prefs ->
            val current = parseProfiles(prefs[Keys.PROFILES])
            val updated = current.map { 
                if (it.sourceUrl == sourceUrl) it.copy(groupName = newGroupName) else it 
            }
            prefs[Keys.PROFILES] = gson.toJson(updated)
        }
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
