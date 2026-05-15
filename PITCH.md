# Bina.ai

### Offline AI for the communities that need it most.

---

## The Problem

**3.7 billion people** still lack reliable internet access. Across rural Southeast Asia, a palm oil farmer diagnosing a crop disease, a warung owner tracking daily sales, or a village midwife triaging a high-risk pregnancy all face the same barrier: the AI revolution is passing them by.

Cloud-based AI requires stable connectivity, low latency, and often English fluency. For the communities that could benefit most from intelligent tools, every one of these assumptions breaks down:

- **No connectivity.** Palm oil plantations in Sabah, fishing villages in Sulawesi, and highland communities in Myanmar have intermittent-to-zero cellular coverage. A tool that needs the cloud is a tool that doesn't work.
- **No technical literacy.** The people who understand crop diseases, maternal health protocols, and micro-business accounting aren't software engineers. They can't write prompts, configure APIs, or debug JSON. They need tools shaped to their expertise, not the other way around.
- **No common language.** Southeast Asia alone has 1,200+ languages. An English-only AI assistant is useless for a Malay-speaking warung owner or an Indonesian midwife. Multilingual support isn't a feature; it's a prerequisite.
- **No trust infrastructure.** When AI gives financial advice to someone managing RM 50/day in revenue, or medical guidance to a pregnant woman in a village with no hospital, getting it wrong isn't a UX problem. It's a safety problem. Every response needs guardrails, domain context, and clear boundaries.

Existing solutions fail these communities in predictable ways. ChatGPT requires internet and costs money. Custom apps require developers and months of build time. Low-code platforms produce generic tools that don't understand the domain. Nobody is building purpose-built, offline, multilingual AI applications for the specific problems these communities face daily.

---

## The Vision

**What if anyone with domain expertise could create an AI-powered mobile app, no coding required, that runs entirely offline on a $150 Android phone?**

Bina is a platform that turns domain knowledge into deployable AI applications. A maternal health NGO writes their triage protocols into a document. Bina's Studio analyzes it and generates a complete mobile recipe: screens, prompts, safety guardrails, translations into 30 languages, all encoded as a single YAML file. That file ships to any Android device, where a 2.6 GB on-device language model powers it without ever touching the internet.

No servers. No API keys. No monthly bills. No data leaving the device.

The farmer gets crop disease diagnosis from a photo. The midwife gets a pregnancy risk triage checklist. The warung owner gets profit calculations and debt tracking. Each tool is shaped by real domain expertise, runs in their language, works without signal, and has safety rails built in.

We call these tools **recipes**. A recipe is a complete, self-contained AI application defined in YAML and executed by Bina's runtime. Think of it as a declarative specification for an intelligent app, where the complexity of LLM orchestration, widget rendering, state management, and safety enforcement is handled entirely by the platform.

**The workflow:**

```
Domain expert writes knowledge     A village midwife documents her
(guides, protocols, manuals)       triage decision tree for high-risk
            |                      pregnancies, built from 20 years of
            v                      field experience.
  Bina Studio analyzes docs        
  and generates a recipe           Studio reads the document, identifies
  (screens, prompts, safety)       5 screen types needed, generates
            |                      system prompts, adds safety keywords,
            v                      translates to Malay and Indonesian.
  Recipe ships as YAML file        
  (~7 KB after compression)        The 8 KB YAML file encodes the entire
            |                      app: UI layout, AI behavior, formulas,
            v                      checklists, emergency contacts.
  Runs offline on any Android      
  with on-device Gemma 4 E2B       A community health worker in rural
            |                      Sarawak opens the app, runs a symptom
            v                      check, and knows to call an ambulance.
  Shared via QR code or BLE        She shares the recipe to a colleague's
  to spread through communities    phone via a 10-second QR scan. No
                                   internet needed at any step.
```

---

## Architecture Overview

Bina has three major components: the **Android runtime** that executes recipes on-device, the **shared KMP module** that contains all platform-agnostic business logic, and the **Web Studio** that lets creators build recipes without code.

```
                                        WEB STUDIO (React/Vite)
                                   ┌─────────────────────────────┐
                                   │  Document Upload & Analysis  │
                                   │  AI-Powered Recipe Builder   │
                                   │  30-Language Translation     │
                                   │  YAML Generation & Preview   │
                                   │  Firebase Publishing         │
                                   └──────────┬──────────────────┘
                                              │ publishes recipe
                                              v
                                     Firebase / Firestore
                                              │
                              ┌───────────────┼───────────────┐
                              v               v               v
                         ┌─────────┐   ┌─────────┐   ┌─────────┐
                         │ Device A │   │ Device B │   │ Device C │
                         │ (online) │   │(offline) │   │(offline) │
                         └────┬────┘   └─────────┘   └─────────┘
                              │              ^               ^
                              │   BLE sync   │   QR code     │
                              └──────────────┘───────────────┘

               ANDROID RUNTIME (Kotlin / Jetpack Compose)
         ┌────────────────────────────────────────────────────┐
         │  ┌──────────────────────────────────────────────┐  │
         │  │          Compose UI Layer                     │  │
         │  │  WidgetRenderer (12 types) | MiniAppScreen    │  │
         │  │  HubScreen | ModelDownloadScreen | NavGraph    │  │
         │  └──────────────────────────────────────────────┘  │
         │  ┌──────────────────────────────────────────────┐  │
         │  │       Platform Implementations                │  │
         │  │  LiteRtLmEngine | BLE Sync | Location | TTS   │  │
         │  │  ModelDownloadManager | ShortcutHelper         │  │
         │  └──────────────────────────────────────────────┘  │
         │  ┌──────────────────────────────────────────────┐  │
         │  │       Shared Module (KMP / commonMain)        │  │
         │  │  ActionDispatcher | VariableStore | Formulas   │  │
         │  │  TriageEngine | MiniApp Model | YAML Parser    │  │
         │  └──────────────────────────────────────────────┘  │
         └────────────────────────────────────────────────────┘
```

### Why this architecture?

Every architectural decision maps back to the constraints of the communities we serve:

| Decision | Why |
|---|---|
| **On-device LLM (no cloud)** | Zero internet dependency. Data never leaves the device. No API costs. |
| **YAML DSL (no code)** | Domain experts create apps without developers. A midwife or agronomist authors a recipe, not an engineer. |
| **Kotlin Multiplatform** | Business logic in `commonMain` has zero Android dependencies. iOS expansion requires only UI and platform actuals, not re-implementing runtime. |
| **BLE + QR transfer** | Recipes spread device-to-device without internet. One connected device can seed an entire village. |
| **Template-based screens** | 7 templates cover 95% of use cases (chat, camera analysis, calculators, checklists, maps, SMS dispatch, info display). Creators pick and configure, not build from scratch. |
| **30-language translation** | Southeast Asia has 1,200+ languages. Bina supports Malay, Indonesian, Thai, Vietnamese, Tagalog, Burmese, Khmer, and 23 more out of the box. |
| **Per-recipe safety config** | Each domain has different risks. Financial tools block investment advice. Health tools block self-medication keywords. Safety is domain-specific, not one-size-fits-all. |

---

## Deep Dive: The YAML Recipe DSL

A recipe is a complete application in ~100-200 lines of YAML. The DSL was designed with one principle: **everything a non-technical creator needs to specify, nothing they don't.**

### What a recipe defines

```yaml
id: kira_mikro
name: "Kira Mikro"
description: "Kira jualan & hutang warung anda"
icon: "\U0001F4B0"
version: "1.0.0"
category: Finance

author:
  name: "Bina Finance"
  organisation: "Bina.ai"
  verified: true

model:
  model_id: gemma-4-e2b-it
  backend: cpu
  system_prompt: |
    Anda adalah Kira Mikro, pembantu kewangan peribadi
    untuk peniaga warung kecil di Malaysia. Anda memahami
    konsep Buku 555 (buku hutang), format RM (Ringgit Malaysia),
    dan cukai SST 6%.

theme:
  primary: "#D97706"
  secondary: "#FDE68A"

variables:
  user_text: { type: string, default: "" }
  ai_response: { type: string, default: "" }
  photo_path: { type: string, default: "" }
  calc_a: { type: number, default: "0" }
  calc_b: { type: number, default: "0" }
  calc_result: { type: number, default: "0" }

screens:
  - id: home
    title: "Kira Mikro"
    body:
      - macro_grid:
          columns: 2
          buttons:
            - { label: "Rekod Jualan", action: "go:rekod_jualan", icon: "\U0001F4DD" }
            - { label: "Kira Untung", action: "go:kira_untung", icon: "\U0001F4B5" }
            - { label: "Imbas Hutang", action: "go:imbas_hutang", icon: "\U0001F4F7" }
            - { label: "Tanya Kira", action: "go:tanya_kira", icon: "\U0001F4AC" }

  - id: kira_untung
    title: "Kira Untung"
    body:
      - text_label: { text: "Kalkulator Untung Rugi", style: subheading }
      - text_input: { bind: calc_a, label: "Jualan (RM)", input_type: number }
      - text_input: { bind: calc_b, label: "Kos (RM)", input_type: number }
      - action_button: { label: "Kira", action: "formula:calc", style: primary }
      - metric_card: { source: calc_result, label: "Untung Bersih", prefix: "RM " }

  - id: imbas_hutang
    title: "Imbas Hutang"
    body:
      - camera_input: { bind: photo_path, label: "Ambil gambar Buku 555" }
      - action_button:
          label: "Analisis Hutang"
          action: "vision_ask:Analisis gambar buku hutang ini. Senaraikan nama, jumlah, dan status."
          style: primary
      - markdown_output: { source: ai_response, streaming: true }

formulas:
  calc:
    expression: "{{calc_a}} - {{calc_b}}"
    output: calc_result

safety:
  blocked_keywords: [pinjaman haram, skim cepat kaya, pelaburan]
  escalation_message: "Kira Mikro tidak boleh memberi nasihat pelaburan. Sila rujuk penasihat kewangan."
  disclaimer: "Alat bantuan kewangan warung. Bukan nasihat kewangan profesional."

permissions: [camera]

localisation:
  supported: [ms, en, id]
  default: ms
```

### How the DSL maps to the runtime

The key insight is that YAML is the **interface contract** between creators and the runtime. The creator specifies *what* the app does. The runtime handles *how*.

**12 widget types** cover the full range of interactions:

| Widget | Purpose | Binds to |
|---|---|---|
| `text_label` | Headings, instructions, static copy | (display only) |
| `text_input` | Free text, numbers, dropdowns, multiline | variable |
| `voice_input` | Speech-to-text input | variable |
| `camera_input` | Full-resolution photo capture via FileProvider | variable (file path) |
| `slider` | Numeric range selection | variable |
| `action_button` | Triggers any action (AI query, navigate, formula, etc.) | action string |
| `markdown_output` | Renders AI responses with streaming support | variable (source) |
| `metric_card` | Formatted numeric display (RM 1,234.50) | variable |
| `geo_display` | Map with nearby points of interest | dataset |
| `macro_grid` | Button grid for home screen navigation | action strings |
| `progress_bar` | Visual progress indicator | variable |
| `checklist_items` | Step-by-step workflow with completion tracking | variable |

**10 action types** are dispatched through a single `ActionDispatcher`:

| Action | Format | What it does |
|---|---|---|
| `ask:` | `ask:{{user_text}}` | Send text to on-device LLM, stream response |
| `vision_ask:` | `vision_ask:Diagnose this leaf` | Send photo + text to LLM (multimodal) |
| `formula:` | `formula:calc` | Evaluate math expression, store result |
| `go:` | `go:screen_id` | Navigate to another screen |
| `geolocate` | `geolocate` | Get GPS coordinates, trigger nearby search |
| `set:` | `set:mode=advanced` | Set a variable to a value |
| `increment:` | `increment:counter` | Increment a numeric variable |
| `sms:` | `sms:+60123456789:Emergency` | Open SMS with pre-filled recipient and body |
| `tel:` | `tel:+60123456789` | Initiate phone call |
| `tts:` | `tts:{{ai_response}}` | Text-to-speech output |

**Variable interpolation** is the glue. Every action string, formula expression, and widget property supports `{{variable}}` syntax. The `VariableStore` resolves placeholders at runtime, enabling dynamic composition:

```yaml
# User types "nasi lemak" into a text_input bound to form_f1
# User types "50" into a text_input bound to form_f2
# Button action:
action: "ask:Rekod jualan {{form_f1}} sebanyak {{form_f2}} unit"
# Resolves to: "ask:Rekod jualan nasi lemak sebanyak 50 unit"
# Sent to the on-device LLM with full domain context from the system prompt
```

### Action chaining

Actions can be chained with `;` separators. Each action in the chain executes sequentially:

```yaml
action: "set:status=calculating;formula:profit;ask:Explain this profit margin: RM{{calc_result}}"
```

This sets a variable, runs a formula, then asks the AI to explain the result, all from a single button tap.

### Conditional visibility

Widgets support `visible_if` and `hidden_if` properties that reference variables. This enables progressive disclosure without any programming:

```yaml
- metric_card:
    source: calc_result
    label: "Untung Bersih"
    visible_if: calc_result  # Only shows after calculation
```

### YAML parsing

The runtime uses `kaml` (kotlinx.serialization YAML) for type-safe deserialization. A custom `WidgetSerializer` handles the polymorphic widget types: each YAML body item is a single-key map (e.g., `text_input: { bind: user_text }`), and the serializer reads the key as the discriminator to select the correct `Widget` sealed class subtype.

This gives us compile-time type safety on the entire recipe structure. A malformed YAML fails at parse time with a clear error, not at runtime with a null pointer.

---

## Deep Dive: On-Device AI Pipeline

### Why Gemma 4 E2B?

The model choice was driven by three hard constraints:

1. **Must run on mid-range Android phones** (~4-6 GB RAM). That eliminates anything above ~3 GB model size.
2. **Must support both text and vision** (for camera analysis screens). That eliminates text-only models.
3. **Must produce coherent output in Malay, Indonesian, and Thai.** That eliminates models without multilingual training.

Google's **Gemma 4 E2B** (Efficient 2-Bit) hits all three: 2.6 GB with mixed 2/4/8-bit quantization, multimodal text+vision architecture, and multilingual training that includes Southeast Asian languages.

### LiteRT-LM integration

We use Google's **LiteRT-LM** (formerly MediaPipe LLM) SDK, which provides hardware-accelerated inference on Android:

```
Model file (.litertlm)
    |
    v
EngineConfig {
    modelPath: "/data/.../gemma-4-E2B-it.litertlm"
    backend: CPU (XNNPack acceleration)
    visionBackend: GPU (fallback: CPU)
    cacheDir: app cache (XNNPack weight cache)
    maxNumImages: 1
}
    |
    v
Engine.initialize()  // ~5s with cache, ~23s first cold start
    |
    v
Conversation {
    systemInstruction: recipe's system_prompt
    samplerConfig: { temperature: 0.3, topK: 40, topP: 0.95 }
}
    |
    v
conversation.sendMessage(prompt)  // streaming token output
```

**Why CPU over GPU?** The CPU backend with XNNPack is more reliable across the fragmented Android GPU landscape. Mid-range devices (Snapdragon 600-series, MediaTek Dimensity) have inconsistent GPU driver quality. CPU inference is slower but universally works. We use GPU only for the vision backend (image preprocessing), with automatic CPU fallback.

### Model download pipeline

The model is too large to bundle in the APK (2.6 GB vs. the ~150 MB Google Play limit). We built a **resume-capable HTTP download manager** that fetches the model on first launch:

```
First app launch
    |
    v
ModelDownloadScreen shows "AI Model Required" (2.6 GB)
    |
    v
User taps "Download"
    |
    v
ModelDownloadManager:
  - HTTP GET with Range header for resume support
  - Saves to context.filesDir/models/ (app-private storage)
  - .part file during download, renamed on completion
  - Progress updates throttled to 250ms (no UI jank)
  - MIN_MODEL_SIZE = 100 MB sanity check
    |
    v
Download complete → LiteRtLmEngine.initialize()
    |
    v
modelReady = true → normal app UI renders
```

The download is resume-capable: if the connection drops mid-transfer, the next attempt picks up where it left off via the HTTP `Range` header. This is critical for users on unstable cellular connections.

### Inference flow

When a user taps an action button bound to `ask:` or `vision_ask:`:

```
1. ActionDispatcher receives "ask:{{user_text}}"
2. VariableStore.interpolate() resolves variables
3. Safety check: scan prompt against blocked_keywords
4. If blocked → show escalation_message, stop
5. If clear → set is_loading = "true"
6. InferenceEngine.generate(prompt, systemPrompt) returns Flow<String>
7. Each token chunk appends to store["ai_response"]
8. Compose recomposes MarkdownOutput widget per chunk (streaming)
9. Flow completes → set is_loading = "false"
```

For vision inference (`vision_ask:`), the camera-captured image is:
1. Loaded from the file path stored in the bound variable
2. Downscaled to max 448px (memory constraint)
3. Compressed to PNG
4. Sent as `Content.ImageBytes` alongside the text prompt

### FormulaEngine

Calculator screens need real math, not LLM approximations. The `FormulaEngine` is a **recursive descent parser** that evaluates expressions with proper operator precedence:

```
Expression: "({{calc_a}} - {{calc_b}}) * (1 - {{calc_rate}} / 100)"

1. VariableStore interpolates: "(5000 - 2000) * (1 - 6 / 100)"
2. Lexer tokenizes: [(, 5000, -, 2000, ), *, (, 1, -, 6, /, 100, )]
3. Parser builds AST respecting precedence: * / before + -
4. Evaluator walks AST: (3000) * (0.94) = 2820.0
5. Result stored in output variable: calc_result = "2820.0"
6. MetricCard widget displays: "RM 2,820.00"
```

This avoids sending simple arithmetic to a 2.6 GB language model. The formula evaluation is instant and deterministic, unlike LLM-generated math which can hallucinate.

---

## Deep Dive: Intelligent Screen Routing (TriageEngine)

Bina recipes support two home screen modes: **grid** (explicit button navigation) and **chat** (conversational triage).

In chat mode, the user describes what they need in natural language, and the on-device LLM routes them to the correct screen. This is powered by the `TriageEngine`:

```
User says: "daun sawit saya ada bintik oren"
("my palm oil leaves have orange spots")
    |
    v
TriageEngine reads screen_catalog:
  - rekod_jualan: "Form to record daily sales transactions"
  - kira_untung: "Calculator for profit and loss"
  - imbas_hutang: "Camera analysis of handwritten debt ledger"
  - diagnosis_daun: "Camera-based leaf disease diagnosis for palm oil"
  - tanya_pakar: "General agricultural Q&A"
    |
    v
LLM prompt: "User said: '{{user_message}}'.
             Available screens: [catalog with descriptions].
             Respond with: GO screen_id
             or: CLARIFY question_to_ask"
    |
    v
LLM responds: "GO diagnosis_daun"
    |
    v
TriageEngine navigates to diagnosis_daun screen
with prefill_hints applied (user's text → relevant input field)
```

The `screen_catalog` in the YAML provides structured metadata for each screen:

```yaml
screen_catalog:
  - id: diagnosis_daun
    title: "Diagnosis Daun"
    template: camera_analysis
    icon: "\U0001F33F"
    description: "Camera-based leaf disease diagnosis for palm oil"
    accepted_inputs: [photo_path, user_text]
    prefill_hints:
      symptom_description: user_text
```

**Clarification loop:** If the LLM can't determine the right screen from the user's message, it responds with `CLARIFY:` followed by a question. The engine supports up to `max_clarifications` rounds (default: 2) before falling back to the `fallback` screen.

This means a farmer can open the app and say what they need in their own words, in their own language, and the on-device model figures out which screen to send them to. No menu navigation required.

---

## Deep Dive: BLE Offline Sync

A recipe that only exists on one phone is a tool. A recipe that spreads across a community is infrastructure. Bina's offline sync system uses **Bluetooth Low Energy** to transfer recipes device-to-device with zero internet dependency.

### The problem with QR-only transfer

A typical recipe YAML is 5-15 KB. After GZIP compression, that's still 2-7 KB. A single QR code holds ~3 KB in binary mode. We could split across multiple QR codes, but that's fragile and slow to scan.

### Our solution: QR-bootstrapped BLE

We use a **two-phase protocol**: a QR code bootstraps a BLE connection, then BLE handles the actual data transfer at higher throughput.

**Phase 1: QR Pairing**

The sender generates a QR code with BLE connection metadata:

```
BINA-BT:<uuid-hex>:<recipe-id>:<size-bytes>:<base64(name|author)>
```

Example: `BINA-BT:a1b2c3d4e5f6....:kira_mikro:4823:S2lyYSBNaWtyb3xCaW5hIEZpbmFuY2U=`

The receiver scans this, sees a confirmation sheet ("Receive Kira Mikro by Bina Finance? 4.8 KB"), and initiates a BLE connection to the advertised service UUID.

**Phase 2: BLE Chunked Transfer**

```
Sender (BLE Peripheral)              Receiver (BLE Central)
        |                                    |
        |  <-- BLE connect to service UUID   |
        |                                    |
        |  NOTIFY chunk[0] (244 bytes) -->   |
        |  (15ms delay)                      |
        |  NOTIFY chunk[1] (244 bytes) -->   |
        |  (15ms delay)                      |
        |  ...                               |
        |  NOTIFY chunk[N] (remaining) -->   |
        |                                    |
        |              buffer.size >= expectedSize
        |                                    |
        |                    GZIP decompress |
        |                    YAML parse      |
        |                    Recipe imported  |
```

**Why 244-byte chunks?** BLE 4.2 has a default ATT MTU of 247 bytes. Subtract 3 bytes for the ATT notification header, and 244 is the maximum payload per NOTIFY packet. We negotiated MTU to 247 on connection.

**Why 15ms inter-chunk delay?** Without throttling, the BLE stack's internal buffer overflows on mid-range devices, causing silent packet loss. 15ms is the empirical sweet spot: fast enough for a 7 KB recipe to transfer in ~0.5 seconds, slow enough to avoid buffer overflow on Snapdragon 600-series chipsets.

### QR-only mode: Base32 encoding optimization

For small recipes or when BLE isn't available, Bina also supports direct QR code transfer. The wire format is:

```
BINA2: + RFC 4648 base32(gzip(yaml-bytes))
```

**Why base32 instead of base64?** Base32's alphabet (`A-Z`, `2-7`) is a subset of QR code's alphanumeric mode character set. This triggers the QR encoder (ZXing) to use alphanumeric mode (5.5 bits per character) instead of byte mode (8 bits per character). The result is a **~30% capacity gain** for the same QR code version.

For a 7 KB compressed recipe, this is the difference between needing two QR codes and fitting in one.

### Recipe import

Once received (via BLE or QR), the `RecipeImporter` writes the YAML to the user's miniapps directory and triggers `MiniAppRepository` to re-scan. The new recipe appears in the Hub immediately. No restart required.

---

## Deep Dive: Web Studio

The Web Studio is where domain experts become recipe creators. It's a 4-step wizard that transforms uploaded documents into deployable AI applications.

### Step 1: Knowledge Upload

The creator uploads their domain documents: training manuals, protocol guides, SOPs, reference sheets. Studio processes them:

1. Read file contents (PDF/TXT support)
2. Chunk into 500-word segments
3. Summarize first 3 chunks via Gemini API
4. Feed all summaries into a structured JSON generation call
5. Gemini returns a complete recipe suggestion: name, screens, system prompt, category, theme, safety keywords, author metadata

The creator reviews each suggestion component and toggles what to keep. This isn't a black-box generator. It's a collaboration between domain expertise and AI capability.

### Step 2: Identity

Configure the recipe's personality: name, icon, system prompt, intro page, disclaimer, author credentials, blocked keywords. The system prompt can be auto-generated from the uploaded documents via a separate Gemini call that focuses on domain-appropriate tone and guardrails.

### Step 3: Style & Layout

Build screens using 7 templates:

| Template | Use case | Example |
|---|---|---|
| **Ask AI** | Chat or form-based Q&A | "Tanya Pakar" (ask the expert) |
| **Camera Analysis** | Photo + AI analysis | "Diagnosis Daun" (leaf disease) |
| **Calculator** | Formula-based computation | "Kira Untung" (profit calculator) |
| **Checklist** | Step-by-step workflows | "Senarai Semak Kecemasan" (emergency checklist) |
| **Nearby Places** | Location-based POI search | "Kedai Agro" (agro shops nearby) |
| **SMS / Phone** | Emergency contacts and dispatch | "Panggil Ambulans" (call ambulance) |
| **Info Display** | Static text cards | "Rujukan Cepat" (quick reference) |

Each template is a pre-configured combination of widgets with editable fields. The creator fills in domain-specific content (headings, prompts, button labels, checklist steps, contact numbers) without understanding the underlying widget system.

**Translation:** One-click translation to all selected languages via Gemini. The system extracts all user-facing strings, preserves action prefixes and variable references, and sends the translatable content in a structured schema. A 1.5-second delay between language calls prevents rate limiting.

### Step 4: Review

Real-time phone mockup preview. The creator sees exactly what the end user will see, including screen navigation, widget layout, and theme colors. Generate and download YAML files (one per language), or publish directly to Firebase for cloud distribution.

### YAML escaping

A subtle but critical detail: YAML values containing double quotes or backslashes will break the parser. This is especially common in multilingual content (Malay financial terms like `"JUMLAH HUTANG: RM ___"` inside action strings). Studio applies a `yamlEsc()` function that escapes `"` to `\"` and `\` to `\\` in all generated YAML values. This was discovered from a production bug where seeded Firestore recipes caused `MalformedYamlException` at runtime.

---

## Deep Dive: Safety & Guardrails

Bina serves domains where wrong AI output can cause real harm. The safety system is layered and domain-specific.

### Layer 1: Keyword blocking (ActionDispatcher)

Before any prompt reaches the LLM, `ActionDispatcher.handleAsk()` scans against the recipe's `blocked_keywords`. A financial recipe blocks "pinjaman haram" (illegal loans), "skim cepat kaya" (get-rich-quick schemes), and "pelaburan" (investment advice). A health recipe blocks "ubat sendiri" (self-medication) and "abaikan doktor" (ignore doctors).

When triggered, the user sees the recipe's `escalation_message` instead of an AI response. This happens entirely on-device with zero latency.

### Layer 2: System prompt guardrails

Each recipe's `system_prompt` instructs the LLM on behavioral boundaries:

```yaml
system_prompt: |
  You are Kira Mikro, a financial helper for warung operators.
  You MUST NOT provide investment advice, loan recommendations,
  or tax planning. You MUST refer users to a qualified financial
  advisor for these topics. You are a bookkeeping assistant only.
```

### Layer 3: Disclaimers and intro pages

Recipes can require users to acknowledge a disclaimer before accessing the app:

```yaml
setup:
  intro_page:
    accept_label: "Saya Faham"
    disclaimer: "Alat bantuan kewangan warung. Bukan nasihat profesional."
    author:
      name: "Bina Finance"
      organisation: "Bina.ai"
      verified: true
```

### Layer 4: Domain-scoped permissions

Each recipe declares only the permissions it needs. A calculator recipe requests nothing. A camera analysis recipe requests `camera`. A nearby places recipe requests `location`. The runtime enforces this at the action level: a `geolocate` action in a recipe without `location` permission will fail gracefully.

---

## Demo Recipes

Three flagship recipes demonstrate Bina's capabilities across different SDG domains:

### Kira Mikro (SDG 8: Decent Work and Economic Growth)

**Target users:** Warung (small shop) owners in Malaysia managing RM 50-200/day revenue

**Problem:** 70% of Malaysian micro-enterprises use handwritten "Buku 555" (debt ledgers). No digital tools speak their language, understand their currency format (RM), or work offline in rural markets.

**Screens:**
- **Rekod Jualan** (Record Sales): Form with dropdown item selection, quantity, price inputs. AI generates daily summary.
- **Kira Untung** (Calculate Profit): Calculator with formula `revenue - costs`. Instant, deterministic, no LLM needed.
- **Imbas Hutang** (Scan Debt): Camera captures handwritten Buku 555 page. Vision AI extracts names, amounts, and payment status.
- **Tanya Kira** (Ask Kira): Open chat for financial questions. System prompt includes SST (6% tax) knowledge and RM formatting.

**Languages:** Malay, English, Indonesian

### Triage Ibu Hamil (SDG 3: Good Health and Well-Being)

**Target users:** Rural midwives and community health workers in areas with no hospital access

**Problem:** High-risk pregnancies in rural Malaysia and Indonesia often go undetected until it's too late. Village midwives have the knowledge but no decision support tools.

**Screens:**
- **Saringan Gejala** (Symptom Screening): Structured form for recording blood pressure, edema, headache severity, fetal movement. AI triages risk level.
- **Senarai Semak Kecemasan** (Emergency Checklist): Step-by-step protocol for eclampsia, hemorrhage, and obstructed labor. Progress tracking with completion indicators.
- **Panggil Ambulans** (Call Ambulance): One-tap SMS/call to pre-configured emergency contacts. Pre-filled message with patient location and symptoms.
- **Rujukan Cepat** (Quick Reference): Static cards with danger signs, normal ranges, and when-to-refer guidelines.

**Safety:** Blocks "ubat sendiri" (self-medication), "abaikan simptom" (ignore symptoms). Escalation: "Ini mungkin kecemasan. Hubungi ambulans segera." (This may be an emergency. Call an ambulance immediately.)

**Languages:** Malay, Indonesian, English

### Pakar Sawit (SDG 2: Zero Hunger)

**Target users:** Smallholder palm oil farmers in Sabah and Sarawak

**Problem:** Palm oil disease can destroy entire harvests. Diagnosis requires an agronomist visit that costs RM 200+ and takes weeks. By then, the disease has spread.

**Screens:**
- **Diagnosis Daun** (Leaf Diagnosis): Camera captures diseased leaf. Vision AI identifies Ganoderma, Basal Stem Rot, nutrient deficiency, and 12 other conditions. Returns treatment recommendations.
- **Senarai Rawatan** (Treatment Checklist): Step-by-step treatment protocol generated from diagnosis. Tracks application schedule.
- **Kedai Agro** (Agro Shops): GPS-powered nearby search for agricultural supply stores. Shows distance, stock availability.
- **Pesan Bekalan** (Order Supplies): SMS dispatch with pre-filled order for fertilizer, fungicide, or equipment.
- **Tanya Pakar** (Ask Expert): Open chat for agricultural questions. System prompt includes palm oil cultivation knowledge, seasonal calendars, and Malaysian agricultural extension service contacts.

**Languages:** Malay, Indonesian, English

---

## Codebase Statistics

| Component | Language | Files | Lines of Code |
|---|---|---|---|
| Android app | Kotlin | 89 | ~10,670 |
| Shared KMP module | Kotlin | 12 | ~1,096 |
| Web Studio | TypeScript/React | ~40 | ~62,400 |
| **Total** | | ~141 | **~74,166** |

### Key module breakdowns

| Module | Lines | What it does |
|---|---|---|
| `ActionDispatcher.kt` | 209 | Routes 10 action types, handles chaining, safety checks, LLM streaming |
| `VariableStore.kt` | 69 | Reactive key-value store, interpolation, comparison operators |
| `FormulaEngine.kt` | 98 | Recursive descent math parser with proper operator precedence |
| `TriageEngine.kt` | 107 | LLM-powered conversational screen routing with clarification loop |
| `WidgetSerializer.kt` | ~100 | Custom YAML polymorphic deserializer for 12 widget types |
| `MiniApp.kt` | 176 | Full data model with 15+ nested data classes |
| `LiteRtLmEngine.kt` | 198 | On-device LLM with GPU vision fallback to CPU |
| `ModelDownloadManager.kt` | ~120 | Resume-capable HTTP download with progress tracking |
| `BleSender.kt` | 186 | BLE peripheral with 244-byte chunked NOTIFY transfer |
| `BleReceiver.kt` | 163 | BLE central with MTU 247, buffer reassembly |
| `RecipePayload.kt` | 79 | GZIP + RFC 4648 base32 encoding for QR transfer |
| `BlePairingPayload.kt` | 78 | QR-bootstrapped BLE pairing wire format |
| `Studio.tsx` | 2,538 | 4-step recipe builder with AI suggestions, translation, YAML generation |
| `screenTemplates.ts` | 317 | 7 template definitions with widget resolution and formula support |

---

## Technology Stack

| Layer | Technology | Why |
|---|---|---|
| **Android UI** | Jetpack Compose (BOM 2026.04) | Declarative UI that maps naturally to our declarative YAML DSL |
| **Shared logic** | Kotlin Multiplatform (commonMain) | Zero-dependency business logic, portable to iOS without rewriting runtime |
| **On-device LLM** | Google LiteRT-LM + Gemma 4 E2B | Hardware-accelerated inference, mixed quantization for RAM-constrained devices |
| **YAML parsing** | kaml 0.67.0 (kotlinx.serialization) | Type-safe deserialization with custom serializers for polymorphic widgets |
| **BLE transfer** | Android BLE API (GATT peripheral/central) | Zero-internet device-to-device transfer, universally available on Android 5.0+ |
| **QR encoding** | ZXing | Alphanumeric-mode optimization with base32 for 30% capacity gain |
| **Compression** | Java GZIP | 60-70% compression on YAML text, critical for QR fit |
| **Web Studio** | React + Vite + TypeScript | Fast iteration on the creator tool |
| **AI generation** | Google Gemini API (gemma-4-31b-it) | Recipe suggestions, translations, system prompt generation |
| **Cloud storage** | Firebase Firestore | Recipe distribution for connected devices |
| **Navigation** | Jetpack Navigation Compose | Deep links, back stack management, fade transitions |
| **Analytics** | Room DB + custom event tracking | On-device analytics with periodic cloud sync |

---

## What Makes This Hard

Building Bina isn't a single hard problem. It's a dozen interlocking constraints that each narrow the solution space:

**The model must be small enough to fit on a phone, smart enough to diagnose crop diseases in Malay.** Gemma 4 E2B with mixed 2/4/8-bit quantization hits 2.6 GB. Smaller models lose multilingual capability. Larger models won't fit.

**The DSL must be simple enough for a midwife to create a recipe, expressive enough to represent calculators, checklists, camera analysis, and SMS dispatch.** 12 widget types and 10 action types, composed through 7 templates, cover the space. Fewer templates means missing use cases. More templates means overwhelming non-technical creators.

**The transfer protocol must work without internet, fit through BLE's 244-byte MTU, and be fast enough that a farmer doesn't walk away.** QR bootstraps the connection (instant), BLE transfers the data (sub-second for typical recipes), base32 encoding squeezes direct QR transfer when BLE isn't available.

**The safety system must be domain-specific without requiring the creator to be a safety engineer.** Per-recipe keyword blocking, system prompt guardrails, mandatory disclaimers, and permission scoping. Each layer catches what the others miss.

**The runtime must be reactive without a framework like React or SwiftUI's diffing engine.** The `VariableStore.onChange` callback bridges to Compose's recomposition via a revision counter. One callback, one integer, full reactivity.

**Translations must preserve action prefixes, variable references, and formula expressions while translating user-facing text across 30 languages.** The translation system extracts only translatable strings, sends them through Gemini with indexed keys, and reassembles the result while preserving all technical content.

None of these problems is individually novel. The engineering challenge is solving all of them simultaneously within the constraints of a $150 Android phone with no internet connection.

---

## Open Source and Extensibility

Bina is designed so that any organization can fork the platform and create recipes for their domain:

- **New screen templates** can be added to `screenTemplates.ts` without modifying the runtime. The template system is purely declarative.
- **New action types** require adding a handler to `ActionDispatcher.kt`. The single-entry-point dispatch pattern makes this a one-function change.
- **New widget types** require a `Widget` sealed class subtype, a YAML key in `WidgetSerializer`, and a Compose composable in `WidgetRenderer`. Three files, three additions.
- **New languages** are added to the `ALL_LANGUAGES` array in Studio. The translation pipeline handles the rest.
- **New models** can be swapped by changing the `.litertlm` file. The runtime is model-agnostic; any LiteRT-LM compatible model works.

The goal is that Bina becomes infrastructure. Not a product you use, but a platform you build on. A maternal health NGO in Myanmar, an agricultural extension service in Indonesia, a microfinance organization in the Philippines: each takes the same platform and creates tools shaped by their expertise, for their communities, in their languages.

**Offline AI for the communities that need it most.**
