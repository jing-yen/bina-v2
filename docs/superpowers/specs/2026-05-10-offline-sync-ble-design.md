# Offline Sync v2 — BLE Discovery Transport Design Spec

**Date:** 2026-05-10
**Branch target:** `feature/offline-sync-ble` (off `main`, after PR #5 merged)
**Owner:** Ing
**Builds on:** `docs/superpowers/specs/2026-05-10-offline-sync-design.md`

## Summary

Replace the direct-QR recipe payload with a **two-phase transport**: QR carries a small pairing offer (service UUID + recipe metadata + size), receiver scans it, then a Bluetooth Low Energy connection transfers the actual recipe YAML. This eliminates the QR size ceiling (currently ~5 KB raw, was hit by Farm Buddy at 7.1 KB) and unlocks recipes of arbitrary size — including those with embedded knowledge files. Existing `RecipePayload` (`BINA2:` direct format) is kept for paste-YAML fallback only.

## Goals

- Eliminate the per-recipe size limit on QR-based sharing.
- Keep the demo's "scan and go" flow — receiver scans one QR, recipe arrives in seconds.
- Reuse the existing post-decode pipeline (`RecipeDetailSheet` → Configurator → install) — don't fork it.
- Pair without bonding: random per-share service UUID is the access control. No system pairing dialog.
- Just-in-time runtime permissions: don't prompt on tab open, only on first Scan/Share tap.
- Graceful failure: BLE timeout / unsupported / permission-denied always degrades to the existing paste-YAML fallback.

## Non-goals

- Bonded BLE pairing (system PIN dialogs, persistent bonds) — intentionally avoided to dodge OEM-specific UX.
- Encryption above the BLE link layer — recipe content isn't sensitive; the per-share random UUID is the privacy mechanism.
- Multi-recipient broadcast (one sender → many receivers in parallel). Single 1:1 transfer per session.
- Resumable transfers — if the connection drops mid-transfer, restart from scratch.
- ACK protocol on top of BLE notifications — L2CAP-level reliability is sufficient for our payload sizes; receiver detects truncation via the size-from-QR.
- Wi-Fi Direct, NFC, or any third transport. Only QR + BLE in this spec; paste-YAML stays as a fallback.
- Removing the `BINA2:` direct-QR format. It stays for paste-YAML use (Studio→phone path, manual share via clipboard, BLE-failure fallback).

## User experience overview

Five surfaces in this spec:

1. **OfflineSyncScreen** — unchanged (two cards: Scan to Receive, Share a Recipe).
2. **ShareQrScreen** — unchanged in layout, but now the QR carries pairing info (BLE starts advertising in the background as soon as this screen is entered).
3. **ScanQrScreen** — unchanged camera UI, but on successful decode, routes to a new `ReceivePairingSheet` instead of straight to `RecipeDetailSheet`.
4. **ReceivePairingSheet (NEW)** — modal bottom sheet showing decoded recipe metadata (name, author, size) + Connect/Cancel buttons. After Connect: replaces with progress UI. After transfer success: dismisses, opens existing `RecipeDetailSheet` with the parsed `MiniApp`.
5. **PasteYamlSheet** — unchanged. Still the fallback when BLE isn't available.

```
Sender:
  Sync → Share → pick recipe → ShareQrScreen
                                  │
                                  │ (start BLE peripheral, advertise random UUID)
                                  │ (display QR with BINA-BT:<uuid>:<id>:<size>:<b64(name|author)>)
                                  │
                                  └── on receiver connect: send YAML chunks → close

Receiver:
  Sync → Scan → ScanQrScreen ──camera detects QR──► ReceivePairingSheet
                                                           │
                                                           │ "Connect"
                                                           ▼
                                                     (BLE central scan for UUID,
                                                      connect, subscribe, buffer)
                                                           │
                                                           │ size bytes received
                                                           ▼
                                                     (decode YAML, parse MiniApp)
                                                           │
                                                           ▼
                                                     RecipeDetailSheet (existing)
                                                           │
                                                           │ Configure & Install
                                                           ▼
                                                     ConfiguratorScreen → install
```

## Architecture

### QR pairing payload format

```
BINA-BT:<service-uuid-hex>:<recipe-id>:<size-bytes>:<b64(name|author)>
```

- **`BINA-BT:`** — magic header. Decoder rejects anything else (also rejects old `BINA2:` direct-format on the BLE-Scan path; that goes through PasteYamlSheet only).
- **`<service-uuid-hex>`** — 32 hex chars, randomly generated per-share (UUIDv4 stripped of hyphens). Used both as the GATT service UUID and as the BLE advertising service-data identifier.
- **`<recipe-id>`** — for collision pre-check before transfer.
- **`<size-bytes>`** — total YAML size for progress UI + EOF detection.
- **`<b64(name|author)>`** — pipe-delimited name and author, URL-safe base64-encoded so display names with spaces/lowercase don't break field delimiting.

Sample payload (~110-130 bytes typical), well under any QR density concern (v8-v10 byte mode).

### BLE service definition

- **Service UUID:** the random per-share UUID from above.
- **Single characteristic UUID:** fixed value `00001234-0000-1000-8000-00805F9B34FB` (used by all Bina sessions; the per-session uniqueness comes from the service UUID).
- **Properties:** `NOTIFY` only. No write, no read.
- **Permissions:** none (no encryption, no authentication required).

### Transfer protocol

1. **Sender side** (`BleSender`):
   1. Generate random `UUID` for this session.
   2. Construct GATT server with one service / one characteristic (as above).
   3. Start BLE advertising with `serviceUuids = [sessionUuid]`. Use connectable advertising. TX power = high.
   4. On `onConnectionStateChange(STATE_CONNECTED)`: request MTU 247 via `BluetoothGattServer.gatt.requestMtu(247)` — fall back to default 23 if rejected.
   5. On characteristic subscribe (CCCD write): begin sending the recipe YAML in chunks. Chunk size = `mtu - 3` (default 244 bytes after BLE header).
   6. After last chunk sent, wait for `onConnectionStateChange(STATE_DISCONNECTED)` from receiver, then stop advertising and close GATT server.
   7. **Timeout:** keep advertising until user taps Done in UI, no automatic timeout.

2. **Receiver side** (`BleReceiver`):
   1. Parse pairing payload from QR → extract session UUID, recipe id, size, name, author.
   2. Show ReceivePairingSheet with metadata. On Connect tap:
   3. Start `BluetoothLeScanner.startScan()` filtered by service UUID. **Timeout: 10 seconds.**
   4. On first matching `ScanResult`: stop scanning, call `device.connectGatt(...)`.
   5. On `onMtuChanged(MTU)`: store MTU for EOF math (informational; not required).
   6. On `onServicesDiscovered(GATT_SUCCESS)`: subscribe to the characteristic by writing to its CCCD descriptor.
   7. On each `onCharacteristicChanged(...)`: append bytes to buffer. Check if `buffer.size >= expectedSize` → done.
   8. On done: gunzip-or-not (the YAML is sent uncompressed since BLE is fast and chunking is the bottleneck), parse via `RecipeImporter`, transition to `IncomingState.Ready` for the existing pipeline.
   9. **Failure modes:** scan timeout → "Couldn't find sender. Move closer." + Retry button. Connection drop mid-transfer → "Transfer interrupted." + Retry. Parse failure → "Recipe file is corrupted: <reason>".

### Permissions

API-version-aware permission set, requested just-in-time:

- **Android 12+ (API 31+):** `BLUETOOTH_CONNECT` (peripheral or central operations), `BLUETOOTH_SCAN` (receiver only), `BLUETOOTH_ADVERTISE` (sender only).
- **Android 6-11 (API 23-30):** `BLUETOOTH` + `BLUETOOTH_ADMIN` (install-time, no runtime prompt) + `ACCESS_FINE_LOCATION` (runtime; OS quirk — BLE scan requires it on these versions).
- **Pre-API 23:** all permissions install-time. We don't actually support these old versions but the code path falls through cleanly.

UI flow on first BLE-using action (Scan or Share button tap):
1. Check current permission state.
2. If granted → proceed.
3. If not → request via Activity Result API (multi-permission contract for API 31+).
4. If denied → display "Bluetooth permission required" with **Open Settings** + **Paste YAML instead** options. Never silently fail.

### State management

`SyncViewModel` gains a new `TransferState`:

```kotlin
sealed interface TransferState {
    data object Idle : TransferState
    data object Advertising : TransferState                  // sender: waiting for receiver
    data class Receiving(val pct: Int) : TransferState       // receiver: 0..100
    data class Sending(val pct: Int) : TransferState         // sender: 0..100
    data class Failed(val message: String) : TransferState
}
```

`incoming: StateFlow<IncomingState>` (existing) keeps its semantics — receiver still drives `Ready` once the recipe YAML is buffered and parsed. The new `TransferState` is for in-flight UI only.

### File map

```
app/src/main/java/com/bina/ai/sync/
├── BlePairingPayload.kt          NEW — encode/decode "BINA-BT:..." format
├── BleSender.kt                  NEW — peripheral: GattServer + advertiser + chunked notify
├── BleReceiver.kt                NEW — central: scan + connect + subscribe + buffer
└── BlePermissions.kt             NEW — version-aware permission helpers + Activity Result hooks

app/src/main/java/com/bina/ai/ui/screens/sync/
├── SyncViewModel.kt              MODIFY — add TransferState + start/stop sender + handlePairing()
└── components/
    ├── ShareQrScreen.kt          MODIFY — start BLE sender on enter, stop on dispose, render BINA-BT QR
    ├── ScanQrScreen.kt           MODIFY — on QR decode, branch by magic (BINA-BT → ReceivePairingSheet,
    │                                      BINA2 → existing direct flow for paste path only)
    └── ReceivePairingSheet.kt    NEW — confirmation + progress UI

app/src/test/java/com/bina/ai/sync/
└── BlePairingPayloadTest.kt      NEW — encode/decode round-trip + edge cases (no BLE hardware needed)

AndroidManifest.xml               MODIFY — add BLUETOOTH_CONNECT/SCAN/ADVERTISE + ACCESS_FINE_LOCATION

app/build.gradle.kts              UNCHANGED — Android BLE APIs are part of the platform, no new deps
```

### Compression

Sender does NOT gzip the YAML before transfer. BLE notifications already negotiate optimized packet flow; the receiver's buffer-and-parse step is fast; gzip would add CPU on both sides for marginal byte savings on already-small recipes. Net: simpler protocol, same end-to-end latency.

(For very large recipes — knowledge-file embedded — gzip on top of BLE would help. Out of scope for v2.)

## Error / edge cases

| Case | Behavior |
|---|---|
| Wrong magic header in QR | "Not a Bina pairing QR" — stay on scan screen |
| Malformed pairing payload (bad UUID, missing fields) | "Pairing data is corrupted" — stay on scan screen |
| User taps Cancel on ReceivePairingSheet | Dismiss sheet, return to ScanQrScreen with camera resumed |
| BLE scan timeout (no advertiser found in 10s) | "Couldn't find sender. Move closer or try again." + Retry button |
| BLE connection fails | "Couldn't connect to sender." + Retry button |
| BLE connection drops mid-transfer | "Transfer interrupted." + Retry button |
| Bytes received != size from QR | "Transfer incomplete." + Retry button |
| YAML parse fails after transfer | "Recipe file is corrupted: <reason>" — fall through to existing error UI |
| Recipe id collision (bundled or imported) | Existing precheck → existing dialogs (BundledConflict / UpdateExisting) |
| Bluetooth turned off | Standard `Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)` prompt |
| BLE permission denied | "Bluetooth permission required" + Open Settings + Paste YAML instead |
| Sender phone doesn't support BLE peripheral mode (rare on modern phones) | Sender shows "This device can't share. Show this QR to a paired Bina user OR send the YAML via paste/USB instead." with "Copy YAML to clipboard" button |
| Receiver leaves ScanQrScreen mid-transfer | Disconnect, stop scanning, free GATT resources |
| Sender leaves ShareQrScreen mid-transfer | Stop advertising, close GATT server. Receiver gets connection-drop error. |

## Testing

**Unit tests** (no hardware required):

- `BlePairingPayloadTest` — round-trip encode/decode, magic-header rejection, malformed payloads, name/author with special chars.

**Manual smoke test** (requires two devices — emulator pair OR emulator + physical phone):

1. Sender (emulator A) — Sync → Share → pick Bidan Pintar → BLE pairing QR appears.
2. Receiver (physical phone) — Sync → Scan → camera scans QR → ReceivePairingSheet shows "Receive Bidan Pintar by ... (3 KB)?" → Connect → progress bar fills → RecipeDetailSheet → Configure & Install → Hub shows it as installed.
3. Repeat with Farm Buddy (7 KB) — same flow, longer transfer (~1-2s vs ~300ms).
4. Repeat with a future ~30 KB recipe (we'll fabricate one for this test) — prove the size ceiling is gone.
5. Permission denial flow — first-launch deny BT permission → see fallback UI → tap "Paste YAML instead" → confirm paste path still works.
6. Failure cases — start scan, then sender quits before receiver connects → see "Couldn't find sender" timeout error.

**What's not tested:** BLE permission flow on Android <12 with location-quirk (we don't have a test device that old). If JY or anyone tests on an older device, surface issues for follow-up.

## Coordination notes

- **No JY territory touched.** All changes in `sync/` (Ing's territory) plus the existing OfflineSync surfaces. MyPocket and Studio are untouched.
- **No schema changes** to `MiniApp.kt`. The transfer is byte-identical YAML.
- **Direct-QR (`BINA2:`) format kept** in the codebase for the paste-YAML fallback path. Anyone copying a `BINA2:` from clipboard or scanning a leftover BINA2 QR (printed or saved) can still paste it through PasteYamlSheet.

## Future v3 (out of scope)

- Resumable transfers with chunk-level acknowledgements (for unreliable links).
- Multi-recipient broadcast.
- Recipe signing for trust-on-first-use validation.
- Wi-Fi Direct as a fallback when BLE is unsupported (some phones lack peripheral mode).
- Background advertising / receiving (right now both screens must be in the foreground).
