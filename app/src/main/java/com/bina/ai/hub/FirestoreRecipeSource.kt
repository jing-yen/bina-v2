package com.bina.ai.hub

import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.platform.Logger
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

class FirestoreRecipeSource(private val cacheDir: File? = null) {

    private val db = FirebaseFirestore.getInstance()
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile = cacheDir?.let { File(it, "cloud_recipes_cache.json") }

    fun loadCached(): List<Pair<MiniApp, String>> {
        val file = cacheFile ?: return emptyList()
        if (!file.exists()) return emptyList()
        return try {
            val yamlStrings = json.decodeFromString(
                ListSerializer(String.serializer()), file.readText()
            )
            yamlStrings.mapNotNull { text ->
                try {
                    yaml.decodeFromString(MiniApp.serializer(), text) to text
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveCache(recipes: List<Pair<MiniApp, String>>) {
        val file = cacheFile ?: return
        try {
            file.writeText(json.encodeToString(
                ListSerializer(String.serializer()), recipes.map { it.second }
            ))
        } catch (_: Exception) {}
    }

    suspend fun fetchRecipes(): List<MiniApp> =
        fetchRecipesWithYaml().map { it.first }

    suspend fun fetchRecipesWithYaml(): List<Pair<MiniApp, String>> {
        return try {
            val snapshot = db.collection(COLLECTION)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val result = snapshot.documents.mapNotNull { doc ->
                val yamlText = doc.getString("generatedYaml")
                if (yamlText.isNullOrBlank()) {
                    Logger.d(TAG, "Skipping ${doc.id}: no generatedYaml field")
                    return@mapNotNull null
                }
                try {
                    yaml.decodeFromString(MiniApp.serializer(), yamlText) to yamlText
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to parse YAML for ${doc.id}", e)
                    null
                }
            }
            if (result.isNotEmpty()) saveCache(result)
            result
        } catch (e: Exception) {
            Logger.e(TAG, "Firestore fetch failed", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "FirestoreRecipeSource"
        private const val COLLECTION = "recipes"
    }
}
