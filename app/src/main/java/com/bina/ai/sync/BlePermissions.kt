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
