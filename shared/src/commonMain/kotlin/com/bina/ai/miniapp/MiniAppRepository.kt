package com.bina.ai.miniapp

import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.platform.Logger
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

class MiniAppRepository(
    private val loadYamlFiles: () -> List<Pair<String, String>>
) {
    private val yaml = Yaml(
        configuration = YamlConfiguration(strictMode = false)
    )

    private var cached: List<MiniApp>? = null
    private var rawYamls: Map<String, String> = emptyMap()

    fun loadAll(): List<MiniApp> {
        cached?.let { return it }
        val apps = mutableListOf<MiniApp>()
        val yamls = mutableMapOf<String, String>()
        for ((filename, text) in loadYamlFiles()) {
            try {
                val app = yaml.decodeFromString(MiniApp.serializer(), text)
                apps.add(app)
                yamls[app.id] = text
                Logger.d(TAG, "Loaded miniapp: ${app.id} (${app.name}) with ${app.screens.size} screens")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to parse $filename", e)
            }
        }
        cached = apps
        rawYamls = yamls
        return apps
    }

    fun getById(id: String): MiniApp? = loadAll().find { it.id == id }

    /** Returns the raw YAML text the recipe was loaded from, or null if id is unknown. */
    fun getYamlById(id: String): String? {
        loadAll()
        return rawYamls[id]
    }

    fun invalidateCache() {
        cached = null
        rawYamls = emptyMap()
    }

    companion object {
        private const val TAG = "MiniAppRepo"
    }
}
