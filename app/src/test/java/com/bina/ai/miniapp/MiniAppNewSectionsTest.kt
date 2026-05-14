package com.bina.ai.miniapp

import com.bina.ai.miniapp.model.MiniApp
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniAppNewSectionsTest {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    @Test
    fun `setup intro_page parses correctly`() {
        val text = """
            id: test
            name: Test
            setup:
              intro_page:
                accept_label: "Got it"
                disclaimer: "This is a test disclaimer"
                cover_photo: true
                author:
                  name: Dr. Test
                  organisation: Test Org
                  verified: true
                links:
                  - { label: "Link 1", url: "https://example.com" }
                  - { label: "Link 2", url: "https://example.org" }
        """.trimIndent()

        val app = yaml.decodeFromString(MiniApp.serializer(), text)

        assertEquals("Got it", app.setup.introPage.acceptLabel)
        assertEquals("This is a test disclaimer", app.setup.introPage.disclaimer)
        assertTrue(app.setup.introPage.coverPhoto)
        assertEquals("Dr. Test", app.setup.introPage.author?.name)
        assertEquals("Test Org", app.setup.introPage.author?.organisation)
        assertTrue(app.setup.introPage.author?.verified == true)
        assertEquals(2, app.setup.introPage.links.size)
        assertEquals("Link 1", app.setup.introPage.links[0].label)
        assertEquals("https://example.com", app.setup.introPage.links[0].url)
    }

    @Test
    fun `triage config parses correctly`() {
        val text = """
            id: test
            name: Test
            triage:
              home_mode: chat
              max_clarifications: 3
              fallback: show_all
        """.trimIndent()

        val app = yaml.decodeFromString(MiniApp.serializer(), text)

        assertEquals("chat", app.triage.homeMode)
        assertEquals(3, app.triage.maxClarifications)
        assertEquals("show_all", app.triage.fallback)
    }

    @Test
    fun `screen_catalog parses correctly`() {
        val text = """
            id: test
            name: Test
            screen_catalog:
              - id: diagnose
                title: Diagnosis
                template: vision_analysis
                icon: "🔬"
                description: "Photo diagnosis"
                accepted_inputs: [photo, text]
                prefill_hints:
                  user_text: "Describe symptoms"
        """.trimIndent()

        val app = yaml.decodeFromString(MiniApp.serializer(), text)

        assertEquals(1, app.screenCatalog.size)
        val entry = app.screenCatalog[0]
        assertEquals("diagnose", entry.id)
        assertEquals("Diagnosis", entry.title)
        assertEquals("vision_analysis", entry.template)
        assertEquals("Photo diagnosis", entry.description)
        assertEquals(listOf("photo", "text"), entry.acceptedInputs)
        assertEquals("Describe symptoms", entry.prefillHints["user_text"])
    }

    @Test
    fun `knowledge config parses correctly`() {
        val text = """
            id: test
            name: Test
            knowledge:
              always_loaded: "Some reference knowledge"
              chunks: 3
        """.trimIndent()

        val app = yaml.decodeFromString(MiniApp.serializer(), text)

        assertEquals("Some reference knowledge", app.knowledge.alwaysLoaded)
        assertEquals(3, app.knowledge.chunks)
    }

    @Test
    fun `questions map parses correctly`() {
        val text = """
            id: test
            name: Test
            questions:
              diagnose:
                - "What crop?"
                - "What symptoms?"
              profit:
                - "What harvest?"
        """.trimIndent()

        val app = yaml.decodeFromString(MiniApp.serializer(), text)

        assertEquals(2, app.questions.size)
        assertEquals(listOf("What crop?", "What symptoms?"), app.questions["diagnose"])
        assertEquals(listOf("What harvest?"), app.questions["profit"])
    }

    @Test
    fun `backward compat - old YAML without new sections parses fine`() {
        val text = """
            id: old_recipe
            name: Old Recipe
            description: "A legacy recipe"
            icon: "📦"
        """.trimIndent()

        val app = yaml.decodeFromString(MiniApp.serializer(), text)

        assertEquals("old_recipe", app.id)
        assertEquals("Old Recipe", app.name)
        assertEquals("I Understand", app.setup.introPage.acceptLabel)
        assertTrue(app.setup.introPage.disclaimer.isEmpty())
        assertTrue(app.setup.introPage.links.isEmpty())
        assertEquals("grid", app.triage.homeMode)
        assertEquals(2, app.triage.maxClarifications)
        assertTrue(app.screenCatalog.isEmpty())
        assertTrue(app.knowledge.alwaysLoaded.isEmpty())
        assertTrue(app.questions.isEmpty())
    }
}
