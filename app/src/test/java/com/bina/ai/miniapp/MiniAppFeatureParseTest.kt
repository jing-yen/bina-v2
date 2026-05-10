package com.bina.ai.miniapp

import com.bina.ai.miniapp.model.MiniApp
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniAppFeatureParseTest {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    @Test
    fun `features field parses with all fields populated`() {
        val text = """
            id: test_recipe
            name: Test
            features:
              - id: alpha
                name: Alpha
                description: First feature
                icon: camera
                recommended: true
                size_kb: 0.5
                requires: [permission:camera]
              - id: beta
                name: Beta
        """.trimIndent()

        val app = yaml.decodeFromString(MiniApp.serializer(), text)

        assertEquals(2, app.features.size)
        val alpha = app.features[0]
        assertEquals("alpha", alpha.id)
        assertEquals("Alpha", alpha.name)
        assertEquals("First feature", alpha.description)
        assertEquals("camera", alpha.icon)
        assertTrue(alpha.recommended)
        assertEquals(0.5f, alpha.sizeKb, 0.001f)
        assertEquals(listOf("permission:camera"), alpha.requires)

        val beta = app.features[1]
        assertEquals("beta", beta.id)
        assertEquals(false, beta.recommended)
        assertEquals(0f, beta.sizeKb, 0.001f)
        assertTrue(beta.requires.isEmpty())
    }

    @Test
    fun `MiniApp without features field decodes with empty features list`() {
        val text = """
            id: bare
            name: Bare
        """.trimIndent()

        val app = yaml.decodeFromString(MiniApp.serializer(), text)

        assertTrue(app.features.isEmpty())
    }
}
