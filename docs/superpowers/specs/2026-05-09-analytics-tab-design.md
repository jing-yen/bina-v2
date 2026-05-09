# Analytics Tab — Design Spec

**Date:** 2026-05-09
**Owner:** User (Hub / Offline Sync / Analytics)
**Status:** Draft for review
**Branch:** `feature/analytics`

## Goal

Build the Architect-mode **Analytics tab** as a fully functional, top-tier UI surfacing real on-device data about recipes the Architect has authored. No mock data, no backend.

## Motivation

Bina.ai is edge-native by design. The Figma mockup of Analytics shows global metrics (total downloads, regional distribution, average ratings) that require a server. Adding Firebase would contradict the pitch ("zero cloud cost, zero internet"), eat hackathon time, and add 5–10 MB to the APK.

Instead: real local-device metrics about the Architect's authoring activity and how those recipes are being used on this device. The data is genuinely real; it's just scoped to one device. For the demo this is sufficient because the Architect can publish recipes via Studio and exercise them, and the screen will populate live.

## Scope

### In scope
- Architect-mode-only screen (Builder mode does not see Analytics).
- Activity tracking: recipe launches, AI questions asked.
- Display layer: hero card, 2×2 metric grid, daily activity chart, recipe leaderboard, achievement card, empty state.
- Live updates: when a tracked event fires, the screen reflects it without manual refresh (Room `Flow` integration).
- Time-range selection: 7d / 30d / All time, in-memory only (no persistence across sessions).

### Out of scope
- Backend / Firebase / cloud sync. Will not be added.
- Global metrics that require multiple users (download counts, average ratings, regional distribution).
- Tracking events for *bundled* recipes in `assets/miniapps/` — only recipes in `filesDir/miniapps/` (Studio output) count toward Architect metrics.
- Drill-down navigation from metric cards or leaderboard rows. Tap feedback (ripple, scale) is implemented; navigation targets are deferred.
- Persisting time-range selection across sessions.
- Schema migrations. `fallbackToDestructiveMigration()` is acceptable for hackathon timeframe.

## UI design

### Layout (top to bottom)

1. **Header strip** — "Analytics" title and subtitle. Right side: a sliding **time-range pill** (`7d` / `30d` / `All`) with a frosted indicator that animates between segments.

2. **Hero card** — full-width, taller than the rest. Displays "Total Launches" as the headline number, animating from 0 to target in ~600 ms when the screen mounts or the time range changes. A thin sparkline below the number shows the trend across the selected window, drawn line-by-line on entry. A delta badge (`+24%`) on the right shows change vs the immediately preceding period of equal length, with an arrow icon and color-shift (green / red / neutral).

3. **Metric grid (2×2)** — four glass cards, stagger-fading in with ~80 ms delay between them:

   | Card | Source |
   |---|---|
   | Recipes Published | count of `*.yaml` files in `filesDir/miniapps/` |
   | Questions Asked | total `ASK` events in window where `recipe_id IN (authoredIds)` |
   | Active Days | distinct calendar days (device local TZ) within window where the user had at least one `LAUNCH` or `ASK` event against an authored recipe |
   | Knowledge | sum of file sizes in `filesDir/knowledge/`, formatted MB/KB |

   Each card has a colored icon chip, an animated number, and a tiny trend indicator. Tap → ripple + scale-down feedback. No drill-down navigation in v1.

4. **Activity chart** — daily stacked bar chart of `LAUNCH` and `ASK` events over the selected window. Bars rise from 0 on entry (staggered left-to-right). Tap a bar → tooltip with exact counts. Drawn directly with Compose `Canvas` for full motion control; brand gradient fills (Bina deep blue + green).

5. **Recipe leaderboard ("My Most-Used Recipes")** — sortable list of authored recipes, each row showing rank badge, recipe icon, name, launch count, ask count, and a tiny inline sparkline. Tap row → ripple + scale feedback. No drill-down in v1.

6. **Achievement card** at the bottom — gold-gradient pinned card showing the most recently unlocked achievement, with sparkle animation on the icon. Locked achievements are tappable below it (greyed out, with progress indicators).

   Achievement triggers (all derivable from local state):
   - **First Author** — first recipe published via Studio
   - **Curious** — 10 questions asked across authored recipes
   - **Streak** — 3+ consecutive active days
   - **Knowledge Architect** — 5+ files uploaded across all recipes' knowledge bases

### Empty state

For fresh devices with no recipes published:

- A friendly Compose-drawn illustration (geometric shapes, brand colors).
- Headline: "No recipes yet."
- Body: "Publish your first recipe in Studio to see analytics."
- CTA button: "Open Studio" → navigates to the Studio tab.

This replaces the entire screen body until the first recipe is published.

### Polish details

- All numbers use a tabular-monospace digit treatment so they don't jitter as they animate.
- Pull-to-refresh on the screen (re-queries everything; not strictly necessary thanks to Flow updates, but expected UX).
- Subtle haptic feedback on time-range changes via `LocalHapticFeedback` (Android Vibrator, ~10 ms tick).
- Glass cards have a subtle elevation shadow that lifts slightly on tap.
- Smooth scroll with parallax on the hero card.

The visual language matches the existing app: `BinaTheme` colors (`BinaPrimary` deep blue + `BinaGreen`), the existing `BinaScreenStart → BinaScreenMid → White` gradient background.

## Data architecture

### Room database

One database, one table:

```sql
CREATE TABLE event_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id    TEXT    NOT NULL,
    event_type   TEXT    NOT NULL,    -- 'LAUNCH' or 'ASK'
    timestamp_ms INTEGER NOT NULL     -- epoch millis, UTC
);
CREATE INDEX idx_event_log_ts_recipe ON event_log(timestamp_ms, recipe_id);
```

`event_type` is stored as a string for forward compatibility; in Kotlin it's modeled as a sealed class / enum (`EventType.LAUNCH`, `EventType.ASK`) to add new types later without schema changes.

`fallbackToDestructiveMigration()` is set on the database builder. Schema changes wipe the local DB.

### Identifying authored recipes

No `is_user_authored` column. The set of authored recipe IDs is derived at query time by listing `filesDir/miniapps/*.yaml` and parsing the `id:` field from each. This means:

- Deleting a YAML from `filesDir` removes the recipe from analytics on next refresh.
- Bundled `assets/miniapps/` recipes are automatically excluded.
- Logging is unfiltered; filtering happens in queries via `recipe_id IN (:authoredIds)`.

### Repository facade

`AnalyticsRepository` exposes coroutine `Flow` APIs:

| Function | Returns | Purpose |
|---|---|---|
| `observeMetrics(window: TimeWindow)` | `Flow<MetricsSnapshot>` | hero + 2×2 metrics |
| `observeChartData(window: TimeWindow)` | `Flow<List<DailyBucket>>` | bar chart data |
| `observeLeaderboard(window: TimeWindow)` | `Flow<List<RecipeStats>>` | leaderboard rows |
| `observeAchievements()` | `Flow<List<Achievement>>` | achievement state |

Room's built-in Flow integration ensures all `observe*` Flows emit a new value whenever `event_log` is mutated.

### ViewModel

`AnalyticsViewModel(repository)` exposes:

- `state: StateFlow<AnalyticsUiState>` (sealed: `Loading`, `Empty`, `Loaded(...)`)
- `setTimeWindow(window: TimeWindow)` — updates a backing `MutableStateFlow<TimeWindow>` that the Flows in the repository transform on.

The Composable observes `state` via `collectAsStateWithLifecycle`.

## Tracking hooks

Three integration points:

### 1. Recipe launch — `app/.../miniapp/ui/MiniAppScreen.kt`

```kotlin
LaunchedEffect(miniApp.id) {
    eventTracker.logLaunch(miniApp.id)
}
```

Fires once per screen entry, not on recomposition. The `eventTracker` is threaded into `MiniAppScreen` from `BinaNavGraph`.

### 2. AI question asked — `shared/.../runtime/ActionDispatcher.kt`

The `ActionDispatcher` lives in the KMP shared module and must not depend on Android / Room. Solution: add an optional callback parameter:

```kotlin
class ActionDispatcher(
    // ...existing parameters unchanged...,
    private val onAskLogged: () -> Unit = {}
) {
    // ...
    private suspend fun handleAsk(action: String) {
        // ...existing safety check + interpolation...
        onAskLogged()  // fires only after safety check passes
        // ...existing inference call...
    }
}
```

Default value of `{}` keeps the API backward-compatible. The app layer constructs `ActionDispatcher` in `MiniAppScreen` and binds the recipe ID in the closure:

```kotlin
ActionDispatcher(
    // ...,
    onAskLogged = { eventTracker.logAsk(miniApp.id) }
)
```

The shared module has zero new dependencies. Only the optional parameter is added.

### 3. The `EventTracker` itself

```kotlin
class EventTracker(private val dao: EventDao) {
    suspend fun logLaunch(recipeId: String) {
        dao.insert(EventEntity(
            recipeId = recipeId,
            eventType = "LAUNCH",
            timestampMs = System.currentTimeMillis()
        ))
    }
    suspend fun logAsk(recipeId: String) {
        dao.insert(EventEntity(
            recipeId = recipeId,
            eventType = "ASK",
            timestampMs = System.currentTimeMillis()
        ))
    }
}
```

Single instance per app lifecycle. Constructed in `MainActivity.onCreate` alongside `AnalyticsDatabase`.

## File structure

### New files

```
app/src/main/java/com/bina/ai/analytics/
├── data/
│   ├── EventEntity.kt
│   ├── EventDao.kt
│   ├── EventType.kt
│   ├── AnalyticsDatabase.kt
│   └── AnalyticsRepository.kt
├── tracking/
│   └── EventTracker.kt
├── viewmodel/
│   └── AnalyticsViewModel.kt
└── ui/
    ├── AnalyticsScreen.kt
    ├── model/
    │   ├── TimeWindow.kt
    │   └── AnalyticsState.kt
    └── components/
        ├── AnalyticsHeader.kt
        ├── HeroCard.kt
        ├── MetricGrid.kt
        ├── MetricCard.kt
        ├── ActivityChart.kt
        ├── RecipeLeaderboard.kt
        ├── AchievementCard.kt
        ├── EmptyState.kt
        └── AnimatedCounter.kt
```

### Edits to existing files

| File | Change |
|---|---|
| `app/build.gradle.kts` | Add Room dependencies + KSP plugin |
| `build.gradle.kts` (root) | Add KSP plugin to `plugins {}` block |
| `MainActivity.kt` | Construct `AnalyticsDatabase` and `EventTracker`; pass tracker into `BinaNavGraph` |
| `BinaNavGraph.kt` | Accept `eventTracker` param; pass to `MiniAppScreen` and `AnalyticsScreen` routes |
| `MiniAppScreen.kt` | Add `LaunchedEffect` to log launch; pass `onAskLogged` lambda to `ActionDispatcher` |
| `shared/.../ActionDispatcher.kt` | Add optional `onAskLogged: () -> Unit = {}` constructor param; invoke after safety check passes |
| `app/.../ui/screens/analytics/AnalyticsScreen.kt` (existing placeholder) | Delete this file; the new screen lives at `app/.../analytics/ui/AnalyticsScreen.kt` and `BinaNavGraph.kt` is updated to import from there |

## Dependencies

### Add to `app/build.gradle.kts`

```kotlin
plugins {
    // existing plugins...
    id("com.google.devtools.ksp")
}

dependencies {
    // existing dependencies...
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
```

### Add to root `build.gradle.kts`

```kotlin
plugins {
    // existing plugins...
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
}
```

(KSP version must match the Kotlin version; verify against `libs.versions.toml` if used.)

### Not added

- **No chart library.** The bar chart is drawn with Compose `Canvas`. Trade-off: ~120 lines of custom code, but unrestricted control over animations, gradients, and tap interactions.
- **No DataStore.** Time-range selection is in-memory `MutableStateFlow`. Persistence across sessions is out of scope.
- **No Hilt / Koin.** Dependencies are constructed manually in `MainActivity` and threaded via composable parameters. The dependency graph is small enough that DI framework adds more ceremony than it saves.

Total new dependency footprint: Room runtime (~150 KB) + KSP plugin.

## Coordination needed

The change to `shared/.../ActionDispatcher.kt` touches Jingyen's module. The change is fully backward-compatible (added parameter has a default), but it should be flagged in advance and merged carefully.

Suggested message:

> "Adding an optional `onAskLogged: () -> Unit = {}` constructor param to `ActionDispatcher`, fired after the safety check passes in `handleAsk`. Default no-op so existing callers don't change. Used by Analytics to count AI questions per recipe. Cool with this?"

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| User has no published recipes → screen looks empty | Empty state with "Publish your first recipe in Studio" CTA + animated illustration |
| Inference is slow on emulator (~2 min) → asks accumulate slowly during testing | Acceptable; events are still logged correctly. Test on real device for snappier demo data. |
| Custom Canvas chart code is buggy or visually off | Fallback option: drop in Vico (`com.patrykandpatrick.vico:compose-m3`) — well-documented, ~5 lines of usage. |
| Room schema changes during development wipe local data | `fallbackToDestructiveMigration()` is enabled; testing data is cheap to regenerate. |
| Jingyen pushes conflicting changes to `ActionDispatcher` | Coordinate via the suggested message; the change is small enough to merge by hand if needed. |

## Open questions

None at design-approval time. (Schema is locked, hooks are locked, UI is locked.)
