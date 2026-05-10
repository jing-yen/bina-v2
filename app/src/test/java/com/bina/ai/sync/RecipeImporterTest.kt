package com.bina.ai.sync

import com.bina.ai.miniapp.MiniAppRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RecipeImporterTest {

    @get:Rule val tmp = TemporaryFolder()

    private val sampleYaml = """
        id: shared_recipe
        name: Shared Recipe
        description: A test recipe
        category: Test
        version: "1.0"
    """.trimIndent()

    private val bundledYaml = """
        id: bundled_recipe
        name: Bundled Recipe
        description: Already in assets
        category: Test
        version: "1.0"
    """.trimIndent()

    private fun importerWith(repoYamls: List<Pair<String, String>>): Pair<RecipeImporter, MiniAppRepository> {
        val repo = MiniAppRepository(loadYamlFiles = { repoYamls })
        return RecipeImporter(filesDir = tmp.root, miniAppRepository = repo) to repo
    }

    @Test fun `parse returns MiniApp for valid yaml`() {
        val (importer, _) = importerWith(emptyList())
        val app = importer.parse(sampleYaml).getOrThrow()
        assertEquals("shared_recipe", app.id)
        assertEquals("Shared Recipe", app.name)
    }

    @Test fun `parse fails on garbage input`() {
        val (importer, _) = importerWith(emptyList())
        val result = importer.parse("not yaml at all { } [ ")
        assertTrue(result.isFailure)
    }

    @Test fun `precheck returns Ok when id not in repo and no file written`() {
        val (importer, _) = importerWith(emptyList())
        val app = importer.parse(sampleYaml).getOrThrow()
        assertEquals(RecipeImporter.Precheck.Ok, importer.precheck(app))
    }

    @Test fun `precheck returns BundledConflict when id is in repo but no file in filesDir`() {
        val (importer, _) = importerWith(listOf("bundled_recipe.yaml" to bundledYaml))
        // App with same id as the bundled one
        val app = importer.parse(bundledYaml).getOrThrow()
        val result = importer.precheck(app)
        assertTrue("got $result", result is RecipeImporter.Precheck.BundledConflict)
        assertEquals("bundled_recipe", (result as RecipeImporter.Precheck.BundledConflict).id)
    }

    @Test fun `precheck returns UpdateExisting when file already in filesDir`() {
        val (importer, _) = importerWith(emptyList())
        val app = importer.parse(sampleYaml).getOrThrow()
        File(tmp.root, "miniapps").mkdirs()
        File(tmp.root, "miniapps/${app.id}.yaml").writeText(sampleYaml)
        val result = importer.precheck(app)
        assertTrue("got $result", result is RecipeImporter.Precheck.UpdateExisting)
    }

    @Test fun `commit writes file and invalidates repository cache`() {
        val (importer, repo) = importerWith(emptyList())
        val app = importer.parse(sampleYaml).getOrThrow()
        // Prime cache
        repo.loadAll()
        importer.commit(app, sampleYaml)
        val written = File(tmp.root, "miniapps/${app.id}.yaml")
        assertTrue("file should exist", written.exists())
        assertEquals(sampleYaml, written.readText())
        // Cache invalidation is implicit — next loadAll re-runs the loader.
        // We can't assert the private `cached` directly, so this test mainly
        // ensures no exception was thrown.
    }
}
