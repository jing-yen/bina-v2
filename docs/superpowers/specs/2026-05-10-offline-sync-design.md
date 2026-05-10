# Offline Sync — Design Spec

**Date:** 2026-05-10
**Branch target:** `feature/offline-sync` (off `main`)
**Owner:** Ing (no coordination touches required — Sync tab is fully my territory)

## Summary

Build the Offline Sync tab so Builders can share installed recipes phone-to-phone via QR code, fully offline. Two primary actions: **Scan to Receive** (camera scan or paste fallback → preview → install via existing Configurator flow) and **Share a Recipe** (pick installed recipe → display QR encoding the recipe YAML). Reuses the Hub's `RecipeDetailSheet` and Configurator on the receive path so an imported recipe goes through the same install pipeline as one tapped in the Hub. Also unblocks the Studio (web) → phone round-trip: Studio's downloaded YAML can be pasted into the Sync tab.

## Goals

- Replace the stub `OfflineSyncScreen` with a working two-action surface.
- Encode/decode recipe YAMLs as QR codes via ZXing, fully on-device.
- Reuse `RecipeDetailSheet` + Configurator route for the receive flow — no parallel install pipeline.
- Persist imported recipes to `filesDir/miniapps/<id>.yaml` so they appear in the Hub on next read.
- Provide a paste-YAML fallback for cases where camera scanning is awkward (clipboard from chat, Studio export, oversized recipe).
- Versioned QR payload format (`BINA1:` magic header) so we can evolve the schema later without breaking older clients.

## Non-goals

- Bluetooth / WiFi Direct / NFC transports (deferred — QR alone tells the edge-native story without the BLE permission/pairing risk).
- Multi-frame / animated QR codes for oversized recipes (out of scope; we cap at single-QR limit and direct users to paste-YAML when over).
- Sync history / log of past imports/shares (no UX value yet, would just inflate state).
- USB sneakernet — that's a deployment narrative for the pitch, not an in-app feature.
- Recipe verification / signing (no trust model yet — user is opting in by scanning).
- Resharing of bundled recipes that already exist on the receiver (rejected with a friendly message rather than no-op).
- Translating between schema versions (an old `BINA1:` payload with a future schema is treated as a parse failure, not auto-migrated).

## User experience overview

Three surfaces:

1. **OfflineSyncScreen (rewrite)** — Top intro card + two large vertical action cards: "Scan to Receive" and "Share a Recipe". This is the entire surface for the Sync tab.
2. **ScanQrScreen** (full screen) — Camera preview with ZXing's `CompoundBarcodeView`, scan reticle overlay, small "Paste YAML instead" link at bottom. On successful scan: dismisses to OfflineSync, opens RecipeDetailSheet. Camera permission requested first time.
3. **ShareQrScreen** (full screen) — Recipe icon + name + author centered above a large QR bitmap mid-screen. "Done" button at bottom. No share-to-other-apps button (out of scope).

Plus two bottom sheets:

- **ShareRecipePickerSheet** — Modal bottom sheet listing installed recipes (icon + name + size). Tap → navigate to ShareQrScreen.
- **PasteYamlSheet** — Modal bottom sheet with multi-line `TextField`, "Paste from clipboard" auto-fill button, "Import" CTA. On Import: same decode → preview path as scan.

Receive flow ends in the **existing `RecipeDetailSheet`** showing the imported recipe with a "Configure & Install" CTA, then the **existing Configurator route**. No new install pipeline.

```
OfflineSync
├── "Scan to Receive" ──► ScanQrScreen ──► (decode) ──┐
│                              │                       │
│                              └── "Paste instead" ──► PasteYamlSheet ──► (decode) ──┐
│                                                                                    │
│                                                                                    ▼
│                                                                          RecipeDetailSheet
│                                                                                    │
│                                                                                    │ "Configure & Install"
│                                                                                    ▼
│                                                                            ConfiguratorScreen
│                                                                                    │
│                                                                                    │ "Install to Pocket"
│                                                                                    ▼
│                                                                          InstallStore.install()
│
└── "Share a Recipe" ──► ShareRecipePickerSheet ──► ShareQrScreen
                                                          │
                                                          └── "Done" ──► back to OfflineSync
```

## Architecture

### File map

```
ui/screens/sync/
├── OfflineSyncScreen.kt                  (REWRITE — was stub, now two-action landing)
├── SyncViewModel.kt                      (NEW — installed-recipe list flow, decode handler)
└── components/
    ├── SyncActionCard.kt                 (NEW — reusable big tappable card)
    ├── ShareRecipePickerSheet.kt         (NEW — installed-recipe picker)
    ├── ShareQrScreen.kt                  (NEW — full-screen QR display)
    ├── ScanQrScreen.kt                   (NEW — ZXing camera preview)
    └── PasteYamlSheet.kt                 (NEW — paste fallback)

sync/
├── RecipePayload.kt                      (NEW — encode/decode YAML <-> "BINA1:" + gzip+base64)
└── RecipeImporter.kt                     (NEW — parse YAML via kaml, precheck, write to filesDir, invalidate cache)

ui/navigation/
├── Screen.kt                             (MODIFY — add Scan, Share routes)
└── BinaNavGraph.kt                       (MODIFY — wire new routes)

AndroidManifest.xml                       (MODIFY — add CAMERA permission)
app/build.gradle.kts                      (MODIFY — add zxing-android-embedded dependency)
```

`MiniAppRepository.invalidateCache()` already exists at `shared/src/commonMain/kotlin/com/bina/ai/miniapp/MiniAppRepository.kt:35` — `RecipeImporter` calls it after writing the file so the next Hub read picks up the new recipe.
```

### QR payload format

```
BINA1:<urlsafe-base64(gzip(yaml-bytes))>
```

- **`BINA1:`** — fixed magic header. Decoder rejects anything not starting with this.
- **gzip** — Java `GZIPOutputStream` / `GZIPInputStream`, default level. Brings ~2.5 KB YAML down to ~1.2 KB.
- **base64** — `Base64.URL_SAFE | NO_WRAP` so the resulting string is QR-safe (no padding `=` issues with some scanners; URL-safe alphabet avoids `+/`).

Sample size math: 2.5 KB YAML → ~1.2 KB gzipped → ~1.6 KB base64. Well under the ~2.3 KB single-QR limit at error-correction level M.

### ZXing integration

- Dependency: `com.journeyapps:zxing-android-embedded:4.3.0` (Apache 2.0).
- Encode: `BarcodeEncoder().encodeBitmap(payload, BarcodeFormat.QR_CODE, 1024, 1024)` → `Bitmap` rendered in Compose `Image`.
- Decode: wrap `CompoundBarcodeView` (a `ViewGroup`) inside `AndroidView { ... }`, set a `BarcodeCallback`, call `barcodeView.resume()` / `pause()` from `DisposableEffect`.

### Permissions

- Add `<uses-permission android:name="android.permission.CAMERA" />` to AndroidManifest.
- Runtime request via `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` — no extra dependency.
- Three states: `Granted` (show camera), `Denied` (show "Allow camera or paste YAML instead" with retry button), initial = trigger request on screen entry.

### Receive pipeline

1. Raw text from scan or paste lands in `SyncViewModel.handleIncoming(raw: String)`.
2. `RecipePayload.decode(raw)` → returns `Result<String yaml>` (errors: wrong magic, base64 fail, gunzip fail).
3. `RecipeImporter.parse(yaml)` → uses kaml the same way `MiniAppRepository` does (`Yaml(YamlConfiguration(strictMode = false)).decodeFromString(MiniApp.serializer(), yaml)`); returns `Result<MiniApp>`.
4. `RecipeImporter.precheck(miniApp, miniAppRepository)` → returns one of: `Ok`, `BundledConflict(id)`, `UpdateExisting(id)`. Bundled vs imported is determined by checking whether a YAML for `id` exists in `filesDir/miniapps/` (imported) vs only being present via the `loadAll()` cache (bundled).
5. UI shows `RecipeDetailSheet` with the parsed `MiniApp` and a write-on-confirm callback.
6. On "Configure & Install" tap: `RecipeImporter.commit(miniApp, yaml)` writes `filesDir/miniapps/<id>.yaml` and calls `miniAppRepository.invalidateCache()`, then nav continues into the Configurator route (same wiring Hub already uses).

### State management

`SyncViewModel`:

- `installedRecipesForShare: StateFlow<List<MiniApp>>` — combines `installStore.installs` with `miniAppRepository.loadAll()` to produce only installed recipes with full metadata.
- `previewState: MutableStateFlow<PreviewState>` — `Idle | Decoding | Ready(MiniApp) | Error(String)`. Drives whether RecipeDetailSheet is shown.
- `handleIncoming(raw: String)` — decode pipeline, sets previewState.
- `confirmInstall(miniApp: MiniApp)` — writes file, invalidates cache, returns `recipeId` for nav.

## Error / edge cases

| Case | Behavior |
|---|---|
| Wrong magic header | Toast "Not a Bina recipe QR" — stay on scan screen |
| Base64 / gzip decode fails | Toast "QR data is corrupted" |
| YAML parse fails | Toast "Recipe file is corrupted" |
| Recipe ID matches a bundled recipe | Refuse with dialog: "Built-in recipe '$name' already exists. Cannot import." |
| Recipe ID matches a previously-imported recipe | Confirm dialog: "Update existing recipe '$name'?" → overwrite on confirm |
| Recipe is already installed (id in `InstallStore.installs`) | Show RecipeDetailSheet with "Open" CTA (existing adaptive behavior) — no fresh install |
| Encode payload > 2300 chars | Inline message on ShareQrScreen: "Recipe too large for QR — copy YAML to share via paste instead" + "Copy YAML" button |
| Camera permission denied | Show "Allow camera or paste YAML instead" with two buttons: "Try again" (re-request) and "Paste instead" |
| Camera permission permanently denied | Same UI, "Try again" deep-links to app settings |
| Scan returns same QR repeatedly | After first successful decode, pause `barcodeView` until preview dismissed |
| User leaves ShareQrScreen | No persistence needed — re-encode on revisit |

## Testing

Unit tests (no Android instrumentation needed):

- `RecipePayloadTest` — round-trip encode/decode, magic header rejection, malformed base64 rejection, gzip-only rejection (no header).
- `RecipeImporterTest` — bundled-conflict detection, update-existing detection, write-then-read parity (using a temp `File` dir + a fake `InstallStore`).
- `SyncViewModelTest` — incoming text → previewState transitions for valid/invalid payloads.

Manual smoke test:

1. From Sync tab → Share → pick `farm_buddy` → QR appears, recipe name visible.
2. From a second emulator instance: Sync tab → Scan → point camera at first emulator's screen → preview sheet appears with `farm_buddy` metadata → Configure & Install → Hub shows `farm_buddy` as installed.
3. From Sync tab → Scan → "Paste instead" → paste the YAML preview Studio shows in its dialog → preview → install. (Validates Studio→phone round-trip path.)
4. Try scanning a non-Bina QR (any random QR code) → toast "Not a Bina recipe QR".
5. Try sharing a recipe that's already bundled → import is refused with the BundledConflict dialog.

## Coordination notes

- **JY's territory (web Studio):** This spec assumes Studio's "Publish to Hub" button will eventually output a YAML the user can paste. Until then, the paste-YAML fallback also works with any valid recipe YAML the user can manually copy from Studio's preview dialog (`Studio.tsx:1036-1041`).
- **JY's territory (MyPocket):** No changes required. Sync tab does NOT add a per-recipe Share menu item to MyPocket in this scope — entry point is exclusively from the Sync tab.
- **MiniApp schema:** Spec assumes the schema in `MiniApp.kt` is the source of truth on the receive side. If JY's Studio emits fields we don't yet parse, kaml will tolerate them (it's lenient by default) — they just won't be honored on the phone until we extend `MiniApp.kt`.
