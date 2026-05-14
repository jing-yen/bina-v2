package com.bina.ai.analytics.tracking

import android.content.Context
import android.os.Build
import com.bina.ai.platform.Logger
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale

class AnalyticsPinger(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val countryCode = Locale.getDefault().country.ifEmpty { "XX" }
    private val appVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (_: Exception) { "unknown" }
    private val deviceHash = hashDeviceId(
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"
    )

    suspend fun onRecipeLaunched(recipeId: String) = withContext(Dispatchers.IO) {
        val pingedKey = "pinged_$recipeId"
        if (prefs.getBoolean(pingedKey, false)) return@withContext

        try {
            db.collection(PINGS_COLLECTION).add(
                mapOf(
                    "recipe_id" to recipeId,
                    "country_code" to countryCode,
                    "app_version" to appVersion,
                    "device_hash" to deviceHash,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection(RECIPES_COLLECTION).document(recipeId)
                .update(
                    mapOf(
                        "stats.downloads" to FieldValue.increment(1),
                        "stats.users" to FieldValue.increment(1)
                    )
                ).await()

            prefs.edit().putBoolean(pingedKey, true).apply()
            Logger.d(TAG, "Ping sent for $recipeId from $countryCode")
        } catch (e: Exception) {
            Logger.e(TAG, "Ping failed for $recipeId", e)
        }
    }

    companion object {
        private const val TAG = "AnalyticsPinger"
        private const val PREFS_NAME = "bina_analytics_pings"
        private const val PINGS_COLLECTION = "pings"
        private const val RECIPES_COLLECTION = "recipes"

        private fun hashDeviceId(id: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(id.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(16)
        }
    }
}
