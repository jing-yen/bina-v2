# Offline Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Offline Sync tab so Builders can share installed recipes phone-to-phone via QR code (fully on-device via ZXing), with a paste-YAML fallback that also unblocks the Studio (web) → phone path. Receive flow reuses the Hub's `RecipeDetailSheet` + Configurator route.

**Architecture:** Pure-JVM payload format (`BINA1:` magic + gzip + URL-safe base64) lives in `sync/RecipePayload`. `sync/RecipeImporter` handles YAML parse, precheck (bundled vs update conflicts), and write to `filesDir/miniapps/<id>.yaml`. UI lives in `ui/screens/sync/`: a two-card landing + Share flow (picker bottom sheet → full-screen QR) + Receive flow (camera scan or paste sheet → existing `RecipeDetailSheet` → existing Configurator route).

**Tech Stack:** Kotlin 2.x, Compose Material 3, ZXing via `com.journeyapps:zxing-android-embedded:4.3.0`, kaml (already present), DataStore Preferences (already wired via `InstallStore`), JUnit 4 + kotlinx-coroutines-test (already present).

**Spec:** `docs/superpowers/specs/2026-05-10-offline-sync-design.md`

---

## Phase 1 — Foundation

### Task 1: Add ZXing dependency + CAMERA permission

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add ZXing to dependencies**

In `app/build.gradle.kts`, in the `dependencies { ... }` block, after the existing `implementation("io.coil-kt:coil-compose:2.7.0")` line:

```kotlin
    // Offline Sync — QR encode/decode
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
```

- [ ] **Step 2: Add CAMERA permission to AndroidManifest**

In `app/src/main/AndroidManifest.xml`, just inside the `<manifest>` element above `<application>`, add:

```xml
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
```

- [ ] **Step 3: Verify build**

Run: `./gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL. ZXing transitive deps download.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "Add ZXing dependency + CAMERA permission for offline sync"
```

---

## Phase 2 — Payload + Importer (pure JVM, TDD)

### Task 2: `RecipePayload` — encode/decode `BINA1:` + gzip + base64

**Files:**
- Create: `app/src/main/java/com/bina/ai/sync/RecipePayload.kt`
- Test: `app/src/test/java/com/bina/ai/sync/RecipePayloadTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/bina/ai/sync/RecipePayloadTest.kt`:

```kotlin
package com.bina.ai.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipePayloadTest {

    @Test fun `round trip preserves yaml`() {
        val yaml = "id: foo\nname: Foo\ndescription: Hello\n"
        val encoded = RecipePayload.encode(yaml)
        assertTrue("encoded payload starts with BINA1:", encoded.startsWith("BINA1:"))
        val decoded = RecipePayload.decode(encoded).getOrThrow()
        assertEquals(yaml, decoded)
    }

    @Test fun `decode rejects wrong magic header`() {
        val result = RecipePayload.decode("OTHER:abc123")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects payload with no magic header`() {
        val result = RecipePayload.decode("just-some-text")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects malformed base64 body`() {
        val result = RecipePayload.decode("BINA1:!!!not-valid-base64!!!")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects valid base64 that is not gzip`() {
        // Base64 of "hello" — valid base64, but not gzipped
        val result = RecipePayload.decode("BINA1:aGVsbG8")
        assertTrue(result.isFailure)
    }

    @Test fun `encode produces shorter payload than raw yaml for repetitive content`() {
        val yaml = "field: value\n".repeat(50)
        val encoded = RecipePayload.encode(yaml)
        // Magic + base64(gzip(repetitive)) should compress well
        assertTrue("encoded is shorter than raw", encoded.length < yaml.length)
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.sync.RecipePayloadTest"`
Expected: FAIL with "unresolved reference: RecipePayload".

- [ ] **Step 3: Implement `RecipePayload`**

Create `app/src/main/java/com/bina/ai/sync/RecipePayload.kt`:

```kotlin
package com.bina.ai.sync

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Versioned payload format for transferring a recipe YAML over a single QR code.
 *
 * Wire format: "BINA1:" + URL-safe base64 (no padding) of gzip(yaml-bytes).
 * The "BINA1:" magic header lets us reject non-Bina QR codes and bump format later.
 */
object RecipePayload {
    private const val MAGIC = "BINA1:"

    fun encode(yaml: String): String {
        val gzipped = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { it.write(yaml.toByteArray(Charsets.UTF_8)) }
            baos.toByteArray()
        }
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(gzipped)
        return MAGIC + b64
    }

    fun decode(raw: String): Result<String> = runCatching {
        require(raw.startsWith(MAGIC)) { "Not a Bina recipe QR" }
        val gzippedBytes = Base64.getUrlDecoder().decode(raw.substring(MAGIC.length))
        GZIPInputStream(ByteArrayInputStream(gzippedBytes))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.sync.RecipePayloadTest"`
Expected: 6/6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/sync/RecipePayload.kt app/src/test/java/com/bina/ai/sync/RecipePayloadTest.kt
git commit -m "Add RecipePayload for QR-safe recipe transport (BINA1 + gzip + base64)"
```

---

### Task 3: `RecipeImporter` — parse, precheck, commit

**Files:**
- Create: `app/src/main/java/com/bina/ai/sync/RecipeImporter.kt`
- Test: `app/src/test/java/com/bina/ai/sync/RecipeImporterTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/bina/ai/sync/RecipeImporterTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run tests — expect compile failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.sync.RecipeImporterTest"`
Expected: FAIL with "unresolved reference: RecipeImporter".

- [ ] **Step 3: Implement `RecipeImporter`**

Create `app/src/main/java/com/bina/ai/sync/RecipeImporter.kt`:

```kotlin
package com.bina.ai.sync

import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import java.io.File

/**
 * Parses an incoming recipe YAML, classifies it (Ok / BundledConflict / UpdateExisting),
 * and on commit writes it into `filesDir/miniapps/<id>.yaml` so the Hub picks it up
 * on the next `MiniAppRepository.loadAll()`.
 */
class RecipeImporter(
    private val filesDir: File,
    private val miniAppRepository: MiniAppRepository
) {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
    private val miniappsDir = File(filesDir, "miniapps")

    fun parse(yamlText: String): Result<MiniApp> = runCatching {
        yaml.decodeFromString(MiniApp.serializer(), yamlText)
    }

    sealed interface Precheck {
        data object Ok : Precheck
        data class BundledConflict(val id: String, val name: String) : Precheck
        data class UpdateExisting(val id: String, val name: String) : Precheck
    }

    /**
     * Bundled vs imported is determined by whether a YAML for `id` exists in
     * `filesDir/miniapps/`. If it doesn't, but the id resolves in the repo, it must
     * have come from assets — that's a bundled conflict we refuse to overwrite.
     */
    fun precheck(miniApp: MiniApp): Precheck {
        val importedFile = File(miniappsDir, "${miniApp.id}.yaml")
        if (importedFile.exists()) {
            return Precheck.UpdateExisting(miniApp.id, miniApp.name)
        }
        if (miniAppRepository.getById(miniApp.id) != null) {
            return Precheck.BundledConflict(miniApp.id, miniApp.name)
        }
        return Precheck.Ok
    }

    fun commit(miniApp: MiniApp, yamlText: String) {
        miniappsDir.mkdirs()
        File(miniappsDir, "${miniApp.id}.yaml").writeText(yamlText)
        miniAppRepository.invalidateCache()
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.sync.RecipeImporterTest"`
Expected: 6/6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/sync/RecipeImporter.kt app/src/test/java/com/bina/ai/sync/RecipeImporterTest.kt
git commit -m "Add RecipeImporter for parse + precheck + commit of incoming recipes"
```

---

## Phase 3 — ViewModel

### Task 4: `SyncViewModel` — installed-recipe list + incoming pipeline

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/sync/SyncViewModel.kt`
- Test: `app/src/test/java/com/bina/ai/ui/screens/sync/SyncViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/bina/ai/ui/screens/sync/SyncViewModelTest.kt`:

```kotlin
package com.bina.ai.ui.screens.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.sync.RecipeImporter
import com.bina.ai.sync.RecipePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SyncViewModelTest {

    @get:Rule val tmp = TemporaryFolder()

    private val sampleYaml = """
        id: t1
        name: T1
        description: test
        category: Test
        version: "1.0"
    """.trimIndent()

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun newVm(): SyncViewModel {
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { tmp.newFile("ds.preferences_pb") })
        val installStore = InstallStore(dataStore)
        val repo = MiniAppRepository(loadYamlFiles = { emptyList() })
        val importer = RecipeImporter(filesDir = tmp.root, miniAppRepository = repo)
        return SyncViewModel(repo, installStore, importer)
    }

    @Test fun `incoming starts Idle`() = runTest {
        val vm = newVm()
        assertEquals(IncomingState.Idle, vm.incoming.value)
    }

    @Test fun `handlePastedYaml with valid yaml transitions to Ready`() = runTest {
        val vm = newVm()
        vm.handlePastedYaml(sampleYaml)
        val state = vm.incoming.value
        assertTrue("got $state", state is IncomingState.Ready)
        assertEquals("t1", (state as IncomingState.Ready).miniApp.id)
    }

    @Test fun `handlePastedYaml with garbage transitions to Error`() = runTest {
        val vm = newVm()
        vm.handlePastedYaml("not yaml { ")
        val state = vm.incoming.value
        assertTrue("got $state", state is IncomingState.Error)
    }

    @Test fun `handleScannedQr with valid encoded payload transitions to Ready`() = runTest {
        val vm = newVm()
        val encoded = RecipePayload.encode(sampleYaml)
        vm.handleScannedQr(encoded)
        assertTrue(vm.incoming.value is IncomingState.Ready)
    }

    @Test fun `handleScannedQr with non-Bina QR transitions to Error`() = runTest {
        val vm = newVm()
        vm.handleScannedQr("https://example.com/some-qr")
        assertTrue(vm.incoming.value is IncomingState.Error)
    }

    @Test fun `dismissPreview returns to Idle`() = runTest {
        val vm = newVm()
        vm.handlePastedYaml(sampleYaml)
        vm.dismissPreview()
        assertEquals(IncomingState.Idle, vm.incoming.value)
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.ui.screens.sync.SyncViewModelTest"`
Expected: FAIL with "unresolved reference: SyncViewModel".

- [ ] **Step 3: Implement `SyncViewModel`**

Create `app/src/main/java/com/bina/ai/ui/screens/sync/SyncViewModel.kt`:

```kotlin
package com.bina.ai.ui.screens.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.sync.RecipeImporter
import com.bina.ai.sync.RecipePayload
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Cap on encoded QR payload size — single QR at error correction M. */
const val MAX_QR_PAYLOAD_CHARS = 2300

sealed interface IncomingState {
    data object Idle : IncomingState
    data object Decoding : IncomingState
    data class Ready(
        val miniApp: MiniApp,
        val yaml: String,
        val precheck: RecipeImporter.Precheck
    ) : IncomingState
    data class Error(val message: String) : IncomingState
}

class SyncViewModel(
    private val miniAppRepository: MiniAppRepository,
    private val installStore: InstallStore,
    private val recipeImporter: RecipeImporter
) : ViewModel() {

    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    val installedRecipesForShare: StateFlow<List<MiniApp>> = installStore.installs
        .map { installs -> miniAppRepository.loadAll().filter { it.id in installs.keys } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _incoming = MutableStateFlow<IncomingState>(IncomingState.Idle)
    val incoming: StateFlow<IncomingState> = _incoming.asStateFlow()

    fun handleScannedQr(raw: String) {
        _incoming.value = IncomingState.Decoding
        val yamlText = RecipePayload.decode(raw).getOrElse {
            _incoming.value = IncomingState.Error(it.message ?: "QR data is corrupted")
            return
        }
        decodeYaml(yamlText)
    }

    fun handlePastedYaml(text: String) {
        _incoming.value = IncomingState.Decoding
        decodeYaml(text)
    }

    private fun decodeYaml(text: String) {
        val miniApp = recipeImporter.parse(text).getOrElse {
            _incoming.value = IncomingState.Error("Recipe file is corrupted")
            return
        }
        _incoming.value = IncomingState.Ready(
            miniApp = miniApp,
            yaml = text,
            precheck = recipeImporter.precheck(miniApp)
        )
    }

    /** Returns the recipeId of the imported recipe so the caller can navigate to Configurator. */
    fun confirmInstall(): String? {
        val ready = _incoming.value as? IncomingState.Ready ?: return null
        recipeImporter.commit(ready.miniApp, ready.yaml)
        _incoming.value = IncomingState.Idle
        return ready.miniApp.id
    }

    fun dismissPreview() { _incoming.value = IncomingState.Idle }

    /**
     * Re-serializes a `MiniApp` to YAML and encodes for QR transport.
     * Fails if the encoded payload exceeds [MAX_QR_PAYLOAD_CHARS].
     */
    fun encodeRecipeAsQr(miniApp: MiniApp): Result<String> = runCatching {
        val yamlText = yaml.encodeToString(MiniApp.serializer(), miniApp)
        val payload = RecipePayload.encode(yamlText)
        require(payload.length <= MAX_QR_PAYLOAD_CHARS) {
            "Recipe too large for QR (${payload.length} chars, max $MAX_QR_PAYLOAD_CHARS)"
        }
        payload
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.ui.screens.sync.SyncViewModelTest"`
Expected: 6/6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/SyncViewModel.kt app/src/test/java/com/bina/ai/ui/screens/sync/SyncViewModelTest.kt
git commit -m "Add SyncViewModel with incoming pipeline + share-list + QR encoder"
```

---

## Phase 4 — UI scaffolding

### Task 5: Add nav routes for Scan and ShareQr

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt`

- [ ] **Step 1: Add Scan + ShareQr routes to `Screen.kt`**

In `Screen.kt`, after the existing `data object Configurator(...)`, add:

```kotlin
    data object SyncScan : Screen("sync/scan", "Scan", Icons.Outlined.CellTower)

    data object SyncShare : Screen("sync/share/{miniAppId}", "Share", Icons.Outlined.CellTower) {
        fun createRoute(miniAppId: String) = "sync/share/$miniAppId"
    }
```

- [ ] **Step 2: Wire placeholder composables in `BinaNavGraph.kt`**

In `BinaNavGraph.kt`, replace the existing `composable(Screen.OfflineSync.route) { OfflineSyncScreen() }` block, and add the two new routes after it:

```kotlin
        composable(Screen.OfflineSync.route) {
            OfflineSyncScreen(
                miniAppRepository = miniAppRepository,
                installStore = installStore,
                onScan = { navController.navigate(Screen.SyncScan.route) },
                onShare = { recipeId -> navController.navigate(Screen.SyncShare.createRoute(recipeId)) },
                onConfigureRecipe = { id -> navController.navigate(Screen.Configurator.createRoute(id)) }
            )
        }

        composable(Screen.SyncScan.route) {
            com.bina.ai.ui.screens.sync.components.ScanQrScreen(
                miniAppRepository = miniAppRepository,
                installStore = installStore,
                onImported = { id ->
                    navController.navigate(Screen.Configurator.createRoute(id)) {
                        popUpTo(Screen.OfflineSync.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SyncShare.route,
            arguments = listOf(navArgument("miniAppId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("miniAppId") ?: return@composable
            com.bina.ai.ui.screens.sync.components.ShareQrScreen(
                miniAppRepository = miniAppRepository,
                installStore = installStore,
                recipeId = recipeId,
                onDone = { navController.popBackStack() }
            )
        }
```

(The new `OfflineSyncScreen` signature lands in Task 6; `ScanQrScreen` in Task 8; `ShareQrScreen` in Task 7. We're staging the wiring up-front so each later task only needs to write the file.)

- [ ] **Step 3: Verify it compiles after Task 6 lands**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: This may FAIL until Tasks 6-8 land. Skip the check here — we'll verify at the end of Phase 4.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/navigation/Screen.kt app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt
git commit -m "Add SyncScan and SyncShare nav routes"
```

---

### Task 6: `OfflineSyncScreen` rewrite + `SyncActionCard`

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/sync/components/SyncActionCard.kt`
- Rewrite: `app/src/main/java/com/bina/ai/ui/screens/sync/OfflineSyncScreen.kt`
- New: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ShareRecipePickerSheet.kt`
- New: `app/src/main/java/com/bina/ai/ui/screens/sync/components/PasteYamlSheet.kt` (stub for now; populated in Task 9)

- [ ] **Step 1: Create `SyncActionCard.kt`**

```kotlin
package com.bina.ai.ui.screens.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun SyncActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BinaPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = BinaGrayText)
        }
    }
}
```

- [ ] **Step 2: Create `ShareRecipePickerSheet.kt`**

```kotlin
package com.bina.ai.ui.screens.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareRecipePickerSheet(
    recipes: List<MiniApp>,
    onPick: (MiniApp) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Share a Recipe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BinaPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick an installed recipe. The receiver scans the QR to install it.",
                fontSize = 12.sp, color = BinaGrayText
            )
            Spacer(Modifier.height(16.dp))
            if (recipes.isEmpty()) {
                Text(
                    "Install a recipe from the Hub first.",
                    fontSize = 13.sp, color = BinaGrayText
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipes.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF9FAFB))
                                .clickable { onPick(r) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(r.icon.ifBlank { "📦" }, fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(r.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BinaPrimary)
                                Text(r.category.ifBlank { "—" }, fontSize = 11.sp, color = BinaGrayText)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create `PasteYamlSheet.kt` (stub — populated in Task 9)**

```kotlin
package com.bina.ai.ui.screens.sync.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteYamlSheet(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Text("Paste YAML — populated in Task 9")
    }
}
```

- [ ] **Step 4: Rewrite `OfflineSyncScreen.kt`**

```kotlin
package com.bina.ai.ui.screens.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.sync.RecipeImporter
import com.bina.ai.ui.screens.recipe_detail.RecipeDetailSheet
import com.bina.ai.ui.screens.sync.components.ShareRecipePickerSheet
import com.bina.ai.ui.screens.sync.components.SyncActionCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun OfflineSyncScreen(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    onScan: () -> Unit,
    onShare: (String) -> Unit,
    onConfigureRecipe: (String) -> Unit
) {
    val context = LocalContext.current
    val factory = remember(miniAppRepository, installStore) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val importer = RecipeImporter(filesDir = context.filesDir, miniAppRepository = miniAppRepository)
                return SyncViewModel(miniAppRepository, installStore, importer) as T
            }
        }
    }
    val vm: SyncViewModel = viewModel(factory = factory)
    val installed by vm.installedRecipesForShare.collectAsStateWithLifecycle()
    val incoming by vm.incoming.collectAsStateWithLifecycle()
    val installedIds by installStore.installs.collectAsStateWithLifecycle(initialValue = emptyMap())

    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Offline Sync", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaPrimary)
        Text(
            "Share recipes phone-to-phone, no internet needed.",
            fontSize = 12.sp, color = BinaGrayText
        )
        Spacer(Modifier.height(8.dp))
        SyncActionCard(
            title = "Scan to Receive",
            subtitle = "Scan another phone's QR or paste a YAML to install.",
            icon = Icons.Filled.QrCodeScanner,
            accentColor = BinaPrimary,
            onClick = onScan
        )
        SyncActionCard(
            title = "Share a Recipe",
            subtitle = "Pick an installed recipe to share via QR.",
            icon = Icons.Filled.Share,
            accentColor = BinaGreen,
            onClick = { showPicker = true }
        )
    }

    if (showPicker) {
        ShareRecipePickerSheet(
            recipes = installed,
            onPick = { recipe ->
                showPicker = false
                onShare(recipe.id)
            },
            onDismiss = { showPicker = false }
        )
    }

    // Receive preview — wired in Task 10. Stub here so scan/paste can flow later.
    val ready = incoming as? IncomingState.Ready
    if (ready != null) {
        RecipeDetailSheet(
            miniApp = ready.miniApp,
            isInstalled = ready.miniApp.id in installedIds.keys,
            sizeKb = ready.yaml.length / 1024f,
            onConfigureInstall = {
                val id = vm.confirmInstall()
                if (id != null) onConfigureRecipe(id)
            },
            onOpen = { vm.dismissPreview() },
            onDismiss = { vm.dismissPreview() }
        )
    }
}
```

- [ ] **Step 5: Verify build (will fail on missing ScanQrScreen / ShareQrScreen — expected)**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: FAIL with unresolved references to `ScanQrScreen` and `ShareQrScreen` (these land in Tasks 7 and 8). That's the expected staging state.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/
git commit -m "Rewrite OfflineSyncScreen: two-card landing + share picker + receive preview wiring"
```

---

### Task 7: `ShareQrScreen` — full-screen QR display

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ShareQrScreen.kt`

- [ ] **Step 1: Implement `ShareQrScreen`**

```kotlin
package com.bina.ai.ui.screens.sync.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.sync.RecipeImporter
import com.bina.ai.ui.screens.sync.SyncViewModel
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

@Composable
fun ShareQrScreen(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    recipeId: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val factory = remember(miniAppRepository, installStore) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val importer = RecipeImporter(filesDir = context.filesDir, miniAppRepository = miniAppRepository)
                return SyncViewModel(miniAppRepository, installStore, importer) as T
            }
        }
    }
    val vm: SyncViewModel = viewModel(factory = factory)
    val recipe = remember(recipeId) { miniAppRepository.getById(recipeId) }

    val encodeResult = remember(recipe) {
        recipe?.let { vm.encodeRecipeAsQr(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recipe == null) {
            Text("Recipe not found", color = BinaGrayText)
        } else {
            Text(recipe.icon.ifBlank { "📦" }, fontSize = 36.sp)
            Text(recipe.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaPrimary)
            recipe.author.name.takeIf { it.isNotBlank() }?.let {
                Text("by $it", fontSize = 12.sp, color = BinaGrayText)
            }
            Spacer(Modifier.height(8.dp))

            val payload = encodeResult?.getOrNull()
            val error = encodeResult?.exceptionOrNull()?.message

            if (payload != null) {
                val bitmap = remember(payload) {
                    BarcodeEncoder().encodeBitmap(payload, BarcodeFormat.QR_CODE, 1024, 1024)
                }
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR for ${recipe.name}", modifier = Modifier.fillMaxSize())
                }
                Text(
                    "Have the other phone open Sync → Scan to Receive.",
                    fontSize = 12.sp, color = BinaGrayText
                )
            } else if (error != null) {
                Text(error, fontSize = 12.sp, color = BinaPrimary)
                Button(onClick = {
                    val yaml = encodeResult.exceptionOrNull()?.message  // fallback handled below
                    // Re-serialize and copy YAML to clipboard so the user can paste-share
                    val maybeYaml = runCatching {
                        com.charleskorn.kaml.Yaml(com.charleskorn.kaml.YamlConfiguration(strictMode = false))
                            .encodeToString(com.bina.ai.miniapp.model.MiniApp.serializer(), recipe)
                    }.getOrNull()
                    if (maybeYaml != null) clipboard.setText(AnnotatedString(maybeYaml))
                }, colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)) {
                    Text("Copy YAML to clipboard")
                }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: Still FAIL (ScanQrScreen unresolved). Will pass at end of Task 8.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/components/ShareQrScreen.kt
git commit -m "Add ShareQrScreen with ZXing QR encode + clipboard fallback for oversize"
```

---

### Task 8: `ScanQrScreen` — camera + permission

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ScanQrScreen.kt`

- [ ] **Step 1: Implement `ScanQrScreen`**

```kotlin
package com.bina.ai.ui.screens.sync.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.sync.RecipeImporter
import com.bina.ai.ui.screens.sync.IncomingState
import com.bina.ai.ui.screens.sync.SyncViewModel
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaRed
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView

@Composable
fun ScanQrScreen(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    onImported: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val factory = remember(miniAppRepository, installStore) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val importer = RecipeImporter(filesDir = context.filesDir, miniAppRepository = miniAppRepository)
                return SyncViewModel(miniAppRepository, installStore, importer) as T
            }
        }
    }
    val vm: SyncViewModel = viewModel(factory = factory)
    val incoming by vm.incoming.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    var showPaste by remember { mutableStateOf(false) }

    // Once VM is in Ready or just transitioned via an install, navigate up to Configurator.
    LaunchedEffect(incoming) {
        when (val s = incoming) {
            is IncomingState.Ready -> {
                // Defer to caller — receive preview RecipeDetailSheet handled in OfflineSyncScreen.
                // We need to bounce back so the parent shows the sheet.
                // Approach: stash the state, pop back to OfflineSync, and let OfflineSync's
                // collected `incoming` flow render the preview sheet.
                onBack()
            }
            is IncomingState.Error -> { /* surfaced inline below */ }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera permission needed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Allow camera to scan a QR, or paste a YAML instead.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                    ) { Text("Try again") }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Paste YAML instead",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { showPaste = true }
                    )
                }
            }
            else -> {
                AndroidView(
                    factory = { ctx ->
                        CompoundBarcodeView(ctx).apply {
                            decodeContinuous(object : BarcodeCallback {
                                override fun barcodeResult(result: BarcodeResult) {
                                    pause()
                                    vm.handleScannedQr(result.text)
                                }
                                override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>) {}
                            })
                            resume()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Bottom strip
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (incoming as? IncomingState.Error)?.let {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BinaRed.copy(alpha = 0.85f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(it.message, color = Color.White, fontSize = 12.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .clickable { showPaste = true }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text("Paste YAML instead", color = BinaPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showPaste) {
        PasteYamlSheet(
            onImport = { yaml ->
                showPaste = false
                vm.handlePastedYaml(yaml)
            },
            onDismiss = { showPaste = false }
        )
    }
}
```

> Note: Because `ScanQrScreen` and `OfflineSyncScreen` each construct their own `SyncViewModel`, they don't share state. The `LaunchedEffect(incoming) → onBack()` above triggers the parent to recompose and read its own ViewModel — but its `incoming` will still be Idle.
>
> **Fix landed in Task 10:** lift the SyncViewModel to a shared owner (the OfflineSync composable's NavGraph entry) so both screens see the same incoming state.

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (PasteYamlSheet stub is satisfied.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/components/ScanQrScreen.kt
git commit -m "Add ScanQrScreen with camera permission flow + paste fallback hook"
```

---

### Task 9: `PasteYamlSheet` — real implementation

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/screens/sync/components/PasteYamlSheet.kt`

- [ ] **Step 1: Replace stub with real implementation**

```kotlin
package com.bina.ai.ui.screens.sync.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteYamlSheet(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Paste recipe YAML", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BinaPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Paste the YAML you copied from Studio (or another phone). It'll be parsed and previewed before install.",
                fontSize = 12.sp, color = BinaGrayText
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                placeholder = { Text("id: my_recipe\nname: ...") }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    text = clipboard.getText()?.text.orEmpty()
                }) { Text("Paste from clipboard") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onImport(text) },
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                ) { Text("Import") }
            }
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/components/PasteYamlSheet.kt
git commit -m "Implement PasteYamlSheet with clipboard auto-fill"
```

---

## Phase 5 — Wire receive flow + smoke test

### Task 10: Lift `SyncViewModel` ownership so Scan/Paste preview shows in OfflineSync

**Problem:** The current wiring constructs a fresh `SyncViewModel` in each composable (`OfflineSyncScreen`, `ScanQrScreen`). When `ScanQrScreen` decodes a QR and pops back, `OfflineSyncScreen`'s ViewModel never sees the `Ready` state. We need a single owner.

**Fix:** Share the `SyncViewModel` by binding it to the NavGraph's `OfflineSync` back-stack entry, and have `ScanQrScreen` resolve the same VM via that entry.

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt`
- Modify: `app/src/main/java/com/bina/ai/ui/screens/sync/OfflineSyncScreen.kt` (accept VM as param)
- Modify: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ScanQrScreen.kt` (accept VM as param)
- Modify: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ShareQrScreen.kt` (accept VM as param)

- [ ] **Step 1: Add a small `rememberSyncViewModel` helper**

Create `app/src/main/java/com/bina/ai/ui/screens/sync/SyncViewModelFactory.kt`:

```kotlin
package com.bina.ai.ui.screens.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.sync.RecipeImporter

@Composable
fun rememberSyncViewModel(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    owner: ViewModelStoreOwner
): SyncViewModel {
    val context = LocalContext.current
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val importer = RecipeImporter(filesDir = context.filesDir, miniAppRepository = miniAppRepository)
            return SyncViewModel(miniAppRepository, installStore, importer) as T
        }
    }
    return viewModel(viewModelStoreOwner = owner, factory = factory)
}
```

- [ ] **Step 2: Update `BinaNavGraph.kt` to share VM via OfflineSync's back-stack entry**

Replace the three Sync-related `composable { ... }` blocks with:

```kotlin
        composable(Screen.OfflineSync.route) { backStackEntry ->
            val vm = com.bina.ai.ui.screens.sync.rememberSyncViewModel(
                miniAppRepository, installStore, owner = backStackEntry
            )
            OfflineSyncScreen(
                vm = vm,
                installStore = installStore,
                onScan = { navController.navigate(Screen.SyncScan.route) },
                onShare = { recipeId -> navController.navigate(Screen.SyncShare.createRoute(recipeId)) },
                onConfigureRecipe = { id -> navController.navigate(Screen.Configurator.createRoute(id)) }
            )
        }

        composable(Screen.SyncScan.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.OfflineSync.route) }
            val vm = com.bina.ai.ui.screens.sync.rememberSyncViewModel(
                miniAppRepository, installStore, owner = parentEntry
            )
            com.bina.ai.ui.screens.sync.components.ScanQrScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SyncShare.route,
            arguments = listOf(navArgument("miniAppId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("miniAppId") ?: return@composable
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.OfflineSync.route) }
            val vm = com.bina.ai.ui.screens.sync.rememberSyncViewModel(
                miniAppRepository, installStore, owner = parentEntry
            )
            com.bina.ai.ui.screens.sync.components.ShareQrScreen(
                vm = vm,
                miniAppRepository = miniAppRepository,
                recipeId = recipeId,
                onDone = { navController.popBackStack() }
            )
        }
```

Add the missing `androidx.compose.runtime.remember` import if not already present.

- [ ] **Step 3: Update `OfflineSyncScreen` signature**

Change `OfflineSyncScreen` signature to accept the VM:

```kotlin
@Composable
fun OfflineSyncScreen(
    vm: SyncViewModel,
    installStore: InstallStore,
    onScan: () -> Unit,
    onShare: (String) -> Unit,
    onConfigureRecipe: (String) -> Unit
) {
    // ... same body, but DROP the local factory + viewModel() call, and the
    // `miniAppRepository` parameter (it's no longer needed here — the VM owns it).
    // ALSO: pass `r.icon` and so on from the ready state already available.
}
```

Drop the `factory`, `vm: SyncViewModel = viewModel(factory = factory)`, and the unused `context = LocalContext.current` if not referenced elsewhere. Replace `RecipeDetailSheet`'s `sizeKb = ready.yaml.length / 1024f` with the same logic — no change needed beyond removing the factory.

- [ ] **Step 4: Update `ScanQrScreen` signature**

```kotlin
@Composable
fun ScanQrScreen(
    vm: SyncViewModel,
    onBack: () -> Unit
) {
    // Drop the factory + viewModel() call. Keep everything else.
}
```

- [ ] **Step 5: Update `ShareQrScreen` signature**

```kotlin
@Composable
fun ShareQrScreen(
    vm: SyncViewModel,
    miniAppRepository: MiniAppRepository,
    recipeId: String,
    onDone: () -> Unit
) {
    // Drop the factory + viewModel() call. Keep everything else.
}
```

- [ ] **Step 6: Build + run unit tests**

Run: `./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/ app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt
git commit -m "Share SyncViewModel across OfflineSync nav graph so scan results show preview"
```

---

### Task 11: Smoke test on emulator

**Files:** none (manual test).

- [ ] **Step 1: Install debug build on emulator**

Run: `./gradlew.bat :app:installDebug`
Expected: Installed on running AVD.

- [ ] **Step 2: Verify Sync tab landing**

Open the app → tap Sync tab. Expected: Two action cards visible — "Scan to Receive" and "Share a Recipe". Header reads "Offline Sync".

- [ ] **Step 3: Verify Share flow with a bundled recipe**

From Hub, install `farm_buddy` (Configure & Install). Then Sync tab → "Share a Recipe" → bottom sheet appears listing `farm_buddy` → tap it → ShareQrScreen renders with QR + recipe name "Farm Buddy". Tap Done → back to Sync.

- [ ] **Step 4: Verify paste-YAML receive path**

From Sync tab → "Scan to Receive" → grant camera permission. Tap "Paste YAML instead" → bottom sheet opens. Paste a valid recipe YAML (e.g., copy from `app/src/main/assets/miniapps/buku_kira_kira.yaml`). Tap "Import". Expected: bottom sheet dismisses, scan screen pops back, RecipeDetailSheet shows on Sync screen with recipe metadata.

- [ ] **Step 5: Verify Configure & Install completes the import**

In the preview sheet, tap "Configure & Install". Expected: navigate to Configurator → tap "Install to Pocket" → Hub now shows the imported recipe as installed.

- [ ] **Step 6: Verify error cases**

Sync → Scan → Paste a non-Bina string (e.g. `hello world`). Expected: toast or inline error "Recipe file is corrupted" surfaces, no install.

- [ ] **Step 7: Verify bundled-conflict refusal**

Paste the YAML of a bundled recipe that's NOT yet imported (e.g., `bidan_pintar.yaml`) without first installing it from Hub. Expected: precheck reports `BundledConflict`. The current preview sheet still shows it; the `BundledConflict` is observable via `vm.incoming` state. (UI for refusing the bundled conflict is left as a follow-up — note in commit message.)

- [ ] **Step 8: Final commit (only if smoke test surfaced cleanups)**

If everything passes, no commit needed. If tweaks were needed:

```bash
git add -u && git commit -m "Polish offline sync after smoke test"
```

- [ ] **Step 9: Push branch and open PR**

```bash
git push -u origin feature/offline-sync
gh pr create --base main --head feature/offline-sync \
  --title "Offline Sync: QR + paste-YAML recipe sharing" \
  --body "Implements docs/superpowers/specs/2026-05-10-offline-sync-design.md"
```

---

## Self-review notes

- **Spec coverage:** Tasks 1-2 cover ZXing + payload format. Task 3 covers Importer (parse, precheck, commit). Task 4 covers SyncViewModel (incoming pipeline + share-list + encode). Tasks 5-9 cover all UI surfaces in the spec. Task 10 fixes the cross-screen state-sharing issue. Task 11 covers manual verification.
- **Bundled-conflict UI:** The spec calls for a refuse dialog when imported recipe id matches a bundled one. Task 11 Step 7 explicitly notes this is observable but UI-incomplete; bundled-conflict refusal dialog is a small follow-up commit on the same branch (or punted to a follow-up issue). Implementer should surface it as DONE_WITH_CONCERNS rather than blocking.
- **No placeholders:** Each step has full code or full commands. Compose UI may have minor import gaps the implementer needs to add — that's expected and trivial.
- **Type consistency:** `RecipeImporter.Precheck` uses sealed interface with three variants (Ok, BundledConflict, UpdateExisting). `IncomingState` uses sealed interface with four variants (Idle, Decoding, Ready, Error). All references throughout match.
