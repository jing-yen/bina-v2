package com.bina.ai.miniapp

import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.platform.Logger
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MiniAppRepository(
    private val loadYamlFiles: () -> List<Pair<String, String>>,
    private val persistYaml: ((String, String) -> Unit)? = null
) {
    private val yaml = Yaml(
        configuration = YamlConfiguration(strictMode = false)
    )

    private var cached: List<MiniApp>? = null
    private var rawYamls: Map<String, String> = emptyMap()
    private var cloudRecipes: List<MiniApp> = emptyList()
    private var cloudRawYamls: Map<String, String> = emptyMap()
    private val _cloudVersion = MutableStateFlow(0)
    val cloudVersion: StateFlow<Int> = _cloudVersion.asStateFlow()

    fun loadAll(): List<MiniApp> {
        cached?.let { return it }
        val byId = linkedMapOf<String, MiniApp>()
        val yamls = mutableMapOf<String, String>()
        for ((filename, text) in loadYamlFiles()) {
            try {
                val app = yaml.decodeFromString(MiniApp.serializer(), text)
                byId[app.id] = app
                yamls[app.id] = text
                Logger.d(TAG, "Loaded miniapp: ${app.id} (${app.name}) with ${app.screens.size} screens")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to parse $filename", e)
            }
        }
        val apps = byId.values.toList()
        cached = apps
        rawYamls = yamls
        return apps
    }

    fun registerCloudRecipes(recipes: List<MiniApp>) {
        cloudRecipes = recipes
        _cloudVersion.value++
    }

    fun registerCloudRecipesWithYaml(recipes: List<Pair<MiniApp, String>>) {
        cloudRecipes = recipes.map { it.first }
        cloudRawYamls = recipes.associate { it.first.id to it.second }
        _cloudVersion.value++
    }

    fun getById(id: String): MiniApp? =
        loadAll().find { it.id == id } ?: cloudRecipes.find { it.id == id }

    /** Returns the raw YAML text the recipe was loaded from, or null if id is unknown. */
    fun getYamlById(id: String): String? {
        loadAll()
        return rawYamls[id] ?: cloudRawYamls[id]
    }

    fun persistRecipeLocally(recipeId: String) {
        val yamlText = cloudRawYamls[recipeId] ?: return
        persistYaml?.invoke("${recipeId}.yaml", yamlText)
        invalidateCache()
    }

    fun invalidateCache() {
        cached = null
        rawYamls = emptyMap()
    }

    companion object {
        private const val TAG = "MiniAppRepo"
    }
}
