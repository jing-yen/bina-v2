# Offline Sync v2 (BLE Discovery) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the direct-QR recipe payload with a two-phase QR + BLE transport. QR encodes a pairing offer (service UUID + metadata + size); BLE peripheral on the sender / central on the receiver streams the recipe YAML over a NOTIFY characteristic. Eliminates the QR size ceiling, unlocks recipes of any size.

**Architecture:** Sender starts a GATT server with a single notify characteristic, advertises a random per-share service UUID, and pushes the YAML in MTU-sized chunks once the receiver subscribes. Receiver scans QR → confirmation sheet → BLE central scan → connect → buffer chunks until size matches → hand off to existing decode/preview/install pipeline. No bonding, no gzip; just-in-time runtime permissions; paste-YAML stays as a fallback for BLE-disabled or unsupported scenarios.

**Tech Stack:** Kotlin 2.x, Compose Material 3, Android `BluetoothManager` / `BluetoothLeAdvertiser` / `BluetoothLeScanner` / `BluetoothGattServer` / `BluetoothGatt` (platform APIs — no new deps), Activity Result API for runtime permissions, JUnit 4 (existing).

**Spec:** `docs/superpowers/specs/2026-05-10-offline-sync-ble-design.md`

---

## Phase 1 — Foundation

### Task 1: Add BLE permissions to AndroidManifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add the permission set**

In `app/src/main/AndroidManifest.xml`, just inside the `<manifest>` element above the existing `<uses-permission>` lines, add:

```xml
    <!-- BLE for offline sync v2 — sender peripheral + receiver central -->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

    <!-- Pre-Android 12 fallback permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />

    <uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
```

`neverForLocation` on `BLUETOOTH_SCAN` tells the OS our scan results aren't being used to derive location — exempts us from location prompts on API 31+. The `<uses-feature ... required="false">` lets users without BLE install but get a graceful unsupported message.

- [ ] **Step 2: Build verification**

Run: `./gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "Add BLE permissions for offline sync v2"
```

---

## Phase 2 — Pairing payload (pure JVM, TDD)

### Task 2: `BlePairingPayload` — encode/decode `BINA-BT:` format

**Files:**
- Create: `app/src/main/java/com/bina/ai/sync/BlePairingPayload.kt`
- Test: `app/src/test/java/com/bina/ai/sync/BlePairingPayloadTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/bina/ai/sync/BlePairingPayloadTest.kt`:

```kotlin
package com.bina.ai.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BlePairingPayloadTest {

    private val sampleUuid = UUID.fromString("0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9")

    @Test fun `round trip preserves all fields`() {
        val offer = BlePairingPayload.Offer(
            serviceUuid = sampleUuid,
            recipeId = "farm_buddy",
            sizeBytes = 7116,
            recipeName = "Farm Buddy",
            authorName = "Universiti Putra Malaysia"
        )
        val encoded = BlePairingPayload.encode(offer)
        assertTrue("starts with BINA-BT:", encoded.startsWith("BINA-BT:"))
        val decoded = BlePairingPayload.decode(encoded).getOrThrow()
        assertEquals(offer, decoded)
    }

    @Test fun `decode rejects wrong magic header`() {
        val result = BlePairingPayload.decode("BINA2:abc")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects payload with missing fields`() {
        val result = BlePairingPayload.decode("BINA-BT:0a1b2c3d4e5f60718293a4b5c6d7e8f9:farm_buddy")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects malformed uuid`() {
        val result = BlePairingPayload.decode("BINA-BT:not-a-uuid:farm_buddy:1234:Rm9v")
        assertTrue(result.isFailure)
    }

    @Test fun `name and author with spaces and lowercase round trip cleanly`() {
        val offer = BlePairingPayload.Offer(
            serviceUuid = sampleUuid,
            recipeId = "weird_id",
            sizeBytes = 100,
            recipeName = "Mixed-Case Name with spaces",
            authorName = "lowercase author"
        )
        val decoded = BlePairingPayload.decode(BlePairingPayload.encode(offer)).getOrThrow()
        assertEquals(offer.recipeName, decoded.recipeName)
        assertEquals(offer.authorName, decoded.authorName)
    }

    @Test fun `encoded payload fits comfortably in a v10 byte mode QR`() {
        val offer = BlePairingPayload.Offer(
            serviceUuid = sampleUuid,
            recipeId = "long_recipe_id_for_testing",
            sizeBytes = 99999,
            recipeName = "A reasonably long recipe name",
            authorName = "An organization with a long name"
        )
        val encoded = BlePairingPayload.encode(offer)
        // QR v10 byte mode capacity is 271 bytes at level L. Pairing payloads
        // should sit well under that for any plausible name/author.
        assertTrue("encoded length ${encoded.length} should be under 200", encoded.length < 200)
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.sync.BlePairingPayloadTest"`
Expected: FAIL with "unresolved reference: BlePairingPayload".

- [ ] **Step 3: Implement `BlePairingPayload`**

Create `app/src/main/java/com/bina/ai/sync/BlePairingPayload.kt`:

```kotlin
package com.bina.ai.sync

import java.util.Base64
import java.util.UUID

/**
 * Wire format for the QR code that bootstraps a BLE recipe transfer.
 *
 * `BINA-BT:<uuid-hex>:<recipe-id>:<size>:<urlsafe-base64(name|author)>`
 *
 * The receiver decodes this, shows a confirmation sheet, then opens a BLE
 * connection to the advertised service UUID to fetch the actual recipe YAML.
 */
object BlePairingPayload {
    private const val MAGIC = "BINA-BT:"
    private const val FIELD_SEP = ":"
    private const val META_SEP = "|"

    data class Offer(
        val serviceUuid: UUID,
        val recipeId: String,
        val sizeBytes: Long,
        val recipeName: String,
        val authorName: String
    )

    fun encode(offer: Offer): String {
        val uuidHex = offer.serviceUuid.toString().replace("-", "")
        val nameAuthor = "${offer.recipeName}$META_SEP${offer.authorName}"
        val nameAuthorB64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(nameAuthor.toByteArray(Charsets.UTF_8))
        return buildString {
            append(MAGIC)
            append(uuidHex); append(FIELD_SEP)
            append(offer.recipeId); append(FIELD_SEP)
            append(offer.sizeBytes); append(FIELD_SEP)
            append(nameAuthorB64)
        }
    }

    fun decode(raw: String): Result<Offer> = runCatching {
        require(raw.startsWith(MAGIC)) { "Not a Bina pairing QR" }
        val body = raw.substring(MAGIC.length)
        val parts = body.split(FIELD_SEP, limit = 4)
        require(parts.size == 4) { "Pairing payload has wrong number of fields" }
        val (uuidHex, recipeId, sizeStr, nameAuthorB64) = parts

        val uuid = uuidFromHex(uuidHex)
        val sizeBytes = sizeStr.toLongOrNull() ?: error("Pairing payload size is not a number")
        val nameAuthor = String(
            Base64.getUrlDecoder().decode(nameAuthorB64),
            Charsets.UTF_8
        )
        val (name, author) = nameAuthor.split(META_SEP, limit = 2).let {
            require(it.size == 2) { "Pairing payload name/author missing delimiter" }
            it[0] to it[1]
        }
        Offer(
            serviceUuid = uuid,
            recipeId = recipeId,
            sizeBytes = sizeBytes,
            recipeName = name,
            authorName = author
        )
    }

    private fun uuidFromHex(hex: String): UUID {
        require(hex.length == 32) { "Service UUID must be 32 hex chars (got ${hex.length})" }
        val withDashes = buildString(36) {
            append(hex, 0, 8); append('-')
            append(hex, 8, 12); append('-')
            append(hex, 12, 16); append('-')
            append(hex, 16, 20); append('-')
            append(hex, 20, 32)
        }
        return UUID.fromString(withDashes)
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.bina.ai.sync.BlePairingPayloadTest"`
Expected: 5/5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bina/ai/sync/BlePairingPayload.kt app/src/test/java/com/bina/ai/sync/BlePairingPayloadTest.kt
git commit -m "Add BlePairingPayload for QR-bootstrapped BLE transfer"
```

---

## Phase 3 — BLE permissions helper

### Task 3: `BlePermissions` — version-aware runtime permission checks

**Files:**
- Create: `app/src/main/java/com/bina/ai/sync/BlePermissions.kt`

This file is non-trivial coordination code but doesn't lend itself to unit tests (it queries `Context.checkSelfPermission` which requires a real Context). Tested manually via the smoke test.

- [ ] **Step 1: Implement `BlePermissions`**

Create `app/src/main/java/com/bina/ai/sync/BlePermissions.kt`:

```kotlin
package com.bina.ai.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * BLE permission set differs between Android 12+ and earlier. This helper
 * centralizes the version branching so callers don't have to know.
 */
object BlePermissions {

    /** Permissions required for the sender (advertise + connect). */
    val SENDER_PERMISSIONS: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        // Pre-API 31, BLUETOOTH and BLUETOOTH_ADMIN are install-time so we
        // don't need to runtime-prompt for them. The sender doesn't strictly
        // need location, but advertise can fail without it on some OEMs.
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /** Permissions required for the receiver (scan + connect). */
    val RECEIVER_PERMISSIONS: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        // BLE scan on pre-API-31 requires location even if you don't care
        // about location data — well-known Android quirk.
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun hasSenderPermissions(context: Context): Boolean =
        SENDER_PERMISSIONS.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }

    fun hasReceiverPermissions(context: Context): Boolean =
        RECEIVER_PERMISSIONS.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/sync/BlePermissions.kt
git commit -m "Add BlePermissions helper with API-version-aware permission sets"
```

---

## Phase 4 — BLE Sender

### Task 4: `BleSender` — peripheral mode with chunked notify

**Files:**
- Create: `app/src/main/java/com/bina/ai/sync/BleSender.kt`

- [ ] **Step 1: Implement `BleSender`**

Create `app/src/main/java/com/bina/ai/sync/BleSender.kt`:

```kotlin
package com.bina.ai.sync

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.bina.ai.platform.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** UUID of the (single) characteristic we expose. Same for every Bina session. */
private val CHARACTERISTIC_UUID: UUID = UUID.fromString("00001234-0000-1000-8000-00805F9B34FB")
private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private const val DEFAULT_CHUNK_SIZE = 20  // safe default before MTU negotiation
private const val TAG = "BleSender"

sealed interface SenderState {
    data object Idle : SenderState
    data object Advertising : SenderState
    data class Sending(val pct: Int) : SenderState
    data object Done : SenderState
    data class Failed(val message: String) : SenderState
}

/**
 * One-shot BLE peripheral that advertises a random service UUID, accepts a
 * single connection, and pushes a YAML payload over a NOTIFY characteristic.
 *
 * Caller must hold BLUETOOTH_CONNECT + BLUETOOTH_ADVERTISE on API 31+.
 * Use [start] to begin advertising; [stop] to clean up.
 */
@SuppressLint("MissingPermission")
class BleSender(
    private val context: Context,
    val serviceUuid: UUID,
    private val payloadBytes: ByteArray
) {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val advertiser: BluetoothLeAdvertiser? =
        bluetoothManager.adapter?.bluetoothLeAdvertiser

    private var gattServer: BluetoothGattServer? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var connectedDevice: BluetoothDevice? = null
    private var chunkSize = DEFAULT_CHUNK_SIZE

    private val _state = MutableStateFlow<SenderState>(SenderState.Idle)
    val state: StateFlow<SenderState> = _state.asStateFlow()

    fun start(): Boolean {
        if (advertiser == null) {
            _state.value = SenderState.Failed("This device can't advertise over BLE")
            return false
        }
        gattServer = bluetoothManager.openGattServer(context, gattCallback) ?: run {
            _state.value = SenderState.Failed("Couldn't open GATT server")
            return false
        }

        characteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).apply {
            addDescriptor(BluetoothGattDescriptor(
                CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            ))
        }
        val service = BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            .also { it.addCharacteristic(characteristic) }
        gattServer?.addService(service)

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(serviceUuid))
            .build()

        advertiser.startAdvertising(settings, data, advertiseCallback)
        _state.value = SenderState.Advertising
        return true
    }

    fun stop() {
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        runCatching { gattServer?.close() }
        gattServer = null
        characteristic = null
        connectedDevice = null
        _state.value = SenderState.Idle
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Logger.w(TAG, "advertise start failed: $errorCode")
            _state.value = SenderState.Failed("Couldn't start BLE advertising (code $errorCode)")
        }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                Logger.d(TAG, "receiver connected: ${device.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (_state.value is SenderState.Sending) {
                    _state.value = SenderState.Failed("Receiver disconnected mid-transfer")
                } else if (_state.value !is SenderState.Done) {
                    // Disconnected before any transfer attempted; harmless.
                }
                connectedDevice = null
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            // CCCD subscribe — receiver is ready to receive notifications.
            if (descriptor.uuid == CCCD_UUID) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
                }
                if (value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    sendPayload(device)
                }
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            // Negotiated MTU - 3 (BLE header) is the max chunk size we can notify.
            chunkSize = (mtu - 3).coerceAtLeast(20)
            Logger.d(TAG, "MTU negotiated: $mtu, chunk size: $chunkSize")
        }
    }

    private fun sendPayload(device: BluetoothDevice) {
        val total = payloadBytes.size
        var sent = 0
        try {
            while (sent < total) {
                val end = (sent + chunkSize).coerceAtMost(total)
                val chunk = payloadBytes.copyOfRange(sent, end)
                characteristic?.value = chunk
                val ok = gattServer?.notifyCharacteristicChanged(device, characteristic!!, false) ?: false
                if (!ok) {
                    _state.value = SenderState.Failed("notify failed at byte $sent")
                    return
                }
                sent = end
                _state.value = SenderState.Sending(((sent.toLong() * 100L) / total).toInt())
                // Small pause to avoid flooding the receiver; BLE stack queues are shallow.
                Thread.sleep(15)
            }
            _state.value = SenderState.Done
        } catch (e: Exception) {
            Logger.e(TAG, "send failed", e)
            _state.value = SenderState.Failed(e.message ?: "send failed")
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/sync/BleSender.kt
git commit -m "Add BleSender: GATT server + advertiser + chunked notify"
```

---

## Phase 5 — BLE Receiver

### Task 5: `BleReceiver` — central mode with subscription buffering

**Files:**
- Create: `app/src/main/java/com/bina/ai/sync/BleReceiver.kt`

- [ ] **Step 1: Implement `BleReceiver`**

Create `app/src/main/java/com/bina/ai/sync/BleReceiver.kt`:

```kotlin
package com.bina.ai.sync

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.bina.ai.platform.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

private val CHARACTERISTIC_UUID: UUID = UUID.fromString("00001234-0000-1000-8000-00805F9B34FB")
private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private const val SCAN_TIMEOUT_MS = 10_000L
private const val TAG = "BleReceiver"

sealed interface ReceiverState {
    data object Idle : ReceiverState
    data object Scanning : ReceiverState
    data object Connecting : ReceiverState
    data class Receiving(val pct: Int) : ReceiverState
    data class Done(val payload: ByteArray) : ReceiverState {
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }
    data class Failed(val message: String) : ReceiverState
}

/**
 * One-shot BLE central that scans for a specific service UUID, connects,
 * subscribes to the characteristic, and buffers chunks until [expectedSize]
 * bytes have arrived.
 *
 * Caller must hold BLUETOOTH_CONNECT + BLUETOOTH_SCAN on API 31+.
 */
@SuppressLint("MissingPermission")
class BleReceiver(
    private val context: Context,
    private val serviceUuid: UUID,
    private val expectedSize: Long
) {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val scanner = bluetoothManager.adapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private val buffer = mutableListOf<Byte>()
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<ReceiverState>(ReceiverState.Idle)
    val state: StateFlow<ReceiverState> = _state.asStateFlow()

    fun start(): Boolean {
        if (scanner == null) {
            _state.value = ReceiverState.Failed("This device can't scan for BLE")
            return false
        }
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        _state.value = ReceiverState.Scanning
        handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS)
        return true
    }

    fun stop() {
        handler.removeCallbacks(scanTimeoutRunnable)
        runCatching { scanner?.stopScan(scanCallback) }
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
        if (_state.value !is ReceiverState.Done) _state.value = ReceiverState.Idle
    }

    private val scanTimeoutRunnable = Runnable {
        if (_state.value is ReceiverState.Scanning) {
            scanner?.stopScan(scanCallback)
            _state.value = ReceiverState.Failed("Couldn't find sender. Move closer or try again.")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            scanner?.stopScan(this)
            handler.removeCallbacks(scanTimeoutRunnable)
            connectTo(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = ReceiverState.Failed("BLE scan failed (code $errorCode)")
        }
    }

    private fun connectTo(device: BluetoothDevice) {
        _state.value = ReceiverState.Connecting
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                g.requestMtu(247)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (_state.value is ReceiverState.Receiving) {
                    _state.value = ReceiverState.Failed("Connection dropped mid-transfer")
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            g.discoverServices()
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(serviceUuid) ?: run {
                _state.value = ReceiverState.Failed("Sender's service UUID not found")
                return
            }
            val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID) ?: run {
                _state.value = ReceiverState.Failed("Sender's characteristic not found")
                return
            }
            g.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(descriptor)
            _state.value = ReceiverState.Receiving(0)
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            buffer.addAll(c.value.toList())
            if (buffer.size >= expectedSize) {
                _state.value = ReceiverState.Done(buffer.toByteArray())
                runCatching { g.disconnect() }
            } else {
                val pct = ((buffer.size.toLong() * 100L) / expectedSize).toInt()
                _state.value = ReceiverState.Receiving(pct)
            }
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/sync/BleReceiver.kt
git commit -m "Add BleReceiver: scan + connect + chunked subscribe + buffer"
```

---

## Phase 6 — ViewModel + UI integration

### Task 6: `SyncViewModel` — wire BLE into existing pipeline

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/screens/sync/SyncViewModel.kt`

- [ ] **Step 1: Update `SyncViewModel`**

In `SyncViewModel.kt`, after the existing `IncomingState` interface, add a new state for in-flight transfers:

```kotlin
sealed interface TransferState {
    data object Idle : TransferState
    data object Connecting : TransferState
    data class InProgress(val pct: Int) : TransferState
    data class Failed(val message: String) : TransferState
}
```

Add a state flow + a `pairing: StateFlow<BlePairingPayload.Offer?>` so the receive sheet can render metadata:

```kotlin
private val _pairing = MutableStateFlow<BlePairingPayload.Offer?>(null)
val pairing: StateFlow<BlePairingPayload.Offer?> = _pairing.asStateFlow()

private val _transfer = MutableStateFlow<TransferState>(TransferState.Idle)
val transfer: StateFlow<TransferState> = _transfer.asStateFlow()
```

Modify `handleScannedQr` to branch by magic header:

```kotlin
fun handleScannedQr(raw: String) {
    val trimmed = raw.trim()
    when {
        trimmed.startsWith("BINA-BT:") -> {
            val offer = BlePairingPayload.decode(trimmed).getOrElse {
                _incoming.value = IncomingState.Error(it.message ?: "Pairing data is corrupted")
                return
            }
            _pairing.value = offer
        }
        trimmed.startsWith("BINA2:") || trimmed.startsWith("BINA1:") -> {
            // Direct-encode QR (paste fallback). Decode then parse via existing path.
            _incoming.value = IncomingState.Decoding
            val yamlText = RecipePayload.decode(trimmed).getOrElse {
                _incoming.value = IncomingState.Error(it.message ?: "QR data is corrupted")
                return
            }
            decodeYaml(yamlText)
        }
        else -> {
            _incoming.value = IncomingState.Error("Not a Bina QR")
        }
    }
}

fun dismissPairing() { _pairing.value = null }

fun onTransferComplete(payloadBytes: ByteArray) {
    _transfer.value = TransferState.Idle
    _pairing.value = null
    decodeYaml(String(payloadBytes, Charsets.UTF_8))
}

fun onTransferProgress(pct: Int) {
    _transfer.value = TransferState.InProgress(pct)
}

fun onTransferFailed(message: String) {
    _transfer.value = TransferState.Failed(message)
}

fun onTransferConnecting() {
    _transfer.value = TransferState.Connecting
}
```

- [ ] **Step 2: Verify build + tests still pass**

Run: `./gradlew.bat :app:testDebugUnitTest`
Expected: All 21 unit tests pass (5 BlePairingPayload + 8 RecipePayload + 6 RecipeImporter + 6 SyncViewModel — minus any test changes from new code paths).

If `SyncViewModelTest` needs an update for the new `BINA-BT:` branch in `handleScannedQr`, add this test:

```kotlin
@Test fun `handleScannedQr with BINA-BT pairing payload sets pairing offer`() = runTest {
    val vm = newVm()
    val offer = BlePairingPayload.Offer(
        serviceUuid = java.util.UUID.randomUUID(),
        recipeId = "t1",
        sizeBytes = 100,
        recipeName = "Test",
        authorName = "Test Author"
    )
    vm.handleScannedQr(BlePairingPayload.encode(offer))
    val pairing = vm.pairing.value
    org.junit.Assert.assertNotNull(pairing)
    org.junit.Assert.assertEquals("t1", pairing!!.recipeId)
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/SyncViewModel.kt app/src/test/java/com/bina/ai/ui/screens/sync/SyncViewModelTest.kt
git commit -m "Wire BlePairingPayload into SyncViewModel with TransferState + pairing flow"
```

---

### Task 7: `ReceivePairingSheet` — confirmation + transfer progress

**Files:**
- Create: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ReceivePairingSheet.kt`

- [ ] **Step 1: Implement `ReceivePairingSheet`**

```kotlin
package com.bina.ai.ui.screens.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.sync.BlePairingPayload
import com.bina.ai.ui.screens.sync.TransferState
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivePairingSheet(
    offer: BlePairingPayload.Offer,
    transferState: TransferState,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Receive Recipe?", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BinaPrimary)
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF9FAFB))
                    .padding(14.dp)
            ) {
                Column {
                    Text(offer.recipeName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = BinaPrimary)
                    Spacer(Modifier.height(2.dp))
                    if (offer.authorName.isNotBlank()) {
                        Text("by ${offer.authorName}", fontSize = 12.sp, color = BinaGrayText)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${offer.sizeBytes / 1024} KB · id: ${offer.recipeId}", fontSize = 11.sp, color = BinaGrayText)
                }
            }
            Spacer(Modifier.height(16.dp))

            when (transferState) {
                is TransferState.Idle -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onCancel) { Text("Cancel") }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = onConnect,
                            colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                        ) { Text("Connect") }
                    }
                }
                is TransferState.Connecting -> {
                    Text("Connecting to sender…", fontSize = 13.sp, color = BinaGrayText)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is TransferState.InProgress -> {
                    Text("Receiving… ${transferState.pct}%", fontSize = 13.sp, color = BinaGrayText)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { transferState.pct / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is TransferState.Failed -> {
                    Text(transferState.message, fontSize = 13.sp, color = BinaRed)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onCancel) { Text("Cancel") }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                        ) { Text("Retry") }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/components/ReceivePairingSheet.kt
git commit -m "Add ReceivePairingSheet for BLE receive confirmation + progress UI"
```

---

### Task 8: `ScanQrScreen` — route BINA-BT QRs through ReceivePairingSheet + BleReceiver

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ScanQrScreen.kt`

- [ ] **Step 1: Add the receive-pairing wiring**

Inside `ScanQrScreen` Composable, after the existing `incoming by vm.incoming.collectAsStateWithLifecycle()`, add:

```kotlin
    val pairing by vm.pairing.collectAsStateWithLifecycle()
    val transferState by vm.transfer.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var receiver by remember { mutableStateOf<com.bina.ai.sync.BleReceiver?>(null) }

    fun startTransfer(offer: com.bina.ai.sync.BlePairingPayload.Offer) {
        if (!com.bina.ai.sync.BlePermissions.hasReceiverPermissions(context)) {
            vm.onTransferFailed("Bluetooth permission required")
            return
        }
        vm.onTransferConnecting()
        val r = com.bina.ai.sync.BleReceiver(context, offer.serviceUuid, offer.sizeBytes)
        receiver = r
        // Observe receiver state and forward to VM. Composition scope is fine for one-shot.
        kotlinx.coroutines.GlobalScope.launch {
            r.state.collect { s ->
                when (s) {
                    is com.bina.ai.sync.ReceiverState.Done -> {
                        vm.onTransferComplete(s.payload)
                        r.stop()
                        receiver = null
                    }
                    is com.bina.ai.sync.ReceiverState.Failed -> {
                        vm.onTransferFailed(s.message)
                    }
                    is com.bina.ai.sync.ReceiverState.Receiving -> {
                        vm.onTransferProgress(s.pct)
                    }
                    else -> {}
                }
            }
        }
        r.start()
    }

    DisposableEffect(Unit) {
        onDispose { receiver?.stop() }
    }
```

Then, just before the existing `if (showPaste) { PasteYamlSheet(...) }` block, add the receive sheet:

```kotlin
    pairing?.let { offer ->
        ReceivePairingSheet(
            offer = offer,
            transferState = transferState,
            onConnect = { startTransfer(offer) },
            onCancel = {
                receiver?.stop()
                receiver = null
                vm.dismissPairing()
            },
            onRetry = { startTransfer(offer) }
        )
    }
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/components/ScanQrScreen.kt
git commit -m "ScanQrScreen routes BINA-BT QRs through ReceivePairingSheet + BleReceiver"
```

---

### Task 9: `ShareQrScreen` — start BleSender and emit BINA-BT QR

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ShareQrScreen.kt`

- [ ] **Step 1: Replace the encode block**

In `ShareQrScreen`, replace the existing `encodeResult = remember(recipe) { recipe?.let { vm.encodeRecipeAsQr(it) } }` block with a BLE-aware version:

```kotlin
    val context = LocalContext.current
    val yamlText = remember(recipe) {
        recipe?.let { miniAppRepository.getYamlById(it.id) }
    }
    val sender = remember(recipe, yamlText) {
        if (recipe == null || yamlText == null) null
        else com.bina.ai.sync.BleSender(
            context = context,
            serviceUuid = java.util.UUID.randomUUID(),
            payloadBytes = yamlText.toByteArray(Charsets.UTF_8)
        )
    }
    val pairingPayload = remember(recipe, sender, yamlText) {
        if (recipe == null || sender == null || yamlText == null) null
        else com.bina.ai.sync.BlePairingPayload.encode(
            com.bina.ai.sync.BlePairingPayload.Offer(
                serviceUuid = sender.serviceUuid,
                recipeId = recipe.id,
                sizeBytes = yamlText.toByteArray(Charsets.UTF_8).size.toLong(),
                recipeName = recipe.name,
                authorName = recipe.author.name
            )
        )
    }

    LaunchedEffect(sender) {
        if (sender != null && com.bina.ai.sync.BlePermissions.hasSenderPermissions(context)) {
            sender.start()
        }
    }
    DisposableEffect(sender) {
        onDispose { sender?.stop() }
    }
```

Then in the rendering block, replace `payload != null` checks with `pairingPayload != null`. The bitmap encode call stays the same (still uses ZXing); only the input string changes to `pairingPayload`.

The error fallback path (`else if (error != null) { ... Copy YAML to clipboard ... }`) becomes the BLE-permission-denied or BLE-unsupported path. Update its text to:

```kotlin
            } else {
                Text(
                    "Bluetooth permission needed to share via QR. Or copy YAML below.",
                    fontSize = 12.sp, color = BinaRed
                )
                Button(onClick = {
                    if (yamlText != null) clipboard.setText(AnnotatedString(yamlText))
                }, colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)) {
                    Text("Copy YAML to clipboard")
                }
            }
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/components/ShareQrScreen.kt
git commit -m "ShareQrScreen now starts BleSender and emits BINA-BT pairing QR"
```

---

## Phase 7 — Permissions request flow

### Task 10: Just-in-time permission prompts

**Files:**
- Modify: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ScanQrScreen.kt` — request RECEIVER_PERMISSIONS on first launch
- Modify: `app/src/main/java/com/bina/ai/ui/screens/sync/components/ShareQrScreen.kt` — request SENDER_PERMISSIONS on first launch

- [ ] **Step 1: Add permission launcher in `ScanQrScreen`**

Just below the existing `LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }` block, add the BLE permission request:

```kotlin
    val blePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* if any denied, the receiver flow will surface its own error */ }
    LaunchedEffect(Unit) {
        if (!com.bina.ai.sync.BlePermissions.hasReceiverPermissions(context)) {
            blePermLauncher.launch(com.bina.ai.sync.BlePermissions.RECEIVER_PERMISSIONS)
        }
    }
```

- [ ] **Step 2: Add permission launcher in `ShareQrScreen`**

Same pattern, but request `SENDER_PERMISSIONS` and re-trigger `sender?.start()` after grant. Add inside the composable:

```kotlin
    val blePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it } && sender != null) {
            sender.start()
        }
    }
    LaunchedEffect(Unit) {
        if (!com.bina.ai.sync.BlePermissions.hasSenderPermissions(context)) {
            blePermLauncher.launch(com.bina.ai.sync.BlePermissions.SENDER_PERMISSIONS)
        }
    }
```

- [ ] **Step 3: Verify build**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bina/ai/ui/screens/sync/components/ScanQrScreen.kt app/src/main/java/com/bina/ai/ui/screens/sync/components/ShareQrScreen.kt
git commit -m "Request BLE permissions just-in-time on Scan and Share screens"
```

---

## Phase 8 — Smoke test + PR

### Task 11: Manual smoke test on real hardware

**Files:** none (manual test).

- [ ] **Step 1: Install on emulator (sender)**

Run: `ANDROID_SERIAL=emulator-5554 ./gradlew.bat :app:installDebug`

- [ ] **Step 2: Install on physical phone (receiver)**

If sideload-only access (Xiaomi-style restrictions): build APK with `./gradlew.bat :app:assembleDebug`, copy `app/build/outputs/apk/debug/app-debug.apk` to phone, install via the file manager.

- [ ] **Step 3: Smoke flow — Bidan Pintar (small recipe, sanity check)**

1. Sender: install Bidan Pintar from Hub if not already.
2. Sender: Sync → Share → Bidan Pintar → grant BT permissions when prompted → BINA-BT QR appears.
3. Receiver: Sync → Scan → grant BT permissions → camera scans QR → ReceivePairingSheet shows "Receive Bidan Pintar by ... (3 KB)? [Connect]".
4. Tap Connect → progress fills → RecipeDetailSheet → Configure & Install.
5. Verify: receiver's Hub now shows Bidan Pintar as installed.

- [ ] **Step 4: Smoke flow — Farm Buddy (the big one this whole effort is for)**

Same as Step 3 with Farm Buddy. Transfer should take ~1-2 seconds.

- [ ] **Step 5: Failure case — sender quits before connect**

1. Sender shows QR.
2. Receiver scans, sees pairing sheet, taps Connect.
3. Sender taps Done before receiver finishes connecting.
4. Receiver: should show "Couldn't find sender" error within 10s with Retry button.

- [ ] **Step 6: Failure case — permission denied**

1. Receiver: Sync → Scan → DENY BT permission.
2. Scan a BINA-BT QR.
3. Tap Connect → should show "Bluetooth permission required" error in pairing sheet.
4. Verify Paste YAML fallback still works (paste a BINA2 or raw YAML).

- [ ] **Step 7: Push branch and open PR**

```bash
git push -u origin feature/offline-sync-ble
gh pr create --base main --head feature/offline-sync-ble \
  --title "Offline Sync v2: BLE discovery transport" \
  --body "Implements docs/superpowers/specs/2026-05-10-offline-sync-ble-design.md"
```

---

## Self-review notes

- **Spec coverage:** Tasks 1-3 cover foundation (manifest, payload, permissions). Tasks 4-5 cover BLE Sender + Receiver. Tasks 6-9 cover ViewModel + UI integration. Task 10 covers just-in-time permission prompts. Task 11 covers manual verification.
- **Type consistency:** `BlePairingPayload.Offer` used by SyncViewModel + ReceivePairingSheet + ShareQrScreen. `SenderState`/`ReceiverState` are internal to each class; only `TransferState` (in SyncViewModel) crosses the VM boundary.
- **Placeholder check:** All steps have full code or full commands. No "TODO" / "TBD" / "fill in later".
- **Known untestable:** BLE peripheral/central code can't be unit-tested without instrumentation. Manual smoke test in Task 11 covers it.
- **Concurrency note:** Task 8 uses `kotlinx.coroutines.GlobalScope.launch` to bridge the BleReceiver's StateFlow to the VM. This is a hackathon pragmatic choice — proper fix is to plumb the receiver's state through the VM directly. Implementer can flag this as DONE_WITH_CONCERNS if uncomfortable; it's load-bearing for the demo but technically a leak risk.
