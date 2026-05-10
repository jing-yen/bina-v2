package com.bina.ai.install

import com.bina.ai.miniapp.model.Feature
import com.bina.ai.miniapp.model.MiniApp
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeSizeTest {

    private val recipe = MiniApp(
        id = "demo",
        name = "Demo",
        features = listOf(
            Feature(id = "a", name = "A", sizeKb = 0.4f),
            Feature(id = "b", name = "B", sizeKb = 0.3f),
            Feature(id = "c", name = "C", sizeKb = 0.2f)
        )
    )

    @Test fun `totalSizeKb with no features enabled equals base size only`() {
        val total = totalSizeKb(recipe, baseSizeKb = 1.2f, enabledFeatureIds = emptySet())
        assertEquals(1.2f, total, 0.001f)
    }

    @Test fun `totalSizeKb sums base plus enabled feature sizes`() {
        val total = totalSizeKb(recipe, baseSizeKb = 1.2f, enabledFeatureIds = setOf("a", "c"))
        assertEquals(1.8f, total, 0.001f)
    }

    @Test fun `totalSizeKb ignores enabled IDs that no longer exist on the recipe`() {
        val total = totalSizeKb(recipe, baseSizeKb = 1.0f, enabledFeatureIds = setOf("a", "ghost"))
        assertEquals(1.4f, total, 0.001f)
    }

    @Test fun `totalSizeKb on a recipe with no features returns base only`() {
        val bare = MiniApp(id = "bare", name = "Bare", features = emptyList())
        val total = totalSizeKb(bare, baseSizeKb = 0.8f, enabledFeatureIds = setOf("anything"))
        assertEquals(0.8f, total, 0.001f)
    }
}
