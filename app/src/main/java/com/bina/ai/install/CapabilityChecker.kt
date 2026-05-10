package com.bina.ai.install

import android.content.Context
import android.content.pm.PackageManager

/**
 * Maps capability tokens (strings like "permission:camera", "hardware:gps", "service:p2p") to a
 * Boolean indicating whether the capability is currently satisfied on this device.
 *
 * - permission:* — always returns true. Permission requests are deferred to use-time.
 * - hardware:* — checks PackageManager.hasSystemFeature(...).
 * - service:* — always returns false. Reserved for runtime services we haven't built yet.
 * - anything else — returns true (lenient default).
 *
 * A feature is available iff ALL of its `requires` tokens are available.
 */
class CapabilityChecker private constructor(
    private val hardwareCheck: (String) -> Boolean
) {
    fun isAvailable(token: String): Boolean = when {
        token.startsWith("permission:") -> true
        token.startsWith("service:") -> false
        token.startsWith("hardware:") -> hardwareCheck(token.removePrefix("hardware:"))
        else -> true
    }

    companion object {
        fun create(context: Context): CapabilityChecker {
            val pm = context.packageManager
            return CapabilityChecker { name ->
                val systemFeature = when (name) {
                    "gps" -> PackageManager.FEATURE_LOCATION_GPS
                    "camera" -> PackageManager.FEATURE_CAMERA_ANY
                    else -> name
                }
                pm.hasSystemFeature(systemFeature)
            }
        }

        /** Test-only factory. Caller supplies a fixed hardware-support map. */
        fun forTest(hardwareSupport: Map<String, Boolean>): CapabilityChecker =
            CapabilityChecker { name -> hardwareSupport[name] ?: false }
    }
}
