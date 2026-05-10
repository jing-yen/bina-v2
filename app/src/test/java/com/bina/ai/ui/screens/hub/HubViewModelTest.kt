package com.bina.ai.ui.screens.hub

import com.bina.ai.miniapp.model.MiniApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HubViewModelTest {

    private fun recipe(id: String, category: String = "Health"): MiniApp =
        MiniApp(id = id, name = id.replaceFirstChar { it.uppercase() }, category = category)

    @Test fun `empty recipes produces no rails`() {
        val rails = computeRails(emptyList(), authored = emptySet(), isArchitect = false)
        assertTrue(rails.isEmpty())
    }

    @Test fun `Builder with one recipe shows All Recipes rail only`() {
        val rails = computeRails(listOf(recipe("a")), authored = emptySet(), isArchitect = false)
        assertEquals(1, rails.size)
        assertEquals("All Recipes", rails[0].title)
        assertEquals(1, rails[0].recipes.size)
    }

    @Test fun `Builder with three recipes in three different categories shows only All Recipes`() {
        val rs = listOf(
            recipe("a", "Health"),
            recipe("b", "Agriculture"),
            recipe("c", "Business")
        )
        val rails = computeRails(rs, authored = emptySet(), isArchitect = false)
        assertEquals(1, rails.size)
        assertEquals("All Recipes", rails[0].title)
        assertEquals(3, rails[0].recipes.size)
    }

    @Test fun `Builder with two-per-category shows category rails too`() {
        val rs = listOf(
            recipe("h1", "Health"),
            recipe("h2", "Health"),
            recipe("a1", "Agriculture"),
            recipe("a2", "Agriculture"),
            recipe("a3", "Agriculture")
        )
        val rails = computeRails(rs, authored = emptySet(), isArchitect = false)
        assertEquals(3, rails.size)
        assertEquals("All Recipes", rails[0].title)
        assertEquals(setOf("Health", "Agriculture"), rails.drop(1).map { it.title }.toSet())
    }

    @Test fun `Architect with no authored shows same rails as Builder`() {
        val rs = listOf(recipe("a"), recipe("b"))
        val builderRails = computeRails(rs, authored = emptySet(), isArchitect = false)
        val architectRails = computeRails(rs, authored = emptySet(), isArchitect = true)
        assertEquals(builderRails.map { it.title }, architectRails.map { it.title })
    }

    @Test fun `Architect with authored gets Your Recipes rail at top`() {
        val rs = listOf(recipe("mine"), recipe("theirs1"), recipe("theirs2"))
        val rails = computeRails(rs, authored = setOf("mine"), isArchitect = true)
        assertEquals("Your Recipes", rails[0].title)
        assertEquals(listOf("mine"), rails[0].recipes.map { it.id })
    }

    @Test fun `Architect's authored recipes are excluded from All Recipes`() {
        val rs = listOf(recipe("mine"), recipe("theirs1"), recipe("theirs2"))
        val rails = computeRails(rs, authored = setOf("mine"), isArchitect = true)
        val allRecipes = rails.first { it.title == "All Recipes" }
        assertEquals(listOf("theirs1", "theirs2"), allRecipes.recipes.map { it.id })
    }

    @Test fun `Builder with authored set ignores authored (no segregation)`() {
        val rs = listOf(recipe("mine"), recipe("theirs"))
        val rails = computeRails(rs, authored = setOf("mine"), isArchitect = false)
        val all = rails.first { it.title == "All Recipes" }
        assertEquals(2, all.recipes.size)
    }

    @Test fun `category rail threshold counts only recipes in All Recipes pool`() {
        val rs = listOf(
            recipe("mh1", "Health"),
            recipe("mh2", "Health"),
            recipe("th1", "Health")
        )
        val rails = computeRails(rs, authored = setOf("mh1", "mh2"), isArchitect = true)
        assertEquals(2, rails.size)
        assertEquals(setOf("Your Recipes", "All Recipes"), rails.map { it.title }.toSet())
    }
}
