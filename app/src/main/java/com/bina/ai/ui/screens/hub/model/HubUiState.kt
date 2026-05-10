package com.bina.ai.ui.screens.hub.model

import com.bina.ai.miniapp.model.MiniApp

data class Rail(val title: String, val recipes: List<MiniApp>)

sealed interface HubUiState {
    data object Loading : HubUiState

    data class Loaded(
        val allRecipes: List<MiniApp>,
        val featured: List<MiniApp>,
        val categories: List<String>,
        val selectedCategory: String,
        val rails: List<Rail>,
        val installedIds: Set<String>
    ) : HubUiState
}
