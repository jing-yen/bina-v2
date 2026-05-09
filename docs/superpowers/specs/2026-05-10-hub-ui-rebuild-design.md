# Hub UI Rebuild — Design Spec

**Date:** 2026-05-10
**Branch target:** new branch from `main` (after `feature/analytics` merges)
**Owner:** Ing (with minimal coordination touches on Jingyen's MyPocket and Studio)

## Summary

Rebuild the Hub screen and add a real install flow. Tap a recipe → bottom-sheet preview → full-screen feature configurator → install to MyPocket → launch from MyPocket. Layout is "Spotify-Hybrid" — featured carousel, category chips, horizontal section rails. Architect mode gets persona-specific surfaces (header copy, pinned authored rail, Publish FAB, Yours badge). Persistence via DataStore. No fake metrics anywhere.

**Customization is one-shot:** feature toggles are chosen exactly once, in the Hub Configurator before install. MyPocket is purely a launchpad — no re-configure, no settings. Users who want to change their feature set must uninstall and re-install (uninstall itself is parked for v1).

## Goals

- Replace the bare `HubScreen` with a polished, persona-aware browse experience using the extended `MiniApp` schema (`cover_image`, `featured`, `emergency`, `dialect`, `tags`).
- Introduce a real install flow with per-feature toggles declared in YAML.
- Persist install records (which recipes installed + which features enabled) via DataStore.
- Auto-install Architect-published recipes on completion of Studio publish.
- Use real edge-native data only — no global download counts, ratings, or reviews.

## Non-goals

- Recipe search (premature for ≤10 recipes)
- Sort/filter UI in Hub or MyPocket
- Uninstall UX (parked — JY's call once MyPocket polish lands)
- Permission requests at install time (use-time is standard Android pattern)
- Global ratings / downloads / reviews
- Recipe versioning, update prompts, drag-to-reorder
- MyPocket layout polish (header redesign, sort, "Yours" badge, empty-state illustration) — left to JY
- Compose preview screenshot tests, full accessibility audit

## User experience overview

Four surfaces in this spec:

1. **Hub** — featured carousel, category chips, horizontal rails of recipe cards, mode-aware header. Architect-only Publish FAB.
2. **Recipe Detail Sheet** — modal bottom sheet over Hub. Cover hero, real stats (Recipe Size, Available Features, Dialect), description, tag list, feature preview. Adaptive primary CTA: "Configure & Install" when not installed, "Open" when installed.
3. **Configurator Screen** — full-screen feature toggle list. Header shows live Total Download Size and Active Features count. Per-feature: icon, name, description, recommended pill, size delta, switch. Greyed when capability not satisfied. Bottom CTA: "Install to Pocket". Reachable only from Hub's detail sheet for not-yet-installed recipes.
4. **MyPocket (minimum touch)** — filtered to installed recipes only. Tap a row to launch. No customize, no settings — MyPocket is a pure launchpad. Layout/aesthetics stay JY's.

```
Hub  ──tap card──►  RecipeDetailSheet (modal over Hub)
                         │
                         │ "Configure & Install"
                         ▼
                    ConfiguratorScreen (full screen)
                         │
                         │ "Install to Pocket"
                         ▼
                    InstallStore.install() → popBackStack(Hub) + snackbar
                                                        │
                                                  user taps MyPocket tab
                                                        │
                                                        ▼
                                            MyPocket (filtered to installed)
                                                        │
                                                  tap row ──► launch (MiniAppView)
```

## Architecture

### File layout

```
app/src/main/java/com/bina/ai/ui/screens/
├── hub/
│   ├── HubScreen.kt                   # entry, owns scroll/filter/sheet state
│   ├── HubViewModel.kt                # combines repo + UserMode + InstallStore → HubUiState
│   ├── model/HubUiState.kt
│   └── components/
│       ├── HubHeader.kt               # mode-aware title, no time pill
│       ├── FeaturedCarousel.kt        # auto-scrolling pager of featured recipes
│       ├── CategoryChips.kt           # horizontal scroll under hero
│       ├── YourRecipesRail.kt         # Architect-only horizontal rail
│       ├── CategoryRail.kt            # reusable horizontal rail (1 per section)
│       ├── RecipeCard.kt              # compact card for rails
│       ├── RecipeListItem.kt          # wide card for filtered vertical list
│       ├── RecipeCover.kt             # cover image OR theme gradient + emoji fallback
│       ├── InstalledBadge.kt          # "Installed ✓" pill
│       └── PublishFab.kt              # Architect-only ExtendedFAB → Studio
│
├── recipe_detail/
│   ├── RecipeDetailSheet.kt           # bottom sheet shown over Hub
│   ├── RecipeStats.kt                 # real stats grid
│   └── FeaturePreviewList.kt          # read-only icons-and-names
│
├── configurator/
│   ├── ConfiguratorScreen.kt          # full-screen
│   ├── ConfiguratorViewModel.kt       # toggle state, size math, install commit
│   └── components/
│       ├── ConfiguratorHeader.kt      # Total Size · Active Features card
│       └── FeatureToggleCard.kt       # per-feature row
│
└── pocket/
    └── MyPocketScreen.kt              # MINIMUM touch: filter to installed

app/src/main/java/com/bina/ai/install/
├── InstallStore.kt                    # DataStore-backed install records
├── CapabilityChecker.kt               # maps capability tokens → Boolean
└── model/InstallRecord.kt

shared/src/commonMain/kotlin/com/bina/ai/miniapp/model/
└── MiniApp.kt                          # UPDATE: add features: List<Feature> + Feature class
```

### State ownership

- `HubViewModel` exposes `HubUiState` (recipes, mode, installedSet, authoredSet, rails). Composes from `MiniAppRepository.loadAll()` + `UserMode` + `InstallStore.installs` + filesDir scan.
- `ConfiguratorViewModel` owns transient toggle state (`Map<featureId, Boolean>`) until Install commits.
- `InstallStore` persists records to DataStore Preferences (`bina_installs`). Single key holding JSON-encoded `Map<recipeId, InstallRecord>`. Observed via Flow.
- MyPocket reads `installStore.installs` directly via `collectAsStateWithLifecycle()` at the Composable level — no new ViewModel for v1, keeps the touch on JY's screen minimal. He can introduce a ViewModel later if he wants.
- `CapabilityChecker` is a stateless helper instantiated with `Context`. No Flow.

### Navigation changes

In `BinaNavGraph.kt`:

- New route `Screen.Configurator` taking `miniAppId` arg.
- Pass `userMode: UserMode` and `installStore: InstallStore` into `HubScreen`.
- Pass `installStore: InstallStore` into `MyPocketScreen` (no userMode — no badges in v1).
- `StudioScreen.onPublished` signature changes from `() -> Unit` to `(recipeId: String) -> Unit` so we can auto-install. **JY: minor heads-up needed.**

## Schema & data shapes

### Feature class — added to `shared/.../miniapp/model/MiniApp.kt`

```kotlin
@Serializable
data class Feature(
    val id: String,                              // stable identifier (e.g. "camera_scanner")
    val name: String,                            // display name
    val description: String = "",
    val icon: String = "",                       // material icon name (camera, mic, …)
    val recommended: Boolean = false,            // default toggle state at first install
    @SerialName("size_kb") val sizeKb: Float = 0f,
    val requires: List<String> = emptyList()     // capability tokens — see below
)
```

And one new field on `MiniApp`:

```kotlin
val features: List<Feature> = emptyList()
```

Default-safe so existing YAMLs still parse and JY's Studio `generateYaml()` keeps working.

### Capability tokens

Strings in `requires`:

| Token form | Behavior in `CapabilityChecker.isAvailable()` | Effect on toggle |
|---|---|---|
| `permission:camera`, `permission:microphone`, `permission:location`, `permission:sms`, `permission:notifications` | Always returns true | Toggle stays enabled; permission requested at use-time |
| `hardware:gps`, `hardware:camera` | Returns `PackageManager.hasSystemFeature(...)` | Greyed only on devices without the hardware (rare on phones) |
| `service:p2p`, `service:smart_notifications`, `service:sms_dispatch` | Always returns false | Reliably greyed for the demo (corresponding services not built) |
| Unknown tokens | Returns true (lenient) | Toggle enabled |

A feature is greyed if **any** of its `requires` tokens evaluates to false.

### Bundled YAML updates

All three bundled recipes need a `features:` block. Distinct, demo-honest features per recipe:

**Farm Buddy** (7 features, 4 recommended):
- `camera_scanner` — Recommended, +0.4 KB, `requires: [permission:camera]`
- `voice_assistant` — Recommended, +0.3 KB, `requires: [permission:microphone]`
- `gps_tracker` — Recommended, +0.2 KB, `requires: [permission:location]`
- `offline_storage` — Recommended, +0.3 KB, no requires
- `sms_dispatcher` — Optional, +0.2 KB, `requires: [service:sms_dispatch]` → greyed
- `p2p_sharing` — Optional, +0.2 KB, `requires: [service:p2p]` → greyed
- `smart_notif` — Optional, +0.1 KB, `requires: [service:smart_notifications]` → greyed

**Bidan Pintar** (4 features, 3 recommended):
- `voice_assistant` — Recommended
- `offline_storage` — Recommended
- `emergency_protocol` — Recommended (always-on emergency referral guidance)
- `smart_notif` — Optional, `service:smart_notifications` → greyed

**Buku Kira-Kira** (4 features, 3 recommended):
- `camera_scanner` (for receipts) — Recommended
- `voice_assistant` — Recommended
- `offline_storage` — Recommended
- `smart_notif` — Optional, greyed

In addition: change `Buku Kira-Kira`'s `theme.primary` from `#047857` to a non-green color (proposed: `#0EA5E9` cyan) so its cover gradient doesn't visually clash with Farm Buddy's green.

### Install state

```kotlin
@Serializable
data class InstallRecord(
    val recipeId: String,
    @SerialName("installed_at") val installedAt: Long,
    @SerialName("enabled_features") val enabledFeatureIds: Set<String>
)

class InstallStore(context: Context) {
    private val Context.dataStore by preferencesDataStore("bina_installs")
    private val INSTALLS_KEY = stringPreferencesKey("installs_json")

    val installs: Flow<Map<String, InstallRecord>>     // recipeId → record
    fun isInstalled(recipeId: String): Flow<Boolean>
    suspend fun install(record: InstallRecord)
    suspend fun uninstall(recipeId: String)             // exists for future MyPocket polish; no UI in v1
}
```

Storage = single Preferences key holding JSON-encoded `Map<recipeId, InstallRecord>`. Observable via Flow. Inspectable via `adb shell run-as com.bina.ai cat ...preferences`.

**Initial state:** empty map. Even bundled recipes start "available, not installed" — user must walk through Configure & Install at least once. This makes the demo land.

**Auto-install for Architect's own work:** `StudioScreen.onPublished(recipeId)` triggers `installStore.install(InstallRecord(..., enabledFeatureIds = recipe.features.filter { it.recommended }.map { it.id }.toSet()))`. Architects don't have to install their own recipe. Only `recommended` features are turned on so we don't auto-enable greyed `service:*` features.

### Recipe size computation

```kotlin
fun MiniApp.baseSizeKb(yamlFile: File): Float = yamlFile.length() / 1024f

fun MiniApp.totalSizeKb(yamlFile: File, enabledIds: Set<String>): Float =
    baseSizeKb(yamlFile) +
    features.filter { it.id in enabledIds }.sumOf { it.sizeKb.toDouble() }.toFloat()
```

`MiniAppRepository` exposes the YAML `File` alongside each `MiniApp` so size is computable. For bundled recipes, asset stream length (`assetManager.openFd(name).length`). For sync-imported, real `File.length()`.

## Hub UI details

### Top-down structure

```
HubHeader                                   # mode-aware title
FeaturedCarousel  (auto-scroll, ~200dp)     # featured=true OR emergency=true
CategoryChips  (All / Health / Agri / …)    # horizontal scroll
YourRecipesRail  (Architect-only, hidden if empty)
CategoryRail × N (when "All" chip active)   # adaptive — see below
PublishFab  (Architect-only)                # bottom-right, above bottom nav
```

When a **specific category chip** is active: rails collapse into a single vertical `RecipeListItem` list filtered to that category.

### Mode-aware header copy

| | Builder | Architect |
|---|---|---|
| Title | "Discover AI Recipes" | "Recipe Marketplace" |
| Subtitle | "Edge-native AI for every domain" | "Author and discover recipes" |
| Accent | Plain text | Subtle "creator" pill next to title |

### Adaptive rail logic

```kotlin
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
    rest.groupBy { it.category }.forEach { (cat, items) ->
        if (items.size >= 2) add(Rail(cat, items))
    }
}
```

With our current 3 recipes (no authored), Builder/Architect both see `[Featured carousel] + [Chips] + [All Recipes rail with 3 cards]`. Once Architect publishes, "Your Recipes" appears. Once Sync brings in more recipes, category rails fill in (≥2 per category threshold).

### Card variants

**`RecipeCard`** (compact, in rails) — width ~140dp:
- Square 140×140 `RecipeCover` on top
- Bottom: name (13sp SemiBold), `category · dialect-short` (11sp gray), optional "Installed ✓" badge
- Top-right overlay: `✓ Verified` and/or `Yours` (Architect-only) stacked
- Top-left overlay: `EMERGENCY` pill if `emergency=true`

**`RecipeListItem`** (wide, in filtered vertical list) — full width:
- 56dp square `RecipeCover` left
- Right column: name + `✓ Verified`, description (2 lines max), `category · dialect`, tags row (max 2 visible + `+N`), `✓ Installed`

Both clickable → opens `RecipeDetailSheet` (modal, no nav change).

### PublishFab

ExtendedFAB pinned bottom-right, ~16dp from edges, above bottom nav. Icon: `Add`. Label: "Publish new". Visible only when `userMode == ARCHITECT`. On click: `navController.navigate(Screen.Studio.route)`.

### Empty states

- **No recipes parsed at all** (worst case): centered illustration + "No recipes available" + adb log hint
- **All filtered out**: "No recipes in this category — pull to refresh after syncing"
- **Architect, zero authored**: `YourRecipesRail` simply doesn't render; FAB serves as the CTA

## RecipeDetailSheet

Modal bottom sheet shown over Hub. Structure:

1. **Hero strip** (200dp): `RecipeCover` full-width with bottom gradient fade, EMERGENCY pill if applicable
2. **Title row**: name + verified pill (`✓ Verified by [organisation]`)
3. **Description** (full)
4. **Stats grid** (3 cells): Recipe Size · `N features Available` · Dialect
5. **Tags chip row**: `#tag1 #tag2 …`
6. **Domain row**: `Domain: [category]`
7. **Feature preview list**: read-only icons + names of all features (full toggles in Configurator)
8. **Primary CTA**: adaptive based on install state

Adaptive CTA:

| Install state | Primary |
|---|---|
| Not installed | Configure & Install |
| Installed | Open |

"Open" → `MiniAppView`. "Configure & Install" → `ConfiguratorScreen` with toggles initialized from `recommended && available`. There is no re-customize path — once installed, the feature set is locked.

## ConfiguratorScreen

Full-screen, navigated from Hub's detail sheet for not-yet-installed recipes. Single entry point. Layout:

1. **TopAppBar**: back arrow + recipe name + subtitle "Choose features you need"
2. **Header card**: Total Download Size (live, computed) · Active Features `N/M` (where M includes greyed)
3. **FeatureToggleCard list**: one per feature in `recipe.features`
   - Icon (40dp, accent color from `recipe.theme.primary`)
   - Name + Recommended pill if recommended
   - Description (small)
   - `+X.X KB`
   - Material `Switch` (right side)
   - Whole row alpha 0.4 if not toggleable; switch disabled
4. **Fixed bottom**: primary button "Install to Pocket" + helper text

### State

```kotlin
data class ConfiguratorState(
    val miniApp: MiniApp,
    val toggles: Map<String, Boolean>,           // featureId → on/off (transient)
    val availability: Map<String, Boolean>,       // featureId → can be toggled
)

val totalSizeKb: Float get() =
    baseSizeKb +
    miniApp.features.filter { toggles[it.id] == true }
        .sumOf { it.sizeKb.toDouble() }.toFloat()

val activeCount: Int get() = toggles.count { it.value }
val totalCount: Int get() = miniApp.features.size
```

### Initialization

`toggles[id] = feature.recommended && availability[id]`. Recommended-but-unavailable features start OFF. There's no re-customize branch — Configurator is only entered for not-yet-installed recipes.

If the user navigates to Configurator for a recipe that became installed in another flow (e.g., through Studio auto-install while the user was on Configurator), commit silently skips and shows a snackbar "Already installed — opening MyPocket."

### Install commit

```kotlin
fun onInstallClick() = viewModelScope.launch {
    installStore.install(InstallRecord(
        recipeId = state.miniApp.id,
        installedAt = System.currentTimeMillis(),
        enabledFeatureIds = state.toggles.filterValues { it }.keys
    ))
    _events.emit(InstallEvent.Installed(state.miniApp.name))
}
```

Screen reacts: `popBackStack(Hub)` + snackbar `"[Name] installed to your Pocket"` with `View` action → MyPocket.

### Cancel / back

User taps TopAppBar back arrow OR Android system back: `popBackStack()`. No state is committed. If they came from the detail sheet, the sheet was dismissed when they navigated forward — pressing back returns to plain Hub.

### Edge cases

- All features disabled: install button stays enabled. Bare-core recipe is valid.
- All features greyed: small banner "This recipe needs services not yet supported on your device." Install still allowed (commits empty `enabledFeatureIds`).
- Recipe missing `features:` entirely: Configurator shows "This recipe has no configurable features" + "Install with defaults" button (commits empty set).
- Recipe not found by ID: TopAppBar back button + "Recipe unavailable" message.

## MyPocket changes (minimum touch)

In scope:

- Add `installStore: InstallStore` constructor parameter to `MyPocketScreen`.
- Filter `miniAppRepository.loadAll()` to `recipeId in installs.keys` (collected from `installStore.installs` directly via `collectAsStateWithLifecycle()`).
- Tap a row → launch (existing behavior, unchanged).

Out of scope (left to JY):

- Header redesign / "X installed · Y KB" summary
- Empty state illustration + "Browse Hub" CTA
- Sort UI (recently used / installed / alphabetical)
- "Yours" rosette in Architect mode
- Uninstall UI
- Card layout polish

MyPocket is purely a launchpad in v1 — no customize, no settings. If users want to change feature toggles, they uninstall and re-install (uninstall itself parked for v1).

## Studio change (one line)

```kotlin
// Before
StudioScreen(onPublished = { ... })
// After
StudioScreen(onPublished = { newRecipeId: String -> ... })
```

In `BinaNavGraph.kt`:

```kotlin
StudioScreen(onPublished = { newRecipeId ->
    miniAppRepository.invalidateCache()
    miniAppRepository.getById(newRecipeId)?.let { recipe ->
        coroutineScope.launch {  // a remembered scope at NavGraph-level
            installStore.install(InstallRecord(
                recipeId = recipe.id,
                installedAt = System.currentTimeMillis(),
                enabledFeatureIds = recipe.features.filter { it.recommended }.map { it.id }.toSet()
            ))
        }
    }
    navController.navigate(Screen.Hub.route) { popUpTo(Screen.Hub.route) { inclusive = true } }
})
```

JY needs to update his `StudioScreen` internals to invoke `onPublished(newId)` instead of `onPublished()` after his publish flow completes. Single string arg.

## Cross-cutting

### RecipeCover — single primitive

Used by carousel hero, `RecipeCard` thumbs, `RecipeListItem` thumbs, `RecipeDetailSheet` hero. One file, one source of truth.

Behavior:
- If `coverImage` is non-empty: load via Coil (`AsyncImage`). Gradient sits underneath as fallback during load and on error.
- If empty: render only the gradient + emoji icon.
- Gradient = `Brush.linearGradient(theme.primary → theme.secondary)`.

Coil dependency to add to `app/build.gradle`:

```
implementation("io.coil-kt:coil-compose:2.7.0")
```

`resolveCoverPath()` handles:
- Bundled-asset path (e.g., `covers/farm_buddy.jpg`) → `file:///android_asset/miniapps/covers/...`
- Absolute URL (e.g., `https://...`) → as-is
- Absolute file path (sync-imported) → `file://...`

### Motion

| Surface | Animation |
|---|---|
| Featured carousel | Auto-advance every 4s, swipeable (`HorizontalPager` + `LaunchedEffect`) |
| Carousel dots | Animate active dot |
| Recipe cards entrance | Staggered fade+slide on first paint (reuse `MetricGrid` pattern) |
| Card tap → sheet | Spring slide-up (`ModalBottomSheet` default) |
| Configurator total size | `AnimatedCounter` (reused from Analytics) |
| Configurator active count | `AnimatedContent` for `4/7` text change |
| Install commit | Brief loading on button + snackbar |
| Toggle on/off | Material Switch default |

No custom physics. Building blocks already in the project.

### Errors

| Failure | UX |
|---|---|
| `MiniAppRepository.loadAll()` returns empty | Hub shows "No recipes available" + retry button |
| Single recipe YAML parse failure | Logged via `Logger.e`, dropped silently, others render |
| Configurator opened with bad `miniAppId` | "Recipe unavailable" + back button |
| `InstallStore.install()` IO failure | Configurator stays open, error snackbar |
| Coil cover image load failure | Gradient fallback shows underneath — invisible failure |
| YAML missing `features:` block | Configurator shows "no configurable features" + "Install with defaults" |

### Testing

In scope:
- Unit: `InstallStore` install/uninstall/observe round-trip with fake DataStore
- Unit: `ConfiguratorViewModel.totalSizeKb` and `activeCount` math
- Unit: `ConfiguratorViewModel` initialization (recommended-and-available toggles on, others off)
- Unit: `HubViewModel.computeRails()` with 0/1/3/7 recipes, with/without authored
- Parse: extend existing bundled-YAML decode test to verify `features:` block round-trips

Out of scope (deferred):
- Compose preview screenshot tests
- Espresso UI tests for navigation
- Accessibility audit

## Coordination notes

For PR description / handoff message to Jingyen:

- **Schema:** added `features: List<Feature>` to `MiniApp` (default-safe, your `generateYaml()` keeps working). Bundled YAMLs updated with `features:` blocks.
- **Studio:** `onPublished` signature changes from `() -> Unit` to `(recipeId: String) -> Unit`. Need to call `onPublished(newId)` after publish completes.
- **MyPocket:** I touched MyPocket only for one-line wiring (added `installStore` param + filter to installed). No customize entry — feature toggles are one-shot at install time. Layout, header, sort, empty state, Yours badge, uninstall — all yours when you're ready.

## Implementation order

1. Schema: add `Feature` class to `MiniApp.kt` + bundled YAML updates (incl. Buku Kira-Kira theme color tweak)
2. Install layer: `InstallStore`, `InstallRecord`, `CapabilityChecker` + unit tests
3. Configurator: `ConfiguratorScreen`, `ConfiguratorViewModel` + unit tests
4. Detail sheet: `RecipeDetailSheet`, `RecipeStats`, `FeaturePreviewList`
5. Cover primitive: `RecipeCover` + Coil dependency
6. Hub UI: `HubViewModel`, `HubScreen`, all card/rail/chip components
7. NavGraph wiring: `Screen.Configurator` route, plumb `InstallStore` to Hub + MyPocket, update Studio callback
8. MyPocket minimum touch: filter to installed only
9. Manual smoke test of the full flow end-to-end on emulator

## Bundled YAML diffs needed

| File | Changes |
|---|---|
| `farm_buddy.yaml` | Add `features:` block (7 features) |
| `bidan_pintar.yaml` | Add `features:` block (4 features) |
| `buku_kira_kira.yaml` | Add `features:` block (4 features); change `theme.primary` from `#047857` to `#0EA5E9` for cover-gradient distinction |
