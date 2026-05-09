# Analytics Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Architect-mode Analytics tab with real on-device metrics — recipe launches, AI questions, active days, authoring stats, leaderboard, achievements — backed by a single Room `event_log` table and rendered with a polished animated Compose UI.

**Architecture:** Single Room database `analytics_db` with one table (`event_log`) and one DAO. An `EventTracker` is instantiated once in `MainActivity` and threaded into `MiniAppScreen` (logs `LAUNCH` on entry) and `ActionDispatcher` (logs `ASK` via callback). An `AnalyticsRepository` exposes `Flow`-typed query results that the `AnalyticsViewModel` collects and surfaces to the screen. The screen renders a hero card, 2×2 metric grid, daily activity chart (Compose Canvas), recipe leaderboard, achievement card, and empty state — all reading the Flows live.

**Tech Stack:** Kotlin 2.2.21, Jetpack Compose (BOM 2026.04.01), Room 2.6.1, KSP (matched to Kotlin version), Android `androidx.lifecycle` ViewModel, no external chart library (custom Canvas).

**Branch:** `feature/analytics`. Commits land on this branch and merge to `main` via PR after the work is verified.

**Testing approach:** No formal unit/instrumentation tests in this plan — the project has no test infrastructure today and adding it would burn hackathon time. Each task ends with a verification step (build green + emulator smoke check + logcat inspection where relevant). After the hackathon, recommended tests to backfill: DAO round-trip with Room InMemory, `AnalyticsRepository` queries with synthetic events, `AnalyticsViewModel` Flow assertions with `kotlinx-coroutines-test`.

---

## File Structure (created in this plan)

```
app/src/main/java/com/bina/ai/analytics/
├── data/
│   ├── EventEntity.kt          // @Entity row (id, recipe_id, event_type, timestamp_ms)
│   ├── EventDao.kt             // @Dao with insert + Flow queries
│   ├── EventType.kt            // enum class { LAUNCH, ASK }
│   ├── AnalyticsDatabase.kt    // @Database singleton + InstanceProvider
│   └── AnalyticsRepository.kt  // facade combining DAO flows + filesDir scan
├── tracking/
│   └── EventTracker.kt         // suspend logLaunch(id), suspend logAsk(id)
├── viewmodel/
│   └── AnalyticsViewModel.kt   // exposes StateFlow<AnalyticsUiState>
└── ui/
    ├── AnalyticsScreen.kt      // top-level composable that BinaNavGraph routes to
    ├── model/
    │   ├── TimeWindow.kt       // enum class { LAST_7D, LAST_30D, ALL_TIME }
    │   └── AnalyticsState.kt   // sealed UiState + supporting data classes
    └── components/
        ├── AnalyticsHeader.kt    // title + subtitle + TimeRangePill row
        ├── TimeRangePill.kt      // sliding-indicator segmented selector
        ├── HeroCard.kt           // big animated launches count + sparkline + delta
        ├── Sparkline.kt          // Canvas mini-chart, drawn line-by-line
        ├── MetricGrid.kt         // 2x2 layout of MetricCards
        ├── MetricCard.kt         // single glass card with animated number
        ├── AnimatedCounter.kt    // count-up Text composable
        ├── ActivityChart.kt      // Canvas bar chart, bars-rise animation, tap tooltip
        ├── RecipeLeaderboard.kt  // ranked list with Authored/Bundled badge
        ├── AchievementCard.kt    // gold gradient card + locked grid
        └── EmptyState.kt         // illustration + dual CTAs
```

## Files modified by this plan

| File | What changes |
|---|---|
| `build.gradle.kts` (root) | Add KSP plugin declaration |
| `app/build.gradle.kts` | Add KSP plugin id, Room dependencies |
| `app/.../MainActivity.kt` | Construct `AnalyticsDatabase` + `EventTracker`; pass tracker to `BinaNavGraph` |
| `app/.../ui/navigation/BinaNavGraph.kt` | Accept `eventTracker` parameter; thread to `MiniAppScreen` and `AnalyticsScreen` |
| `app/.../miniapp/ui/MiniAppScreen.kt` | Accept `eventTracker`; `LaunchedEffect` to log launch; pass `onAskLogged` lambda to `ActionDispatcher` |
| `shared/.../miniapp/runtime/ActionDispatcher.kt` | Add optional `onAskLogged: () -> Unit = {}` constructor param; invoke after safety check passes in `handleAsk` |
| `app/.../ui/screens/analytics/AnalyticsScreen.kt` | Delete (placeholder); replaced by new file at `analytics/ui/AnalyticsScreen.kt` |

---

## Task 1: Add Room and KSP dependencies

**Files:**
- Modify: `D:\AndroidStudioProjects\bina-v2\build.gradle.kts`
- Modify: `D:\AndroidStudioProjects\bina-v2\app\build.gradle.kts`

- [ ] **Step 1: Add KSP to root `build.gradle.kts`**

Replace the contents of `build.gradle.kts` (root) with:

```kotlin
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
}
```

The KSP version must exactly track the Kotlin version. `2.2.21-2.0.4` matches Kotlin `2.2.21`.

- [ ] **Step 2: Add KSP plugin + Room deps in `app/build.gradle.kts`**

In `app/build.gradle.kts`, add `id("com.google.devtools.ksp")` to the `plugins {}` block:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}
```

In the `dependencies {}` block, append (just before `debugImplementation("androidx.compose.ui:ui-tooling")`):

```kotlin
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
```

- [ ] **Step 3: Sync and verify the build**

In Android Studio: **File → Sync Project with Gradle Files**. Or from PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL. Common issues:
- If KSP version doesn't match Kotlin: error like "Inconsistent JVM target". Fix the KSP version in root `build.gradle.kts`.
- If Room can't find compiler: re-sync Gradle.

- [ ] **Step 4: Commit**

```powershell
git add build.gradle.kts app/build.gradle.kts
git commit -m "Add Room and KSP dependencies for Analytics"
```

---

## Task 2: Create the Room schema (Entity + EventType)

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/data/EventType.kt`
- Create: `app/src/main/java/com/bina/ai/analytics/data/EventEntity.kt`

- [ ] **Step 1: Create `EventType.kt`**

```kotlin
package com.bina.ai.analytics.data

/**
 * Discrete event categories logged in event_log.
 * Stored as the enum name string; new types can be appended without schema migration.
 */
enum class EventType {
    LAUNCH,  // user opened a recipe (MiniAppScreen entered)
    ASK      // user sent an `ask:` action that passed safety checks
}
```

- [ ] **Step 2: Create `EventEntity.kt`**

```kotlin
package com.bina.ai.analytics.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_log",
    indices = [Index(value = ["timestamp_ms", "recipe_id"])]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "recipe_id")
    val recipeId: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,  // EventType.name

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long
)
```

- [ ] **Step 3: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL. Room's annotation processor should not complain (the DAO + Database don't exist yet, so no other Room errors yet).

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/data/
git commit -m "Add EventType and EventEntity for analytics"
```

---

## Task 3: Create the EventDao with insert + count queries

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/data/EventDao.kt`

- [ ] **Step 1: Create `EventDao.kt`**

```kotlin
package com.bina.ai.analytics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the event_log table.
 *
 * All read methods return Flow so collectors auto-update when the table changes.
 * All write methods are suspend and must be called from a coroutine.
 */
@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: EventEntity)

    /** Total events of a given type within an inclusive timestamp window. */
    @Query("""
        SELECT COUNT(*) FROM event_log
        WHERE event_type = :eventType
          AND timestamp_ms >= :sinceMs
    """)
    fun observeCountSince(eventType: String, sinceMs: Long): Flow<Int>

    /** Distinct calendar days (UTC) within the window that had any event. */
    @Query("""
        SELECT COUNT(DISTINCT date(timestamp_ms / 1000, 'unixepoch')) FROM event_log
        WHERE timestamp_ms >= :sinceMs
    """)
    fun observeActiveDaysSince(sinceMs: Long): Flow<Int>

    /** All events newer than `sinceMs`, oldest-first. Used for chart bucketing. */
    @Query("""
        SELECT * FROM event_log
        WHERE timestamp_ms >= :sinceMs
        ORDER BY timestamp_ms ASC
    """)
    fun observeEventsSince(sinceMs: Long): Flow<List<EventEntity>>

    /** Per-recipe (event_type, count) for a window — used by the leaderboard. */
    @Query("""
        SELECT recipe_id, event_type, COUNT(*) AS cnt
        FROM event_log
        WHERE timestamp_ms >= :sinceMs
        GROUP BY recipe_id, event_type
    """)
    fun observeCountsByRecipe(sinceMs: Long): Flow<List<RecipeEventCount>>
}

/** Projection for [EventDao.observeCountsByRecipe]. */
data class RecipeEventCount(
    @androidx.room.ColumnInfo(name = "recipe_id") val recipeId: String,
    @androidx.room.ColumnInfo(name = "event_type") val eventType: String,
    @androidx.room.ColumnInfo(name = "cnt") val cnt: Int
)
```

The `date(... 'unixepoch')` SQLite function buckets timestamps to UTC days; we'll handle local-TZ display in the repository layer when we render charts.

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL. Room's annotation processor will not yet generate the implementation because there's no `@Database` referencing this DAO. That's fine.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/data/EventDao.kt
git commit -m "Add EventDao with insert and aggregate queries"
```

---

## Task 4: Create AnalyticsDatabase singleton

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/data/AnalyticsDatabase.kt`

- [ ] **Step 1: Create `AnalyticsDatabase.kt`**

```kotlin
package com.bina.ai.analytics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class],
    version = 1,
    exportSchema = false  // disable schema dump (we don't need migrations for hackathon)
)
abstract class AnalyticsDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AnalyticsDatabase? = null

        fun get(context: Context): AnalyticsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnalyticsDatabase::class.java,
                    "bina_analytics.db"
                )
                    // Hackathon: schema changes wipe the DB. Acceptable.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
```

`fallbackToDestructiveMigration(dropAllTables = true)` requires Room 2.6+ — already on 2.6.1 from Task 1.

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL. Now Room *will* generate the DAO implementation (the `@Database` references it). If you see errors like "Cannot find implementation for AnalyticsDatabase. AnalyticsDatabase_Impl does not exist", re-sync Gradle and rebuild.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/data/AnalyticsDatabase.kt
git commit -m "Add AnalyticsDatabase singleton"
```

---

## Task 5: Create EventTracker

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/tracking/EventTracker.kt`

- [ ] **Step 1: Create `EventTracker.kt`**

```kotlin
package com.bina.ai.analytics.tracking

import com.bina.ai.analytics.data.EventDao
import com.bina.ai.analytics.data.EventEntity
import com.bina.ai.analytics.data.EventType

/**
 * Single instance per app lifecycle. Used by:
 *  - MiniAppScreen (logs LAUNCH on first composition)
 *  - ActionDispatcher (logs ASK after safety check passes, via callback)
 */
class EventTracker(private val dao: EventDao) {

    suspend fun logLaunch(recipeId: String) {
        dao.insert(
            EventEntity(
                recipeId = recipeId,
                eventType = EventType.LAUNCH.name,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun logAsk(recipeId: String) {
        dao.insert(
            EventEntity(
                recipeId = recipeId,
                eventType = EventType.ASK.name,
                timestampMs = System.currentTimeMillis()
            )
        )
    }
}
```

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/tracking/EventTracker.kt
git commit -m "Add EventTracker for logging launches and asks"
```

---

## Task 6: Wire EventTracker into MainActivity and BinaNavGraph

**Files:**
- Modify: `app/src/main/java/com/bina/ai/MainActivity.kt`
- Modify: `app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt`

- [ ] **Step 1: Update `MainActivity.kt` to instantiate the database + tracker**

Find the `onCreate` body. After the existing `inferenceEngine = LiteRtLmEngine(applicationContext)` line, add the analytics initialization. The full `onCreate` should look like this (replace the existing one):

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val miniAppRepository = MiniAppRepository {
            val assetFiles = applicationContext.assets.list("miniapps") ?: emptyArray()
            val fromAssets = assetFiles.filter { it.endsWith(".yaml") || it.endsWith(".yml") }
                .map { it to applicationContext.assets.open("miniapps/$it").bufferedReader().readText() }

            val userDir = java.io.File(applicationContext.filesDir, "miniapps")
            val fromUser = if (userDir.isDirectory) {
                userDir.listFiles()
                    ?.filter { it.extension in listOf("yaml", "yml") }
                    ?.map { it.name to it.readText() }
                    ?: emptyList()
            } else emptyList()

            fromAssets + fromUser
        }

        inferenceEngine = LiteRtLmEngine(applicationContext)
        lifecycleScope.launch { inferenceEngine.initialize() }

        // Analytics infrastructure
        val analyticsDb = com.bina.ai.analytics.data.AnalyticsDatabase.get(applicationContext)
        val eventTracker = com.bina.ai.analytics.tracking.EventTracker(analyticsDb.eventDao())

        setContent {
            BinaTheme {
                var userMode by remember { mutableStateOf(UserMode.BUILDER) }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showShell = currentRoute in listOf(
                    Screen.Hub.route,
                    Screen.MyPocket.route,
                    Screen.OfflineSync.route,
                    Screen.Studio.route,
                    Screen.Analytics.route
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BinaScreenStart,
                                    BinaScreenMid,
                                    Color.White
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                    if (showShell) {
                        BinaTopBar(
                            userMode = userMode,
                            onToggleMode = {
                                userMode = if (userMode == UserMode.BUILDER)
                                    UserMode.ARCHITECT else UserMode.BUILDER
                                navController.navigate(Screen.Hub.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        BinaNavGraph(
                            navController = navController,
                            userMode = userMode,
                            miniAppRepository = miniAppRepository,
                            inferenceEngine = inferenceEngine,
                            eventTracker = eventTracker
                        )
                    }

                    if (showShell) {
                        BinaBottomNav(
                            userMode = userMode,
                            currentRoute = currentRoute,
                            onTabClick = { screen ->
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Hub.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
```

- [ ] **Step 2: Update `BinaNavGraph.kt` to accept and forward the tracker**

Replace the file contents:

```kotlin
package com.bina.ai.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bina.ai.analytics.tracking.EventTracker
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.ui.MiniAppScreen
import com.bina.ai.ui.screens.analytics.AnalyticsScreen
import com.bina.ai.ui.screens.hub.HubScreen
import com.bina.ai.ui.screens.pocket.MyPocketScreen
import com.bina.ai.ui.screens.studio.StudioScreen
import com.bina.ai.ui.screens.sync.OfflineSyncScreen

@Composable
fun BinaNavGraph(
    navController: NavHostController,
    userMode: UserMode,
    miniAppRepository: MiniAppRepository,
    inferenceEngine: InferenceEngine? = null,
    eventTracker: EventTracker
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Hub.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        composable(Screen.Hub.route) {
            HubScreen(
                miniAppRepository = miniAppRepository,
                onMiniAppClick = { miniAppId ->
                    navController.navigate(Screen.MiniAppView.createRoute(miniAppId))
                }
            )
        }

        composable(Screen.MyPocket.route) {
            MyPocketScreen(
                miniAppRepository = miniAppRepository,
                onMiniAppClick = { miniAppId ->
                    navController.navigate(Screen.MiniAppView.createRoute(miniAppId))
                }
            )
        }

        composable(Screen.OfflineSync.route) {
            OfflineSyncScreen()
        }

        composable(Screen.Studio.route) {
            StudioScreen(onPublished = {
                miniAppRepository.invalidateCache()
                navController.navigate(Screen.Hub.route) {
                    popUpTo(Screen.Hub.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }

        composable(
            route = Screen.MiniAppView.route,
            arguments = listOf(navArgument("miniAppId") { type = NavType.StringType })
        ) { backStackEntry ->
            val miniAppId = backStackEntry.arguments?.getString("miniAppId") ?: return@composable
            val miniApp = remember { miniAppRepository.getById(miniAppId) }
            if (miniApp != null) {
                MiniAppScreen(
                    miniApp = miniApp,
                    inferenceEngine = inferenceEngine,
                    eventTracker = eventTracker,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

We've added one parameter (`eventTracker: EventTracker`) and forwarded it to `MiniAppScreen`. The `AnalyticsScreen` route still calls the placeholder for now — we'll replace it in Task 18.

- [ ] **Step 3: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD will FAIL because `MiniAppScreen` doesn't accept `eventTracker` yet. We'll fix that in Task 7. This is OK — proceed.

- [ ] **Step 4: Commit**

Skip commit until Task 7 compiles. We're mid-flight.

---

## Task 7: Hook recipe launch tracking in MiniAppScreen

**Files:**
- Modify: `app/src/main/java/com/bina/ai/miniapp/ui/MiniAppScreen.kt`

- [ ] **Step 1: Add `eventTracker` parameter and `LaunchedEffect`**

Open `MiniAppScreen.kt`. Update the function signature and the body to log the launch event. Replace the existing function header and the first ~20 lines of the body:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import com.bina.ai.analytics.tracking.EventTracker

@Composable
fun MiniAppScreen(
    miniApp: MiniApp,
    inferenceEngine: InferenceEngine? = null,
    eventTracker: EventTracker,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val backingMap = remember { mutableStateMapOf<String, String>() }
    val store = remember { VariableStore(miniApp.variables, backingMap) }
    val formulaEngine = remember { FormulaEngine(miniApp.formulas) }
    var currentScreenId by remember { mutableStateOf(miniApp.screens.firstOrNull()?.id ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val themeColor = parseColor(miniApp.theme.primary)

    // Log launch event once per screen entry (re-fires if miniApp.id changes)
    LaunchedEffect(miniApp.id) {
        eventTracker.logLaunch(miniApp.id)
    }

    val dispatcher = remember {
        ActionDispatcher(
            store = store,
            miniApp = miniApp,
            formulaEngine = formulaEngine,
            locationProvider = AndroidLocationProvider(context),
            inferenceEngine = inferenceEngine,
            onNavigate = { screenId ->
                if (screenId == "home" || screenId == "back") {
                    currentScreenId = miniApp.screens.first().id
                } else {
                    currentScreenId = screenId
                }
            }
        )
    }

    // ... rest of function unchanged
```

The two imports (`LaunchedEffect` and `EventTracker`) need to be added at the top of the file alongside the other imports.

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL. The chain `MainActivity → BinaNavGraph → MiniAppScreen` is now type-consistent.

- [ ] **Step 3: Smoke test on emulator**

```powershell
$adb = 'C:\Users\ingzh\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb shell am force-stop com.bina.ai
& $adb logcat -c
.\gradlew.bat installDebug
& $adb shell am start -n com.bina.ai/.MainActivity
```

Then in the app: tap **Farm Buddy** in the Hub. The recipe screen should open as before.

To verify the launch event was logged, dump the database:

```powershell
& $adb shell run-as com.bina.ai sqlite3 /data/data/com.bina.ai/databases/bina_analytics.db "SELECT * FROM event_log;"
```

Expected: one row with `recipe_id=farm_buddy`, `event_type=LAUNCH`, recent timestamp.

If `run-as` fails with "package has corrupted ..." — that happens on some emulator images. Alternative: the next task adds asks, and Task 9 builds a UI that surfaces all events. You can defer DB inspection until then.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/bina/ai/MainActivity.kt
git add app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt
git add app/src/main/java/com/bina/ai/miniapp/ui/MiniAppScreen.kt
git commit -m "Wire EventTracker into NavGraph and log recipe launches"
```

---

## Task 8: Add `onAskLogged` callback to ActionDispatcher (shared module)

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/bina/ai/miniapp/runtime/ActionDispatcher.kt`

> Coordination: this is Jingyen's file. The change is fully backward-compatible (added param has default `{}`). Send the message you and Claude drafted earlier before merging this task to `main`. For now, on the `feature/analytics` branch it's safe to proceed.

- [ ] **Step 1: Add the optional parameter and invoke it after the safety check**

Replace the `ActionDispatcher` constructor signature and the `handleAsk` method. The full updated file:

```kotlin
package com.bina.ai.miniapp.runtime

import com.bina.ai.inference.InferenceEngine
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.platform.LocationProvider
import com.bina.ai.platform.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class ActionDispatcher(
    private val store: VariableStore,
    private val miniApp: MiniApp,
    private val formulaEngine: FormulaEngine,
    private val locationProvider: LocationProvider? = null,
    private val inferenceEngine: InferenceEngine? = null,
    private val onNavigate: (String) -> Unit,
    private val onAskLogged: () -> Unit = {}
) {

    suspend fun dispatch(action: String) {
        val interpolated = store.interpolate(action)
        val colonIndex = interpolated.indexOf(':')
        val prefix = if (colonIndex > 0) interpolated.substring(0, colonIndex) else interpolated
        val payload = if (colonIndex > 0) interpolated.substring(colonIndex + 1) else ""

        Logger.d(TAG, "Dispatch: $prefix | $payload")

        when (prefix) {
            "ask" -> handleAsk(payload)
            "vision_ask" -> handleVisionAsk(payload)
            "formula" -> handleFormula(payload)
            "go" -> handleGo(payload)
            "geolocate" -> handleGeolocate()
            "set" -> handleSet(payload)
            else -> Logger.w(TAG, "Unknown action: $prefix")
        }
    }

    private suspend fun handleAsk(prompt: String) {
        if (prompt.isBlank() || store.isTrue("is_loading")) return

        val blocked = miniApp.safety.blockedKeywords.any { kw ->
            prompt.contains(kw, ignoreCase = true)
        }
        if (blocked) {
            store["ai_response"] = miniApp.safety.escalationMessage
                .ifEmpty { "This request has been blocked for safety." }
            return
        }

        // Safety passed — log this as a real ask event
        onAskLogged()

        store["ai_response"] = ""
        store["is_loading"] = "true"

        val engine = inferenceEngine
        if (engine == null || !engine.isReady) {
            store["ai_response"] = "**Model not loaded.** Push a `.litertlm` model to `/data/local/tmp/` to enable AI."
            store["is_loading"] = "false"
            return
        }

        try {
            val sb = StringBuilder()
            engine.generate(prompt, miniApp.model.systemPrompt)
                .onEach { chunk ->
                    sb.append(chunk)
                    store["ai_response"] = sb.toString()
                }
                .catch { e ->
                    Logger.e(TAG, "Inference error", e)
                    store["ai_response"] = sb.toString().ifEmpty { "Error: ${e.message}" }
                }
                .collect()
        } finally {
            store["is_loading"] = "false"
        }
    }

    private suspend fun handleVisionAsk(prompt: String) {
        if (store.isTrue("is_loading")) return

        val photoPath = store["photo_path"]
        if (photoPath.isBlank()) {
            store["ai_response"] = "Please take a photo first."
            return
        }

        store["ai_response"] = ""
        store["is_loading"] = "true"

        val engine = inferenceEngine
        if (engine == null || !engine.isReady) {
            store["ai_response"] = "**Model not loaded.** Push a `.litertlm` model to `/data/local/tmp/` to enable vision AI."
            store["is_loading"] = "false"
            return
        }

        try {
            val sb = StringBuilder()
            engine.generateWithImage(prompt, photoPath, miniApp.model.systemPrompt)
                .onEach { chunk ->
                    sb.append(chunk)
                    store["ai_response"] = sb.toString()
                }
                .catch { e ->
                    Logger.e(TAG, "Vision inference error", e)
                    store["ai_response"] = sb.toString().ifEmpty { "Error: ${e.message}" }
                }
                .collect()
        } finally {
            store["is_loading"] = "false"
        }
    }

    private fun handleFormula(formulaId: String) {
        formulaEngine.evaluate(formulaId, store)
    }

    private fun handleGo(screenId: String) {
        onNavigate(screenId)
    }

    private suspend fun handleGeolocate() {
        store["is_loading"] = "true"
        try {
            val location = locationProvider?.getCurrentLocation()
            if (location != null) {
                store["user_location"] = "${location.first},${location.second}"
                Logger.d(TAG, "Got location: ${location.first}, ${location.second}")
            } else {
                store["user_location"] = DEFAULT_LOCATION
                Logger.w(TAG, "Location unavailable, using default coords")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Location error", e)
            store["user_location"] = DEFAULT_LOCATION
        }
        store["is_loading"] = "false"
    }

    private fun handleSet(payload: String) {
        val eqIndex = payload.indexOf('=')
        if (eqIndex > 0) {
            val key = payload.substring(0, eqIndex)
            val value = payload.substring(eqIndex + 1)
            store[key] = value
        }
    }

    companion object {
        private const val TAG = "ActionDispatcher"
        private const val DEFAULT_LOCATION = "3.139,101.687"
    }
}
```

The only changes vs the existing file: added the `onAskLogged: () -> Unit = {}` constructor param and the call to `onAskLogged()` after the safety check passes in `handleAsk`. Default is a no-op so existing call sites don't need to change.

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add shared/src/commonMain/kotlin/com/bina/ai/miniapp/runtime/ActionDispatcher.kt
git commit -m "Add optional onAskLogged callback to ActionDispatcher"
```

---

## Task 9: Wire ask tracking from MiniAppScreen via the callback

**Files:**
- Modify: `app/src/main/java/com/bina/ai/miniapp/ui/MiniAppScreen.kt`

- [ ] **Step 1: Pass the `onAskLogged` lambda into ActionDispatcher**

In `MiniAppScreen.kt`, update the `dispatcher = remember { ... }` block to bind the recipe ID:

```kotlin
    val dispatcher = remember {
        ActionDispatcher(
            store = store,
            miniApp = miniApp,
            formulaEngine = formulaEngine,
            locationProvider = AndroidLocationProvider(context),
            inferenceEngine = inferenceEngine,
            onNavigate = { screenId ->
                if (screenId == "home" || screenId == "back") {
                    currentScreenId = miniApp.screens.first().id
                } else {
                    currentScreenId = screenId
                }
            },
            onAskLogged = {
                // Fire-and-forget: launch a child coroutine on the screen's scope
                scope.launch { eventTracker.logAsk(miniApp.id) }
            }
        )
    }
```

The `scope.launch { ... }` indirection is needed because `onAskLogged: () -> Unit` is non-suspend but `eventTracker.logAsk` is suspend.

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Smoke test on emulator**

```powershell
$adb = 'C:\Users\ingzh\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb shell am force-stop com.bina.ai
.\gradlew.bat installDebug
& $adb shell am start -n com.bina.ai/.MainActivity
```

In the app: open Farm Buddy → type "hello" in the text input → tap **Ask Farm Buddy** → wait for the response.

Then dump the DB:

```powershell
& $adb shell run-as com.bina.ai sqlite3 /data/data/com.bina.ai/databases/bina_analytics.db "SELECT recipe_id, event_type, datetime(timestamp_ms/1000,'unixepoch','localtime') FROM event_log ORDER BY id;"
```

Expected: at least one `LAUNCH` row and one `ASK` row, both with `recipe_id=farm_buddy`.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/bina/ai/miniapp/ui/MiniAppScreen.kt
git commit -m "Log ask events from MiniAppScreen via dispatcher callback"
```

---

## Task 10: Define UI state types (TimeWindow + AnalyticsState)

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/model/TimeWindow.kt`
- Create: `app/src/main/java/com/bina/ai/analytics/ui/model/AnalyticsState.kt`

- [ ] **Step 1: Create `TimeWindow.kt`**

```kotlin
package com.bina.ai.analytics.ui.model

import java.util.concurrent.TimeUnit

enum class TimeWindow(val label: String) {
    LAST_7D("7d"),
    LAST_30D("30d"),
    ALL_TIME("All");

    /** Inclusive lower bound for SQL queries. ALL_TIME returns 0 (epoch). */
    fun sinceMs(now: Long = System.currentTimeMillis()): Long = when (this) {
        LAST_7D -> now - TimeUnit.DAYS.toMillis(7)
        LAST_30D -> now - TimeUnit.DAYS.toMillis(30)
        ALL_TIME -> 0L
    }

    /** Number of days in the window for chart bucketing. ALL_TIME defaults to 30. */
    val chartBucketCount: Int get() = when (this) {
        LAST_7D -> 7
        LAST_30D -> 30
        ALL_TIME -> 30
    }
}
```

- [ ] **Step 2: Create `AnalyticsState.kt`**

```kotlin
package com.bina.ai.analytics.ui.model

/** Top-level UI state for AnalyticsScreen. */
sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data object Empty : AnalyticsUiState
    data class Loaded(
        val window: TimeWindow,
        val metrics: MetricsSnapshot,
        val chart: List<DailyBucket>,
        val leaderboard: List<RecipeStats>,
        val achievements: List<Achievement>
    ) : AnalyticsUiState
}

/** Numbers for the hero card + 2x2 metric grid. */
data class MetricsSnapshot(
    val totalLaunches: Int,
    val totalLaunchesPrevious: Int,   // for delta calculation
    val recipesPublished: Int,
    val questionsAsked: Int,
    val activeDays: Int,
    val knowledgeBytes: Long
)

/** One day's worth of stacked-bar data for the activity chart. */
data class DailyBucket(
    val dayStartMs: Long,
    val launches: Int,
    val asks: Int
) {
    val total: Int get() = launches + asks
}

/** A row in the recipe leaderboard. */
data class RecipeStats(
    val recipeId: String,
    val displayName: String,   // resolved from MiniAppRepository, fallback to id
    val icon: String,          // emoji from recipe, fallback "📦"
    val launches: Int,
    val asks: Int,
    val isAuthored: Boolean
) {
    val total: Int get() = launches + asks
}

/** Achievement state shown in the bottom card. */
data class Achievement(
    val id: AchievementId,
    val title: String,
    val description: String,
    val emoji: String,
    val unlocked: Boolean,
    val progress: Float       // 0f..1f for progress bars on locked items
)

enum class AchievementId { FIRST_AUTHOR, CURIOUS, STREAK, KNOWLEDGE_ARCHITECT }
```

- [ ] **Step 3: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/model/
git commit -m "Add TimeWindow and AnalyticsUiState types"
```

---

## Task 11: Build AnalyticsRepository (metrics + filesDir scan)

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/data/AnalyticsRepository.kt`

- [ ] **Step 1: Create `AnalyticsRepository.kt`**

```kotlin
package com.bina.ai.analytics.data

import android.content.Context
import com.bina.ai.analytics.ui.model.Achievement
import com.bina.ai.analytics.ui.model.AchievementId
import com.bina.ai.analytics.ui.model.DailyBucket
import com.bina.ai.analytics.ui.model.MetricsSnapshot
import com.bina.ai.analytics.ui.model.RecipeStats
import com.bina.ai.analytics.ui.model.TimeWindow
import com.bina.ai.miniapp.MiniAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.util.Calendar
import java.util.TimeZone

/**
 * Combines the Room event_log Flows with filesystem-derived authoring stats.
 *
 * - Usage queries (launches, asks, active days, chart, leaderboard) span ALL recipes.
 * - Authoring queries (recipesPublished, knowledgeBytes) are scoped to filesDir.
 *
 * The `miniAppRepository` is used to resolve display names and icons for the leaderboard.
 */
class AnalyticsRepository(
    private val dao: EventDao,
    private val miniAppRepository: MiniAppRepository,
    private val filesDir: File
) {

    private val miniappsDir = File(filesDir, "miniapps")
    private val knowledgeDir = File(filesDir, "knowledge")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMetrics(window: Flow<TimeWindow>): Flow<MetricsSnapshot> =
        window.flatMapLatest { tw ->
            val now = System.currentTimeMillis()
            val sinceMs = tw.sinceMs(now)
            val previousSinceMs = previousWindowStart(tw, now)

            combine(
                dao.observeCountSince("LAUNCH", sinceMs),
                dao.observeCountSince("LAUNCH", previousSinceMs),
                dao.observeCountSince("ASK", sinceMs),
                dao.observeActiveDaysSince(sinceMs)
            ) { launches, prevLaunches, asks, activeDays ->
                MetricsSnapshot(
                    totalLaunches = launches,
                    totalLaunchesPrevious = (prevLaunches - launches).coerceAtLeast(0),
                    recipesPublished = scanAuthoredRecipeIds().size,
                    questionsAsked = asks,
                    activeDays = activeDays,
                    knowledgeBytes = scanKnowledgeBytes()
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeChartData(window: Flow<TimeWindow>): Flow<List<DailyBucket>> =
        window.flatMapLatest { tw ->
            val now = System.currentTimeMillis()
            dao.observeEventsSince(tw.sinceMs(now)).map { events ->
                bucketizeByDay(events, tw, now)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLeaderboard(window: Flow<TimeWindow>): Flow<List<RecipeStats>> =
        window.flatMapLatest { tw ->
            dao.observeCountsByRecipe(tw.sinceMs()).map { rows ->
                val authoredIds = scanAuthoredRecipeIds()
                rows.groupBy { it.recipeId }.map { (recipeId, eventRows) ->
                    val launches = eventRows.firstOrNull { it.eventType == "LAUNCH" }?.cnt ?: 0
                    val asks = eventRows.firstOrNull { it.eventType == "ASK" }?.cnt ?: 0
                    val app = miniAppRepository.getById(recipeId)
                    RecipeStats(
                        recipeId = recipeId,
                        displayName = app?.name ?: recipeId,
                        icon = app?.icon ?: "📦",
                        launches = launches,
                        asks = asks,
                        isAuthored = recipeId in authoredIds
                    )
                }.sortedByDescending { it.total }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAchievements(window: Flow<TimeWindow>): Flow<List<Achievement>> =
        window.flatMapLatest { tw ->
            combine(
                dao.observeCountSince("ASK", 0L),                    // all-time asks
                dao.observeEventsSince(tw.sinceMs())                 // recent events for streak calc
            ) { totalAsks, recentEvents ->
                val authored = scanAuthoredRecipeIds()
                val knowledgeFiles = scanKnowledgeFileCount()
                val streak = computeMaxConsecutiveActiveDays(recentEvents, System.currentTimeMillis())

                listOf(
                    Achievement(
                        id = AchievementId.FIRST_AUTHOR,
                        title = "First Author",
                        description = "Publish your first recipe in Studio",
                        emoji = "📜",
                        unlocked = authored.isNotEmpty(),
                        progress = if (authored.isNotEmpty()) 1f else 0f
                    ),
                    Achievement(
                        id = AchievementId.CURIOUS,
                        title = "Curious",
                        description = "Ask 10 questions across any recipes",
                        emoji = "💬",
                        unlocked = totalAsks >= 10,
                        progress = (totalAsks / 10f).coerceIn(0f, 1f)
                    ),
                    Achievement(
                        id = AchievementId.STREAK,
                        title = "Streak",
                        description = "Use Bina 3 consecutive days",
                        emoji = "🔥",
                        unlocked = streak >= 3,
                        progress = (streak / 3f).coerceIn(0f, 1f)
                    ),
                    Achievement(
                        id = AchievementId.KNOWLEDGE_ARCHITECT,
                        title = "Knowledge Architect",
                        description = "Upload 5 files across your recipes",
                        emoji = "📚",
                        unlocked = knowledgeFiles >= 5,
                        progress = (knowledgeFiles / 5f).coerceIn(0f, 1f)
                    )
                )
            }
        }

    /** True when the user has zero events AND zero authored recipes. */
    fun observeIsEmpty(): Flow<Boolean> =
        combine(
            dao.observeCountSince("LAUNCH", 0L),
            dao.observeCountSince("ASK", 0L)
        ) { launches, asks ->
            launches == 0 && asks == 0 && scanAuthoredRecipeIds().isEmpty()
        }

    // ---- helpers ----------------------------------------------------------

    private fun scanAuthoredRecipeIds(): Set<String> {
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

    private fun scanKnowledgeBytes(): Long {
        if (!knowledgeDir.isDirectory) return 0L
        return knowledgeDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun scanKnowledgeFileCount(): Int {
        if (!knowledgeDir.isDirectory) return 0
        return knowledgeDir.walkTopDown().count { it.isFile }
    }

    private fun previousWindowStart(window: TimeWindow, now: Long): Long {
        val span = now - window.sinceMs(now)
        return (window.sinceMs(now) - span).coerceAtLeast(0L)
    }

    private fun bucketizeByDay(
        events: List<EventEntity>,
        window: TimeWindow,
        now: Long
    ): List<DailyBucket> {
        val tz = TimeZone.getDefault()
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        val buckets = (0 until window.chartBucketCount).map { i ->
            todayStart - (window.chartBucketCount - 1 - i) * 86_400_000L
        }
        val bucketLaunches = IntArray(buckets.size)
        val bucketAsks = IntArray(buckets.size)

        events.forEach { ev ->
            val idx = ((ev.timestampMs - buckets.first()) / 86_400_000L).toInt()
            if (idx in buckets.indices) {
                if (ev.eventType == "LAUNCH") bucketLaunches[idx]++
                else if (ev.eventType == "ASK") bucketAsks[idx]++
            }
        }
        return buckets.indices.map { i ->
            DailyBucket(buckets[i], bucketLaunches[i], bucketAsks[i])
        }
    }

    private fun computeMaxConsecutiveActiveDays(events: List<EventEntity>, now: Long): Int {
        if (events.isEmpty()) return 0
        val tz = TimeZone.getDefault()
        val daysWithActivity = events.map { ev ->
            Calendar.getInstance(tz).apply {
                timeInMillis = ev.timestampMs
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSortedSet()

        var maxRun = 0
        var run = 0
        var prev: Long? = null
        for (day in daysWithActivity) {
            run = if (prev != null && day - prev == 86_400_000L) run + 1 else 1
            if (run > maxRun) maxRun = run
            prev = day
        }
        return maxRun
    }
}
```

This file is the largest piece of pure logic in the plan. Two notes:
- The `MetricsSnapshot.totalLaunchesPrevious` field is used by `HeroCard` to show the delta arrow. The math (`prevLaunches - launches`) intentionally returns the previous *period's* count by subtracting current from cumulative — adjust later if the delta math feels wrong on the screen.
- Streak calculation uses local TZ start-of-day buckets and walks the sorted set looking for consecutive 24h gaps.

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/data/AnalyticsRepository.kt
git commit -m "Add AnalyticsRepository combining Room + filesDir scan"
```

---

## Task 12: Build AnalyticsViewModel

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/viewmodel/AnalyticsViewModel.kt`

- [ ] **Step 1: Create the ViewModel**

```kotlin
package com.bina.ai.analytics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.analytics.data.AnalyticsRepository
import com.bina.ai.analytics.ui.model.AnalyticsUiState
import com.bina.ai.analytics.ui.model.TimeWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class AnalyticsViewModel(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _window = MutableStateFlow(TimeWindow.LAST_7D)
    val window: StateFlow<TimeWindow> = _window.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.observeMetrics(_window),
        repository.observeChartData(_window),
        repository.observeLeaderboard(_window),
        repository.observeAchievements(_window),
        repository.observeIsEmpty()
    ) { metrics, chart, leaderboard, achievements, isEmpty ->
        if (isEmpty) AnalyticsUiState.Empty
        else AnalyticsUiState.Loaded(
            window = _window.value,
            metrics = metrics,
            chart = chart,
            leaderboard = leaderboard,
            achievements = achievements
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = AnalyticsUiState.Loading
    )

    fun setWindow(newWindow: TimeWindow) {
        _window.value = newWindow
    }
}
```

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/viewmodel/AnalyticsViewModel.kt
git commit -m "Add AnalyticsViewModel with combined state flow"
```

---

## Task 13: Build small UI primitives (AnimatedCounter, TimeRangePill, MetricCard)

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/AnimatedCounter.kt`
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/TimeRangePill.kt`
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/MetricCard.kt`

- [ ] **Step 1: Create `AnimatedCounter.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFeature
import androidx.compose.ui.text.font.FontFeatureSettings

/**
 * Counts up smoothly from the current displayed value to [target].
 *
 * The Text uses tabular-monospace digits via fontFeatureSettings so digits
 * don't jitter horizontally as they animate.
 */
@Composable
fun AnimatedCounter(
    target: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    durationMs: Int = 600,
    formatter: (Int) -> String = { it.toString() }
) {
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = durationMs),
        label = "AnimatedCounter"
    )
    Text(
        text = formatter(animated),
        modifier = modifier,
        style = style.copy(fontFeatureSettings = "tnum")
    )
}
```

- [ ] **Step 2: Create `TimeRangePill.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.TimeWindow
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun TimeRangePill(
    selected: TimeWindow,
    onSelect: (TimeWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = TimeWindow.entries
    val pillWidth = 64.dp
    val pillHeight = 32.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(4.dp)
            .height(pillHeight)
    ) {
        val selectedIndex = options.indexOf(selected)
        val indicatorOffsetX by animateDpAsState(
            targetValue = pillWidth * selectedIndex,
            animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f),
            label = "indicator"
        )

        // Sliding indicator
        Box(
            modifier = Modifier
                .width(pillWidth)
                .fillMaxHeight()
                .padding(start = indicatorOffsetX)
                .clip(RoundedCornerShape(16.dp))
                .background(BinaPrimary)
        )

        // Labels
        androidx.compose.foundation.layout.Row {
            options.forEach { window ->
                val isSelected = window == selected
                val textAlpha by animateFloatAsState(if (isSelected) 1f else 0.6f, label = "alpha")
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .fillMaxHeight()
                        .clickable { onSelect(window) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = window.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else BinaPrimary.copy(alpha = textAlpha)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create `MetricCard.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun MetricCard(
    label: String,
    value: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    formatter: (Int) -> String = { it.toString() }
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            AnimatedCounter(
                target = value,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BinaPrimary
                ),
                formatter = formatter
            )
            Text(label, fontSize = 12.sp, color = BinaGrayText)
        }
    }
}
```

- [ ] **Step 4: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/components/AnimatedCounter.kt
git add app/src/main/java/com/bina/ai/analytics/ui/components/TimeRangePill.kt
git add app/src/main/java/com/bina/ai/analytics/ui/components/MetricCard.kt
git commit -m "Add Analytics UI primitives: counter, time pill, metric card"
```

---

## Task 14: Build Sparkline + HeroCard

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/Sparkline.kt`
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/HeroCard.kt`

- [ ] **Step 1: Create `Sparkline.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun Sparkline(
    values: List<Int>,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    if (values.size < 2) {
        Canvas(modifier) { /* nothing to draw */ }
        return
    }

    var animationKey by remember { mutableStateOf(values) }
    LaunchedEffect(values) { animationKey = values }

    val drawProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "sparkline-draw"
    )

    Canvas(modifier = modifier) {
        val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(1)
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height * (1f - v.toFloat() / maxValue) * 0.9f + size.height * 0.05f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
```

- [ ] **Step 2: Create `HeroCard.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

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
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.MetricsSnapshot
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaRed
import com.bina.ai.ui.theme.BinaSecondary
import kotlin.math.roundToInt

@Composable
fun HeroCard(
    metrics: MetricsSnapshot,
    sparklineValues: List<Int>,
    modifier: Modifier = Modifier
) {
    val deltaPct = if (metrics.totalLaunchesPrevious > 0) {
        ((metrics.totalLaunches - metrics.totalLaunchesPrevious).toFloat()
            / metrics.totalLaunchesPrevious * 100f).roundToInt()
    } else if (metrics.totalLaunches > 0) {
        100
    } else {
        0
    }

    val (deltaIcon, deltaColor) = when {
        deltaPct > 0 -> Icons.Filled.TrendingUp to BinaGreen
        deltaPct < 0 -> Icons.Filled.TrendingDown to BinaRed
        else -> Icons.Filled.TrendingFlat to Color.White.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(BinaPrimary, BinaSecondary))
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Total Launches",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    AnimatedCounter(
                        target = metrics.totalLaunches,
                        style = TextStyle(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(deltaIcon, null, tint = deltaColor, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (deltaPct >= 0) "+${deltaPct}%" else "${deltaPct}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = deltaColor
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Sparkline(
                values = sparklineValues,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
```

- [ ] **Step 3: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/components/Sparkline.kt
git add app/src/main/java/com/bina/ai/analytics/ui/components/HeroCard.kt
git commit -m "Add Sparkline and HeroCard with delta and trend"
```

---

## Task 15: Build MetricGrid (uses MetricCard)

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/MetricGrid.kt`

- [ ] **Step 1: Create `MetricGrid.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bina.ai.analytics.ui.model.MetricsSnapshot
import com.bina.ai.ui.theme.BinaAmber
import com.bina.ai.ui.theme.BinaBlue
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary
import kotlinx.coroutines.delay

@Composable
fun MetricGrid(
    metrics: MetricsSnapshot,
    modifier: Modifier = Modifier
) {
    var visibleCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..4) { delay(80); visibleCount = i }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StaggeredCard(visibleAt = 0, current = visibleCount, modifier = Modifier.weight(1f)) {
                MetricCard(
                    label = "Recipes Published",
                    value = metrics.recipesPublished,
                    icon = Icons.Filled.AccountTree,
                    accentColor = BinaPrimary
                )
            }
            StaggeredCard(visibleAt = 1, current = visibleCount, modifier = Modifier.weight(1f)) {
                MetricCard(
                    label = "Questions Asked",
                    value = metrics.questionsAsked,
                    icon = Icons.Filled.QuestionAnswer,
                    accentColor = BinaGreen
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StaggeredCard(visibleAt = 2, current = visibleCount, modifier = Modifier.weight(1f)) {
                MetricCard(
                    label = "Active Days",
                    value = metrics.activeDays,
                    icon = Icons.Filled.CalendarMonth,
                    accentColor = BinaBlue
                )
            }
            StaggeredCard(visibleAt = 3, current = visibleCount, modifier = Modifier.weight(1f)) {
                MetricCard(
                    label = "Knowledge",
                    value = (metrics.knowledgeBytes / 1024).toInt(),  // KB
                    icon = Icons.Filled.MenuBook,
                    accentColor = BinaAmber,
                    formatter = { kb ->
                        if (kb >= 1024) "%.1f MB".format(kb / 1024f) else "$kb KB"
                    }
                )
            }
        }
    }
}

@Composable
private fun StaggeredCard(
    visibleAt: Int,
    current: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = current > visibleAt,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 4 },
        modifier = modifier
    ) {
        content()
    }
}
```

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/components/MetricGrid.kt
git commit -m "Add MetricGrid with staggered entrance animation"
```

---

## Task 16: Build ActivityChart (Canvas bar chart)

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/ActivityChart.kt`

- [ ] **Step 1: Create `ActivityChart.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.DailyBucket
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityChart(
    buckets: List<DailyBucket>,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(buckets) {
        animationProgress = 0f
        animationProgress = 1f
    }
    val animProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label = "chart-rise"
    )

    var selectedIndex by remember { mutableStateOf(-1) }
    val selectedLabel = if (selectedIndex in buckets.indices) {
        val b = buckets[selectedIndex]
        val day = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(b.dayStartMs))
        "$day · ${b.launches} launches · ${b.asks} asks"
    } else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text("Daily Activity", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BinaPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            selectedLabel ?: "Tap a bar for details",
            fontSize = 11.sp,
            color = BinaGrayText
        )
        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(buckets) {
                    detectTapGestures { tap ->
                        if (buckets.isEmpty()) return@detectTapGestures
                        val barTotalWidth = size.width / buckets.size
                        val idx = (tap.x / barTotalWidth).toInt().coerceIn(0, buckets.size - 1)
                        selectedIndex = if (selectedIndex == idx) -1 else idx
                    }
                }
        ) {
            if (buckets.isEmpty()) return@Canvas
            val maxTotal = (buckets.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
            val barTotalWidth = size.width / buckets.size
            val barWidth = barTotalWidth * 0.6f
            val barOffsetX = (barTotalWidth - barWidth) / 2

            buckets.forEachIndexed { i, b ->
                val staggerStart = i.toFloat() / buckets.size * 0.3f
                val barProgress = ((animProgress - staggerStart) / (1f - staggerStart))
                    .coerceIn(0f, 1f)

                val totalH = (b.total.toFloat() / maxTotal) * size.height * barProgress
                val launchH = (b.launches.toFloat() / maxTotal) * size.height * barProgress
                val askH = (b.asks.toFloat() / maxTotal) * size.height * barProgress

                val x = i * barTotalWidth + barOffsetX
                val isSelected = i == selectedIndex
                val alpha = if (selectedIndex == -1 || isSelected) 1f else 0.4f

                // Launches segment (bottom)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(BinaPrimary.copy(alpha = alpha), BinaPrimary.copy(alpha = alpha * 0.7f))
                    ),
                    topLeft = Offset(x, size.height - launchH),
                    size = Size(barWidth, launchH)
                )
                // Asks segment (stacked on top)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(BinaGreen.copy(alpha = alpha), BinaGreen.copy(alpha = alpha * 0.7f))
                    ),
                    topLeft = Offset(x, size.height - launchH - askH),
                    size = Size(barWidth, askH)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/components/ActivityChart.kt
git commit -m "Add ActivityChart with stacked bars and tap tooltip"
```

---

## Task 17: Build RecipeLeaderboard

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/RecipeLeaderboard.kt`

- [ ] **Step 1: Create `RecipeLeaderboard.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

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
import androidx.compose.foundation.shape.CircleShape
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
import com.bina.ai.analytics.ui.model.RecipeStats
import com.bina.ai.ui.theme.BinaAmber
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun RecipeLeaderboard(
    rows: List<RecipeStats>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Most-Used Recipes",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = BinaPrimary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No recipe activity in this window yet",
                    fontSize = 12.sp,
                    color = BinaGrayText
                )
            }
        } else {
            rows.take(8).forEachIndexed { index, row ->
                LeaderboardRow(rank = index + 1, row = row)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, row: RecipeStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(rankColor(rank).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rankColor(rank))
        }
        Text(row.icon, fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaPrimary)
                AuthoredBadge(authored = row.isAuthored)
            }
            Text(
                "${row.launches} launches · ${row.asks} asks",
                fontSize = 11.sp,
                color = BinaGrayText
            )
        }
        Text(
            "${row.total}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BinaPrimary
        )
    }
}

@Composable
private fun AuthoredBadge(authored: Boolean) {
    val (label, color) = if (authored) "Authored" to BinaGreen else "Bundled" to BinaGrayText
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun rankColor(rank: Int) = when (rank) {
    1 -> BinaAmber
    2 -> Color(0xFF94A3B8)  // silver
    3 -> Color(0xFFB45309)  // bronze
    else -> BinaPrimary
}
```

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/components/RecipeLeaderboard.kt
git commit -m "Add RecipeLeaderboard with rank colors and authored badge"
```

---

## Task 18: Build AchievementCard

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/AchievementCard.kt`

- [ ] **Step 1: Create `AchievementCard.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.Achievement
import com.bina.ai.ui.theme.BinaAmber
import com.bina.ai.ui.theme.BinaGrayBorder
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun AchievementCard(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier
) {
    val featured = achievements.firstOrNull { it.unlocked } ?: achievements.firstOrNull()
    val locked = achievements.filter { it != featured }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Achievements",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = BinaPrimary,
            modifier = Modifier.padding(start = 4.dp)
        )
        if (featured != null) FeaturedAchievement(featured)
        if (locked.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                locked.forEach { LockedAchievement(it) }
            }
        }
    }
}

@Composable
private fun FeaturedAchievement(a: Achievement) {
    val infinite = rememberInfiniteTransition(label = "sparkle")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(BinaAmber, Color(0xFFFFC56C)))
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(if (a.unlocked) pulse else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(a.emoji, fontSize = 28.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(a.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Text(
                    if (a.unlocked) "Unlocked" else a.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                if (!a.unlocked) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { a.progress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedAchievement(a: Achievement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BinaGrayBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(a.emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(a.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BinaPrimary)
            Text(a.description, fontSize = 11.sp, color = BinaGrayText)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { a.progress },
                color = BinaPrimary,
                trackColor = BinaGrayBorder,
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
            )
        }
    }
}
```

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/components/AchievementCard.kt
git commit -m "Add AchievementCard with featured pulse and locked progress"
```

---

## Task 19: Build EmptyState

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/EmptyState.kt`

- [ ] **Step 1: Create `EmptyState.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaSecondary

@Composable
fun EmptyState(
    onOpenHub: () -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Compose-drawn illustration: three concentric circles with icon
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(BinaPrimary.copy(alpha = 0.05f))
            )
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(BinaPrimary.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(BinaPrimary, BinaSecondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📊", fontSize = 32.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "No activity yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = BinaPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Open a recipe from the Hub or publish your own in Studio to start seeing analytics.",
            fontSize = 13.sp,
            color = BinaGrayText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onOpenHub,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Open Hub", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onOpenStudio,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
            ) {
                Text("Open Studio", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
```

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/components/EmptyState.kt
git commit -m "Add EmptyState illustration and dual CTAs"
```

---

## Task 20: Build AnalyticsHeader

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/components/AnalyticsHeader.kt`

- [ ] **Step 1: Create `AnalyticsHeader.kt`**

```kotlin
package com.bina.ai.analytics.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.TimeWindow
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun AnalyticsHeader(
    selectedWindow: TimeWindow,
    onSelectWindow: (TimeWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaPrimary)
            Text("Your authoring and on-device usage", fontSize = 12.sp, color = BinaGrayText)
        }
        TimeRangePill(selected = selectedWindow, onSelect = onSelectWindow)
    }
}
```

- [ ] **Step 2: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/components/AnalyticsHeader.kt
git commit -m "Add AnalyticsHeader with title and time range pill"
```

---

## Task 21: Assemble AnalyticsScreen and replace placeholder

**Files:**
- Create: `app/src/main/java/com/bina/ai/analytics/ui/AnalyticsScreen.kt`
- Modify: `app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt`
- Delete: `app/src/main/java/com/bina/ai/ui/screens/analytics/AnalyticsScreen.kt`

- [ ] **Step 1: Create the new `AnalyticsScreen.kt`**

```kotlin
package com.bina.ai.analytics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.bina.ai.analytics.data.AnalyticsRepository
import com.bina.ai.analytics.ui.components.AchievementCard
import com.bina.ai.analytics.ui.components.ActivityChart
import com.bina.ai.analytics.ui.components.AnalyticsHeader
import com.bina.ai.analytics.ui.components.EmptyState
import com.bina.ai.analytics.ui.components.HeroCard
import com.bina.ai.analytics.ui.components.MetricGrid
import com.bina.ai.analytics.ui.components.RecipeLeaderboard
import com.bina.ai.analytics.ui.model.AnalyticsUiState
import com.bina.ai.analytics.viewmodel.AnalyticsViewModel

@Composable
fun AnalyticsScreen(
    repository: AnalyticsRepository,
    onOpenHub: () -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val factory = remember(repository) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                AnalyticsViewModel(repository) as T
        }
    }
    val viewModel: AnalyticsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        AnalyticsUiState.Loading -> {
            // Brief loading: show header but empty body so the screen never flashes blank.
            Column(modifier = modifier.fillMaxSize()) {
                AnalyticsHeader(
                    selectedWindow = viewModel.window.collectAsStateWithLifecycle().value,
                    onSelectWindow = viewModel::setWindow
                )
            }
        }
        AnalyticsUiState.Empty -> {
            Column(modifier = modifier.fillMaxSize()) {
                AnalyticsHeader(
                    selectedWindow = viewModel.window.collectAsStateWithLifecycle().value,
                    onSelectWindow = viewModel::setWindow
                )
                EmptyState(onOpenHub = onOpenHub, onOpenStudio = onOpenStudio)
            }
        }
        is AnalyticsUiState.Loaded -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    AnalyticsHeader(
                        selectedWindow = s.window,
                        onSelectWindow = viewModel::setWindow
                    )
                }
                item {
                    HeroCard(
                        metrics = s.metrics,
                        sparklineValues = s.chart.map { it.total },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    MetricGrid(
                        metrics = s.metrics,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    ActivityChart(
                        buckets = s.chart,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    RecipeLeaderboard(
                        rows = s.leaderboard,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    AchievementCard(
                        achievements = s.achievements,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
```

- [ ] **Step 2: Delete the old placeholder**

```powershell
git rm app/src/main/java/com/bina/ai/ui/screens/analytics/AnalyticsScreen.kt
```

- [ ] **Step 3: Update `BinaNavGraph.kt` to use the new screen and pass dependencies**

In `BinaNavGraph.kt`, change the import:

```kotlin
// Replace this line:
//   import com.bina.ai.ui.screens.analytics.AnalyticsScreen
// with this:
import com.bina.ai.analytics.ui.AnalyticsScreen
```

We also need to construct an `AnalyticsRepository` for the screen. The cleanest place: pass it in as a parameter, just like `eventTracker`. Update the `BinaNavGraph` signature and the Analytics route block:

```kotlin
@Composable
fun BinaNavGraph(
    navController: NavHostController,
    userMode: UserMode,
    miniAppRepository: MiniAppRepository,
    inferenceEngine: InferenceEngine? = null,
    eventTracker: EventTracker,
    analyticsRepository: com.bina.ai.analytics.data.AnalyticsRepository
) {
    // ... existing routes unchanged ...

    composable(Screen.Analytics.route) {
        AnalyticsScreen(
            repository = analyticsRepository,
            onOpenHub = {
                navController.navigate(Screen.Hub.route) {
                    popUpTo(Screen.Hub.route) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onOpenStudio = {
                navController.navigate(Screen.Studio.route) {
                    popUpTo(Screen.Hub.route) { saveState = true }
                    launchSingleTop = true
                }
            }
        )
    }
}
```

- [ ] **Step 4: Update `MainActivity.kt` to construct the repository**

Just after the `eventTracker` line, add:

```kotlin
        val analyticsRepository = com.bina.ai.analytics.data.AnalyticsRepository(
            dao = analyticsDb.eventDao(),
            miniAppRepository = miniAppRepository,
            filesDir = applicationContext.filesDir
        )
```

And update the `BinaNavGraph(...)` call to pass it:

```kotlin
                        BinaNavGraph(
                            navController = navController,
                            userMode = userMode,
                            miniAppRepository = miniAppRepository,
                            inferenceEngine = inferenceEngine,
                            eventTracker = eventTracker,
                            analyticsRepository = analyticsRepository
                        )
```

- [ ] **Step 5: Build verify**

```powershell
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Smoke test on emulator**

```powershell
$adb = 'C:\Users\ingzh\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb shell am force-stop com.bina.ai
.\gradlew.bat installDebug
& $adb shell am start -n com.bina.ai/.MainActivity
```

In the app:
1. Toggle to **Architect** mode (top right)
2. Tap the **Analytics** tab
3. Expected: empty state (illustration + "No activity yet") on first run.
4. Tap **Open Hub**, tap **Farm Buddy**, ask one question, return to Analytics.
5. Expected: hero card animates a count, chart shows a tiny bar, leaderboard shows Farm Buddy.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/AnalyticsScreen.kt
git add app/src/main/java/com/bina/ai/ui/navigation/BinaNavGraph.kt
git add app/src/main/java/com/bina/ai/MainActivity.kt
git commit -m "Wire AnalyticsScreen into NavGraph and remove placeholder"
```

---

## Task 22: Polish — haptic feedback on time-range changes

**Files:**
- Modify: `app/src/main/java/com/bina/ai/analytics/ui/components/TimeRangePill.kt`

- [ ] **Step 1: Add haptic feedback when a different segment is tapped**

In `TimeRangePill.kt`, add the imports and update the click handler:

```kotlin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

Inside the `TimeRangePill` composable, add:

```kotlin
    val haptics = LocalHapticFeedback.current
```

(Place it near the top, before `options`.)

Then update the click handler from:

```kotlin
                        .clickable { onSelect(window) },
```

to:

```kotlin
                        .clickable {
                            if (window != selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(window)
                            }
                        },
```

`TextHandleMove` is the lightest predefined haptic — feels like a tiny tick.

- [ ] **Step 2: Build verify and smoke test**

```powershell
.\gradlew.bat installDebug
& $adb shell am start -n com.bina.ai/.MainActivity
```

In the app: Architect → Analytics → tap each segment of the time-range pill. On a real device you'd feel a tiny vibration; on emulator you may hear a sound but no haptic.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/bina/ai/analytics/ui/components/TimeRangePill.kt
git commit -m "Add haptic feedback on time-range change"
```

---

## Task 23: Final integration smoke test on emulator

**Files:** none modified.

This task verifies the full screen end-to-end. No code changes.

- [ ] **Step 1: Clean install and run**

```powershell
$adb = 'C:\Users\ingzh\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb shell pm clear com.bina.ai   # wipe app data so we start from empty state
.\gradlew.bat installDebug
& $adb shell am start -n com.bina.ai/.MainActivity
```

(Note: `pm clear` deletes the analytics DB. The model file at `/data/local/tmp/` is unaffected.)

- [ ] **Step 2: Walk the demo flow**

1. **Architect mode → Analytics:** confirm the empty state shows ("No activity yet" + dual CTAs).
2. Tap **Open Hub** → it should navigate to Hub.
3. Tap **Farm Buddy** in Hub → MiniApp opens. Wait briefly.
4. Type "hi" in the text input → tap **Ask Farm Buddy** → wait for the AI to respond.
5. Hit back → return to Hub → switch to Architect mode → tap Analytics.
6. **Expected:**
   - Hero card shows `1` Total Launches with count-up animation.
   - Sparkline draws.
   - 2×2 grid: 0 Recipes Published, 1 Question Asked, 1 Active Day, 0 KB Knowledge.
   - Activity chart: tiny bar on today's date.
   - Leaderboard: Farm Buddy listed with `1 launches · 1 asks · Bundled` badge.
   - Achievement card: First Author, Curious, Streak, Knowledge Architect — all locked, with progress bars (Curious at 10%, Streak at 33%).
7. Tap a different time-range pill segment (`30d` / `All`). All values should re-animate.
8. Open Farm Buddy again, ask another question. Return. Numbers should bump.

- [ ] **Step 3: If anything looks wrong, log evidence**

```powershell
& $adb logcat -d | Select-String -Pattern 'AndroidRuntime|FATAL|Bina|EventTracker' | Select-Object -Last 60
```

- [ ] **Step 4: Final commit (if any tweaks were needed)**

If you adjusted spacing, colors, or copy during the visual smoke test, commit those:

```powershell
git status
git add <whatever-changed>
git commit -m "Polish: <brief description>"
```

- [ ] **Step 5: Push the branch**

```powershell
git push -u origin feature/analytics
```

Then open a PR to `main` from the GitHub UI when you're ready.

---

## Spec deviations (intentional, for time)

The spec mentions three pieces of polish that this plan does **not** implement, to keep the hackathon scope tight. None of them block the demo:

- **Pull-to-refresh** — redundant given Room Flow integration auto-updates the UI on every event. The screen always reflects current state without a manual refresh gesture.
- **Glass cards lift on tap** — cards already get a Material ripple via `clickable`; an explicit elevation animation on tap is deferred.
- **Parallax on the hero card during scroll** — `LazyColumn` doesn't easily expose continuous scroll offset; would require switching to `rememberLazyListState` + `nestedScroll` and computing translation. Deferred.

If you finish the 23 tasks with time to spare, these are the next polish items to pick up.

---

## Post-hackathon: tests to backfill

If you have time after the hackathon to harden this, add (in order of value):

1. **`AnalyticsRepositoryTest`** (instrumented test, `androidTest/`): use Room's `inMemoryDatabaseBuilder`, insert synthetic events with known timestamps, assert that `observeMetrics`, `observeChartData`, `observeLeaderboard` emit expected values. This catches regressions in the SQL query logic.
2. **`AnalyticsViewModelTest`** (unit, `test/`): use `kotlinx-coroutines-test` and a fake repository, assert `uiState` transitions Loading → Empty → Loaded as the fake emits.
3. **Compose UI smoke tests**: `ComposeTestRule` + `onNodeWithText("Total Launches")` to verify the screen renders without crashing.

Required test deps to add (when you're ready):

```kotlin
// app/build.gradle.kts
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.room:room-testing:2.6.1")
```
