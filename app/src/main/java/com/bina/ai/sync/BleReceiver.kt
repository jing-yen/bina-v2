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
    @Volatile private var done = false
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

    private val scanTimeoutRunnable: Runnable = Runnable {
        if (_state.value is ReceiverState.Scanning) {
            scanner?.stopScan(scanCallback)
            _state.value = ReceiverState.Failed("Couldn't find sender. Move closer or try again.")
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
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
                runCatching { g.close() }
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
            if (done) return
            buffer.addAll(c.value.toList())
            if (buffer.size >= expectedSize) {
                done = true
                _state.value = ReceiverState.Done(buffer.toByteArray())
                runCatching { g.disconnect() }
            } else {
                val pct = ((buffer.size.toLong() * 100L) / expectedSize).toInt()
                _state.value = ReceiverState.Receiving(pct)
            }
        }
    }
}
