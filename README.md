# Bina

**Offline-first AI from verified experts for grassroots communities via Gemma 4 LiteRT.**

Experts author workflows. Gemma 4 LiteRT assembles zero-cost, local-language AI agents that spread via an offline P2P QR + BLE mesh.

---

## What is Bina?

Bina is a platform that turns domain knowledge into deployable AI applications — no coding required. A maternal health NGO writes their triage protocols into a document. Bina's [Web Studio](#web-studio) analyzes it and generates a complete mobile **recipe**: screens, prompts, safety guardrails, and translations into 30+ languages, all encoded as a single YAML file. That file ships to any Android device, where a 2.6 GB on-device Gemma 4 model powers it without ever touching the internet.

No servers. No API keys. No monthly bills. No data leaving the device.

*"Bina" means "to build" or "to nurture" in Malay.*

## Demo Recipes

| Recipe | Domain | SDG | What it does |
|--------|--------|-----|-------------|
| **Kira Mikro** | Finance | SDG 8 | Warung bookkeeping: sales recording with preset items, profit calculator with sliders, debt ledger scanning via camera, financial Q&A |
| **Triage Ibu Hamil** | Health | SDG 3 | Maternal emergency triage: symptom screening (BP, edema, headache), emergency checklist, one-tap ambulance SMS/call with country-specific numbers |
| **Pakar Sawit** | Agriculture | SDG 2 | Palm oil expert: leaf disease diagnosis from photo, treatment checklists, nearby agro shop finder, supply ordering via SMS |

All recipes support 9 languages: Malay, English, Indonesian, Vietnamese, Thai, Khmer, Burmese, Tamil, Chinese.

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
| Kotlin Multiplatform | Business logic portable to iOS without rewriting runtime. |
| BLE + QR transfer | Recipes spread device-to-device without internet. |
| 30-language translation | Southeast Asia has 1,200+ languages. Multilingual is a prerequisite. |
| Per-recipe safety | Each domain has different risks. Safety is domain-specific. |

## Key Technical Components

### YAML Recipe DSL

A recipe is a complete application in ~100-200 lines of YAML with 12 widget types, 10 action types, variable interpolation, formula evaluation, and conditional visibility. See [`recipes/`](recipes/) for examples.

### On-Device AI (Gemma 4 E2B via LiteRT-LM)

- **2.6 GB** mixed 2/4/8-bit quantized model
- Text + vision (multimodal) inference
- CPU backend with XNNPack acceleration (GPU for vision preprocessing)
- Resume-capable model download on first launch
- Per-recipe language instruction for multilingual output

### Intelligent Screen Routing (TriageEngine)

In chat mode, the user describes what they need in natural language. The on-device LLM reads the `screen_catalog` and routes to the correct screen with prefilled inputs. Supports clarification loops before fallback.

### BLE Offline Sync

QR code bootstraps a BLE connection, then 244-byte chunked NOTIFY packets transfer GZIP-compressed YAML. A 7 KB recipe transfers in ~0.5 seconds. Base32 encoding enables single-QR transfer for smaller recipes.

### Web Studio

4-step wizard: upload domain documents (PDF/TXT) &rarr; Gemini analyzes and suggests recipe structure &rarr; configure screens from 7 templates &rarr; one-click translation to all languages &rarr; publish to Firestore or download YAML.

### Safety & Guardrails

Layered: keyword blocking before LLM &rarr; system prompt behavioral boundaries &rarr; mandatory disclaimers &rarr; domain-scoped permissions. Each recipe configures its own blocked keywords and escalation messages.

## Project Structure

```
bina-v2/
├── app/                          # Android app (Kotlin, Jetpack Compose)
│   └── src/main/java/com/bina/ai/
│       ├── inference/            # LiteRT-LM engine
│       ├── miniapp/ui/           # Recipe runtime renderer
│       ├── miniapp/widgets/      # 12 widget composables
│       ├── hub/                  # Firestore recipe source
│       ├── sync/                 # BLE sender/receiver
│       └── ui/                   # Hub, navigation, screens
├── shared/                       # Kotlin Multiplatform (commonMain)
│   └── src/commonMain/.../
│       ├── miniapp/model/        # MiniApp, Widget sealed class, serializers
│       └── miniapp/runtime/      # ActionDispatcher, VariableStore, FormulaEngine, TriageEngine
├── web/                          # Web Studio (React + Vite + TypeScript)
│   ├── src/app/components/bina/  # Studio, Dashboard, Analytics
│   └── scripts/                  # Firestore seeding
├── recipes/                      # Recipe YAML definitions (seeded to Firestore)
└── docs/                         # DSL spec, mockups
```

## Build & Run

### Prerequisites

- Android Studio with SDK 35
- An Android device (ARM64) or emulator
- Node.js 18+ and pnpm (for Web Studio)

### Android App

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app downloads the Gemma 4 E2B model (~2.6 GB) on first launch. Recipes are fetched from Firestore on first connection and cached locally for offline use.

### Web Studio

```bash
cd web
pnpm install
pnpm dev
```

Requires a Gemini API key for AI-powered recipe generation and translation.

### Seed Firestore

```bash
cd web
node scripts/seed-demos.mjs
```

Uploads all recipe YAMLs from `recipes/` to Firestore for cloud distribution.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Android UI | Jetpack Compose (BOM 2026.04) |
| Shared logic | Kotlin Multiplatform (commonMain) |
| On-device LLM | Google LiteRT-LM + Gemma 4 E2B |
| YAML parsing | kaml 0.67.0 (kotlinx.serialization) |
| BLE transfer | Android BLE API (GATT peripheral/central) |
| QR encoding | ZXing (alphanumeric mode with base32) |
| Web Studio | React + Vite + TypeScript |
| AI generation | Google Gemini API |
| Cloud storage | Firebase Firestore |

## Authors

- **Lee Ing Zhen** — Software Engineer, Telekom Malaysia
- **Tong Jing Yen** — Computer Engineering, National University of Singapore

## License

[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)

---

*Offline AI for the communities that need it most.*
