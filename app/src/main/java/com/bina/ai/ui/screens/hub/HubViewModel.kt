package com.bina.ai.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.navigation.UserMode
import com.bina.ai.ui.screens.hub.model.HubUiState
import com.bina.ai.ui.screens.hub.model.Rail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.io.File

const val ALL_CATEGORY = "All"
const val MIN_CATEGORY_RAIL_SIZE = 2

/**
 * Pure rail-computation logic. Tested without ViewModel/Flow ceremony.
 */
fun computeRails(
    recipes: List<MiniApp>,
    authored: Set<String>,
    isArchitect: Boolean
): List<Rail> = buildList {
    if (isArchitect) {
        val mine = recipes.filter { it.id in authored }
        if (mine.isNotEmpty()) add(Rail("Your Recipes", mine))
    }
    val rest = recipes.filterNot { isArchitect && it.id in authored }
    if (rest.isNotEmpty()) add(Rail("All Recipes", rest))
    rest.groupBy { it.category }
        .toSortedMap()
        .forEach { (cat, items) ->
            if (cat.isNotBlank() && items.size >= MIN_CATEGORY_RAIL_SIZE) {
                add(Rail(cat, items))
            }
        }
}

/** Scans filesDir for authored YAML IDs (same logic as AnalyticsRepository). */
fun scanAuthoredRecipeIds(filesDir: File): Set<String> {
    val miniappsDir = File(filesDir, "miniapps")
    if (!miniappsDir.isDirectory) return emptySet()
    return miniappsDir.listFiles()
        ?.filter { it.isFile && (it.extension == "yaml" || it.extension == "yml") }
        ?.mapNotNull { extractIdFromYaml(it) }
        ?.toSet()
        ?: emptySet()
}

private fun extractIdFromYaml(file: File): String? = runCatching {
    file.useLines { lines ->
        lines
            .firstOrNull { it.trimStart().startsWith("id:") }
            ?.substringAfter(":")
            ?.trim()
            ?.trim('"', '\'')
            ?.takeIf { it.isNotEmpty() }
    }
}.getOrNull()

class HubViewModel(
    private val repo: MiniAppRepository,
    private val installStore: InstallStore,
    private val mode: UserMode,
    private val filesDir: File
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(ALL_CATEGORY)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun selectCategory(category: String) { _selectedCategory.value = category }

    val uiState: StateFlow<HubUiState> = combine(
        flowOf(repo.loadAll()),
        installStore.installs,
        _selectedCategory
    ) { recipes, installs, category ->
        val authored = scanAuthoredRecipeIds(filesDir)
        val featured = recipes.filter { it.featured || it.emergency }
        val categories = listOf(ALL_CATEGORY) + recipes.map { it.category }.filter { it.isNotBlank() }.toSortedSet()
        val visibleRecipes = if (category == ALL_CATEGORY) recipes else recipes.filter { it.category == category }
        val rails = if (category == ALL_CATEGORY) computeRails(visibleRecipes, authored, mode == UserMode.ARCHITECT) else emptyList()
        HubUiState.Loaded(
            mode = mode,
            allRecipes = visibleRecipes,
            featured = featured,
            categories = categories,
            selectedCategory = category,
            rails = rails,
            installedIds = installs.keys,
            authoredIds = authored
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HubUiState.Loading)
}
