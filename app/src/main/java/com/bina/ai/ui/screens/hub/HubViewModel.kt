package com.bina.ai.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.hub.FirestoreRecipeSource
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.Author
import com.bina.ai.miniapp.model.LocalisationConfig
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.miniapp.model.ThemeConfig
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

private val MOCK_YOUR_RECIPES = listOf(
    MiniApp(
        id = "mock_plant_doctor", name = "Doktor Pokok", icon = "🌿",
        description = "AI crop diagnosis — snap a photo of your plant, get instant disease identification and treatment",
        category = "Agriculture", featured = true,
        author = Author(name = "Jabatan Pertanian Malaysia", organisation = "Jabatan Pertanian Malaysia", verified = true),
        localisation = LocalisationConfig(supported = listOf("ms", "en", "zh", "ta", "th", "id", "vi", "km"), defaultLanguage = "ms"),
        theme = ThemeConfig(primary = "#2D7D46", secondary = "#F0F7F2")
    ),
    MiniApp(
        id = "mock_dengue", name = "Cegah Denggi", icon = "🦟",
        description = "Dengue prevention awareness chatbot for community health campaigns",
        category = "Health",
        author = Author(name = "Jing Yen", organisation = "Ministry of Health, Malaysia", verified = true),
        localisation = LocalisationConfig(supported = listOf("ms", "en", "ta", "zh"), defaultLanguage = "ms"),
        theme = ThemeConfig(primary = "#C45A3A", secondary = "#FFF5F0")
    ),
    MiniApp(
        id = "mock_nutrition", name = "Panduan Nutrisi Kanak-Kanak", icon = "🥦",
        description = "Child nutrition screening and dietary guidance for rural clinics",
        category = "Health",
        author = Author(name = "Jing Yen", organisation = "Ministry of Health, Malaysia", verified = true),
        localisation = LocalisationConfig(supported = listOf("ms", "en", "id"), defaultLanguage = "ms"),
        theme = ThemeConfig(primary = "#C45A3A", secondary = "#FFF5F0")
    ),
)

private val MOCK_TRENDING = listOf(
    MiniApp(
        id = "mock_thai", name = "ผู้ช่วยร้านค้า", icon = "🏪",
        description = "Shop management assistant for small Thai retailers",
        category = "Business",
        author = Author(name = "Bangkok SME Network", organisation = "Bangkok SME Network", verified = true),
        localisation = LocalisationConfig(supported = listOf("th", "en"), defaultLanguage = "th")
    ),
    MiniApp(
        id = "mock_viet", name = "Trợ Lý Nông Nghiệp", icon = "🌾",
        description = "Agriculture assistant for Vietnamese farmers",
        category = "Agriculture",
        author = Author(name = "Mekong Delta Co-op", organisation = "Mekong Delta Co-op", verified = true),
        localisation = LocalisationConfig(supported = listOf("vi", "en", "km"), defaultLanguage = "vi")
    ),
    MiniApp(
        id = "mock_khmer", name = "កសិកម្មឆ្លាត", icon = "🌱",
        description = "Smart farming guide for Cambodian smallholders",
        category = "Agriculture",
        author = Author(name = "Cambodia Rural Dev", organisation = "Cambodia Rural Dev", verified = true),
        localisation = LocalisationConfig(supported = listOf("km", "en", "th"), defaultLanguage = "km")
    ),
    MiniApp(
        id = "mock_sawit", name = "Pakar Sawit", icon = "🌴",
        description = "Palm oil cultivation expert for Malaysian smallholders",
        category = "Agriculture",
        author = Author(name = "FELDA Malaysia", organisation = "FELDA Malaysia", verified = true),
        localisation = LocalisationConfig(supported = listOf("ms", "en", "id", "th"), defaultLanguage = "ms")
    ),
    MiniApp(
        id = "mock_myanmar", name = "ရွာကျန်းမာရေး", icon = "🏥",
        description = "Village health guide for rural Myanmar communities",
        category = "Health",
        author = Author(name = "Mandalay Health Dept", organisation = "Mandalay Health Dept", verified = true),
        localisation = LocalisationConfig(supported = listOf("my", "en"), defaultLanguage = "my")
    ),
)

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
        val cloudOnly = cloudRecipes.filter { it.id !in localIds }
        val mockIds = (MOCK_YOUR_RECIPES + MOCK_TRENDING).map { it.id }.toSet()
        val baseRecipes = localRecipes + cloudOnly
        val merged = MOCK_YOUR_RECIPES + baseRecipes.filter { it.id !in mockIds } + MOCK_TRENDING

        val featured = listOf(MOCK_YOUR_RECIPES.first()) +
            baseRecipes.filter { it.featured || it.emergency }.take(2) +
            baseRecipes.filterNot { it.featured || it.emergency }.take(2)

        val categories = listOf(ALL_CATEGORY) + merged.map { it.category }.filter { it.isNotBlank() }.toSortedSet()
        val visibleRecipes = if (category == ALL_CATEGORY) merged else merged.filter { it.category == category }
        val rails = if (category == ALL_CATEGORY) computeRails(visibleRecipes) else emptyList()
        HubUiState.Loaded(
            allRecipes = visibleRecipes,
            featured = featured.take(5),
            categories = categories,
            selectedCategory = category,
            rails = rails,
            installedIds = installs.keys,
            yourRecipes = MOCK_YOUR_RECIPES,
            trending = MOCK_TRENDING
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HubUiState.Loading)

    companion object {
        private const val TAG = "HubViewModel"
    }
}
