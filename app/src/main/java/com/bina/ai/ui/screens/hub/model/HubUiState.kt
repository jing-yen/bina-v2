package com.bina.ai.ui.screens.hub.model

import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.navigation.UserMode

data class Rail(val title: String, val recipes: List<MiniApp>)

sealed interface HubUiState {
    data object Loading : HubUiState

    data class Loaded(
        val mode: UserMode,
        val allRecipes: List<MiniApp>,
        val featured: List<MiniApp>,
        val categories: List<String>,
        val selectedCategory: String,
        val rails: List<Rail>,
        val installedIds: Set<String>,
        val authoredIds: Set<String>
    ) : HubUiState
}
