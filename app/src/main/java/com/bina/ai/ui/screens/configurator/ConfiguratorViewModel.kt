package com.bina.ai.ui.screens.configurator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.install.CapabilityChecker
import com.bina.ai.install.InstallStore
import com.bina.ai.install.model.InstallRecord
import com.bina.ai.install.totalSizeKb
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ConfiguratorEvent {
    data class Installed(val recipeName: String) : ConfiguratorEvent
    data object AlreadyInstalled : ConfiguratorEvent
    data class Failed(val message: String) : ConfiguratorEvent
}

/**
 * Transient UI state for ConfiguratorScreen. Pure data — no Android dependencies — so it's
 * unit-testable on the JVM. Use [initial] to build the starting state for a given recipe.
 */
data class ConfiguratorState(
    val miniApp: MiniApp,
    val toggles: Map<String, Boolean>,
    val availability: Map<String, Boolean>
) {
    val activeCount: Int get() = toggles.count { it.value }
    val totalCount: Int get() = miniApp.features.size

    val enabledFeatureIds: Set<String>
        get() = toggles.filterValues { it }.keys

    fun totalSizeKb(baseSizeKb: Float): Float =
        totalSizeKb(miniApp, baseSizeKb, enabledFeatureIds)

    fun withToggle(featureId: String, on: Boolean): ConfiguratorState {
        if (availability[featureId] == false) return this
        return copy(toggles = toggles + (featureId to on))
    }

    companion object {
        fun initial(recipe: MiniApp, checker: CapabilityChecker): ConfiguratorState {
            val availability = recipe.features.associate { feature ->
                feature.id to feature.requires.all { checker.isAvailable(it) }
            }
            val toggles = recipe.features.associate { feature ->
                val available = availability[feature.id] == true
                feature.id to (feature.recommended && available)
            }
            return ConfiguratorState(recipe, toggles, availability)
        }
    }
}

class ConfiguratorViewModel(
    initialState: ConfiguratorState,
    private val baseSizeKb: Float,
    private val installStore: InstallStore,
    private val miniAppRepository: MiniAppRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<ConfiguratorState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ConfiguratorEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<ConfiguratorEvent> = _events.asSharedFlow()

    fun toggleFeature(featureId: String, on: Boolean) {
        _state.value = _state.value.withToggle(featureId, on)
    }

    fun currentSizeKb(): Float = _state.value.totalSizeKb(baseSizeKb)

    fun install() = viewModelScope.launch {
        val s = _state.value
        if (installStore.isInstalled(s.miniApp.id).first()) {
            _events.emit(ConfiguratorEvent.AlreadyInstalled)
            return@launch
        }
        try {
            miniAppRepository?.persistRecipeLocally(s.miniApp.id)
            installStore.install(InstallRecord(
                recipeId = s.miniApp.id,
                installedAt = System.currentTimeMillis(),
                enabledFeatureIds = s.enabledFeatureIds
            ))
            _events.emit(ConfiguratorEvent.Installed(s.miniApp.name))
        } catch (t: Throwable) {
            _events.emit(ConfiguratorEvent.Failed(t.message ?: "Couldn't save install"))
        }
    }
}
