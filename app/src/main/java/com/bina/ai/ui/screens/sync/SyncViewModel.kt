package com.bina.ai.ui.screens.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.sync.RecipeImporter
import com.bina.ai.sync.RecipePayload
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Cap on encoded QR payload size — single QR at error correction M. */
const val MAX_QR_PAYLOAD_CHARS = 2300

sealed interface IncomingState {
    data object Idle : IncomingState
    data object Decoding : IncomingState
    data class Ready(
        val miniApp: MiniApp,
        val yaml: String,
        val precheck: RecipeImporter.Precheck
    ) : IncomingState
    data class Error(val message: String) : IncomingState
}

class SyncViewModel(
    private val miniAppRepository: MiniAppRepository,
    private val installStore: InstallStore,
    private val recipeImporter: RecipeImporter
) : ViewModel() {

    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    val installedRecipesForShare: StateFlow<List<MiniApp>> = installStore.installs
        .map { installs -> miniAppRepository.loadAll().filter { it.id in installs.keys } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _incoming = MutableStateFlow<IncomingState>(IncomingState.Idle)
    val incoming: StateFlow<IncomingState> = _incoming.asStateFlow()

    fun handleScannedQr(raw: String) {
        _incoming.value = IncomingState.Decoding
        val yamlText = RecipePayload.decode(raw).getOrElse {
            _incoming.value = IncomingState.Error(it.message ?: "QR data is corrupted")
            return
        }
        decodeYaml(yamlText)
    }

    fun handlePastedYaml(text: String) {
        _incoming.value = IncomingState.Decoding
        decodeYaml(text)
    }

    private fun decodeYaml(text: String) {
        val miniApp = recipeImporter.parse(text).getOrElse {
            _incoming.value = IncomingState.Error("Recipe file is corrupted")
            return
        }
        _incoming.value = IncomingState.Ready(
            miniApp = miniApp,
            yaml = text,
            precheck = recipeImporter.precheck(miniApp)
        )
    }

    /** Returns the recipeId of the imported recipe so the caller can navigate to Configurator. */
    fun confirmInstall(): String? {
        val ready = _incoming.value as? IncomingState.Ready ?: return null
        recipeImporter.commit(ready.miniApp, ready.yaml)
        _incoming.value = IncomingState.Idle
        return ready.miniApp.id
    }

    fun dismissPreview() { _incoming.value = IncomingState.Idle }

    /**
     * Re-serializes a `MiniApp` to YAML and encodes for QR transport.
     * Fails if the encoded payload exceeds [MAX_QR_PAYLOAD_CHARS].
     */
    fun encodeRecipeAsQr(miniApp: MiniApp): Result<String> = runCatching {
        val yamlText = yaml.encodeToString(MiniApp.serializer(), miniApp)
        val payload = RecipePayload.encode(yamlText)
        require(payload.length <= MAX_QR_PAYLOAD_CHARS) {
            "Recipe too large for QR (${payload.length} chars, max $MAX_QR_PAYLOAD_CHARS)"
        }
        payload
    }
}
