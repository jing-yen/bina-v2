package com.bina.ai.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.screens.hub.model.HubUiState
import com.bina.ai.ui.screens.hub.model.Rail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

const val ALL_CATEGORY = "All"
const val MIN_CATEGORY_RAIL_SIZE = 2

/**
 * Pure rail-computation logic for the Builder Hub. Always shows an "All Recipes" rail;
 * adds per-category rails when a category has >= MIN_CATEGORY_RAIL_SIZE recipes.
 */
fun computeRails(recipes: List<MiniApp>): List<Rail> = buildList {
    if (recipes.isNotEmpty()) add(Rail("All Recipes", recipes))
    recipes.groupBy { it.category }
        .toSortedMap()
        .forEach { (cat, items) ->
            if (cat.isNotBlank() && items.size >= MIN_CATEGORY_RAIL_SIZE) {
                add(Rail(cat, items))
            }
        }
}

class HubViewModel(
    private val repo: MiniAppRepository,
    private val installStore: InstallStore
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(ALL_CATEGORY)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun selectCategory(category: String) { _selectedCategory.value = category }

    val uiState: StateFlow<HubUiState> = combine(
        flowOf(repo.loadAll()),
        installStore.installs,
        _selectedCategory
    ) { recipes, installs, category ->
        val featured = recipes.filter { it.featured || it.emergency }
        val categories = listOf(ALL_CATEGORY) + recipes.map { it.category }.filter { it.isNotBlank() }.toSortedSet()
        val visibleRecipes = if (category == ALL_CATEGORY) recipes else recipes.filter { it.category == category }
        val rails = if (category == ALL_CATEGORY) computeRails(visibleRecipes) else emptyList()
        HubUiState.Loaded(
            allRecipes = visibleRecipes,
            featured = featured,
            categories = categories,
            selectedCategory = category,
            rails = rails,
            installedIds = installs.keys
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HubUiState.Loading)
}
