package com.bina.ai.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.hub.FirestoreRecipeSource
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.platform.Logger
import com.bina.ai.ui.screens.hub.model.HubUiState
import com.bina.ai.ui.screens.hub.model.Rail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val ALL_CATEGORY = "All"
const val MIN_CATEGORY_RAIL_SIZE = 2

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
    private val installStore: InstallStore,
    private val firestoreSource: FirestoreRecipeSource? = null
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(ALL_CATEGORY)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _cloudRecipes = MutableStateFlow<List<MiniApp>>(emptyList())

    init {
        if (firestoreSource != null) {
            viewModelScope.launch {
                try {
                    val recipesWithYaml = firestoreSource.fetchRecipesWithYaml()
                    _cloudRecipes.value = recipesWithYaml.map { it.first }
                    repo.registerCloudRecipesWithYaml(recipesWithYaml)
                    Logger.d(TAG, "Loaded ${recipesWithYaml.size} cloud recipes")
                } catch (e: Exception) {
                    Logger.e(TAG, "Cloud recipe fetch failed", e)
                }
            }
        }
    }

    fun selectCategory(category: String) { _selectedCategory.value = category }

    fun refreshCloud() {
        if (firestoreSource == null) return
        viewModelScope.launch {
            try {
                val recipesWithYaml = firestoreSource.fetchRecipesWithYaml()
                _cloudRecipes.value = recipesWithYaml.map { it.first }
                repo.registerCloudRecipesWithYaml(recipesWithYaml)
            } catch (e: Exception) {
                Logger.e(TAG, "Cloud refresh failed", e)
            }
        }
    }

    val uiState: StateFlow<HubUiState> = combine(
        _cloudRecipes,
        installStore.installs,
        _selectedCategory
    ) { cloudRecipes, installs, category ->
        val localRecipes = repo.loadAll()
        val localIds = localRecipes.map { it.id }.toSet()
        val merged = localRecipes + cloudRecipes.filter { it.id !in localIds }

        val featured = merged.filter { it.featured || it.emergency }
        val categories = listOf(ALL_CATEGORY) + merged.map { it.category }.filter { it.isNotBlank() }.toSortedSet()
        val visibleRecipes = if (category == ALL_CATEGORY) merged else merged.filter { it.category == category }
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

    companion object {
        private const val TAG = "HubViewModel"
    }
}
