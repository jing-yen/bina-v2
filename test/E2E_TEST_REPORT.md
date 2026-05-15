# Comprehensive E2E App Test Report

**Date:** 2026-05-15
**Branch:** `feature/studio-arch-heatmap`
**Device:** Android emulator (1080x2400)
**Tester:** Automated via adb + uiautomator

---

## Target User Persona

**Rina**, 32, agricultural extension worker in rural Negeri Sembilan, Malaysia. Has a mid-range Android phone, intermittent connectivity. Needs tools to help smallholder farmers diagnose crop diseases, calculate profit, find nearby agro input shops, and get general farming advice. Not technical — expects things to "just work."

**Key expectations:** Simple navigation, clear labels in familiar language, works offline, fast AI responses, easy to share tools with other farmers.

---

## Test Results by Screen

### 1. Hub (Recipe Discovery)

| Aspect | Status | Notes |
|--------|--------|-------|
| Recipes load from Firestore | PASS | Farm Buddy, Buku Kira-Kira, Bidan Pintar visible |
| Category filtering | PASS | Agriculture section with Crop Disease, Smallholder tags |
| Recipe detail sheet | PASS | Opens on tap, shows description |
| Install flow | PASS | Farm Buddy installed successfully to My Pocket |
| Visual polish | PASS | Warm stone palette, Outfit font, clean cards |

### 2. My Pocket (Saved Recipes)

| Aspect | Status | Notes |
|--------|--------|-------|
| Shows installed recipes | PASS | Farm Buddy with icon, description, "Offline Ready" badge |
| Stats cards | PASS | 1 Saved, 5 Screens, Ready status |
| Delete button | PASS | Trash icon present on card |
| Open MiniApp | PASS | Tapping card launches MiniApp |
| Visual polish | PASS | Clean layout, proper spacing |

### 3. Offline Sync (P2P Sharing)

| Aspect | Status | Notes |
|--------|--------|-------|
| Scan to Receive | PASS | Card present with clear description |
| Share a Recipe | PASS | Card present, "Pick an installed recipe to share via QR" |
| BLE pairing | NOT TESTED | Requires two physical devices |

### 4. Analytics (Usage Tracking)

| Aspect | Status | Notes |
|--------|--------|-------|
| Total launches count | PASS | Incremented from 16 to 17 after MiniApp use |
| Recipes installed count | PASS | Shows 1 |
| Questions asked | PASS | Shows 1 |
| Active days / streak | PASS | 1 day / 1 day streak |
| Daily activity heatmap | PASS | Visible with colored dots |
| Time period filters | PASS | 7d, 30d, All tabs present |

### 5. Farm Buddy MiniApp — Home Screen

| Aspect | Status | Notes |
|--------|--------|-------|
| Grid buttons | PASS | Dark green background, white text, Outfit font |
| Emoji icons on buttons | PASS | Microscope, money bag, pin, chat bubble |
| Heading "What would you like to do?" | PASS | Larger 24sp heading |
| Bottom nav | PASS | Home icon expanded with label, emoji-only for others |
| Back button in nav | PASS | Arrow left of Home icon |
| Intro image area | ISSUE | Large empty green space where intro image should render (known stub) |
| Vertical whitespace | PASS | 10dp spacing between buttons, 14dp between widgets |

### 6. Farm Buddy — Leaf Diagnosis (Camera AI)

| Aspect | Status | Notes |
|--------|--------|-------|
| Camera button | PASS | Opens device camera on tap |
| Symptoms text input | PASS | Placeholder "Describe symptoms (optional)" |
| Diagnose button | PASS | Dark green, "Diagnose" label |
| AI response area | PASS | Empty area ready for response |
| Reset button | PASS | Refresh icon in top-right header |
| Back arrow | PASS | Top-left navigates to home |
| Context isolation | IMPLEMENTED | `screenResponses` map stores per-screen AI state |
| AI inference | NOT TESTED | Requires LLM model pushed to device |

### 7. Farm Buddy — Profit Calculator

| Aspect | Status | Notes |
|--------|--------|-------|
| Revenue input | PASS | "Total Revenue (RM)" with 0 default |
| Costs input | PASS | "Total Costs (RM)" with 0 default |
| Tax slider | PASS | "Estimated Tax" 0-30% range, currently at 5% |
| Calculate button | PASS | Dark green, triggers formula |
| Net Profit display | PASS | "RM 0.00 / Net Profit" shown |
| "Get AI advice" button | PASS | Secondary action available |
| Bottom nav tab expanded | PASS | "Profit Calculator" label shown when active |

### 8. Farm Buddy — Nearest Agro Shop

| Aspect | Status | Notes |
|--------|--------|-------|
| Shows all shops before location | PASS | 4 shops listed without distance |
| "Get My Location" button | PASS | Dark green, ready to sort by distance |
| Shop cards | PASS | Kedai Baja Pak Ali, AgriMart Seremban, Tani Supply Melaka, Koperasi Tani Johor |
| Shop details | PASS | Name + description (hours, specialty, etc.) |
| Distance hidden when no location | PASS | No distance shown until "Get My Location" pressed |
| Static data | NOTE | Coordinates are hardcoded in YAML, no live API |

### 9. Farm Buddy — Ask Farm Buddy (General Q&A)

| Aspect | Status | Notes |
|--------|--------|-------|
| Heading | PASS | "Ask anything about farming" (24sp) |
| Text input | PASS | Placeholder "Ask about crops, pests, soil..." |
| Ask button | PASS | Dark green |
| Response placeholder | PASS | "Responses will appear here..." |
| Markdown rendering | IMPLEMENTED | `SimpleMarkdown` composable for bold/italic/lists/code |
| Auto-scroll | IMPLEMENTED | `LaunchedEffect(aiResponse)` scrolls to bottom |

---

## Cross-Cutting Features

| Feature | Status | Notes |
|---------|--------|-------|
| Outfit font globally | PASS | All text uses Outfit family via `BinaTypography` |
| Dark green buttons + white text | PASS | Matches web Studio preview |
| Per-screen context isolation | PASS | `screenResponses` map in `MiniAppScreen.kt` |
| Reset button on non-home screens | PASS | Clears `ai_response`, `photo_path`, `screenResponses` |
| Bottom nav emoji-only tabs | PASS | Expand with label when selected |
| Back button in bottom nav | PASS | Left of Home icon |
| System prompt conciseness | PASS | "Be concise. Give a clear diagnosis..." prepended |
| Knowledge embedding | PASS | `alwaysLoaded` from Gemini summary prepended to system prompt |
| Analytics tracking | PASS | Launches increment, events logged |
| Edge-to-edge display | PASS | Content extends to edges with proper padding |

---

## Known Issues & Gaps

### Critical

1. **Intro image not rendering** — Large empty green area on every MiniApp screen. Studio uploads images but drops them during YAML generation. Android has no renderer for intro images. This is the most visually jarring issue for end users.

2. **AI inference untestable without model** — Requires `.litertlm` model pushed to `/data/local/tmp/`. Markdown rendering, auto-scroll, and concise responses are implemented in code but cannot be verified on-device without the model.

### High

3. **Web Studio has 18 reported issues** (4 critical) — Firebase save may be broken, Gemini 500 errors on generation, emoji rendering issues, preview nav includes "Intro". Not yet fixed.

4. **Back button in bottom nav exits MiniApp entirely** — The `onBack()` callback exits to My Pocket rather than navigating to the previous screen within the MiniApp. User may expect intra-app back navigation.

### Medium

5. **Translation is metadata-only** — Language selection in Studio is cosmetic. No actual translation occurs at upload or download time. The `labels` map is unused at runtime.

6. **Knowledge embedding is lossy** — Only a 2-3 sentence Gemini summary from the first 3 chunks of a document is stored. Full documents are discarded after summarization. No RAG pipeline. May miss critical details from longer documents.

7. **Location data is static** — Nearby places use hardcoded coordinates baked into the YAML at recipe creation time. No dynamic API or real-time data source.

### Low

8. **BLE P2P sharing untested** — Requires two physical devices. The Scan to Receive and Share a Recipe UI is present and functional, but the actual BLE transfer was not verified.

---

## Persona Verdict

### What Rina would love
- Grid layout is clear, familiar emoji icons make features easy to find
- Profit calculator is immediately useful with no setup
- "Offline Ready" badge is reassuring for field work
- QR sharing is perfect for her village visits
- Analytics help her report impact metrics to her NGO

### What Rina would struggle with
- The big empty green space on every screen looks broken ("is it loading?")
- Can't get AI responses without the on-device model — the core value prop is blocked
- Nearby shops are helpful but static; she'd want real local shops
- No way to share diagnosis results or screenshots with farmers

### What Rina would expect more
- Actual translation into Bahasa Melayu for her farmers
- Photo gallery support (not just live camera) for leaf diagnosis
- History of past diagnoses and calculations
- Richer knowledge base that preserves full document content
