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
    // Also check system locale as fallback when app locale is not explicitly set
    val systemLocale = java.util.Locale.getDefault().language.take(2)
    val effectiveLocale = appLocale.ifEmpty { systemLocale }
    return when {
        effectiveLocale in supported -> effectiveLocale
        effectiveLocale == "id" && "in" in supported -> "in"
        effectiveLocale == "in" && "id" in supported -> "id"
        "en" in supported -> "en"
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
