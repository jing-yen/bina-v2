package com.bina.ai.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bina.ai.R
import com.bina.ai.miniapp.model.MiniApp

private val CATEGORY_MAP = mapOf(
    "All" to R.string.cat_all,
    "Agriculture" to R.string.cat_agriculture,
    "Health" to R.string.cat_health,
    "Business" to R.string.cat_business,
    "Finance" to R.string.cat_finance,
    "Education" to R.string.cat_education
)

@Composable
fun localizedCategory(category: String): String {
    val resId = CATEGORY_MAP[category] ?: return category
    return stringResource(resId)
}

fun resolveAppLang(miniApp: MiniApp): String {
    val appLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags().split(",").firstOrNull()?.take(2) ?: ""
    val supported = miniApp.localisation.supported
    return when {
        appLocale in supported -> appLocale
        appLocale == "id" && "in" in supported -> "in"
        appLocale == "in" && "id" in supported -> "id"
        else -> miniApp.localisation.defaultLanguage.ifEmpty { supported.firstOrNull() ?: "en" }
    }
}

fun localizedName(miniApp: MiniApp): String {
    val lang = resolveAppLang(miniApp)
    return miniApp.localisation.labels[lang]?.get("recipe_name") ?: miniApp.name
}

fun localizedDescription(miniApp: MiniApp): String {
    val lang = resolveAppLang(miniApp)
    return miniApp.localisation.labels[lang]?.get("recipe_desc") ?: miniApp.description
}

@Composable
fun localizedDescription(recipeId: String, fallback: String): String = fallback
