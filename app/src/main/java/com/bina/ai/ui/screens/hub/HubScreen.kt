package com.bina.ai.ui.screens.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bina.ai.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.bina.ai.hub.FirestoreRecipeSource
import com.bina.ai.install.InstallStore
import com.bina.ai.install.model.InstallRecord
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.screens.hub.components.CategoryChips
import com.bina.ai.ui.screens.hub.components.CategoryRail
import com.bina.ai.ui.screens.hub.components.FeaturedCarousel
import com.bina.ai.ui.screens.hub.components.HubHeader
import com.bina.ai.ui.screens.hub.components.RecipeListItem
import com.bina.ai.ui.screens.hub.model.HubUiState
import com.bina.ai.ui.screens.recipe_detail.RecipeDetailSheet
import com.bina.ai.ui.theme.BinaGrayText
import kotlinx.coroutines.launch

@Composable
fun HubScreen(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    firestoreRecipeSource: FirestoreRecipeSource? = null,
    onConfigureRecipe: (recipeId: String) -> Unit,
    onOpenRecipe: (recipeId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val factory = remember(miniAppRepository, installStore, firestoreRecipeSource) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                HubViewModel(miniAppRepository, installStore, firestoreRecipeSource) as T
        }
    }
    val vm: HubViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    var sheetRecipe by remember { mutableStateOf<MiniApp?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            HubUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    HubHeader()
                }
            }
            is HubUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item { HubHeader() }
                    if (s.featured.isNotEmpty()) {
                        item {
                            FeaturedCarousel(
                                recipes = s.featured,
                                onRecipeClick = { sheetRecipe = it }
                            )
                        }
                    }
                    item {
                        CategoryChips(
                            categories = s.categories,
                            selected = s.selectedCategory,
                            onSelect = vm::selectCategory
                        )
                    }
                    if (s.selectedCategory == "All") {
                        if (s.yourRecipes.isNotEmpty()) {
                            item {
                                CategoryRail(
                                    title = stringResource(R.string.hub_your_recipes),
                                    recipes = s.yourRecipes,
                                    installedIds = s.installedIds,
                                    onRecipeClick = { sheetRecipe = it }
                                )
                            }
                        }
                        if (s.trending.isNotEmpty()) {
                            item {
                                CategoryRail(
                                    title = stringResource(R.string.hub_trending),
                                    recipes = s.trending,
                                    installedIds = s.installedIds,
                                    onRecipeClick = { sheetRecipe = it }
                                )
                            }
                        }
                        items(s.rails, key = { it.title }) { rail ->
                            CategoryRail(
                                title = rail.title,
                                recipes = rail.recipes,
                                installedIds = s.installedIds,
                                onRecipeClick = { sheetRecipe = it }
                            )
                        }
                        if (s.rails.isEmpty() && s.yourRecipes.isEmpty() && s.trending.isEmpty()) {
                            item { EmptyHub(stringResource(R.string.hub_empty_all)) }
                        }
                    } else {
                        if (s.allRecipes.isEmpty()) {
                            item { EmptyHub(stringResource(R.string.hub_empty_category)) }
                        } else {
                            items(s.allRecipes, key = { it.id }) { recipe ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    RecipeListItem(
                                        miniApp = recipe,
                                        isInstalled = recipe.id in s.installedIds,
                                        onClick = { sheetRecipe = recipe }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        sheetRecipe?.let { recipe ->
            val baseSizeKb = remember(recipe.id) {
                val yaml = miniAppRepository.getYamlById(recipe.id)
                if (yaml != null) yaml.length / 1024f
                else runCatching {
                    context.assets.openFd("miniapps/${recipe.id}.yaml").use { it.length / 1024f }
                }.getOrDefault(1.0f)
            }
            RecipeDetailSheet(
                miniApp = recipe,
                isInstalled = recipe.id in (state as? HubUiState.Loaded)?.installedIds.orEmpty(),
                sizeKb = baseSizeKb,
                onConfigureInstall = {
                    sheetRecipe = null
                    if (recipe.features.isEmpty()) {
                        scope.launch {
                            try {
                                miniAppRepository.persistRecipeLocally(recipe.id)
                                installStore.install(InstallRecord(
                                    recipeId = recipe.id,
                                    installedAt = System.currentTimeMillis(),
                                    enabledFeatureIds = emptySet()
                                ))
                            } catch (_: Exception) { }
                            onOpenRecipe(recipe.id)
                        }
                    } else {
                        onConfigureRecipe(recipe.id)
                    }
                },
                onOpen = {
                    sheetRecipe = null
                    onOpenRecipe(recipe.id)
                },
                onDismiss = { sheetRecipe = null }
            )
        }
    }
}

@Composable
private fun EmptyHub(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = BinaGrayText)
    }
}
