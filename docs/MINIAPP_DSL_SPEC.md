# Bina.ai MiniApp DSL Specification v2.0

## Overview

A **MiniApp** is a self-contained offline application defined entirely in a single YAML file. Architects (non-coders) author these files. The Bina.ai runtime parses the YAML and dynamically renders native Android UI, routes actions, and runs on-device AI inference — all without writing a single line of code.

**One YAML file = one complete app.**

```
┌─────────────────────────────────────────────────────────┐
│                    farm_buddy.yaml                       │
│                                                         │
│  metadata ──► Hub card (icon, name, category)           │
│  model    ──► LiteRT-LM engine config + system prompt   │
│  theme    ──► Colors, text size, accessibility          │
│  screens  ──► Multi-screen native UI                    │
│  widgets  ──► 10 composable types (input, output, etc.) │
│  actions  ──► AI calls, formulas, navigation, GPS       │
│  formulas ──► Math expressions on variables             │
│  data     ──► Bundled datasets (geo points, etc.)       │
│  safety   ──► Keyword blocking, disclaimers             │
└─────────────────────────────────────────────────────────┘
```

## Architecture

```
                    ┌──────────────┐
                    │  YAML File   │
                    │ (in assets/) │
                    └──────┬───────┘
                           │ kaml parser
                           ▼
                    ┌──────────────┐
                    │  MiniApp     │
                    │  data model  │
                    │  (Kotlin)    │
                    └──────┬───────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   ┌────────────┐  ┌─────────────┐  ┌────────────┐
   │  Variable  │  │   Widget    │  │   Action   │
   │   Store    │  │  Renderer   │  │ Dispatcher │
   │ (reactive) │  │ (Compose)   │  │            │
   └─────┬──────┘  └──────┬──────┘  └──────┬─────┘
         │                │                │
         │    ┌───────────┘                │
         │    │                            │
         ▼    ▼                            ▼
   ┌──────────────┐               ┌──────────────┐
   │  MiniApp     │               │  LiteRT-LM   │
   │  Screen      │◄──────────────│  Engine       │
   │  (dynamic    │  ai_response  │  (Gemma 4)   │
   │   renderer)  │               └──────────────┘
   └──────────────┘
```

### Data flow for a single user action

```
User taps "Ask Farm Buddy" button
         │
         ▼
ActionDispatcher.dispatch("ask:{{user_text}}")
         │
         ├─► interpolate("ask:{{user_text}}")
         │   → "ask:My chilli leaves have yellow spots"
         │
         ├─► GuardrailEngine.check(blocked_keywords)
         │   → ALLOW
         │
         ├─► LiteRtLmEngine.generate(prompt)
         │   → Flow<String> streaming tokens
         │
         └─► store["ai_response"] += each token
                    │
                    ▼
             markdown_output widget
             auto-recomposes (Compose observes store)
```

---

## YAML Schema

### Top-Level Structure

```yaml
# ── Required ────────────────────────────────
id: string              # Unique ID (e.g., "farm_buddy")
name: string            # Display name
description: string     # Short tagline for Hub card
icon: string            # Emoji (e.g., "🌾")
version: string         # Semver (e.g., "1.0.0")
category: string        # Agriculture | Health | Business | Education | Emergency

# ── Optional ────────────────────────────────
author:
  name: string
  organisation: string
  verified: boolean     # Default: false

model: { ... }          # AI model configuration
theme: { ... }          # Visual theming
localisation: { ... }   # Multi-language labels
variables: { ... }      # Reactive state
screens: [ ... ]        # UI screens
formulas: { ... }       # Math expressions
data: { ... }           # Bundled datasets
safety: { ... }         # Guardrails
permissions: [ ... ]    # Device capabilities needed
```

### `model` — AI Engine Configuration

Maps directly to LiteRT-LM `EngineConfig` + `ConversationConfig`.

```yaml
model:
  model_id: gemma-4-e2b-it       # Model identifier
  backend: cpu                     # cpu | gpu | npu
  system_prompt: |                 # Multi-line system instruction
    You are Farm Buddy, an agricultural advisor...
  sampler:
    temperature: 0.3               # 0.0 = deterministic, 0.8 = creative
    top_k: 40                      # Top-K sampling
    top_p: 0.95                    # Nucleus sampling
    max_tokens: 512                # Max output length
```

### `theme` — Visual Configuration

```yaml
theme:
  primary: "#2E7D32"               # Buttons, headers, active states
  secondary: "#A5D6A7"            # Accents, backgrounds
  text_size: standard              # standard | large | extra_large
```

### `localisation` — Multi-Language Support

```yaml
localisation:
  default_language: ms             # "en" or "ms"
  labels:
    en:
      chat_placeholder: "Ask Farm Buddy..."
    ms:
      chat_placeholder: "Tanya Pakar Tani..."
```

### `variables` — Reactive State

In-memory key-value store. Widgets read/write via `bind` (input) and `source` (output). Actions interpolate via `{{variable_name}}`.

```yaml
variables:
  user_text:    { type: string,  default: "" }
  photo_path:   { type: string,  default: "" }
  revenue:      { type: number,  default: "0" }
  tax_rate:     { type: number,  default: "5" }
  profit:       { type: number,  default: "0" }
```

**Types:** `string`, `number`, `boolean`, `location`

**Auto-created variables** (always available, do not declare):
| Variable | Type | Description |
|---|---|---|
| `ai_response` | string | LLM output from `ask:` / `vision_ask:` actions |
| `is_loading` | boolean | `true` while an async action is running |
| `current_screen` | string | ID of the currently displayed screen |

### `screens` — UI Screens

Ordered list. First screen is the entry point. Each screen has a `body` — a list of widgets rendered top-to-bottom in a scrollable column.

```yaml
screens:
  - id: home
    title: Farm Buddy
    body:
      - text_label:
          text: "What would you like to do?"
          style: subheading
      - macro_grid:
          columns: 2
          buttons:
            - { label: "🍃 Diagnose", action: "go:diagnose" }
            - { label: "💰 Profit",   action: "go:profit" }
      - text_input:
          bind: user_text
          hint: "Ask anything..."
      - action_button:
          label: "Ask"
          action: "ask:{{user_text}}"
      - markdown_output:
          source: ai_response
```

### `formulas` — Math Expressions

Named expressions evaluated by `formula:` actions. Support `{{variable}}` interpolation and standard arithmetic: `+ - * / % ( )`.

```yaml
formulas:
  profit:
    expression: "({{revenue}} - {{costs}}) * (1 - {{tax_rate}} / 100)"
    output: profit
  bmi:
    expression: "{{weight}} / ({{height}} / 100) ^ 2"
    output: bmi_value
```

### `data` — Bundled Datasets

Static data shipped with the miniapp. Used by `geo_display` and available for LLM context.

```yaml
data:
  agro_shops:
    type: points
    items:
      - { name: "Kedai Baja Pak Ali", lat: 3.139, lng: 101.687, info: "Open 8am-5pm" }
      - { name: "AgriMart Seremban",  lat: 2.725, lng: 101.938, info: "Fertiliser specialist" }
```

### `safety` — Guardrails

```yaml
safety:
  blocked_keywords:
    - mix pesticide
    - poison
    - drink chemical
  escalation_message: "Please contact your local agriculture officer."
  disclaimer: "AI-generated guidance. Not a professional consultation."
```

### `permissions` — Device Capabilities

```yaml
permissions:
  - camera       # camera_input widget
  - location     # geo_display + geolocate action
  - microphone   # voice_input widget
```

---

## Widget Reference

Each widget is a single-key map in a screen's `body` list:

```yaml
- widget_type:
    prop1: value1
    prop2: value2
```

All widgets support these optional props:
- `visible_if: variable_name` — only render when variable is truthy
- `hidden_if: variable_name` — hide when variable is truthy

### 1. `text_label` — Static Text

| Prop | Type | Default | Description |
|---|---|---|---|
| `text` | string | *required* | Content. Supports `{{var}}` interpolation |
| `style` | string | `body` | `heading` \| `subheading` \| `body` \| `caption` |
| `align` | string | `left` | `left` \| `center` \| `right` |
| `color` | string | theme | Hex color override |

```yaml
- text_label:
    text: "Welcome to Farm Buddy"
    style: heading
    align: center
```

### 2. `text_input` — Text Field

| Prop | Type | Default | Description |
|---|---|---|---|
| `bind` | string | *required* | Variable to write to |
| `hint` | string | `""` | Placeholder text |
| `label` | string | — | Label above field |
| `input_type` | string | `text` | `text` \| `number` \| `multiline` |

```yaml
- text_input:
    bind: user_text
    hint: "Ask anything..."
    label: "Your question"
```

### 3. `voice_input` — Speech-to-Text

Uses Android `SpeechRecognizer`. Falls back to `text_input` if STT unavailable.

| Prop | Type | Default | Description |
|---|---|---|---|
| `bind` | string | *required* | Variable to write transcribed text to |
| `hint` | string | `""` | Placeholder for text fallback |
| `language` | string | from localisation | STT locale (e.g., `ms-MY`, `en-US`) |
| `mode` | string | `tap` | `tap` (tap-to-speak) \| `hold` (push-to-talk) |

```yaml
- voice_input:
    bind: user_text
    hint: "Tap mic to speak..."
    language: ms-MY
```

### 4. `camera_input` — Photo Capture

Captures via device camera. Stores file path for `vision_ask:` actions.

| Prop | Type | Default | Description |
|---|---|---|---|
| `bind` | string | *required* | Variable to write photo path to |
| `label` | string | `"Take Photo"` | Button text |
| `preview` | boolean | `true` | Show captured image preview |

```yaml
- camera_input:
    bind: photo_path
    label: "Take photo of leaf"
```

### 5. `macro_grid` — Button Grid

| Prop | Type | Default | Description |
|---|---|---|---|
| `columns` | int | `2` | Grid columns (1, 2, or 3) |
| `buttons` | list | *required* | List of button objects |

Each button:
| Prop | Type | Default | Description |
|---|---|---|---|
| `label` | string | *required* | Button text (emoji OK) |
| `action` | string | *required* | Action string |
| `color` | string | theme.primary | Hex color override |

```yaml
- macro_grid:
    columns: 2
    buttons:
      - { label: "🍃 Diagnose Leaf", action: "go:diagnose" }
      - { label: "💰 Profit Calc",   action: "go:profit" }
```

### 6. `slider` — Analog Input

| Prop | Type | Default | Description |
|---|---|---|---|
| `bind` | string | *required* | Variable to write value to |
| `min` | number | `0` | Minimum value |
| `max` | number | `100` | Maximum value |
| `step` | number | `1` | Step interval |
| `label` | string | — | Label above slider |
| `left_label` | string | — | Label at min end |
| `right_label` | string | — | Label at max end |
| `show_value` | boolean | `true` | Display current value |

```yaml
- slider:
    bind: tax_rate
    min: 0
    max: 30
    step: 1
    label: "Tax Rate"
    left_label: "0%"
    right_label: "30%"
```

### 7. `action_button` — Action Trigger

| Prop | Type | Default | Description |
|---|---|---|---|
| `label` | string | *required* | Button text |
| `action` | string | *required* | Action string |
| `style` | string | `primary` | `primary` \| `secondary` \| `danger` |
| `icon` | string | — | Emoji prefix |
| `confirm` | string | — | Confirmation dialog message |

```yaml
- action_button:
    label: "Calculate Profit"
    action: "formula:profit"
    style: primary
    icon: "💰"
```

### 8. `markdown_output` — AI Response Display

| Prop | Type | Default | Description |
|---|---|---|---|
| `source` | string | *required* | Variable to read from |
| `empty_text` | string | `""` | Shown when source is empty |
| `streaming` | boolean | `true` | Show typing indicator while `is_loading` |

```yaml
- markdown_output:
    source: ai_response
    empty_text: "Responses will appear here..."
```

### 9. `metric_card` — Prominent Number Display

| Prop | Type | Default | Description |
|---|---|---|---|
| `source` | string | *required* | Variable to read from |
| `label` | string | *required* | Label below the number |
| `prefix` | string | — | Before number (e.g., "RM") |
| `suffix` | string | — | After number (e.g., "kg") |
| `color` | string | theme.primary | Hex color for the number |
| `format` | string | `decimal_2` | `integer` \| `decimal_1` \| `decimal_2` |

```yaml
- metric_card:
    source: profit
    label: "Net Profit"
    prefix: "RM "
    format: decimal_2
```

### 10. `geo_display` — Nearest Locations

Sorts bundled geo points by distance from user's current location.

| Prop | Type | Default | Description |
|---|---|---|---|
| `data` | string | *required* | Dataset name from `data:` section |
| `limit` | int | `5` | Max points to show |
| `show_distance` | boolean | `true` | Show distance in km |
| `empty_text` | string | — | Shown before location available |

```yaml
- geo_display:
    data: agro_shops
    limit: 3
    show_distance: true
    empty_text: "Tap 'Get Location' to find nearby shops"
```

---

## Action Reference

Actions are strings with a prefix that determines behavior. Used in `action_button.action` and `macro_grid.buttons[].action`. All actions support `{{variable}}` interpolation.

### `ask:` — Text LLM Call

Sends text prompt to on-device LLM. Streams response into `ai_response`. Checks `safety.blocked_keywords` before sending. System prompt from `model.system_prompt`.

```yaml
action: "ask:{{user_text}}"
action: "ask:What fertiliser for rice in {{soil_type}} soil?"
```

### `vision_ask:` — Multimodal LLM Call

Sends captured image + text prompt to vision-capable LLM. Reads image path from `photo_path` variable. Writes response to `ai_response`.

```yaml
action: "vision_ask:Diagnose this leaf. {{user_text}}"
```

### `formula:` — Math Evaluation

Evaluates a named formula from the `formulas:` section. Interpolates `{{vars}}` in the expression, evaluates arithmetic, writes result to the formula's `output` variable.

```yaml
action: "formula:profit"
```

### `go:` — Screen Navigation

Navigates to another screen within the miniapp. `go:home` returns to the first screen.

```yaml
action: "go:diagnose"
action: "go:home"
```

### `geolocate` — GPS Location

Grabs current coordinates from device GPS. Calculates haversine distances to all points in bundled datasets. Updates `geo_display` widgets automatically.

```yaml
action: "geolocate"
```

### `set:` — Direct Variable Assignment

Sets a variable value directly. Useful for resetting state.

```yaml
action: "set:ai_response="        # Clear AI response
action: "set:mode=advanced"        # Set a flag
```

---

## Complete Example — Farm Buddy

```yaml
id: farm_buddy
name: Farm Buddy
description: "Diagnose crops, calculate profit, find nearby agro shops."
icon: "🌾"
version: "1.0.0"
category: Agriculture

author:
  name: Universiti Putra Malaysia
  organisation: AgriTech Faculty
  verified: true

model:
  model_id: gemma-4-e2b-it
  backend: cpu
  system_prompt: |
    You are Farm Buddy (Pakar Tani), an agricultural education assistant
    for Southeast Asian smallholder farmers.
    - Give simple, practical, safe guidance
    - Do not claim certainty in diagnoses
    - Ask clarifying questions about crop type, symptoms, weather
    - If the issue seems severe, advise contacting a local agriculture officer
    - Never recommend restricted or dangerous chemicals
  sampler:
    temperature: 0.3
    top_k: 40
    top_p: 0.95
    max_tokens: 512

theme:
  primary: "#2E7D32"
  secondary: "#A5D6A7"
  text_size: standard

localisation:
  default_language: ms
  labels:
    en:
      chat_placeholder: "Ask Farm Buddy..."
    ms:
      chat_placeholder: "Tanya Pakar Tani..."

variables:
  user_text:    { type: string, default: "" }
  photo_path:   { type: string, default: "" }
  ai_response:  { type: string, default: "" }
  revenue:      { type: number, default: "0" }
  costs:        { type: number, default: "0" }
  tax_rate:     { type: number, default: "5" }
  profit:       { type: number, default: "0" }

screens:
  - id: home
    title: Farm Buddy
    body:
      - text_label:
          text: "What would you like to do?"
          style: subheading

      - macro_grid:
          columns: 2
          buttons:
            - { label: "🍃 Diagnose Leaf",     action: "go:diagnose" }
            - { label: "💰 Profit Calculator",  action: "go:profit" }
            - { label: "🧪 Fertiliser Guide",   action: "ask:What fertiliser should I use for rice?" }
            - { label: "📍 Nearest Agro Shop",  action: "go:nearby" }

      - voice_input:
          bind: user_text
          hint: "Or ask anything..."

      - action_button:
          label: "Ask Farm Buddy"
          action: "ask:{{user_text}}"

      - markdown_output:
          source: ai_response
          empty_text: "Responses will appear here..."

  - id: diagnose
    title: Leaf Diagnosis
    body:
      - text_label:
          text: "Take a photo of the affected leaf for AI diagnosis."
          style: body

      - camera_input:
          bind: photo_path
          label: "📷 Take Photo"
          preview: true

      - text_input:
          bind: user_text
          hint: "Describe symptoms (optional)"
          label: "Additional info"

      - action_button:
          label: "🔍 Diagnose"
          action: "vision_ask:Diagnose this crop leaf. {{user_text}}"
          style: primary

      - markdown_output:
          source: ai_response
          streaming: true

  - id: profit
    title: Profit Calculator
    body:
      - text_label:
          text: "Calculate your farming profit"
          style: subheading

      - text_input:
          bind: revenue
          hint: "e.g., 5000"
          label: "Total Revenue (RM)"
          input_type: number

      - text_input:
          bind: costs
          hint: "e.g., 2000"
          label: "Total Costs (RM)"
          input_type: number

      - slider:
          bind: tax_rate
          min: 0
          max: 30
          step: 1
          label: "Estimated Tax"
          left_label: "0%"
          right_label: "30%"

      - action_button:
          label: "💰 Calculate"
          action: "formula:profit"
          style: primary

      - metric_card:
          source: profit
          label: "Net Profit"
          prefix: "RM "
          format: decimal_2

      - action_button:
          label: "📊 Get AI advice on my profit"
          action: "ask:I made RM{{revenue}} with RM{{costs}} costs and {{tax_rate}}% tax. Net: RM{{profit}}. Advice?"
          style: secondary

  - id: nearby
    title: Nearest Agro Shop
    body:
      - text_label:
          text: "Find fertiliser and supply shops near you"
          style: subheading

      - action_button:
          label: "📍 Get My Location"
          action: "geolocate"
          style: primary

      - geo_display:
          data: agro_shops
          limit: 5
          show_distance: true
          empty_text: "Tap above to find nearby shops"

      - action_button:
          label: "Ask for directions"
          action: "ask:How do I get to the nearest agro shop?"
          style: secondary

formulas:
  profit:
    expression: "({{revenue}} - {{costs}}) * (1 - {{tax_rate}} / 100)"
    output: profit

data:
  agro_shops:
    type: points
    items:
      - { name: "Kedai Baja Pak Ali",  lat: 3.139, lng: 101.687, info: "Open 8am-5pm" }
      - { name: "AgriMart Seremban",   lat: 2.725, lng: 101.938, info: "Fertiliser specialist" }
      - { name: "Tani Supply Melaka",  lat: 2.189, lng: 102.250, info: "Seeds & tools" }
      - { name: "Koperasi Tani Johor", lat: 1.485, lng: 103.761, info: "Wholesale pricing" }

safety:
  blocked_keywords:
    - mix pesticide
    - poison
    - kill pest with fuel
    - dangerous spray
    - drink chemical
  escalation_message: "This may be dangerous. Please contact your local agriculture officer."
  disclaimer: "AI-generated guidance. Not a professional consultation."

permissions:
  - camera
  - location
```

---

## Design Principles

1. **One YAML = one app.** No external code, no compilation. Drop a file in `assets/miniapps/`, it appears in the Hub.

2. **String actions, not graphs.** Actions are prefix-dispatched strings (`ask:`, `go:`, `formula:`), not complex DAGs. Simple to author, simple to parse.

3. **`{{variable}}` interpolation everywhere.** Variables are referenced inline in action strings, text labels, and formula expressions. One syntax to learn.

4. **Widgets are dumb, actions are smart.** Widgets only render state and capture input. All logic flows through the ActionDispatcher.

5. **AI is just another action.** `ask:` is no different from `formula:` or `go:` — it's a string action that writes to a variable. The widget displaying the result doesn't know or care that it came from an LLM.

6. **Offline by default.** No network calls. Model runs on-device. Data is bundled. GPS works offline. Everything works in airplane mode.
