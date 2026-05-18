# Bina

**Offline-first AI from verified experts for grassroots communities via Gemma 4 LiteRT.**

Experts author workflows. Gemma 4 LiteRT assembles zero-cost, local-language AI agents that spread via an offline P2P QR + BLE mesh.

---

## What is Bina?

Bina is a platform that turns domain knowledge into deployable AI applications — no coding required. A maternal health NGO writes their triage protocols into a document. Bina's Web Studio analyzes it and generates a complete mobile **recipe**: screens, prompts, safety guardrails, and translations, all encoded as a single YAML file. That file ships to any Android device, where a 2.6 GB on-device Gemma 4 model powers it without ever touching the internet.

No servers. No API keys. No monthly bills. No data leaving the device.

The Web Studio supports translation into 30+ languages. Demo recipes ship with 9 Southeast Asian languages: Malay, English, Indonesian, Vietnamese, Thai, Khmer, Burmese, Tamil, and Chinese.

*"Bina" means "to build" or "to nurture" in Malay.*

## Demo Recipes

| Recipe | Domain | SDG | What it does |
|--------|--------|-----|-------------|
| **Kira Mikro** | Finance | SDG 8 | Warung bookkeeping: sales recording with preset item buttons, profit calculator with sliders and margin display, debt ledger scanning via camera (vision AI), financial Q&A |
| **Triage Ibu Hamil** | Health | SDG 3 | Maternal emergency triage: symptom screening (blood pressure slider, edema/headache toggles), emergency checklist, one-tap ambulance SMS/call with country-specific numbers (999 MY, 118 ID, 115 VN, etc.) |
| **Pakar Sawit** | Agriculture | SDG 2 | Palm oil expert: leaf disease diagnosis from photo, treatment checklists, nearby agro shop finder via GPS, supply ordering via SMS |
| + 9 more | Various | | Dengue prevention, Khmer agriculture, Myanmar health, Thai business, Vietnamese farming, nutrition guide, and more |

All 12 recipes are in [`recipes/`](recipes/) and are seeded to Firestore for cloud distribution.

## Architecture

```
                    +-----------------------+
                    |     Web Studio        |  Recipe creation wizard
                    |  (React + Vite + TS)  |  with AI-powered generation
                    +-----------+-----------+
                                |
                         YAML recipes
                         stored in Firestore
                                |
        +-----------------------+-----------------------+
        |                                               |
+-------v--------+                            +--------v--------+
|  Android App   |   BLE + QR P2P transfer    |  Android App    |
|  (Kotlin/KMP)  | <------------------------> |  (another user) |
|  Gemma 4 E2B   |                            |  Gemma 4 E2B    |
+----------------+                            +-----------------+
```

| Decision | Why |
|----------|-----|
| On-device LLM (no cloud) | Zero internet dependency. Data never leaves the device. No API costs. |
| YAML DSL (no code) | Domain experts create apps without developers. |
| Kotlin Multiplatform | Business logic in `commonMain` has zero Android dependencies. iOS expansion requires only UI and platform actuals. |
| BLE + QR transfer | Recipes spread device-to-device without internet. One connected device seeds an entire village. |
| 30-language translation | Southeast Asia has 1,200+ languages. Multilingual is a prerequisite, not a feature. |
| Per-recipe safety | Each domain has different risks. Financial tools block investment advice. Health tools block self-medication. |

## Technical Overview

### YAML Recipe DSL

A recipe is a complete application in ~100-500 lines of YAML. The DSL provides:

- **12 widget types**: text_label, text_input, voice_input, camera_input, macro_grid, slider, action_button, markdown_output, metric_card, geo_display, progress_bar, checklist_items
- **11 action types**: ask (LLM query), vision_ask (multimodal), formula (math), go (navigate), geolocate (GPS), set (variable), increment, tts (speech), share, sms, tel
- **Variable interpolation**: `{{variable}}` syntax in actions, formulas, and widget properties
- **Action chaining**: semicolon-separated sequential execution (`formula:profit;ask:Explain RM{{calc_result}}`)
- **Conditional visibility**: `visible_if` / `hidden_if` on any widget
- **Localisation**: `{{l10n.key}}` placeholders resolved per-language from a `localisation.labels` block

See [`recipes/`](recipes/) for complete examples and [`docs/MINIAPP_DSL_SPEC.md`](docs/MINIAPP_DSL_SPEC.md) for the formal specification.

### On-Device AI (Gemma 4 E2B via LiteRT-LM)

- **Model**: Gemma 4 E2B-IT — 2.6 GB, mixed 2/4/8-bit quantization, multimodal (text + vision)
- **Backend**: CPU with XNNPack acceleration; GPU for vision preprocessing with CPU fallback
- **Download**: Resume-capable HTTP download on first launch with progress UI. Range headers handle interrupted connections.
- **Inference**: Streaming token output via Kotlin Flow. Temperature 0.3, topK 40, topP 0.95.
- **Language**: System prompt includes `CRITICAL INSTRUCTION: Reply entirely in {language}` based on the user's selected language flag.
- **Init time**: ~5s with XNNPack weight cache, ~23s cold start

### Intelligent Screen Routing (TriageEngine)

In chat home mode, the user describes what they need in natural language. The on-device LLM reads the `screen_catalog` (screen IDs, descriptions, accepted inputs) and responds with `GO screen_id` or `CLARIFY question`. Supports up to `max_clarifications` rounds before falling back to a default screen. Prefill hints auto-populate input fields from the user's message.

### BLE + QR Offline Sync

Two-phase protocol for zero-internet recipe transfer:

1. **QR pairing**: `BINA-BT:<uuid>:<recipe-id>:<size>:<base64(name|author)>` — receiver scans, sees confirmation, initiates BLE connection
2. **BLE transfer**: 244-byte chunked NOTIFY packets (max ATT MTU minus header), 15ms inter-chunk delay to prevent buffer overflow on mid-range chipsets. A 7 KB compressed recipe transfers in ~0.5 seconds.
3. **QR-only fallback**: `BINA2:` + RFC 4648 base32(gzip(yaml)) — base32's alphabet triggers ZXing alphanumeric mode for ~30% capacity gain over base64

### Web Studio

Browser-based recipe creation wizard at `web/`:

1. **Knowledge Upload**: Upload domain documents (PDF, TXT, DOC). Chunked into 500-word segments, summarized, then Gemini generates a complete recipe suggestion.
2. **Identity**: Configure name, icon, system prompt, disclaimer, safety keywords, author credentials.
3. **Screen Builder**: 7 templates (ask_ai, camera_analysis, calculator, checklist, nearby_places, sms_dispatch, info_display). Drag to reorder, toggle widgets on/off.
4. **Translation**: One-click translation to any of 30+ languages via Gemini API. Extracts translatable strings while preserving action prefixes and variable references.
5. **Publish**: Real-time phone mockup preview. Publish to Firestore or download YAML.

### Safety & Guardrails

Four layers, all domain-specific and configurable per recipe:

1. **Keyword blocking** (ActionDispatcher): Scans prompts against `blocked_keywords` before reaching the LLM. Zero latency, on-device.
2. **System prompt**: Behavioral boundaries (`You MUST NOT provide investment advice...`).
3. **Disclaimers**: Mandatory acknowledgment screen before first use, with author credentials and reference links.
4. **Permission scoping**: Each recipe declares only the permissions it needs (camera, location). Actions requiring undeclared permissions fail gracefully.

### Formula Engine

Recursive descent parser for deterministic calculations. Supports `+`, `-`, `*`, `/`, `%`, `^`, parentheses, and ternary expressions. Variables are interpolated before evaluation. Avoids sending simple arithmetic to a 2.6 GB language model.

## Project Structure

```
bina-v2/
├── app/                              # Android app (Kotlin, Jetpack Compose)
│   └── src/main/java/com/bina/ai/
│       ├── analytics/                # On-device analytics (Room DB) + cloud sync
│       ├── hub/                      # FirestoreRecipeSource with local cache
│       ├── inference/                # LiteRtLmEngine (LiteRT-LM SDK wrapper)
│       ├── install/                  # InstallStore (recipe install tracking)
│       ├── miniapp/
│       │   ├── ui/MiniAppScreen.kt   # Recipe runtime renderer + intro page
│       │   └── widgets/              # 12 widget composables + WidgetRenderer
│       ├── sync/                     # BLE sender/receiver, QR pairing
│       └── ui/
│           ├── components/           # BinaTopBar, BinaBottomNav
│           ├── navigation/           # BinaNavGraph, Screen routes
│           └── screens/              # Hub, MyPocket, OfflineSync, ModelDownload,
│                                     #   RecipeDetail, Analytics
├── shared/                           # Kotlin Multiplatform (commonMain)
│   └── src/commonMain/.../
│       ├── miniapp/
│       │   ├── model/                # MiniApp.kt, Widget.kt (sealed class),
│       │   │                         #   WidgetSerializer.kt
│       │   ├── runtime/              # ActionDispatcher, VariableStore,
│       │   │                         #   FormulaEngine, TriageEngine
│       │   └── MiniAppRepository.kt  # Local + cloud recipe loading
│       └── platform/                 # Logger, LocationProvider, Clock (expect/actual)
├── web/                              # Web Studio (React + Vite + TypeScript)
│   ├── src/app/components/bina/
│   │   ├── Studio.tsx                # 4-step recipe builder (~2,500 lines)
│   │   ├── Dashboard.tsx             # Landing page
│   │   ├── Analytics.tsx             # Usage analytics dashboard
│   │   └── documentParser.ts         # PDF/DOC/TXT text extraction
│   ├── scripts/
│   │   ├── seed-demos.mjs            # Firestore recipe seeder
│   │   └── seed-analytics.mjs        # Analytics test data seeder
│   └── public/                       # Static assets
├── recipes/                          # 12 recipe YAML definitions
├── docs/                             # DSL spec, design mockups
├── firebase.json                     # Firebase hosting + Firestore config
└── firestore.rules                   # Security rules (public read, guarded write)
```

## Build & Run

### Prerequisites

- Android Studio with SDK 35
- Android device (ARM64) or emulator
- Node.js 18+ and pnpm (for Web Studio)

### Android App

```bash
# Debug build
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Release build (unsigned)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

On first launch, the app prompts to download the Gemma 4 E2B model (~2.6 GB). Recipes are fetched from Firestore on first internet connection and cached locally for offline use. Subsequent launches work fully offline.

### Web Studio

```bash
cd web
pnpm install
pnpm dev
```

Opens at `http://localhost:5173`. Requires a Gemini API key (entered in the Studio UI) for AI-powered recipe generation and translation.

### Seed Firestore

```bash
cd web
node scripts/seed-demos.mjs
```

Deletes all existing recipes in Firestore and uploads all 12 YAML files from [`recipes/`](recipes/). Run this after modifying recipe files to sync changes to the cloud.

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Android UI | Jetpack Compose | BOM 2026.04.01 |
| Shared logic | Kotlin Multiplatform | Kotlin 2.2.21 |
| On-device LLM | Google LiteRT-LM + Gemma 4 E2B | latest |
| YAML parsing | kaml (kotlinx.serialization) | 0.67.0 |
| BLE transfer | Android BLE API (GATT) | Android 5.0+ |
| QR encoding | ZXing | 4.3.0 |
| Web frontend | React + Vite + TypeScript | React 19 |
| AI generation | Google Gemini API | gemini-2.5-flash |
| Cloud storage | Firebase Firestore | 25.1.4 |
| Analytics | Room DB | 2.7.1 |
| Compression | Java GZIP | (stdlib) |

## Authors

- **Lee Ing Zhen** — Software Engineer, Maxis
- **Tong Jing Yen** — Computer Engineering, National University of Singapore

## License

[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)

---

*Offline AI for the communities that need it most.*
