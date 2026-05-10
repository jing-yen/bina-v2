package com.bina.ai.install

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bina.ai.install.model.InstallRecord
import com.bina.ai.platform.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * DataStore-backed persistence of install records. Storage = single Preferences key holding
 * a JSON-encoded Map<recipeId, InstallRecord>. Observing returns a Flow that emits the full
 * map whenever any install changes.
 *
 * Construct via [InstallStore.create] in production code (uses a Context-backed DataStore).
 * Tests inject their own DataStore<Preferences> directly via the primary constructor.
 */
class InstallStore(private val dataStore: DataStore<Preferences>) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapSerializer = MapSerializer(String.serializer(), InstallRecord.serializer())

    val installs: Flow<Map<String, InstallRecord>> = dataStore.data.map { prefs ->
        decode(prefs[INSTALLS_KEY])
    }

    fun isInstalled(recipeId: String): Flow<Boolean> =
        installs.map { recipeId in it }

    suspend fun install(record: InstallRecord) {
        dataStore.edit { prefs ->
            val current = decode(prefs[INSTALLS_KEY])
            val updated = current + (record.recipeId to record)
            prefs[INSTALLS_KEY] = json.encodeToString(mapSerializer, updated)
        }
    }

    suspend fun uninstall(recipeId: String) {
        dataStore.edit { prefs ->
            val current = decode(prefs[INSTALLS_KEY])
            if (recipeId !in current) return@edit
            val updated = current - recipeId
            prefs[INSTALLS_KEY] = json.encodeToString(mapSerializer, updated)
        }
    }

    private fun decode(raw: String?): Map<String, InstallRecord> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString(mapSerializer, raw) }.getOrElse { e ->
            // Corrupt or schema-incompatible blob — start over rather than crash.
            // Surface it in logcat so we don't lose installs silently in the wild.
            Logger.w(TAG, "Failed to decode installs JSON; treating as empty. Cause: ${e.message}")
            emptyMap()
        }
    }

    companion object {
        private const val TAG = "InstallStore"
        private val INSTALLS_KEY = stringPreferencesKey("installs_json")

        private val Context.binaInstallsDataStore by preferencesDataStore("bina_installs")

        fun create(context: Context): InstallStore =
            InstallStore(context.binaInstallsDataStore)
    }
}
