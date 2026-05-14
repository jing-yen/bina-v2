package com.bina.ai.hub

import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.platform.Logger
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreRecipeSource {

    private val db = FirebaseFirestore.getInstance()
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    suspend fun fetchRecipes(): List<MiniApp> {
        return try {
            val snapshot = db.collection(COLLECTION)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val yamlText = doc.getString("generatedYaml")
                if (yamlText.isNullOrBlank()) {
                    Logger.d(TAG, "Skipping ${doc.id}: no generatedYaml field")
                    return@mapNotNull null
                }
                try {
                    yaml.decodeFromString(MiniApp.serializer(), yamlText)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to parse YAML for ${doc.id}", e)
                    null
                }
            }
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
