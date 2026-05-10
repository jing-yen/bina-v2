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
private const val DEFAULT_CHUNK_SIZE = 244  // assumes MTU 247 (default after receiver requestMtu(247))
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
        if (_state.value != SenderState.Idle) return true  // already started
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
                    Thread { sendPayload(device) }.start()
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
