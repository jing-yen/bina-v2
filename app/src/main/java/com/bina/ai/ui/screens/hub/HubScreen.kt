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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.navigation.UserMode
import com.bina.ai.ui.screens.hub.components.CategoryChips
import com.bina.ai.ui.screens.hub.components.CategoryRail
import com.bina.ai.ui.screens.hub.components.FeaturedCarousel
import com.bina.ai.ui.screens.hub.components.HubHeader
import com.bina.ai.ui.screens.hub.components.PublishFab
import com.bina.ai.ui.screens.hub.components.RecipeListItem
import com.bina.ai.ui.screens.hub.model.HubUiState
import com.bina.ai.ui.screens.recipe_detail.RecipeDetailSheet
import com.bina.ai.ui.theme.BinaGrayText

@Composable
fun HubScreen(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    userMode: UserMode,
    onConfigureRecipe: (recipeId: String) -> Unit,
    onOpenRecipe: (recipeId: String) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val factory = remember(miniAppRepository, installStore, userMode) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                HubViewModel(miniAppRepository, installStore, userMode, context.filesDir) as T
        }
    }
    val vm: HubViewModel = viewModel(key = "hub-${userMode.name}", factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    var sheetRecipe by remember { mutableStateOf<MiniApp?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            HubUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    HubHeader(mode = userMode)
                }
            }
            is HubUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { HubHeader(mode = s.mode) }
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
                        items(s.rails, key = { it.title }) { rail ->
                            CategoryRail(
                                title = rail.title,
                                recipes = rail.recipes,
                                installedIds = s.installedIds,
                                authoredIds = s.authoredIds,
                                onRecipeClick = { sheetRecipe = it }
                            )
                        }
                        if (s.rails.isEmpty()) {
                            item { EmptyHub("No recipes available.") }
                        }
                    } else {
                        if (s.allRecipes.isEmpty()) {
                            item { EmptyHub("No recipes in this category.") }
                        } else {
                            items(s.allRecipes, key = { it.id }) { recipe ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    RecipeListItem(
                                        miniApp = recipe,
                                        isInstalled = recipe.id in s.installedIds,
                                        isAuthored = recipe.id in s.authoredIds,
                                        onClick = { sheetRecipe = recipe }
                                    )
                                }
                            }
                        }
                    }
                }

                if (s.mode == UserMode.ARCHITECT) {
                    PublishFab(
                        onClick = onOpenStudio,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }
            }
        }

        sheetRecipe?.let { recipe ->
            val baseSizeKb = remember(recipe.id) {
                runCatching {
                    context.assets.openFd("miniapps/${recipe.id}.yaml").use { it.length / 1024f }
                }.getOrDefault(1.0f)
            }
            RecipeDetailSheet(
                miniApp = recipe,
                isInstalled = recipe.id in (state as? HubUiState.Loaded)?.installedIds.orEmpty(),
                sizeKb = baseSizeKb,
                onConfigureInstall = {
                    sheetRecipe = null
                    onConfigureRecipe(recipe.id)
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
