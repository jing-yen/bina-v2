package com.bina.ai.sync

import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import java.io.File

/**
 * Parses an incoming recipe YAML, classifies it (Ok / BundledConflict / UpdateExisting),
 * and on commit writes it into `filesDir/miniapps/<id>.yaml` so the Hub picks it up
 * on the next `MiniAppRepository.loadAll()`.
 */
class RecipeImporter(
    private val filesDir: File,
    private val miniAppRepository: MiniAppRepository
) {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
    private val miniappsDir = File(filesDir, "miniapps")

    fun parse(yamlText: String): Result<MiniApp> = runCatching {
        yaml.decodeFromString(MiniApp.serializer(), yamlText)
    }

    sealed interface Precheck {
        data object Ok : Precheck
        data class BundledConflict(val id: String, val name: String) : Precheck
        data class UpdateExisting(val id: String, val name: String) : Precheck
    }

    /**
     * Bundled vs imported is determined by whether a YAML for `id` exists in
     * `filesDir/miniapps/`. If it doesn't, but the id resolves in the repo, it must
     * have come from assets — that's a bundled conflict we refuse to overwrite.
     */
    fun precheck(miniApp: MiniApp): Precheck {
        val importedFile = File(miniappsDir, "${miniApp.id}.yaml")
        if (importedFile.exists()) {
            return Precheck.UpdateExisting(miniApp.id, miniApp.name)
        }
        if (miniAppRepository.getById(miniApp.id) != null) {
            return Precheck.BundledConflict(miniApp.id, miniApp.name)
        }
        return Precheck.Ok
    }

    fun commit(miniApp: MiniApp, yamlText: String) {
        miniappsDir.mkdirs()
        File(miniappsDir, "${miniApp.id}.yaml").writeText(yamlText)
        miniAppRepository.invalidateCache()
    }
}
