package com.bina.ai.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bina.ai.R

private val CATEGORY_MAP = mapOf(
    "All" to R.string.cat_all,
    "Agriculture" to R.string.cat_agriculture,
    "Health" to R.string.cat_health,
    "Business" to R.string.cat_business,
    "Finance" to R.string.cat_finance,
    "Education" to R.string.cat_education
)

private val DESC_MAP = mapOf(
    "mock_plant_doctor" to R.string.desc_mock_plant_doctor,
    "mock_dengue" to R.string.desc_mock_dengue,
    "mock_nutrition" to R.string.desc_mock_nutrition,
    "mock_thai" to R.string.desc_mock_thai,
    "mock_viet" to R.string.desc_mock_viet,
    "mock_khmer" to R.string.desc_mock_khmer,
    "mock_sawit" to R.string.desc_mock_sawit,
    "mock_myanmar" to R.string.desc_mock_myanmar
)

@Composable
fun localizedCategory(category: String): String {
    val resId = CATEGORY_MAP[category] ?: return category
    return stringResource(resId)
}

@Composable
fun localizedDescription(recipeId: String, fallback: String): String {
    val resId = DESC_MAP[recipeId] ?: return fallback
    return stringResource(resId)
}
