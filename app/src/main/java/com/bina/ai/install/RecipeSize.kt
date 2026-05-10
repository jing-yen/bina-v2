package com.bina.ai.install

import com.bina.ai.miniapp.model.MiniApp

/**
 * Compute the total install size in KB for a recipe with a given set of enabled features.
 * baseSizeKb is the size of the recipe's YAML payload itself.
 * Enabled feature IDs not present in the recipe's features list are ignored silently.
 */
fun totalSizeKb(
    recipe: MiniApp,
    baseSizeKb: Float,
    enabledFeatureIds: Set<String>
): Float {
    val featureBytes = recipe.features
        .filter { it.id in enabledFeatureIds }
        .sumOf { it.sizeKb.toDouble() }
        .toFloat()
    return baseSizeKb + featureBytes
}
