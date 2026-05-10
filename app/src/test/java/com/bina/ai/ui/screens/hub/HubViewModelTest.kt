package com.bina.ai.ui.screens.hub

import com.bina.ai.miniapp.model.MiniApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HubViewModelTest {

    private fun recipe(id: String, category: String = "Health"): MiniApp =
        MiniApp(id = id, name = id.replaceFirstChar { it.uppercase() }, category = category)

    @Test fun `empty recipes produces no rails`() {
        val rails = computeRails(emptyList())
        assertTrue(rails.isEmpty())
    }

    @Test fun `single recipe shows All Recipes rail only`() {
        val rails = computeRails(listOf(recipe("a")))
        assertEquals(1, rails.size)
        assertEquals("All Recipes", rails[0].title)
        assertEquals(1, rails[0].recipes.size)
    }

    @Test fun `three recipes in three different categories shows only All Recipes`() {
        val rs = listOf(
            recipe("a", "Health"),
            recipe("b", "Agriculture"),
            recipe("c", "Business")
        )
        val rails = computeRails(rs)
        assertEquals(1, rails.size)
        assertEquals("All Recipes", rails[0].title)
        assertEquals(3, rails[0].recipes.size)
    }

    @Test fun `two-per-category shows category rails too`() {
        val rs = listOf(
            recipe("h1", "Health"),
            recipe("h2", "Health"),
            recipe("a1", "Agriculture"),
            recipe("a2", "Agriculture"),
            recipe("a3", "Agriculture")
        )
        val rails = computeRails(rs)
        assertEquals(3, rails.size)
        assertEquals("All Recipes", rails[0].title)
        assertEquals(setOf("Health", "Agriculture"), rails.drop(1).map { it.title }.toSet())
    }

    @Test fun `blank category does not produce a category rail`() {
        val rs = listOf(
            recipe("a", ""),
            recipe("b", "")
        )
        val rails = computeRails(rs)
        assertEquals(1, rails.size)
        assertEquals("All Recipes", rails[0].title)
    }
}
