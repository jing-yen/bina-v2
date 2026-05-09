# Bina.ai - Technical Documentation

**Version:** 2.0.0-hackathon
**Platform:** Android (min SDK 26, target SDK 35)
**Language:** Kotlin 2.2.21
**UI Framework:** Jetpack Compose (BOM 2026.04.01)
**Architecture:** Kotlin Multiplatform (KMP) with shared business logic

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Module Structure](#3-module-structure)
4. [Shared Module (commonMain)](#4-shared-module-commonmain)
5. [App Module](#5-app-module)
6. [MiniApp YAML DSL](#6-miniapp-yaml-dsl)
7. [On-Device AI Inference](#7-on-device-ai-inference)
8. [Widget System](#8-widget-system)
9. [Action Dispatch System](#9-action-dispatch-system)
10. [Navigation & User Modes](#10-navigation--user-modes)
11. [Safety & Guardrails](#11-safety--guardrails)
12. [Build & Run](#12-build--run)
13. [Known Limitations](#13-known-limitations)
14. [Future Roadmap](#14-future-roadmap)

---

## 1. Overview

Bina.ai is an **edge-native AI platform** that enables offline, interactive AI-powered applications ("recipes") for underserved communities in Southeast Asia. The platform allows non-technical users ("Architects") to define multi-screen apps using a simple YAML file, while the runtime renders them dynamically with Compose UI and connects them to on-device LLM inference via Google's LiteRT-LM.

### Key Capabilities

- **Offline-first AI inference** using Gemma 4 E2B (2.4GB) running entirely on-device
- **Declarative mini-app DSL** - define interactive apps in YAML without code
- **10 widget types** - text, camera, voice, sliders, maps, formulas, AI chat
- **Safety guardrails** - keyword blocking, escalation messages, content disclaimers
- **Kotlin Multiplatform** - shared business logic ready for iOS expansion

### Demo App: Farm Buddy

The bundled "Farm Buddy" recipe demonstrates all platform capabilities:
- **Diagnose Leaf** - take a photo for AI-powered crop disease diagnosis
- **Fertiliser Guide** - ask the AI about fertiliser recommendations
- **Profit Calculator** - compute net profit with formulas and get AI advice
- **Nearest Agro Shop** - geolocate and find nearby agricultural supply stores

---

## 2. Architecture

```
+-----------------------------------------------------+
|                    App Module                        |
|  +-----------------------------------------------+  |
|  |              Compose UI Layer                  |  |
|  |  HubScreen | MiniAppScreen | WidgetRenderer    |  |
|  |  BinaTopBar | BinaBottomNav | BinaNavGraph      |  |
|  +-----------------------------------------------+  |
|  |         Platform Implementations               |  |
|  |  LiteRtLmEngine | AndroidLocationProvider      |  |
|  +-----------------------------------------------+  |
+---------------------------|--------------------------+
                            | depends on
+---------------------------v--------------------------+
|                  Shared Module (KMP)                 |
|  +-----------------------------------------------+  |
|  |              commonMain                        |  |
|  |  MiniApp Model | Widget DSL | WidgetSerializer |  |
|  |  ActionDispatcher | VariableStore | FormulaEngine|  |
|  |  MiniAppRepository | InferenceEngine (interface)|  |
|  |  Logger (expect) | LocationProvider (interface) |  |
|  +-----------------------------------------------+  |
|  |              androidMain                       |  |
|  |  Logger (actual: android.util.Log)             |  |
|  +-----------------------------------------------+  |
+------------------------------------------------------+
```

### Design Principles

1. **Platform-agnostic business logic** - All models, runtime, and dispatch logic live in `commonMain` with zero Android dependencies
2. **Reactive state bridging** - The shared `VariableStore` uses a callback (`onChange`) that the Compose layer hooks into for recomposition
3. **Action-based architecture** - All user interactions are expressed as string-based actions (`ask:`, `go:`, `formula:`, `geolocate`, `set:`) dispatched through a single entry point
4. **Widget polymorphism** - A sealed class hierarchy with custom YAML deserialization enables type-safe, extensible widget rendering

---

## 3. Module Structure

```
bina-v2/
+-- build.gradle.kts                    # Root: plugin declarations
+-- settings.gradle.kts                 # Modules: app, shared
+-- app/
|   +-- build.gradle.kts                # Android app config
|   +-- src/main/
|       +-- AndroidManifest.xml         # Permissions, FileProvider
|       +-- assets/miniapps/            # Bundled YAML recipes
|       |   +-- farm_buddy.yaml
|       +-- res/
|       |   +-- values/strings.xml
|       |   +-- values/themes.xml
|       |   +-- xml/file_paths.xml      # FileProvider paths
|       +-- java/com/bina/ai/
|           +-- MainActivity.kt
|           +-- inference/
|           |   +-- LiteRtLmEngine.kt   # On-device LLM
|           +-- miniapp/
|           |   +-- ui/MiniAppScreen.kt  # Runtime renderer
|           |   +-- widgets/
|           |       +-- WidgetRenderer.kt
|           |       +-- MiniAppWidgets.kt  # 10 widget composables
|           +-- platform/
|           |   +-- AndroidLocationProvider.kt
|           +-- ui/
|               +-- components/          # TopBar, BottomNav
|               +-- navigation/          # NavGraph, Screen, UserMode
|               +-- screens/             # Hub, MyPocket, Sync, etc.
|               +-- theme/               # Colors, Material3 theme
+-- shared/
    +-- build.gradle.kts                 # KMP: android + common
    +-- src/
        +-- commonMain/kotlin/com/bina/ai/
        |   +-- inference/
        |   |   +-- InferenceEngine.kt   # Interface
        |   +-- miniapp/
        |   |   +-- MiniAppRepository.kt
        |   |   +-- model/
        |   |   |   +-- MiniApp.kt       # Data classes
        |   |   |   +-- Widget.kt        # Sealed class
        |   |   |   +-- WidgetSerializer.kt
        |   |   +-- runtime/
        |   |       +-- ActionDispatcher.kt
        |   |       +-- FormulaEngine.kt
        |   |       +-- VariableStore.kt
        |   +-- platform/
        |       +-- LocationProvider.kt  # Interface
        |       +-- Logger.kt           # expect
        +-- androidMain/
            +-- kotlin/com/bina/ai/platform/
            |   +-- Logger.kt           # actual
            +-- AndroidManifest.xml     # Empty (library)
```

---

## 4. Shared Module (commonMain)

### 4.1 Data Model (`miniapp/model/`)

All data classes use `@Serializable` annotations from `kotlinx.serialization` and are parsed from YAML via the `kaml` library.

#### MiniApp.kt

Top-level miniapp definition with nested configs:

| Class | Fields | Purpose |
|-------|--------|---------|
| `MiniApp` | id, name, description, icon, version, category, author, model, theme, localisation, variables, screens, formulas, data, safety, permissions | Root definition |
| `Author` | name, organisation, verified | Authorship metadata |
| `ModelConfig` | modelId, backend, systemPrompt, sampler | LLM configuration |
| `SamplerConfig` | temperature, topK, topP, maxTokens | Generation parameters |
| `ThemeConfig` | primary, secondary (hex), textSize | Visual theming |
| `LocalisationConfig` | defaultLanguage, labels | i18n support |
| `VariableDef` | type (string/number/boolean/location), default | Variable declaration |
| `MiniAppScreen` | id, title, body (List\<Widget\>) | Screen definition |
| `FormulaDef` | expression, output | Math formula |
| `DataSet` / `DataPoint` | type, items / name, lat, lng, info | Geo data points |
| `SafetyConfig` | blockedKeywords, escalationMessage, disclaimer | Content safety |
| `GridButton` | label, action, color | Button in macro grid |

#### Widget.kt

Sealed class with 10 widget subtypes. Every widget supports `visibleIf` and `hiddenIf` for conditional rendering based on variable state.

#### WidgetSerializer.kt

Custom `KSerializer<Widget>` that handles YAML polymorphism. Each YAML body item is a single-key map (e.g., `text_input: { bind: user_text }`). The serializer reads the key as the widget type and maps properties to the corresponding data class.

### 4.2 Runtime (`miniapp/runtime/`)

#### VariableStore

Reactive key-value store for miniapp state. Initialized from `VariableDef` defaults. Supports:
- `get(key)` / `set(key, value)` - string storage with onChange notification
- `getNumber(key)` - numeric coercion
- `isTrue(key)` - truthy evaluation
- `interpolate(template)` - replaces `{{variable}}` placeholders via regex

#### FormulaEngine

Evaluates mathematical expressions using a recursive descent parser. Supports `+`, `-`, `*`, `/`, `%`, `^`, and parentheses. Variables are interpolated before evaluation.

#### ActionDispatcher

Routes action strings to handlers. See [Section 9](#9-action-dispatch-system) for details.

### 4.3 Platform Abstractions

| Abstraction | Type | Purpose |
|-------------|------|---------|
| `Logger` | expect/actual | Logging (android.util.Log on Android) |
| `LocationProvider` | interface | GPS coordinates |
| `InferenceEngine` | interface | LLM text/vision generation |

---

## 5. App Module

### 5.1 MainActivity

Entry point. Responsibilities:
1. Creates `MiniAppRepository` with asset-loading lambda
2. Initializes `LiteRtLmEngine` and starts model loading
3. Sets up Compose UI with gradient background, top bar, bottom nav, and navigation graph
4. Manages user mode toggle (Builder / Architect)

### 5.2 LiteRtLmEngine

Implements `InferenceEngine` using Google's LiteRT-LM SDK.

**Model discovery** searches these directories in order:
1. `/data/local/tmp/`
2. `/sdcard/Download/`
3. `/sdcard/Models/`

Looks for files: `gemma-4-E2B-it.litertlm`, `gemma-3-1B-it.litertlm`, or `model.litertlm`.

**Engine configuration:**
- Backend: CPU with XNNPack acceleration
- Cache directory: app cache (speeds up subsequent loads)
- Sampler: temperature 0.3, topK 40, topP 0.95

**API:**
- `initialize()` - loads model, creates engine (~5s with cache, ~23s first time)
- `generate(prompt, systemPrompt)` - returns `Flow<String>` of streamed tokens
- `generateWithImage(prompt, imagePath, systemPrompt)` - vision inference (see [Known Limitations](#13-known-limitations))

### 5.3 MiniAppScreen

The core runtime renderer. For each miniapp:
1. Creates a `VariableStore` from the miniapp's variable definitions
2. Creates a `FormulaEngine` from the miniapp's formulas
3. Creates an `ActionDispatcher` with location provider and inference engine
4. Bridges `VariableStore.onChange` to Compose recomposition via a revision counter
5. Renders the current screen's widgets in a `LazyColumn`
6. Handles back navigation (to previous screen or back to hub)

### 5.4 AndroidLocationProvider

Implements `LocationProvider` using Google Play Services `FusedLocationProviderClient`. Falls back to `LocationManager` (GPS/Network providers) if fused location fails. Returns `null` if no location permission granted.

### 5.5 UI Layer

| Component | File | Purpose |
|-----------|------|---------|
| BinaTopBar | `ui/components/BinaTopBar.kt` | App branding + Builder/Architect mode toggle |
| BinaBottomNav | `ui/components/BinaBottomNav.kt` | Tab bar (3 tabs per mode) |
| BinaNavGraph | `ui/navigation/BinaNavGraph.kt` | Jetpack Navigation with fade transitions |
| Screen | `ui/navigation/Screen.kt` | Route definitions (hub, pocket, sync, studio, analytics, miniapp/{id}) |
| HubScreen | `ui/screens/hub/HubScreen.kt` | Recipe discovery with miniapp cards |
| MyPocketScreen | `ui/screens/pocket/MyPocketScreen.kt` | Saved offline recipes |
| BinaTheme | `ui/theme/BinaTheme.kt` | Material3 color scheme and design tokens |

**Color palette:**
- Primary: `#091A7A` (deep blue)
- Green: `#10B981` (Bina green)
- Screen gradient: `#E8EEF8` -> `#F0F4FA` -> White

---

## 6. MiniApp YAML DSL

### Structure

```yaml
id: unique_id
name: Display Name
description: Short description
icon: "emoji"
version: "1.0.0"
category: Category

author:
  name: Author Name
  organisation: Org
  verified: true

model:
  model_id: gemma-4-e2b-it
  backend: cpu
  system_prompt: |
    System prompt for the AI model.
  sampler:
    temperature: 0.3
    top_k: 40
    top_p: 0.95
    max_tokens: 512

theme:
  primary: "#HEX"
  secondary: "#HEX"

variables:
  var_name: { type: string, default: "" }
  num_var:  { type: number, default: "0" }

screens:
  - id: screen_id
    title: Screen Title
    body:
      - widget_type:
          property: value

formulas:
  formula_id:
    expression: "{{var1}} + {{var2}}"
    output: result_var

data:
  dataset_id:
    type: points
    items:
      - { name: "Point", lat: 0.0, lng: 0.0, info: "Details" }

safety:
  blocked_keywords: [keyword1, keyword2]
  escalation_message: "Safety message"
  disclaimer: "Disclaimer text"

permissions: [camera, location]
```

### Variable Interpolation

Variables are referenced in action strings and formulas using `{{variable_name}}` syntax. The `VariableStore.interpolate()` method replaces these placeholders with current values at runtime.

### Conditional Visibility

Every widget supports `visible_if` and `hidden_if` properties that reference variable names. The widget renders only when the referenced variable evaluates as truthy (non-empty, non-zero, non-false).

---

## 7. On-Device AI Inference

### Model: Gemma 4 E2B

| Property | Value |
|----------|-------|
| Model ID | gemma-4-e2b-it |
| File | gemma-4-E2B-it.litertlm |
| Size | ~2.4 GB |
| Format | LiteRT-LM (.litertlm) |
| Capabilities | Text generation (vision blocked by SDK bug) |
| Backend | CPU with XNNPack |
| Cache | XNNPack weight cache in app cache directory |

### Inference Flow

```
User taps "Ask Farm Buddy"
    |
    v
ActionDispatcher.dispatch("ask:{{user_text}}")
    |
    v
store.interpolate() resolves "ask:What fertiliser for rice?"
    |
    v
handleAsk() -> safety keyword check
    |
    v
store["is_loading"] = "true"
    |
    v
engine.generate(prompt, systemPrompt) -> Flow<String>
    |
    v
Tokens stream into store["ai_response"]
    |
    v
MarkdownOutputWidget recomposes with each chunk
    |
    v
store["is_loading"] = "false"
```

### Performance (Emulated ARM CPU)

- Engine init (first time): ~23 seconds
- Engine init (with cache): ~5 seconds
- Text generation: ~2 minutes for a full response
- On real device: significantly faster

---

## 8. Widget System

### Available Widgets

| Widget Type | YAML Key | Purpose | Key Properties |
|-------------|----------|---------|----------------|
| Text Label | `text_label` | Static text display | text, style (heading/subheading/body/caption), align, color |
| Text Input | `text_input` | User text entry | bind, hint, label, input_type (text/number/multiline) |
| Voice Input | `voice_input` | Speech-to-text | bind, hint, language, mode (tap) |
| Camera Input | `camera_input` | Photo capture | bind, label, preview |
| Macro Grid | `macro_grid` | Button grid | columns, buttons [{label, action, color}] |
| Slider | `slider` | Range selector | bind, min, max, step, label, left_label, right_label |
| Action Button | `action_button` | Trigger actions | label, action, style (primary/secondary/danger), icon |
| Markdown Output | `markdown_output` | Display AI response | source (variable), empty_text, streaming |
| Metric Card | `metric_card` | Formatted number | source, label, prefix, suffix, format (decimal_2/percentage/integer) |
| Geo Display | `geo_display` | Nearby locations | data (dataset), limit, show_distance, empty_text |

### Widget Rendering Pipeline

```
YAML body item
    |
    v
WidgetSerializer.deserialize() -> Widget sealed class
    |
    v
WidgetRenderer.RenderWidget() -> visibility check
    |
    v
Specific widget composable (e.g., ActionButtonWidget)
    |
    v
Compose UI with theme colors, store bindings, action callbacks
```

### Camera Widget (Full-Resolution Capture)

The camera widget uses `ActivityResultContracts.TakePicture()` with `FileProvider` to capture full-resolution photos (not thumbnails). The photo is saved to the app cache directory and its absolute path is stored in the bound variable.

---

## 9. Action Dispatch System

All user interactions in a miniapp are expressed as action strings. The `ActionDispatcher` parses and routes them.

### Action Types

| Prefix | Format | Handler | Example |
|--------|--------|---------|---------|
| `ask` | `ask:prompt text` | Text AI inference | `ask:What fertiliser for rice?` |
| `vision_ask` | `vision_ask:prompt` | Vision AI (falls back to text) | `vision_ask:Diagnose this leaf.` |
| `formula` | `formula:formula_id` | Evaluate math formula | `formula:profit` |
| `go` | `go:screen_id` | Navigate to screen | `go:diagnose` |
| `geolocate` | `geolocate` | Get GPS coordinates | `geolocate` |
| `set` | `set:key=value` | Set a variable | `set:mode=advanced` |

### Variable Interpolation in Actions

Actions are interpolated before dispatch: `ask:I made RM{{revenue}} with RM{{costs}} costs` becomes `ask:I made RM5000 with RM2000 costs` based on current variable values.

### Safety Checks

Before AI inference, the `handleAsk` method checks the prompt against the miniapp's `blocked_keywords`. If a match is found, the escalation message is shown instead of sending the prompt to the model.

---

## 10. Navigation & User Modes

### User Modes

| Mode | Tabs | Purpose |
|------|------|---------|
| **Builder** | Hub, My Pocket, Offline Sync | End users who consume recipes |
| **Architect** | Hub, Recipe Studio, Analytics | Creators who build recipes |

### Navigation Routes

| Route | Screen | Description |
|-------|--------|-------------|
| `hub` | HubScreen | Browse available recipes |
| `pocket` | MyPocketScreen | View saved/offline recipes |
| `sync` | OfflineSyncScreen | Manage offline data (placeholder) |
| `studio` | StudioScreen | Build recipes (placeholder) |
| `analytics` | AnalyticsScreen | Usage analytics (placeholder) |
| `miniapp/{id}` | MiniAppScreen | Run a specific miniapp |

### Navigation Flow

```
Hub (recipe cards)
    |-- tap card --> MiniAppScreen (home screen)
    |                   |-- tap macro button --> MiniAppScreen (sub-screen)
    |                   |-- back arrow --> MiniAppScreen (home) or Hub
    |
    |-- bottom nav --> My Pocket / Offline Sync
    |-- top bar toggle --> Architect mode tabs
```

---

## 11. Safety & Guardrails

### Per-MiniApp Safety Config

Each YAML recipe defines its own safety rules:

```yaml
safety:
  blocked_keywords:
    - mix pesticide
    - poison
    - dangerous spray
  escalation_message: "This may be dangerous. Contact your local agriculture officer."
  disclaimer: "AI-generated guidance. Not a professional consultation."
```

### Enforcement Points

1. **Keyword blocking** - `ActionDispatcher.handleAsk()` checks all prompts against blocked keywords before sending to the model
2. **Escalation messages** - When a blocked keyword is detected, the configured escalation message is displayed instead
3. **Disclaimers** - Shown in the miniapp header bar below the title
4. **System prompt** - The model's system prompt instructs it to avoid dangerous recommendations and refer users to experts

### Permission Model

| Permission | Android Permission | Usage |
|------------|-------------------|-------|
| `camera` | `CAMERA` | Photo capture for diagnosis |
| `location` | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Finding nearby shops |
| (implicit) | `RECORD_AUDIO` | Voice input widget |

---

## 12. Build & Run

### Prerequisites

- Android Studio (with bundled JDK)
- Android SDK 35
- An Android device or emulator (ARM64 recommended for LiteRT-LM)

### Build

```bash
# Set JAVA_HOME if needed
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Build the full project
./gradlew assembleDebug

# Build shared module only
./gradlew :shared:build
```

### Run on Device

```bash
# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.bina.ai/.MainActivity
```

### Model Setup

The on-device model must be pushed separately (not bundled in the APK due to size):

```bash
# Download Gemma 4 E2B from HuggingFace
# Push to device
adb push gemma-4-E2B-it.litertlm /data/local/tmp/
```

The engine searches `/data/local/tmp/`, `/sdcard/Download/`, and `/sdcard/Models/` for `.litertlm` files.

---

## 13. Known Limitations

### Vision Inference (SIGSEGV)

Gemma 4 multimodal (vision) inference crashes with a native SIGSEGV in `liblitertlm_jni.so`. This is a **known SDK bug** ([LiteRT-LM Issue #1874](https://github.com/google-ai-edge/LiteRT-LM/issues/1874)): the Kotlin API does not expose `SetOverwritePromptTemplate()`, which Gemma 4 requires for image input. The `vision_ask` action currently falls back to text-only inference with a contextual diagnostic prompt.

**Workaround options:**
- Use Gemma 3n models (vision works out of the box with Kotlin API)
- Wait for SDK fix to land
- Use C++ API which supports custom prompt templates

### Camera on Emulator

The Android emulator provides a virtual scene camera that produces pixelated images. On real devices, the camera captures proper photos.

### Placeholder Screens

`OfflineSyncScreen`, `StudioScreen`, and `AnalyticsScreen` are placeholder screens for future implementation.

### Model Size

The Gemma 4 E2B model is ~2.4 GB and must be sideloaded via ADB. A future version could include a model download manager.

---

## 14. Future Roadmap

### iOS Expansion (KMP)

The shared module is already structured for KMP. Adding iOS requires:
1. Add `iosTarget()` to `shared/build.gradle.kts`
2. Implement `iosMain` actuals for `Logger` and `LocationProvider`
3. Build iOS UI with SwiftUI or Compose Multiplatform
4. Use CoreML or similar for on-device inference

### Planned Features

- **Recipe Studio** - visual YAML editor for Architects
- **Offline Sync** - P2P recipe sharing via Bluetooth/Wi-Fi Direct
- **Model Manager** - in-app model download with progress
- **Analytics** - usage tracking for recipe authors
- **Voice-first UX** - speech recognition for low-literacy users
- **Multi-language** - full localisation support (Malay, Bahasa, Thai, Vietnamese)

---

## Appendix: Dependencies

| Module | Dependency | Version | Purpose |
|--------|-----------|---------|---------|
| shared | kotlinx-serialization-json | 1.9.0 | JSON/YAML serialization |
| shared | kaml | 0.67.0 | YAML parsing |
| shared | kotlinx-coroutines-core | 1.10.2 | Async operations |
| shared | play-services-location | 21.3.0 | Geolocation (Android) |
| app | compose-bom | 2026.04.01 | Compose UI framework |
| app | core-ktx | 1.15.0 | Android KTX extensions |
| app | activity-compose | 1.9.3 | Activity + Compose |
| app | lifecycle-viewmodel-compose | 2.8.7 | ViewModel integration |
| app | lifecycle-runtime-compose | 2.8.7 | Lifecycle-aware Compose |
| app | navigation-compose | 2.8.5 | Jetpack Navigation |
| app | material3 | (BOM) | Material Design 3 |
| app | material-icons-extended | (BOM) | Extended icon set |
| app | litertlm-android | latest | On-device LLM inference |

## Appendix: File Count & Lines of Code

| Module | Files | LOC | Description |
|--------|-------|-----|-------------|
| shared/commonMain | 9 | ~530 | Models, runtime, interfaces |
| shared/androidMain | 1 | ~16 | Logger actual |
| app (inference) | 1 | ~161 | LiteRT-LM engine |
| app (miniapp) | 3 | ~850 | Screen renderer, widgets |
| app (platform) | 1 | ~60 | Location provider |
| app (UI) | 10 | ~850 | Screens, navigation, theme |
| app (main) | 1 | ~126 | MainActivity |
| **Total** | **26** | **~2,600** | |
