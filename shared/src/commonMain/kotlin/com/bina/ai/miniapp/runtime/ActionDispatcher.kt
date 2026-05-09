package com.bina.ai.miniapp.runtime

import com.bina.ai.inference.InferenceEngine
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.platform.LocationProvider
import com.bina.ai.platform.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class ActionDispatcher(
    private val store: VariableStore,
    private val miniApp: MiniApp,
    private val formulaEngine: FormulaEngine,
    private val locationProvider: LocationProvider? = null,
    private val inferenceEngine: InferenceEngine? = null,
    private val onNavigate: (String) -> Unit
) {

    suspend fun dispatch(action: String) {
        val interpolated = store.interpolate(action)
        val colonIndex = interpolated.indexOf(':')
        val prefix = if (colonIndex > 0) interpolated.substring(0, colonIndex) else interpolated
        val payload = if (colonIndex > 0) interpolated.substring(colonIndex + 1) else ""

        Logger.d(TAG, "Dispatch: $prefix | $payload")

        when (prefix) {
            "ask" -> handleAsk(payload)
            "vision_ask" -> handleVisionAsk(payload)
            "formula" -> handleFormula(payload)
            "go" -> handleGo(payload)
            "geolocate" -> handleGeolocate()
            "set" -> handleSet(payload)
            else -> Logger.w(TAG, "Unknown action: $prefix")
        }
    }

    private suspend fun handleAsk(prompt: String) {
        if (prompt.isBlank()) return

        val blocked = miniApp.safety.blockedKeywords.any { kw ->
            prompt.contains(kw, ignoreCase = true)
        }
        if (blocked) {
            store["ai_response"] = miniApp.safety.escalationMessage
                .ifEmpty { "This request has been blocked for safety." }
            return
        }

        store["ai_response"] = ""
        store["is_loading"] = "true"

        val engine = inferenceEngine
        if (engine == null || !engine.isReady) {
            store["ai_response"] = "**Model not loaded.** Push a `.litertlm` model to `/data/local/tmp/` to enable AI."
            store["is_loading"] = "false"
            return
        }

        try {
            val sb = StringBuilder()
            engine.generate(prompt, miniApp.model.systemPrompt)
                .onEach { chunk ->
                    sb.append(chunk)
                    store["ai_response"] = sb.toString()
                }
                .catch { e ->
                    Logger.e(TAG, "Inference error", e)
                    store["ai_response"] = sb.toString().ifEmpty { "Error: ${e.message}" }
                }
                .collect()
        } finally {
            store["is_loading"] = "false"
        }
    }

    private suspend fun handleVisionAsk(prompt: String) {
        val photoPath = store["photo_path"]
        if (photoPath.isBlank()) {
            store["ai_response"] = "Please take a photo first."
            return
        }

        store["ai_response"] = ""
        store["is_loading"] = "true"

        val engine = inferenceEngine
        if (engine == null || !engine.isReady) {
            store["ai_response"] = "**Model not loaded.** Push a `.litertlm` model to `/data/local/tmp/` to enable vision AI."
            store["is_loading"] = "false"
            return
        }

        // Gemma 4 vision via Kotlin LiteRT-LM API has a known SDK limitation
        // (missing SetOverwritePromptTemplate in JNI). Fall back to text-only
        // inference with the prompt, noting that a photo was captured.
        val textPrompt = "A farmer has taken a photo of their crop leaf and asks: $prompt " +
            "Provide a general diagnostic guide covering the most common leaf diseases " +
            "(bacterial blight, brown spot, leaf blast, tungro), their visual symptoms, " +
            "likely causes, and recommended treatments."

        try {
            val sb = StringBuilder()
            engine.generate(textPrompt, miniApp.model.systemPrompt)
                .onEach { chunk ->
                    sb.append(chunk)
                    store["ai_response"] = sb.toString()
                }
                .catch { e ->
                    Logger.e(TAG, "Vision inference error", e)
                    store["ai_response"] = sb.toString().ifEmpty { "Error: ${e.message}" }
                }
                .collect()
        } finally {
            store["is_loading"] = "false"
        }
    }

    private fun handleFormula(formulaId: String) {
        formulaEngine.evaluate(formulaId, store)
    }

    private fun handleGo(screenId: String) {
        onNavigate(screenId)
    }

    private suspend fun handleGeolocate() {
        store["is_loading"] = "true"
        try {
            val location = locationProvider?.getCurrentLocation()
            if (location != null) {
                store["user_location"] = "${location.first},${location.second}"
                Logger.d(TAG, "Got location: ${location.first}, ${location.second}")
            } else {
                store["user_location"] = DEFAULT_LOCATION
                Logger.w(TAG, "Location unavailable, using default coords")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Location error", e)
            store["user_location"] = DEFAULT_LOCATION
        }
        store["is_loading"] = "false"
    }

    private fun handleSet(payload: String) {
        val eqIndex = payload.indexOf('=')
        if (eqIndex > 0) {
            val key = payload.substring(0, eqIndex)
            val value = payload.substring(eqIndex + 1)
            store[key] = value
        }
    }

    companion object {
        private const val TAG = "ActionDispatcher"
        private const val DEFAULT_LOCATION = "3.139,101.687"
    }
}
