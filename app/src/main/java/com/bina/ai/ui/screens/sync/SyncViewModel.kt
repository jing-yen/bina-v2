package com.bina.ai.ui.screens.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.sync.BlePairingPayload
import com.bina.ai.sync.RecipeImporter
import com.bina.ai.sync.RecipePayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Cap on encoded QR payload size — QR version 40 alphanumeric at error-correction
 * level L holds 4296 chars. We give a small headroom for the magic prefix.
 */
const val MAX_QR_PAYLOAD_CHARS = 4200

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

sealed interface TransferState {
    data object Idle : TransferState
    data object Connecting : TransferState
    data class InProgress(val pct: Int) : TransferState
    data class Failed(val message: String) : TransferState
}

class SyncViewModel(
    private val miniAppRepository: MiniAppRepository,
    private val installStore: InstallStore,
    private val recipeImporter: RecipeImporter
) : ViewModel() {

    val installedRecipesForShare: StateFlow<List<MiniApp>> = installStore.installs
        .map { installs -> miniAppRepository.loadAll().filter { it.id in installs.keys } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _incoming = MutableStateFlow<IncomingState>(IncomingState.Idle)
    val incoming: StateFlow<IncomingState> = _incoming.asStateFlow()

    private val _pairing = MutableStateFlow<BlePairingPayload.Offer?>(null)
    val pairing: StateFlow<BlePairingPayload.Offer?> = _pairing.asStateFlow()

    private val _transfer = MutableStateFlow<TransferState>(TransferState.Idle)
    val transfer: StateFlow<TransferState> = _transfer.asStateFlow()

    fun handleScannedQr(raw: String) {
        val trimmed = raw.trim()
        when {
            trimmed.startsWith("BINA-BT:") -> {
                val offer = BlePairingPayload.decode(trimmed).getOrElse {
                    _incoming.value = IncomingState.Error(it.message ?: "Pairing data is corrupted")
                    return
                }
                _pairing.value = offer
            }
            trimmed.startsWith("BINA2:") || trimmed.startsWith("BINA1:") -> {
                // Direct-encode QR (paste fallback). Decode then parse via existing path.
                _incoming.value = IncomingState.Decoding
                val yamlText = RecipePayload.decode(trimmed).getOrElse {
                    _incoming.value = IncomingState.Error(it.message ?: "QR data is corrupted")
                    return
                }
                decodeYaml(yamlText)
            }
            else -> {
                _incoming.value = IncomingState.Error("Not a Bina QR")
            }
        }
    }

    /**
     * Handles paste-sheet input. Accepts either a raw YAML or a `BINA2:`-prefixed
     * payload (e.g., the text someone copied from a generic QR scanner app).
     */
    fun handlePastedYaml(text: String) {
        val trimmed = text.trim()
        if (trimmed.startsWith("BINA2:") || trimmed.startsWith("BINA1:")) {
            handleScannedQr(trimmed)
            return
        }
        _incoming.value = IncomingState.Decoding
        decodeYaml(trimmed)
    }

    private fun decodeYaml(text: String) {
        val miniApp = recipeImporter.parse(text).getOrElse {
            _incoming.value = IncomingState.Error("Recipe file is corrupted: ${it.message ?: "parse failed"}")
            return
        }
        _incoming.value = IncomingState.Ready(
            miniApp = miniApp,
            yaml = text,
            precheck = recipeImporter.precheck(miniApp)
        )
    }

    /** Returns the recipeId of the imported recipe. */
    fun confirmInstall(): String? {
        val ready = _incoming.value as? IncomingState.Ready ?: return null
        recipeImporter.commit(ready.miniApp, ready.yaml)
        _incoming.value = IncomingState.Idle
        return ready.miniApp.id
    }

    fun dismissPreview() { _incoming.value = IncomingState.Idle }

    fun dismissPairing() {
        _pairing.value = null
        _transfer.value = TransferState.Idle
    }

    fun onTransferConnecting() {
        _transfer.value = TransferState.Connecting
    }

    fun onTransferProgress(pct: Int) {
        _transfer.value = TransferState.InProgress(pct)
    }

    fun onTransferFailed(message: String) {
        _transfer.value = TransferState.Failed(message)
    }

    fun onTransferComplete(payloadBytes: ByteArray) {
        _transfer.value = TransferState.Idle
        _pairing.value = null
        decodeYaml(String(payloadBytes, Charsets.UTF_8))
    }

    /**
     * Encodes the recipe's source YAML for QR transport. Uses the raw YAML the
     * repository loaded — the kaml MiniApp serializer is decode-only because
     * `WidgetSerializer.serialize` is intentionally unimplemented.
     * Fails if the encoded payload exceeds [MAX_QR_PAYLOAD_CHARS].
     */
    fun encodeRecipeAsQr(miniApp: MiniApp): Result<String> = runCatching {
        val yamlText = miniAppRepository.getYamlById(miniApp.id)
            ?: error("Recipe YAML not found for id: ${miniApp.id}")
        val payload = RecipePayload.encode(yamlText)
        require(payload.length <= MAX_QR_PAYLOAD_CHARS) {
            "Recipe too large for QR (${payload.length} chars, max $MAX_QR_PAYLOAD_CHARS)"
        }
        payload
    }
}
