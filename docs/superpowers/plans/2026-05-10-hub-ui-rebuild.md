# Hub UI Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Hub screen with a polished Spotify-Hybrid layout (carousel + adaptive section rails), add a real install flow (detail sheet → full-screen Configurator → Install to Pocket), persist install state via DataStore, and minimum-touch MyPocket so it filters to installed recipes only.

**Architecture:** Compose Material 3 UI on top of an `InstallStore` (DataStore Preferences holding JSON-encoded `Map<recipeId, InstallRecord>`). `MiniApp` schema gains a `features: List<Feature>` field with `Feature.requires` capability tokens that drive toggle availability. Customization is one-shot at install time; MyPocket is a pure launchpad.

**Tech Stack:** Kotlin 2.x, Compose Material 3, Compose BOM 2026.04.01, kotlinx-serialization, charleskorn/kaml, DataStore Preferences 1.1.1, Coil 2.7.0, JUnit 4 + kotlinx-coroutines-test for unit tests.

**Spec:** `docs/superpowers/specs/2026-05-10-hub-ui-rebuild-design.md`

---

## Phase 1 — Foundation

### Task 1: Add dependencies + test source set

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add dependencies block**

In `app/build.gradle.kts` `dependencies { … }` block, add these lines after the existing `implementation(...)` lines:

```kotlin
    // Hub UI rebuild
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
```

- [ ] **Step 2: Sync Gradle and verify build**

Run: `./gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL. New deps download.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "Add DataStore, Coil, and unit-test dependencies for Hub rebuild"
```

---

### Task 2: Add `Feature` class and `MiniApp.features` field

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/bina/ai/miniapp/model/MiniApp.kt`

- [ ] **Step 1: Add the Feature data class**

In `MiniApp.kt`, after the existing `data class GridButton(...)` (the last class in the file), add:

```kotlin
@Serializable
data class Feature(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String = "",
    val recommended: Boolean = false,
    @SerialName("size_kb") val sizeKb: Float = 0f,
    val requires: List<String> = emptyList()
)
```

- [ ] **Step 2: Add `features` field to MiniApp**

In `MiniApp.kt`, find the existing `data class MiniApp(...)` declaration. Add a new field after `tags`:

```kotlin
    val tags: List<String> = emptyList(),
    val features: List<Feature> = emptyList(),    // <-- new line
    val author: Author = Author(),
```

- [ ] **Step 3: Build to verify schema compiles**

Run: `./gradlew.bat :shared:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/bina/ai/miniapp/model/MiniApp.kt
git commit -m "Add Feature class and MiniApp.features for install-flow toggles"
```

---

### Task 3: Add `features:` block to all bundled YAMLs

**Files:**
- Modify: `app/src/main/assets/miniapps/farm_buddy.yaml`
- Modify: `app/src/main/assets/miniapps/bidan_pintar.yaml`
- Modify: `app/src/main/assets/miniapps/buku_kira_kira.yaml`

- [ ] **Step 1: Update farm_buddy.yaml**

Append to the END of `farm_buddy.yaml` (after the existing `permissions:` block):

```yaml
features:
  - { id: camera_scanner,  name: "Camera Scanner",   icon: camera,        recommended: true,  size_kb: 0.4, requires: [permission:camera],            description: "Allows the AI to see crop diseases" }
  - { id: voice_assistant, name: "Voice Assistant",  icon: mic,           recommended: true,  size_kb: 0.3, requires: [permission:microphone],        description: "Allows you to speak your questions" }
  - { id: gps_tracker,     name: "GPS Tracker",      icon: gps_fixed,     recommended: true,  size_kb: 0.2, requires: [permission:location],          description: "Saves the location of inspected fields" }
  - { id: offline_storage, name: "Offline Storage",  icon: storage,       recommended: true,  size_kb: 0.3, requires: [],                              description: "Stores history for later review" }
  - { id: sms_dispatcher,  name: "SMS Dispatcher",   icon: sms,           recommended: false, size_kb: 0.2, requires: [service:sms_dispatch],          description: "Texts the nearest agro shop" }
  - { id: p2p_sharing,     name: "P2P Recipe Share", icon: share,         recommended: false, size_kb: 0.2, requires: [service:p2p],                   description: "Shares recipe with neighbours" }
  - { id: smart_notif,     name: "Smart Notifications", icon: notifications, recommended: false, size_kb: 0.1, requires: [service:smart_notifications], description: "Alerts for important updates" }
```

- [ ] **Step 2: Update bidan_pintar.yaml**

Append to the END of `bidan_pintar.yaml`:

```yaml
features:
  - { id: voice_assistant,     name: "Voice Assistant",  icon: mic,        recommended: true,  size_kb: 0.3, requires: [permission:microphone], description: "Allows midwives to speak case details" }
  - { id: offline_storage,     name: "Offline Storage",  icon: storage,    recommended: true,  size_kb: 0.3, requires: [],                       description: "Stores patient history offline" }
  - { id: emergency_protocol,  name: "Emergency Protocol", icon: warning,  recommended: true,  size_kb: 0.2, requires: [],                       description: "Always-on referral guidance for danger signs" }
  - { id: smart_notif,         name: "Smart Notifications", icon: notifications, recommended: false, size_kb: 0.1, requires: [service:smart_notifications], description: "Reminders for follow-up visits" }
```

- [ ] **Step 3: Update buku_kira_kira.yaml — feature block + theme color**

In `buku_kira_kira.yaml`, find the existing `theme:` block and change `primary` from `#047857` to `#0EA5E9`:

```yaml
theme:
  primary: "#0EA5E9"
  secondary: "#34D399"
  text_size: standard
```

Append to the END of the file:

```yaml
features:
  - { id: camera_scanner,  name: "Receipt Scanner",  icon: camera,        recommended: true,  size_kb: 0.4, requires: [permission:camera],     description: "Snap receipts and extract amounts" }
  - { id: voice_assistant, name: "Voice Assistant",  icon: mic,           recommended: true,  size_kb: 0.3, requires: [permission:microphone], description: "Speak entries instead of typing" }
  - { id: offline_storage, name: "Offline Storage",  icon: storage,       recommended: true,  size_kb: 0.3, requires: [],                       description: "Keeps daily ledger between sessions" }
  - { id: smart_notif,     name: "Smart Notifications", icon: notifications, recommended: false, size_kb: 0.1, requires: [service:smart_notifications], description: "Daily-close reminders" }
```

- [ ] **Step 4: Build + parse-verify**

Run: `./gradlew.bat installDebug`
Expected: BUILD SUCCESSFUL.

Then launch app and check logcat:
```
$adb = 'D:\Android\Sdk\platform-tools\adb.exe'
& $adb shell am force-stop com.bina.ai
& $adb logcat -c
& $adb shell am start -n com.bina.ai/.MainActivity
Start-Sleep -Seconds 4
& $adb logcat -d -s MiniAppRepo:* | Select-Object -Last 5
```
Expected output: 3 "Loaded miniapp" lines, no "Failed to parse" lines.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/miniapps/farm_buddy.yaml app/src/main/assets/miniapps/bidan_pintar.yaml app/src/main/assets/miniapps/buku_kira_kira.yaml
git commit -m "Populate features blocks in bundled recipes; recolor Buku Kira-Kira"
```

---

### Task 4: Smoke-test YAML parser picks up `features` on existing decode

**Files:**
- Create: `app/src/test/java/com/bina/ai/miniapp/MiniAppFeatureParseTest.kt`

- [ ] **Step 1: Write failing test**

Create `app/src/test/java/com/bina/ai/miniapp/MiniAppFeatureParseTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.miniapp.MiniAppFeatureParseTest"`
Expected: 2 tests run, all PASS.

If it fails because the `app` module doesn't have `kaml` on its test classpath: in `app/build.gradle.kts`, add to dependencies block:
```kotlin
    testImplementation("com.charleskorn.kaml:kaml:0.61.0")
```
(Match the version used by `:shared`. If unsure, run `./gradlew.bat :shared:dependencies` and find the kaml version.)

Then re-run the test.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/bina/ai/miniapp/MiniAppFeatureParseTest.kt app/build.gradle.kts
git commit -m "Test Feature schema round-trips through YAML decode"
```

---

## Phase 2 — Install layer

### Task 5: Add `InstallRecord` data class

**Files:**
- Create: `app/src/main/java/com/bina/ai/install/model/InstallRecord.kt`

- [ ] **Step 1: Write the data class**

```kotlin
package com.bina.ai.install.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstallRecord(
    val recipeId: String,
    @SerialName("installed_at") val installedAt: Long,
    @SerialName("enabled_features") val enabledFeatureIds: Set<String>
)
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/install/model/InstallRecord.kt
git commit -m "Add InstallRecord schema for install state persistence"
```

---

### Task 6: `CapabilityChecker` + tests (TDD)

**Files:**
- Create: `app/src/test/java/com/bina/ai/install/CapabilityCheckerTest.kt`
- Create: `app/src/main/java/com/bina/ai/install/CapabilityChecker.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/bina/ai/install/CapabilityCheckerTest.kt`:

```kotlin
package com.bina.ai.install

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityCheckerTest {
    // Note: These tests use the pure-logic constructor (no Context) so they run on JVM.
    // Service tokens are always false, permissions are always true (use-time prompt model),
    // hardware is supplied by the caller.

    private val checker = CapabilityChecker.forTest(
        hardwareSupport = mapOf("gps" to true, "camera" to true)
    )

    @Test fun `permission tokens are always available`() {
        assertTrue(checker.isAvailable("permission:camera"))
        assertTrue(checker.isAvailable("permission:microphone"))
        assertTrue(checker.isAvailable("permission:location"))
        assertTrue(checker.isAvailable("permission:sms"))
        assertTrue(checker.isAvailable("permission:notifications"))
    }

    @Test fun `service tokens are always unavailable`() {
        assertFalse(checker.isAvailable("service:p2p"))
        assertFalse(checker.isAvailable("service:smart_notifications"))
        assertFalse(checker.isAvailable("service:sms_dispatch"))
        assertFalse(checker.isAvailable("service:anything_else"))
    }

    @Test fun `hardware tokens reflect supplied support map`() {
        assertTrue(checker.isAvailable("hardware:gps"))
        assertTrue(checker.isAvailable("hardware:camera"))
        assertFalse(checker.isAvailable("hardware:nfc"))
    }

    @Test fun `unknown tokens default to true (lenient)`() {
        assertTrue(checker.isAvailable("foo:bar"))
        assertTrue(checker.isAvailable("garbage"))
    }

    @Test fun `feature with all-available requires is available`() {
        val feature = listOf("permission:camera", "permission:microphone")
        assertTrue(feature.all { checker.isAvailable(it) })
    }

    @Test fun `feature with any unavailable require is NOT available`() {
        val feature = listOf("permission:camera", "service:p2p")
        assertFalse(feature.all { checker.isAvailable(it) })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.install.CapabilityCheckerTest"`
Expected: FAIL with "unresolved reference: CapabilityChecker".

- [ ] **Step 3: Implement CapabilityChecker**

Create `app/src/main/java/com/bina/ai/install/CapabilityChecker.kt`:

```kotlin
package com.bina.ai.install

import android.content.Context
import android.content.pm.PackageManager

/**
 * Maps capability tokens (strings like "permission:camera", "hardware:gps", "service:p2p") to a
 * Boolean indicating whether the capability is currently satisfied on this device.
 *
 * Token rules:
 * - permission:* — always returns true. Permission requests are deferred to use-time, not install-time.
 * - hardware:* — checks PackageManager.hasSystemFeature(...).
 * - service:* — always returns false. Reserved for runtime services we haven't built yet
 *               (P2P sync, smart notifications, SMS auto-dispatch). Reliably greys those features.
 * - anything else — returns true (lenient default for unknown future tokens).
 *
 * A feature is available iff ALL of its `requires` tokens are available.
 */
class CapabilityChecker private constructor(
    private val hardwareCheck: (String) -> Boolean
) {
    fun isAvailable(token: String): Boolean = when {
        token.startsWith("permission:") -> true
        token.startsWith("service:") -> false
        token.startsWith("hardware:") -> hardwareCheck(token.removePrefix("hardware:"))
        else -> true
    }

    companion object {
        fun create(context: Context): CapabilityChecker {
            val pm = context.packageManager
            return CapabilityChecker { name ->
                val systemFeature = when (name) {
                    "gps" -> PackageManager.FEATURE_LOCATION_GPS
                    "camera" -> PackageManager.FEATURE_CAMERA_ANY
                    else -> name   // pass-through for unknown hardware
                }
                pm.hasSystemFeature(systemFeature)
            }
        }

        /** Test-only factory. Caller supplies a fixed hardware-support map. */
        fun forTest(hardwareSupport: Map<String, Boolean>): CapabilityChecker =
            CapabilityChecker { name -> hardwareSupport[name] ?: false }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.install.CapabilityCheckerTest"`
Expected: 6 tests run, all PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/install/CapabilityChecker.kt app/src/test/java/com/bina/ai/install/CapabilityCheckerTest.kt
git commit -m "Add CapabilityChecker mapping tokens to availability with unit tests"
```

---

### Task 7: `InstallStore` + tests (TDD)

**Files:**
- Create: `app/src/test/java/com/bina/ai/install/InstallStoreTest.kt`
- Create: `app/src/main/java/com/bina/ai/install/InstallStore.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/bina/ai/install/InstallStoreTest.kt`:

```kotlin
package com.bina.ai.install

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.bina.ai.install.model.InstallRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class InstallStoreTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: InstallStore

    @Before fun setUp() {
        val testFile = tempFolder.newFile("test_installs.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create { testFile }
        store = InstallStore(dataStore)
    }

    @After fun tearDown() {
        // DataStore closes when the file goes out of scope; rule cleans the folder.
    }

    @Test fun `installs flow starts empty`() = runTest {
        assertTrue(store.installs.first().isEmpty())
    }

    @Test fun `install records survive a round-trip`() = runTest {
        val record = InstallRecord(
            recipeId = "farm_buddy",
            installedAt = 1_700_000_000_000L,
            enabledFeatureIds = setOf("camera_scanner", "offline_storage")
        )

        store.install(record)

        val out = store.installs.first()
        assertEquals(1, out.size)
        assertEquals(record, out["farm_buddy"])
    }

    @Test fun `installing the same recipe overwrites the previous record`() = runTest {
        val first = InstallRecord("farm_buddy", 1L, setOf("a"))
        val second = InstallRecord("farm_buddy", 2L, setOf("a", "b"))

        store.install(first)
        store.install(second)

        val out = store.installs.first()
        assertEquals(1, out.size)
        assertEquals(second, out["farm_buddy"])
    }

    @Test fun `installing different recipes keeps both`() = runTest {
        store.install(InstallRecord("farm_buddy", 1L, emptySet()))
        store.install(InstallRecord("bidan_pintar", 2L, setOf("voice_assistant")))

        val out = store.installs.first()
        assertEquals(2, out.size)
        assertTrue("farm_buddy" in out)
        assertTrue("bidan_pintar" in out)
    }

    @Test fun `uninstall removes the record`() = runTest {
        store.install(InstallRecord("farm_buddy", 1L, setOf("a")))
        store.install(InstallRecord("bidan_pintar", 2L, emptySet()))

        store.uninstall("farm_buddy")

        val out = store.installs.first()
        assertEquals(1, out.size)
        assertNull(out["farm_buddy"])
        assertTrue("bidan_pintar" in out)
    }

    @Test fun `uninstalling a recipe that was never installed is a no-op`() = runTest {
        store.install(InstallRecord("bidan_pintar", 1L, emptySet()))

        store.uninstall("not_there")

        val out = store.installs.first()
        assertEquals(1, out.size)
        assertTrue("bidan_pintar" in out)
    }

    @Test fun `isInstalled reflects current state`() = runTest {
        assertEquals(false, store.isInstalled("farm_buddy").first())
        store.install(InstallRecord("farm_buddy", 1L, emptySet()))
        assertEquals(true, store.isInstalled("farm_buddy").first())
        store.uninstall("farm_buddy")
        assertEquals(false, store.isInstalled("farm_buddy").first())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.install.InstallStoreTest"`
Expected: FAIL with "unresolved reference: InstallStore".

- [ ] **Step 3: Implement InstallStore**

Create `app/src/main/java/com/bina/ai/install/InstallStore.kt`:

```kotlin
package com.bina.ai.install

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bina.ai.install.model.InstallRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * DataStore-backed persistence of install records. Storage = single Preferences key holding
 * a JSON-encoded Map<recipeId, InstallRecord>. Observing returns a Flow that emits the full
 * map whenever any install changes.
 *
 * Construct via [InstallStore.create] in production code (uses a Context-backed DataStore).
 * Tests inject their own DataStore<Preferences> directly via the primary constructor.
 */
class InstallStore(private val dataStore: DataStore<Preferences>) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapSerializer = MapSerializer(String.serializer(), InstallRecord.serializer())

    val installs: Flow<Map<String, InstallRecord>> = dataStore.data.map { prefs ->
        decode(prefs[INSTALLS_KEY])
    }

    fun isInstalled(recipeId: String): Flow<Boolean> =
        installs.map { recipeId in it }

    suspend fun install(record: InstallRecord) {
        dataStore.edit { prefs ->
            val current = decode(prefs[INSTALLS_KEY])
            val updated = current + (record.recipeId to record)
            prefs[INSTALLS_KEY] = json.encodeToString(mapSerializer, updated)
        }
    }

    suspend fun uninstall(recipeId: String) {
        dataStore.edit { prefs ->
            val current = decode(prefs[INSTALLS_KEY])
            if (recipeId !in current) return@edit
            val updated = current - recipeId
            prefs[INSTALLS_KEY] = json.encodeToString(mapSerializer, updated)
        }
    }

    private fun decode(raw: String?): Map<String, InstallRecord> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString(mapSerializer, raw) }.getOrElse { emptyMap() }
    }

    companion object {
        private val INSTALLS_KEY = stringPreferencesKey("installs_json")

        private val Context.binaInstallsDataStore by preferencesDataStore("bina_installs")

        /** Production factory — uses a Context-backed DataStore at "bina_installs". */
        fun create(context: Context): InstallStore =
            InstallStore(context.binaInstallsDataStore)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.install.InstallStoreTest"`
Expected: 7 tests run, all PASS.

If a test fails with "datastore-preferences-core not on test classpath": in `app/build.gradle.kts` test deps, add:
```kotlin
    testImplementation("androidx.datastore:datastore-preferences-core:1.1.1")
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/install/InstallStore.kt app/src/test/java/com/bina/ai/install/InstallStoreTest.kt app/build.gradle.kts
git commit -m "Add InstallStore with DataStore Preferences persistence and unit tests"
```

---

### Task 8: Recipe-size helper functions + tests (TDD)

**Files:**
- Create: `app/src/test/java/com/bina/ai/install/RecipeSizeTest.kt`
- Create: `app/src/main/java/com/bina/ai/install/RecipeSize.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/bina/ai/install/RecipeSizeTest.kt`:

```kotlin
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
        assertEquals(1.8f, total, 0.001f)   // 1.2 + 0.4 + 0.2
    }

    @Test fun `totalSizeKb ignores enabled IDs that no longer exist on the recipe`() {
        val total = totalSizeKb(recipe, baseSizeKb = 1.0f, enabledFeatureIds = setOf("a", "ghost"))
        assertEquals(1.4f, total, 0.001f)   // 1.0 + 0.4 (ghost ignored)
    }

    @Test fun `totalSizeKb on a recipe with no features returns base only`() {
        val bare = MiniApp(id = "bare", name = "Bare", features = emptyList())
        val total = totalSizeKb(bare, baseSizeKb = 0.8f, enabledFeatureIds = setOf("anything"))
        assertEquals(0.8f, total, 0.001f)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.install.RecipeSizeTest"`
Expected: FAIL with "unresolved reference: totalSizeKb".

- [ ] **Step 3: Implement RecipeSize.kt**

Create `app/src/main/java/com/bina/ai/install/RecipeSize.kt`:

```kotlin
package com.bina.ai.install

import com.bina.ai.miniapp.model.MiniApp

/**
 * Compute the total install size in KB for a recipe with a given set of enabled features.
 *
 * `baseSizeKb` is the size of the recipe's YAML payload itself (caller supplies — for bundled
 * recipes use AssetManager openFd().length, for sync-imported use File.length()).
 *
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.install.RecipeSizeTest"`
Expected: 4 tests run, all PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/install/RecipeSize.kt app/src/test/java/com/bina/ai/install/RecipeSizeTest.kt
git commit -m "Add totalSizeKb helper for live recipe-size computation"
```

---

## Phase 3 — Configurator

### Task 9: `ConfiguratorState` model + ViewModel skeleton + initialization tests (TDD)

**Files:**
- Create: `app/src/test/java/com/bina/ai/ui/screens/configurator/ConfiguratorViewModelTest.kt`
- Create: `app/src/main/java/com/bina/ai/ui/screens/configurator/ConfiguratorViewModel.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/bina/ai/ui/screens/configurator/ConfiguratorViewModelTest.kt`:

```kotlin
package com.bina.ai.ui.screens.configurator

import com.bina.ai.install.CapabilityChecker
import com.bina.ai.miniapp.model.Feature
import com.bina.ai.miniapp.model.MiniApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfiguratorViewModelTest {

    private val checker = CapabilityChecker.forTest(hardwareSupport = mapOf("gps" to true))

    private val recipe = MiniApp(
        id = "farm_buddy",
        name = "Farm Buddy",
        features = listOf(
            Feature(id = "cam", name = "Cam",   recommended = true,  sizeKb = 0.4f, requires = listOf("permission:camera")),
            Feature(id = "voice", name = "Voice", recommended = true,  sizeKb = 0.3f, requires = listOf("permission:microphone")),
            Feature(id = "sms", name = "SMS",   recommended = false, sizeKb = 0.2f, requires = listOf("service:sms_dispatch")),
            Feature(id = "p2p", name = "P2P",   recommended = true,  sizeKb = 0.2f, requires = listOf("service:p2p"))   // recommended-but-unavailable
        )
    )

    @Test fun `initial state turns on recommended-and-available, leaves others off`() {
        val state = ConfiguratorState.initial(recipe, checker)

        // recommended + available
        assertTrue(state.toggles["cam"] == true)
        assertTrue(state.toggles["voice"] == true)
        // not recommended → off (and unavailable too)
        assertFalse(state.toggles["sms"] == true)
        // recommended but unavailable → off (per spec)
        assertFalse(state.toggles["p2p"] == true)
    }

    @Test fun `availability reflects checker for each feature`() {
        val state = ConfiguratorState.initial(recipe, checker)

        assertTrue(state.availability["cam"] == true)
        assertTrue(state.availability["voice"] == true)
        assertFalse(state.availability["sms"] == true)
        assertFalse(state.availability["p2p"] == true)
    }

    @Test fun `totalCount equals features size including unavailable ones`() {
        val state = ConfiguratorState.initial(recipe, checker)
        assertEquals(4, state.totalCount)
    }

    @Test fun `activeCount counts only enabled toggles`() {
        val state = ConfiguratorState.initial(recipe, checker)
        assertEquals(2, state.activeCount)   // cam + voice
    }

    @Test fun `totalSizeKb sums base plus enabled-feature sizes`() {
        val state = ConfiguratorState.initial(recipe, checker)
        // base 1.2 + cam 0.4 + voice 0.3 = 1.9
        assertEquals(1.9f, state.totalSizeKb(baseSizeKb = 1.2f), 0.001f)
    }

    @Test fun `withToggle flips a feature on or off and returns a new state`() {
        val state = ConfiguratorState.initial(recipe, checker)

        val turnedOff = state.withToggle("cam", on = false)
        assertEquals(false, turnedOff.toggles["cam"])
        assertEquals(1, turnedOff.activeCount)

        val turnedBackOn = turnedOff.withToggle("cam", on = true)
        assertEquals(true, turnedBackOn.toggles["cam"])
        assertEquals(2, turnedBackOn.activeCount)
    }

    @Test fun `enabledFeatureIds returns the set of currently-on features`() {
        val state = ConfiguratorState.initial(recipe, checker)
        assertEquals(setOf("cam", "voice"), state.enabledFeatureIds)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.ui.screens.configurator.ConfiguratorViewModelTest"`
Expected: FAIL with "unresolved reference: ConfiguratorState".

- [ ] **Step 3: Implement ConfiguratorState in ConfiguratorViewModel.kt**

Create `app/src/main/java/com/bina/ai/ui/screens/configurator/ConfiguratorViewModel.kt`:

```kotlin
package com.bina.ai.ui.screens.configurator

import com.bina.ai.install.CapabilityChecker
import com.bina.ai.install.totalSizeKb
import com.bina.ai.miniapp.model.MiniApp

/**
 * Transient UI state for ConfiguratorScreen. Pure data — no Android dependencies — so it's
 * unit-testable on the JVM. Use [initial] to build the starting state for a given recipe.
 */
data class ConfiguratorState(
    val miniApp: MiniApp,
    val toggles: Map<String, Boolean>,           // featureId → on/off
    val availability: Map<String, Boolean>        // featureId → can be toggled
) {
    val activeCount: Int get() = toggles.count { it.value }
    val totalCount: Int get() = miniApp.features.size

    val enabledFeatureIds: Set<String>
        get() = toggles.filterValues { it }.keys

    fun totalSizeKb(baseSizeKb: Float): Float =
        totalSizeKb(miniApp, baseSizeKb, enabledFeatureIds)

    fun withToggle(featureId: String, on: Boolean): ConfiguratorState {
        if (availability[featureId] == false) return this   // can't toggle unavailable
        return copy(toggles = toggles + (featureId to on))
    }

    companion object {
        fun initial(recipe: MiniApp, checker: CapabilityChecker): ConfiguratorState {
            val availability = recipe.features.associate { feature ->
                feature.id to feature.requires.all { checker.isAvailable(it) }
            }
            val toggles = recipe.features.associate { feature ->
                val available = availability[feature.id] == true
                feature.id to (feature.recommended && available)
            }
            return ConfiguratorState(recipe, toggles, availability)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.ui.screens.configurator.ConfiguratorViewModelTest"`
Expected: 7 tests run, all PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/configurator/ConfiguratorViewModel.kt app/src/test/java/com/bina/ai/ui/screens/configurator/ConfiguratorViewModelTest.kt
git commit -m "Add ConfiguratorState with toggle, size, and initial-state logic + tests"
```

---

### Task 10: `ConfiguratorViewModel` install commit + event bus

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/screens/configurator/ConfiguratorViewModel.kt`

- [ ] **Step 1: Add the ViewModel class to the file**

In `ConfiguratorViewModel.kt`, ABOVE the existing `data class ConfiguratorState` (i.e., add at the top of the file under the imports), add:

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.install.InstallStore
import com.bina.ai.install.model.InstallRecord
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ConfiguratorEvent {
    data class Installed(val recipeName: String) : ConfiguratorEvent
    data object AlreadyInstalled : ConfiguratorEvent
    data class Failed(val message: String) : ConfiguratorEvent
}

class ConfiguratorViewModel(
    initialState: ConfiguratorState,
    private val baseSizeKb: Float,
    private val installStore: InstallStore
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<ConfiguratorState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ConfiguratorEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<ConfiguratorEvent> = _events.asSharedFlow()

    fun toggleFeature(featureId: String, on: Boolean) {
        _state.value = _state.value.withToggle(featureId, on)
    }

    fun currentSizeKb(): Float = _state.value.totalSizeKb(baseSizeKb)

    fun install() = viewModelScope.launch {
        val s = _state.value
        // Race protection: someone else already installed this recipe.
        if (installStore.isInstalled(s.miniApp.id).first()) {
            _events.emit(ConfiguratorEvent.AlreadyInstalled)
            return@launch
        }
        try {
            installStore.install(InstallRecord(
                recipeId = s.miniApp.id,
                installedAt = System.currentTimeMillis(),
                enabledFeatureIds = s.enabledFeatureIds
            ))
            _events.emit(ConfiguratorEvent.Installed(s.miniApp.name))
        } catch (t: Throwable) {
            _events.emit(ConfiguratorEvent.Failed(t.message ?: "Couldn't save install"))
        }
    }
}
```

- [ ] **Step 2: Compile to verify**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Re-run earlier tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.ui.screens.configurator.ConfiguratorViewModelTest"`
Expected: 7 tests still PASS (the existing tests only used `ConfiguratorState`, not the ViewModel — they should be unaffected).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/configurator/ConfiguratorViewModel.kt
git commit -m "Add ConfiguratorViewModel with install commit and event bus"
```

---

### Task 11: `FeatureToggleCard` composable

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/configurator/components/FeatureToggleCard.kt`

- [ ] **Step 1: Implement the composable**

Create the file:

```kotlin
package com.bina.ai.ui.screens.configurator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.Feature
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

/**
 * One row in the Configurator feature list. Whole row dims to alpha 0.4 if not toggleable;
 * the Switch is also disabled in that state. The accent color tints the icon background.
 */
@Composable
fun FeatureToggleCard(
    feature: Feature,
    isEnabled: Boolean,
    isToggleable: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowAlpha = if (isToggleable) 1f else 0.4f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp)
            .alpha(rowAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconFor(feature.icon), null, tint = accentColor, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(feature.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaPrimary)
                if (feature.recommended) RecommendedPill()
            }
            if (feature.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(feature.description, fontSize = 11.sp, color = BinaGrayText)
            }
            Spacer(Modifier.height(2.dp))
            Text("+${"%.1f".format(feature.sizeKb)} KB", fontSize = 10.sp, color = BinaGrayText, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = if (isToggleable) onToggle else null,
            enabled = isToggleable
        )
    }
}

@Composable
private fun RecommendedPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BinaGreen.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("RECOMMENDED", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = BinaGreen)
    }
}

/** Maps Feature.icon string from YAML to a Compose ImageVector. Falls back to Bolt. */
private fun iconFor(name: String): ImageVector = when (name) {
    "camera" -> Icons.Filled.Camera
    "mic" -> Icons.Filled.Mic
    "gps_fixed" -> Icons.Filled.GpsFixed
    "storage" -> Icons.Filled.Storage
    "sms" -> Icons.Filled.Sms
    "share" -> Icons.Filled.Share
    "notifications" -> Icons.Filled.Notifications
    "warning" -> Icons.Filled.Warning
    else -> Icons.Filled.Bolt
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/configurator/components/FeatureToggleCard.kt
git commit -m "Add FeatureToggleCard component with greyed-state and recommended pill"
```

---

### Task 12: `ConfiguratorHeader` composable

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/configurator/components/ConfiguratorHeader.kt`

- [ ] **Step 1: Implement the composable**

```kotlin
package com.bina.ai.ui.screens.configurator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun ConfiguratorHeader(
    totalSizeKb: Float,
    activeCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total Download Size", fontSize = 11.sp, color = BinaGrayText, fontWeight = FontWeight.Medium)
                AnimatedContent(
                    targetState = totalSizeKb,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "size"
                ) { size ->
                    Text(
                        text = "%.1f KB".format(size),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = BinaPrimary
                    )
                }
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("Active Features", fontSize = 11.sp, color = BinaGrayText, fontWeight = FontWeight.Medium)
                AnimatedContent(
                    targetState = "$activeCount/$totalCount",
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "count"
                ) { txt ->
                    Text(txt, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BinaGreen)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/configurator/components/ConfiguratorHeader.kt
git commit -m "Add ConfiguratorHeader with animated size + active-count display"
```

---

### Task 13: `ConfiguratorScreen` UI assembly

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/configurator/ConfiguratorScreen.kt`

- [ ] **Step 1: Implement the screen**

```kotlin
package com.bina.ai.ui.screens.configurator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.bina.ai.install.CapabilityChecker
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.screens.configurator.components.ConfiguratorHeader
import com.bina.ai.ui.screens.configurator.components.FeatureToggleCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguratorScreen(
    miniAppId: String,
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    capabilityChecker: CapabilityChecker,
    baseSizeKb: Float,
    onInstalled: (recipeName: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recipe = remember(miniAppId) { miniAppRepository.getById(miniAppId) }

    if (recipe == null) {
        UnavailableScaffold(onBack = onBack, modifier = modifier)
        return
    }

    if (recipe.features.isEmpty()) {
        EmptyFeaturesScaffold(
            recipe = recipe,
            installStore = installStore,
            onInstalled = onInstalled,
            onBack = onBack,
            modifier = modifier
        )
        return
    }

    val factory = remember(recipe, capabilityChecker, baseSizeKb, installStore) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ConfiguratorViewModel(
                    initialState = ConfiguratorState.initial(recipe, capabilityChecker),
                    baseSizeKb = baseSizeKb,
                    installStore = installStore
                ) as T
        }
    }
    val vm: ConfiguratorViewModel = viewModel(key = recipe.id, factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val snackHost = remember { SnackbarHostState() }
    val accent = parseHexColor(recipe.theme.primary, fallback = BinaPrimary)

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is ConfiguratorEvent.Installed -> onInstalled(event.recipeName)
                is ConfiguratorEvent.AlreadyInstalled -> {
                    snackHost.showSnackbar("Already installed — opening MyPocket.")
                    onInstalled(recipe.name)
                }
                is ConfiguratorEvent.Failed -> {
                    snackHost.showSnackbar("Couldn't save install: ${event.message}")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(recipe.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Choose features you need", fontSize = 11.sp, color = BinaGrayText)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackHost) },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = vm::install,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                ) {
                    Text("Install to Pocket", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "You can change these settings anytime by uninstalling and re-installing.",
                    fontSize = 10.sp,
                    color = BinaGrayText,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ConfiguratorHeader(
                    totalSizeKb = state.totalSizeKb(baseSizeKb),
                    activeCount = state.activeCount,
                    totalCount = state.totalCount
                )
            }
            items(recipe.features, key = { it.id }) { feature ->
                FeatureToggleCard(
                    feature = feature,
                    isEnabled = state.toggles[feature.id] == true,
                    isToggleable = state.availability[feature.id] == true,
                    accentColor = accent,
                    onToggle = { on -> vm.toggleFeature(feature.id, on) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnavailableScaffold(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Recipe unavailable") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("This recipe is no longer available.", color = BinaGrayText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyFeaturesScaffold(
    recipe: MiniApp,
    installStore: InstallStore,
    onInstalled: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val factory = remember(recipe, installStore) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ConfiguratorViewModel(
                    initialState = ConfiguratorState.initial(recipe, CapabilityChecker.forTest(emptyMap())),
                    baseSizeKb = 0f,
                    installStore = installStore
                ) as T
        }
    }
    val vm: ConfiguratorViewModel = viewModel(key = recipe.id + "_empty", factory = factory)

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            if (event is ConfiguratorEvent.Installed) onInstalled(event.recipeName)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(recipe.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("This recipe has no configurable features.", fontSize = 14.sp, color = BinaGrayText)
            Spacer(Modifier.height(16.dp))
            Button(onClick = vm::install, colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)) {
                Text("Install with defaults", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun parseHexColor(hex: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
```

- [ ] **Step 2: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/configurator/ConfiguratorScreen.kt
git commit -m "Add ConfiguratorScreen with toggle list, install button, and edge cases"
```

---

## Phase 4 — RecipeCover and detail sheet

### Task 14: `RecipeCover` composable

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/components/RecipeCover.kt`

- [ ] **Step 1: Implement the composable**

```kotlin
package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaSecondary

/**
 * Renders a recipe's cover. If `coverImage` is set, loads via Coil with the gradient as
 * a fallback during load and on error. If empty, renders the gradient + emoji icon.
 */
@Composable
fun RecipeCover(
    miniApp: MiniApp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    showEmoji: Boolean = true,
    emojiFontSize: Int = 36
) {
    val primary = parseHex(miniApp.theme.primary, BinaPrimary)
    val secondary = parseHex(miniApp.theme.secondary, BinaSecondary)

    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        // Gradient always — image overlays it when present.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(primary, secondary)))
        )
        if (showEmoji && miniApp.icon.isNotBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(miniApp.icon, fontSize = emojiFontSize.sp)
            }
        }
        if (miniApp.coverImage.isNotBlank()) {
            AsyncImage(
                model = resolveCoverPath(miniApp.coverImage),
                contentDescription = miniApp.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun resolveCoverPath(raw: String): String = when {
    raw.startsWith("http://") || raw.startsWith("https://") -> raw
    raw.startsWith("/") || raw.startsWith("file://") -> raw
    else -> "file:///android_asset/miniapps/$raw"
}

private fun parseHex(hex: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
```

- [ ] **Step 2: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/components/RecipeCover.kt
git commit -m "Add RecipeCover composable with Coil image + theme-gradient fallback"
```

---

### Task 15: `RecipeStats` and `FeaturePreviewList` composables

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/recipe_detail/RecipeStats.kt`
- Create: `app/src/main/java/com/bina/ai/ui/screens/recipe_detail/FeaturePreviewList.kt`

- [ ] **Step 1: Implement RecipeStats**

```kotlin
package com.bina.ai.ui.screens.recipe_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun RecipeStats(
    sizeKb: Float,
    availableFeatures: Int,
    dialect: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(value = "%.1f KB".format(sizeKb), label = "Recipe Size")
        Divider()
        StatCell(value = availableFeatures.toString(), label = "Features Available")
        Divider()
        StatCell(value = dialect.ifBlank { "—" }, label = "Dialect")
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BinaPrimary, maxLines = 1)
        Text(label, fontSize = 10.sp, color = BinaGrayText)
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .background(Color(0xFFE5E7EB))
            .padding(end = 0.dp)
    ) {
        // 1px wide vertical line, 24dp tall
        Box(Modifier.background(Color(0xFFE5E7EB)).padding(0.dp))
    }
}
```

- [ ] **Step 2: Implement FeaturePreviewList**

```kotlin
package com.bina.ai.ui.screens.recipe_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.Feature
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

/**
 * Read-only preview of features. Just shows icon + name as small chips. Full toggling lives
 * in the Configurator.
 */
@Composable
fun FeaturePreviewList(
    features: List<Feature>,
    modifier: Modifier = Modifier
) {
    if (features.isEmpty()) return
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(count = features.size) { i ->
            FeatureChip(features[i])
        }
    }
}

@Composable
private fun FeatureChip(feature: Feature) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.Bolt, null, tint = BinaPrimary, modifier = Modifier.size(12.dp))
        Text(feature.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = BinaPrimary)
    }
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/recipe_detail/
git commit -m "Add RecipeStats grid and FeaturePreviewList for detail sheet"
```

---

### Task 16: `RecipeDetailSheet` composable

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/recipe_detail/RecipeDetailSheet.kt`

- [ ] **Step 1: Implement the bottom sheet**

```kotlin
package com.bina.ai.ui.screens.recipe_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.screens.hub.components.RecipeCover
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailSheet(
    miniApp: MiniApp,
    isInstalled: Boolean,
    sizeKb: Float,
    onConfigureInstall: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            // Hero strip
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp)) {
                RecipeCover(miniApp, modifier = Modifier.fillMaxWidth().height(180.dp), cornerRadius = 18.dp, emojiFontSize = 56)
                if (miniApp.emergency) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BinaRed)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("EMERGENCY", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
                Text(miniApp.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BinaPrimary)
                if (miniApp.author.verified && miniApp.author.organisation.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "✓ Verified by ${miniApp.author.organisation}",
                        fontSize = 12.sp,
                        color = BinaGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (miniApp.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(miniApp.description, fontSize = 13.sp, color = BinaGrayText)
                }

                Spacer(Modifier.height(16.dp))
                RecipeStats(
                    sizeKb = sizeKb,
                    availableFeatures = miniApp.features.size,
                    dialect = miniApp.dialect
                )

                if (miniApp.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(count = miniApp.tags.size) { i ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("#${miniApp.tags[i]}", fontSize = 11.sp, color = BinaPrimary)
                            }
                        }
                    }
                }

                if (miniApp.category.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Domain: ${miniApp.category}", fontSize = 12.sp, color = BinaGrayText)
                }

                if (miniApp.features.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Features in this recipe", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BinaGrayText)
                    Spacer(Modifier.height(6.dp))
                    FeaturePreviewList(miniApp.features)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = if (isInstalled) onOpen else onConfigureInstall,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                ) {
                    Text(
                        if (isInstalled) "Open" else "Configure & Install",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/recipe_detail/RecipeDetailSheet.kt
git commit -m "Add RecipeDetailSheet bottom sheet with adaptive Configure/Open CTA"
```

---

## Phase 5 — Hub UI

### Task 17: `HubUiState` model

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/model/HubUiState.kt`

- [ ] **Step 1: Implement the state model**

```kotlin
package com.bina.ai.ui.screens.hub.model

import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.navigation.UserMode

data class Rail(val title: String, val recipes: List<MiniApp>)

sealed interface HubUiState {
    data object Loading : HubUiState

    data class Loaded(
        val mode: UserMode,
        val allRecipes: List<MiniApp>,
        val featured: List<MiniApp>,
        val categories: List<String>,
        val selectedCategory: String,                 // "All" or a specific category
        val rails: List<Rail>,                        // computed for current mode + selection
        val installedIds: Set<String>,
        val authoredIds: Set<String>
    ) : HubUiState
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/model/HubUiState.kt
git commit -m "Add HubUiState model with Loaded/Loading variants and Rail data class"
```

---

### Task 18: `HubViewModel.computeRails` + tests (TDD)

**Files:**
- Create: `app/src/test/java/com/bina/ai/ui/screens/hub/HubViewModelTest.kt`
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/HubViewModel.kt`

- [ ] **Step 1: Write failing test**

Create `app/src/test/java/com/bina/ai/ui/screens/hub/HubViewModelTest.kt`:

```kotlin
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
        // Each category has 1 recipe, below the >=2 threshold for category rails.
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
        // All Recipes + Health (2) + Agriculture (3)
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
        // Authored set only matters for Architect mode.
        val rs = listOf(recipe("mine"), recipe("theirs"))
        val rails = computeRails(rs, authored = setOf("mine"), isArchitect = false)
        val all = rails.first { it.title == "All Recipes" }
        assertEquals(2, all.recipes.size)
    }

    @Test fun `category rail threshold counts only recipes in All Recipes pool`() {
        // 2 authored Health + 1 non-authored Health = only 1 in non-authored pool, no Health rail.
        val rs = listOf(
            recipe("mh1", "Health"),
            recipe("mh2", "Health"),
            recipe("th1", "Health")
        )
        val rails = computeRails(rs, authored = setOf("mh1", "mh2"), isArchitect = true)
        // Your Recipes + All Recipes; no category rail (only 1 non-authored Health)
        assertEquals(2, rails.size)
        assertEquals(setOf("Your Recipes", "All Recipes"), rails.map { it.title }.toSet())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.ui.screens.hub.HubViewModelTest"`
Expected: FAIL with "unresolved reference: computeRails".

- [ ] **Step 3: Implement HubViewModel.kt with computeRails as top-level function**

Create `app/src/main/java/com/bina/ai/ui/screens/hub/HubViewModel.kt`:

```kotlin
package com.bina.ai.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.navigation.UserMode
import com.bina.ai.ui.screens.hub.model.HubUiState
import com.bina.ai.ui.screens.hub.model.Rail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.io.File

const val ALL_CATEGORY = "All"
const val MIN_CATEGORY_RAIL_SIZE = 2

/**
 * Pure rail-computation logic. Tested without ViewModel/Flow ceremony.
 *
 *  1. Architect-only: pin "Your Recipes" rail (recipes in authored).
 *  2. "All Recipes" rail of everything else (Architect: minus authored; Builder: all).
 *  3. Category rails for any category with >= MIN_CATEGORY_RAIL_SIZE non-authored recipes.
 */
fun computeRails(
    recipes: List<MiniApp>,
    authored: Set<String>,
    isArchitect: Boolean
): List<Rail> = buildList {
    if (isArchitect) {
        val mine = recipes.filter { it.id in authored }
        if (mine.isNotEmpty()) add(Rail("Your Recipes", mine))
    }
    val rest = recipes.filterNot { isArchitect && it.id in authored }
    if (rest.isNotEmpty()) add(Rail("All Recipes", rest))
    rest.groupBy { it.category }
        .toSortedMap()
        .forEach { (cat, items) ->
            if (cat.isNotBlank() && items.size >= MIN_CATEGORY_RAIL_SIZE) {
                add(Rail(cat, items))
            }
        }
}

/** Scans filesDir for authored YAML IDs (same logic as AnalyticsRepository.scanAuthoredRecipeIds). */
fun scanAuthoredRecipeIds(filesDir: File): Set<String> {
    val miniappsDir = File(filesDir, "miniapps")
    if (!miniappsDir.isDirectory) return emptySet()
    return miniappsDir.listFiles()
        ?.filter { it.isFile && (it.extension == "yaml" || it.extension == "yml") }
        ?.mapNotNull { extractIdFromYaml(it) }
        ?.toSet()
        ?: emptySet()
}

private fun extractIdFromYaml(file: File): String? = runCatching {
    file.useLines { lines ->
        lines
            .firstOrNull { it.trimStart().startsWith("id:") }
            ?.substringAfter(":")
            ?.trim()
            ?.trim('"', '\'')
            ?.takeIf { it.isNotEmpty() }
    }
}.getOrNull()

class HubViewModel(
    private val repo: MiniAppRepository,
    private val installStore: InstallStore,
    private val mode: UserMode,
    private val filesDir: File
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(ALL_CATEGORY)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun selectCategory(category: String) { _selectedCategory.value = category }

    val uiState: StateFlow<HubUiState> = combine(
        flowOf(repo.loadAll()),
        installStore.installs,
        _selectedCategory
    ) { recipes, installs, category ->
        val authored = scanAuthoredRecipeIds(filesDir)
        val featured = recipes.filter { it.featured || it.emergency }
        val categories = listOf(ALL_CATEGORY) + recipes.map { it.category }.filter { it.isNotBlank() }.toSortedSet()
        val visibleRecipes = if (category == ALL_CATEGORY) recipes else recipes.filter { it.category == category }
        val rails = if (category == ALL_CATEGORY) computeRails(visibleRecipes, authored, mode == UserMode.ARCHITECT) else emptyList()
        HubUiState.Loaded(
            mode = mode,
            allRecipes = visibleRecipes,
            featured = featured,
            categories = categories,
            selectedCategory = category,
            rails = rails,
            installedIds = installs.keys,
            authoredIds = authored
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HubUiState.Loading)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.ui.screens.hub.HubViewModelTest"`
Expected: 9 tests run, all PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/HubViewModel.kt app/src/test/java/com/bina/ai/ui/screens/hub/HubViewModelTest.kt
git commit -m "Add HubViewModel with adaptive rail logic and unit tests"
```

---

### Task 19: `HubHeader` composable

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/components/HubHeader.kt`

- [ ] **Step 1: Implement the composable**

```kotlin
package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.navigation.UserMode
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun HubHeader(
    mode: UserMode,
    modifier: Modifier = Modifier
) {
    val (title, subtitle) = when (mode) {
        UserMode.BUILDER -> "Discover AI Recipes" to "Edge-native AI for every domain"
        UserMode.ARCHITECT -> "Recipe Marketplace" to "Author and discover recipes"
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaPrimary)
                if (mode == UserMode.ARCHITECT) CreatorPill()
            }
            Text(subtitle, fontSize = 12.sp, color = BinaGrayText)
        }
    }
}

@Composable
private fun CreatorPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BinaGreen.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text("CREATOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BinaGreen)
    }
}
```

- [ ] **Step 2: Compile + commit**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/components/HubHeader.kt
git commit -m "Add HubHeader with mode-aware title and creator pill"
```

---

### Task 20: `FeaturedCarousel` composable

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/components/FeaturedCarousel.kt`

- [ ] **Step 1: Implement the composable**

```kotlin
package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaRed
import kotlinx.coroutines.delay

@Composable
fun FeaturedCarousel(
    recipes: List<MiniApp>,
    onRecipeClick: (MiniApp) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recipes.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { recipes.size })

    LaunchedEffect(recipes.size) {
        if (recipes.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % recipes.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth().height(200.dp)
        ) { page ->
            FeaturedCard(recipes[page], onClick = { onRecipeClick(recipes[page]) })
        }
        if (recipes.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(recipes.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) Color(0xFF091A7A) else Color(0xFFCBD5E1))
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(miniApp: MiniApp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        RecipeCover(miniApp, modifier = Modifier.fillMaxSize(), cornerRadius = 20.dp, emojiFontSize = 72)
        // Bottom gradient for text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (miniApp.emergency) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BinaRed)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("EMERGENCY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(miniApp.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (miniApp.description.isNotBlank()) {
                Text(
                    miniApp.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2
                )
            }
        }
    }
}
```

- [ ] **Step 2: Compile + commit**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/components/FeaturedCarousel.kt
git commit -m "Add FeaturedCarousel with auto-advance and emergency badge"
```

---

### Task 21: `CategoryChips` composable

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/components/CategoryChips.kt`

- [ ] **Step 1: Implement the composable**

```kotlin
package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaSecondary

@Composable
fun CategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            Chip(label = cat, isActive = cat == selected, onClick = { onSelect(cat) })
        }
    }
}

@Composable
private fun Chip(label: String, isActive: Boolean, onClick: () -> Unit) {
    val containerModifier = if (isActive) {
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(BinaPrimary, BinaSecondary)))
    } else {
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    }
    Text(
        text = label,
        modifier = containerModifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isActive) Color.White else BinaPrimary
    )
}
```

- [ ] **Step 2: Compile + commit**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/components/CategoryChips.kt
git commit -m "Add CategoryChips horizontal selector"
```

---

### Task 22: `RecipeCard` composable (compact, for rails)

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/components/RecipeCard.kt`

- [ ] **Step 1: Implement the composable**

```kotlin
package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaRed

@Composable
fun RecipeCard(
    miniApp: MiniApp,
    isInstalled: Boolean,
    isAuthored: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            RecipeCover(miniApp, modifier = Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 12.dp, emojiFontSize = 38)
            if (miniApp.emergency) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BinaRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("EMERGENCY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (miniApp.author.verified) BadgePill("✓ Verified")
                if (isAuthored) BadgePill("Yours")
            }
        }
        Column {
            Text(miniApp.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BinaPrimary, maxLines = 1)
            val meta = listOfNotNull(
                miniApp.category.takeIf { it.isNotBlank() },
                miniApp.dialect.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, fontSize = 10.sp, color = BinaGrayText, maxLines = 1)
            }
            if (isInstalled) {
                Spacer(Modifier.height(2.dp))
                Text("✓ Installed", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = BinaGreen)
            }
        }
    }
}

@Composable
private fun BadgePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text, fontSize = 8.sp, color = BinaPrimary, fontWeight = FontWeight.SemiBold)
    }
}
```

- [ ] **Step 2: Compile + commit**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/components/RecipeCard.kt
git commit -m "Add compact RecipeCard for use in horizontal rails"
```

---

### Task 23: `RecipeListItem` composable (wide, for filtered list)

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/components/RecipeListItem.kt`

- [ ] **Step 1: Implement the composable**

```kotlin
package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun RecipeListItem(
    miniApp: MiniApp,
    isInstalled: Boolean,
    isAuthored: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RecipeCover(miniApp, modifier = Modifier.size(64.dp), cornerRadius = 12.dp, emojiFontSize = 28)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(miniApp.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaPrimary, maxLines = 1)
                if (miniApp.author.verified) {
                    Text("✓", fontSize = 12.sp, color = BinaGreen)
                }
                if (isAuthored) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BinaPrimary.copy(alpha = 0.10f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("Yours", fontSize = 8.sp, color = BinaPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (miniApp.description.isNotBlank()) {
                Text(miniApp.description, fontSize = 11.sp, color = BinaGrayText, maxLines = 2)
            }
            val meta = listOfNotNull(
                miniApp.category.takeIf { it.isNotBlank() },
                miniApp.dialect.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, fontSize = 10.sp, color = BinaGrayText)
            }
            if (miniApp.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    miniApp.tags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("#$tag", fontSize = 9.sp, color = BinaPrimary)
                        }
                    }
                    if (miniApp.tags.size > 2) {
                        Text("+${miniApp.tags.size - 2}", fontSize = 9.sp, color = BinaGrayText)
                    }
                }
            }
        }
        if (isInstalled) {
            Text("✓ Installed", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BinaGreen)
        }
    }
}
```

- [ ] **Step 2: Compile + commit**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/components/RecipeListItem.kt
git commit -m "Add wide RecipeListItem for filtered category list"
```

---

### Task 24: `CategoryRail` composable

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/components/CategoryRail.kt`

- [ ] **Step 1: Implement the composable**

```kotlin
package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun CategoryRail(
    title: String,
    recipes: List<MiniApp>,
    installedIds: Set<String>,
    authoredIds: Set<String>,
    onRecipeClick: (MiniApp) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = BinaPrimary
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(recipes, key = { it.id }) { recipe ->
                RecipeCard(
                    miniApp = recipe,
                    isInstalled = recipe.id in installedIds,
                    isAuthored = recipe.id in authoredIds,
                    onClick = { onRecipeClick(recipe) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
```

- [ ] **Step 2: Compile + commit**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/components/CategoryRail.kt
git commit -m "Add reusable CategoryRail for horizontal recipe sections"
```

---

### Task 25: `PublishFab` composable

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/hub/components/PublishFab.kt`

- [ ] **Step 1: Implement the composable**

```kotlin
package com.bina.ai.ui.screens.hub.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun PublishFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = BinaPrimary,
        contentColor = androidx.compose.ui.graphics.Color.White,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text("Publish new", fontWeight = FontWeight.SemiBold) }
    )
}
```

- [ ] **Step 2: Compile + commit**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/components/PublishFab.kt
git commit -m "Add Architect-only PublishFab routing to Studio"
```

---

### Task 26: `HubScreen` composition root

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/screens/hub/HubScreen.kt` (rewrite)

- [ ] **Step 1: Rewrite HubScreen.kt**

Replace the entire contents of `app/src/main/java/com/bina/ai/ui/screens/hub/HubScreen.kt` with:

```kotlin
package com.bina.ai.ui.screens.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.navigation.UserMode
import com.bina.ai.ui.screens.hub.components.CategoryChips
import com.bina.ai.ui.screens.hub.components.CategoryRail
import com.bina.ai.ui.screens.hub.components.FeaturedCarousel
import com.bina.ai.ui.screens.hub.components.HubHeader
import com.bina.ai.ui.screens.hub.components.PublishFab
import com.bina.ai.ui.screens.hub.components.RecipeListItem
import com.bina.ai.ui.screens.hub.model.HubUiState
import com.bina.ai.ui.screens.recipe_detail.RecipeDetailSheet
import com.bina.ai.ui.theme.BinaGrayText

@Composable
fun HubScreen(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    userMode: UserMode,
    onConfigureRecipe: (recipeId: String) -> Unit,
    onOpenRecipe: (recipeId: String) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val factory = remember(miniAppRepository, installStore, userMode) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                HubViewModel(miniAppRepository, installStore, userMode, context.filesDir) as T
        }
    }
    val vm: HubViewModel = viewModel(key = "hub-${userMode.name}", factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    var sheetRecipe by remember { mutableStateOf<MiniApp?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            HubUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    HubHeader(mode = userMode)
                }
            }
            is HubUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { HubHeader(mode = s.mode) }
                    if (s.featured.isNotEmpty()) {
                        item {
                            FeaturedCarousel(
                                recipes = s.featured,
                                onRecipeClick = { sheetRecipe = it }
                            )
                        }
                    }
                    item {
                        CategoryChips(
                            categories = s.categories,
                            selected = s.selectedCategory,
                            onSelect = vm::selectCategory
                        )
                    }
                    if (s.selectedCategory == "All") {
                        items(s.rails, key = { it.title }) { rail ->
                            CategoryRail(
                                title = rail.title,
                                recipes = rail.recipes,
                                installedIds = s.installedIds,
                                authoredIds = s.authoredIds,
                                onRecipeClick = { sheetRecipe = it }
                            )
                        }
                        if (s.rails.isEmpty()) {
                            item {
                                EmptyHub("No recipes available.")
                            }
                        }
                    } else {
                        if (s.allRecipes.isEmpty()) {
                            item {
                                EmptyHub("No recipes in this category.")
                            }
                        } else {
                            items(s.allRecipes, key = { it.id }) { recipe ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    RecipeListItem(
                                        miniApp = recipe,
                                        isInstalled = recipe.id in s.installedIds,
                                        isAuthored = recipe.id in s.authoredIds,
                                        onClick = { sheetRecipe = recipe }
                                    )
                                }
                            }
                        }
                    }
                }

                if (s.mode == UserMode.ARCHITECT) {
                    PublishFab(
                        onClick = onOpenStudio,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }
            }
        }

        sheetRecipe?.let { recipe ->
            RecipeDetailSheet(
                miniApp = recipe,
                isInstalled = recipe.id in (state as? HubUiState.Loaded)?.installedIds.orEmpty(),
                sizeKb = 1.2f,   // base size only — total size lives in Configurator
                onConfigureInstall = {
                    sheetRecipe = null
                    onConfigureRecipe(recipe.id)
                },
                onOpen = {
                    sheetRecipe = null
                    onOpenRecipe(recipe.id)
                },
                onDismiss = { sheetRecipe = null }
            )
        }
    }
}

@Composable
private fun EmptyHub(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = BinaGrayText)
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD FAILED with "HubScreen call sites missing installStore/userMode/onConfigureRecipe params" — that's expected; the NavGraph rewires in Task 28.

To make compilation pass while keeping the new signature, temporarily comment out the old `HubScreen(...)` call in `BinaNavGraph.kt` to get a clean compile. Or skip ahead to Task 28 and combine. For tracking, accept BUILD FAILED here and proceed to NavGraph wiring.

- [ ] **Step 3: Commit (with broken-NavGraph note)**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/hub/HubScreen.kt
git commit -m "Rebuild HubScreen with carousel + chips + rails + detail sheet (NavGraph wiring follows)"
```

---

## Phase 6 — NavGraph wiring

### Task 27: Add `Screen.Configurator` route

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/navigation/Screen.kt`

- [ ] **Step 1: Read the current Screen file**

Run: `./gradlew.bat --version` (just to be sure the shell works), then view `Screen.kt` contents.

- [ ] **Step 2: Add the route**

Open `app/src/main/java/com/bina/ai/ui/navigation/Screen.kt`. Find the `MiniAppView` route definition. After it, add:

```kotlin
data object Configurator : Screen("configurator/{miniAppId}") {
    fun createRoute(miniAppId: String) = "configurator/$miniAppId"
}
```

(Match the existing style of the file. If `Screen` is a sealed class with constructors taking `route`, follow that pattern.)

- [ ] **Step 3: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/navigation/Screen.kt
git commit -m "Add Screen.Configurator route taking miniAppId arg"
```

---

### Task 28: Update `BinaNavGraph` — plumb InstallStore + Configurator route + auto-install

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt`

- [ ] **Step 1: Update the function signature**

Replace the `BinaNavGraph` function signature in `BinaNavGraph.kt` to add an `installStore` and `capabilityChecker` parameter:

```kotlin
import com.bina.ai.install.CapabilityChecker
import com.bina.ai.install.InstallStore
import com.bina.ai.install.model.InstallRecord
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BinaNavGraph(
    navController: NavHostController,
    userMode: UserMode,
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,                    // <-- new
    capabilityChecker: CapabilityChecker,          // <-- new
    inferenceEngine: InferenceEngine? = null,
    eventTracker: EventTracker,
    analyticsRepository: com.bina.ai.analytics.data.AnalyticsRepository
) {
    val coroutineScope = rememberCoroutineScope()
    // ... existing NavHost { ... } follows
```

- [ ] **Step 2: Replace the Hub composable block**

Inside the `NavHost(...)` block, find the existing `composable(Screen.Hub.route) { ... }` and replace with:

```kotlin
composable(Screen.Hub.route) {
    HubScreen(
        miniAppRepository = miniAppRepository,
        installStore = installStore,
        userMode = userMode,
        onConfigureRecipe = { id ->
            navController.navigate(Screen.Configurator.createRoute(id))
        },
        onOpenRecipe = { id ->
            navController.navigate(Screen.MiniAppView.createRoute(id))
        },
        onOpenStudio = {
            navController.navigate(Screen.Studio.route)
        }
    )
}
```

- [ ] **Step 3: Update the Studio composable for auto-install**

Replace the existing `composable(Screen.Studio.route) { ... }` block with:

```kotlin
composable(Screen.Studio.route) {
    StudioScreen(onPublished = { newRecipeId ->
        miniAppRepository.invalidateCache()
        miniAppRepository.getById(newRecipeId)?.let { recipe ->
            coroutineScope.launch {
                installStore.install(InstallRecord(
                    recipeId = recipe.id,
                    installedAt = System.currentTimeMillis(),
                    enabledFeatureIds = recipe.features.filter { it.recommended }.map { it.id }.toSet()
                ))
            }
        }
        navController.navigate(Screen.Hub.route) {
            popUpTo(Screen.Hub.route) { inclusive = true }
        }
    })
}
```

Note: this assumes `StudioScreen.onPublished` already takes a `String` parameter. **If it currently takes no args, that's the JY change we need to coordinate.** Confirm by checking `StudioScreen.kt`. If still `() -> Unit`, leave a TODO comment in the spot above and proceed (auto-install will simply not fire until JY's update).

- [ ] **Step 4: Add the Configurator composable block**

Inside the `NavHost(...)` block, after the `MiniAppView` composable, add:

```kotlin
composable(
    route = Screen.Configurator.route,
    arguments = listOf(navArgument("miniAppId") { type = NavType.StringType })
) { backStackEntry ->
    val miniAppId = backStackEntry.arguments?.getString("miniAppId") ?: return@composable
    val context = androidx.compose.ui.platform.LocalContext.current
    val recipe = remember(miniAppId) { miniAppRepository.getById(miniAppId) }
    val baseSizeKb = remember(recipe) {
        if (recipe != null) {
            try {
                context.assets.openFd("miniapps/${recipe.id}.yaml").use { it.length / 1024f }
            } catch (e: Exception) {
                1.0f   // sane fallback for sync-imported recipes
            }
        } else 0f
    }
    com.bina.ai.ui.screens.configurator.ConfiguratorScreen(
        miniAppId = miniAppId,
        miniAppRepository = miniAppRepository,
        installStore = installStore,
        capabilityChecker = capabilityChecker,
        baseSizeKb = baseSizeKb,
        onInstalled = { recipeName ->
            navController.navigate(Screen.Hub.route) { popUpTo(Screen.Hub.route) { inclusive = true } }
        },
        onBack = { navController.popBackStack() }
    )
}
```

- [ ] **Step 5: Compile**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD FAILED if `MainActivity` doesn't pass `installStore` + `capabilityChecker` (next task fixes this).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt
git commit -m "Wire InstallStore and Configurator route into BinaNavGraph; auto-install on Studio publish"
```

---

### Task 29: Update `MainActivity` to construct InstallStore + CapabilityChecker

**Files:**
- Modify: `app/src/main/java/com/bina/ai/MainActivity.kt`

- [ ] **Step 1: Read the current MainActivity**

Open `app/src/main/java/com/bina/ai/MainActivity.kt` and find where `BinaNavGraph` is invoked.

- [ ] **Step 2: Add InstallStore + CapabilityChecker construction**

Near the other top-level `remember { ... }` blocks where `analyticsRepository` etc. are constructed, add:

```kotlin
val installStore = remember { InstallStore.create(applicationContext) }
val capabilityChecker = remember { CapabilityChecker.create(applicationContext) }
```

Add the imports:
```kotlin
import com.bina.ai.install.CapabilityChecker
import com.bina.ai.install.InstallStore
```

- [ ] **Step 3: Pass them into BinaNavGraph**

In the `BinaNavGraph(...)` call, add the two new arguments:

```kotlin
BinaNavGraph(
    navController = navController,
    userMode = userMode,
    miniAppRepository = miniAppRepository,
    installStore = installStore,                  // <-- new
    capabilityChecker = capabilityChecker,        // <-- new
    inferenceEngine = inferenceEngine,
    eventTracker = eventTracker,
    analyticsRepository = analyticsRepository
)
```

- [ ] **Step 4: Compile + install**

Run: `./gradlew.bat :app:installDebug`
Expected: BUILD SUCCESSFUL. App installs.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/MainActivity.kt
git commit -m "Construct InstallStore and CapabilityChecker in MainActivity"
```

---

### Task 30: Minimum-touch update to `MyPocketScreen`

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/screens/pocket/MyPocketScreen.kt`
- Modify: `app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt`

- [ ] **Step 1: Read MyPocketScreen.kt**

Open `app/src/main/java/com/bina/ai/ui/screens/pocket/MyPocketScreen.kt`. Identify:
- Its current parameter list (likely `miniAppRepository`, `onMiniAppClick`)
- Where it calls `miniAppRepository.loadAll()`

- [ ] **Step 2: Add InstallStore parameter and filter**

At the top of the file, add the import:
```kotlin
import com.bina.ai.install.InstallStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
```

Add an `installStore: InstallStore` parameter to `MyPocketScreen`'s argument list (place it after `miniAppRepository`).

Inside the composable, replace the line that does `val miniApps = remember { miniAppRepository.loadAll() }` (or equivalent) with:

```kotlin
val installs by installStore.installs.collectAsStateWithLifecycle(initialValue = emptyMap())
val miniApps = remember(installs) {
    miniAppRepository.loadAll().filter { it.id in installs.keys }
}
```

Do not modify the rest of MyPocketScreen's layout.

- [ ] **Step 3: Update the NavGraph call to MyPocketScreen**

In `BinaNavGraph.kt`, find `MyPocketScreen(...)` and add `installStore = installStore`:

```kotlin
composable(Screen.MyPocket.route) {
    MyPocketScreen(
        miniAppRepository = miniAppRepository,
        installStore = installStore,           // <-- new
        onMiniAppClick = { miniAppId ->
            navController.navigate(Screen.MiniAppView.createRoute(miniAppId))
        }
    )
}
```

- [ ] **Step 4: Compile + install**

Run: `./gradlew.bat :app:installDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/pocket/MyPocketScreen.kt app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt
git commit -m "Filter MyPocket to installed recipes only (minimum-touch wiring)"
```

---

### Task 31: Confirm StudioScreen.onPublished signature change is in place

**Files:**
- Inspect: `app/src/main/java/com/bina/ai/ui/screens/studio/StudioScreen.kt`

- [ ] **Step 1: Inspect Studio's current signature**

Run: `grep -n 'onPublished' app/src/main/java/com/bina/ai/ui/screens/studio/StudioScreen.kt`

Expected output: line(s) showing `onPublished` parameter and where it's invoked.

- [ ] **Step 2: Decision branch**

If `onPublished: () -> Unit` (no String arg):
- DO NOT modify StudioScreen.kt — that's JY's territory.
- Instead, update the NavGraph block from Task 28 to handle this gracefully. Replace the auto-install block with:
  ```kotlin
  StudioScreen(onPublished = {
      miniAppRepository.invalidateCache()
      // TODO: when JY's StudioScreen passes the new recipe ID, auto-install here.
      navController.navigate(Screen.Hub.route) { popUpTo(Screen.Hub.route) { inclusive = true } }
  })
  ```
- Note this in your PR description so JY knows to update his side.

If `onPublished: (String) -> Unit` already (or trivial change can be made by JY):
- Leave Task 28's auto-install code in place.

- [ ] **Step 3: Re-compile if needed**

Run: `./gradlew.bat :app:installDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit (only if NavGraph was edited in Step 2)**

```bash
git add app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt
git commit -m "Adapt NavGraph Studio block to current onPublished signature"
```

---

## Phase 7 — Verification

### Task 32: Run full test suite

- [ ] **Step 1: Run all tests**

Run: `./gradlew.bat :app:testDebugUnitTest`
Expected: All tests PASS (29 total: 2 parse + 6 capability + 7 install + 4 size + 7 configurator + 9 hub-rail tests = 35 actually, count may vary).

- [ ] **Step 2: If any fail**

Read failure output, fix the production code (NOT the test). Re-run.

---

### Task 33: Manual smoke test on emulator

- [ ] **Step 1: Build and install fresh**

```powershell
$adb = 'D:\Android\Sdk\platform-tools\adb.exe'
& $adb shell pm clear com.bina.ai
.\gradlew.bat installDebug
& $adb shell am start -n com.bina.ai/.MainActivity
```

- [ ] **Step 2: Builder-mode flow**

  1. App launches in Builder mode. Hub shows: "Discover AI Recipes" header, featured carousel auto-advancing with Farm Buddy + Bidan Pintar, category chips with at least "All", "Health", "Agriculture", "Business".
  2. Tap a recipe card. Detail sheet slides up from the bottom. Stats grid shows real Recipe Size · Available Features · Dialect (no Downloads/Rating). "Configure & Install" button visible.
  3. Tap "Configure & Install". Configurator slides in. Header shows live Total Size + Active Count. Recommended toggles are on. SMS/P2P/Smart Notifications toggles are greyed (alpha 0.4, switch disabled).
  4. Toggle a feature off. Total size decreases, active count decrements with animation.
  5. Tap "Install to Pocket". Returns to Hub. Recipe card now shows "✓ Installed" badge.
  6. Tap the recipe again. Detail sheet now shows "Open" button instead of "Configure & Install".
  7. Tap "Open". Recipe launches as a MiniAppView. Press back.
  8. Switch to MyPocket tab. Only the installed recipe appears.

- [ ] **Step 3: Architect-mode flow**

  1. Toggle to Architect mode. Hub header changes to "Recipe Marketplace" with "CREATOR" pill.
  2. ExtendedFAB "Publish new" appears bottom-right.
  3. (No "Your Recipes" rail until something is published.) Tap FAB → Studio screen.
  4. (Smoke test only — do not actually publish unless Studio is in a working state.)

- [ ] **Step 4: Edge case — clear data and re-test empty state**

```powershell
& $adb shell pm clear com.bina.ai
& $adb shell am start -n com.bina.ai/.MainActivity
```

  1. MyPocket tab should be empty (or however JY's empty UI displays).
  2. Hub should show 3 recipes, all with no "✓ Installed" badge.

- [ ] **Step 5: If any flow breaks**

Pull logcat for the bina.ai PID:
```powershell
& $adb logcat -d -s MiniAppRepo:* AndroidRuntime:E *:E | Select-Object -Last 30
```
Identify the bug, fix it, commit, retest. Common issues: missing dialect string in YAML, capability token typo, theme color hex parse failure.

- [ ] **Step 6: Final commit if anything changed**

```bash
git status
# If changes: git add -A && git commit -m "Smoke-test polish"
```

---

## Coordination handoff for Jingyen

Before opening the PR, copy this into the PR body so JY knows what to expect:

```
Schema: added `Feature` data class + `MiniApp.features` field, default-safe — your generateYaml() keeps working.
Bundled YAMLs: `farm_buddy.yaml`, `bidan_pintar.yaml`, `buku_kira_kira.yaml` got `features:` blocks. Buku Kira-Kira's theme.primary recolored to #0EA5E9.
Studio: I assumed `onPublished` takes a recipe ID string. If your current signature is still `() -> Unit`, the auto-install block in BinaNavGraph is stubbed out with a TODO — please call `onPublished(newRecipeId)` after publish completes.
MyPocket: I added `installStore: InstallStore` parameter and filter to `recipeId in installs.keys`. Layout, header, sort, empty state, Yours badge, uninstall — all yours.
```

---

## Self-review checklist run

After completing all tasks:

1. **Spec coverage:** Every section of the spec has at least one task.
   - Schema (Feature + features field): Task 2
   - Bundled YAMLs: Task 3
   - InstallStore + InstallRecord + CapabilityChecker: Tasks 5-7
   - Recipe size: Task 8
   - ConfiguratorViewModel + state: Tasks 9-10
   - ConfiguratorScreen + components: Tasks 11-13
   - RecipeCover (cross-cutting): Task 14
   - RecipeDetailSheet + RecipeStats + FeaturePreviewList: Tasks 15-16
   - HubViewModel + computeRails: Tasks 17-18
   - Hub UI components: Tasks 19-25
   - HubScreen: Task 26
   - NavGraph wiring: Tasks 27-28
   - MainActivity: Task 29
   - MyPocket minimum touch: Task 30
   - Studio coordination: Task 31
   - Tests: Task 32
   - Smoke test: Task 33

2. **No placeholders:** Searched plan for TBD/TODO/etc — none in instructions (Task 31 has a TODO in *production* code that's intentionally a coordination cue for JY).

3. **Type consistency verified:**
   - `InstallRecord` shape consistent across InstallStore (Task 7), ConfiguratorViewModel (Task 10), and NavGraph auto-install (Task 28).
   - `Feature` fields used consistently across schema (Task 2), YAML (Task 3), tests (Tasks 4 + 9), and FeatureToggleCard (Task 11).
   - `computeRails` return type `List<Rail>` consistent.

---

**End of plan.**
