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
    private val onNavigate: (String) -> Unit,
    private val onAskLogged: () -> Unit = {},
    private val onNativeIntent: ((NativeIntent) -> Unit)? = null
) {

    sealed interface NativeIntent {
        data class Sms(val phone: String, val body: String) : NativeIntent
        data class Tel(val phone: String) : NativeIntent
        data class Tts(val text: String) : NativeIntent
        data class Share(val text: String) : NativeIntent
    }

    suspend fun dispatch(action: String) {
        val parts = action.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        for (part in parts) {
            dispatchSingle(part)
        }
    }

    private suspend fun dispatchSingle(action: String) {
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
            "increment" -> handleIncrement(payload)
            "tts" -> onNativeIntent?.invoke(NativeIntent.Tts(payload))
            "share" -> onNativeIntent?.invoke(NativeIntent.Share(payload))
            "sms" -> handleSms(interpolated)
            "tel" -> onNativeIntent?.invoke(NativeIntent.Tel(payload))
            else -> Logger.w(TAG, "Unknown action: $prefix")
        }
    }

    private fun handleSms(raw: String) {
        // Format: sms:<phone>:<body>
        val withoutPrefix = raw.removePrefix("sms:")
        val colonIdx = withoutPrefix.indexOf(':')
        if (colonIdx > 0) {
            val phone = withoutPrefix.substring(0, colonIdx)
            val body = withoutPrefix.substring(colonIdx + 1)
            onNativeIntent?.invoke(NativeIntent.Sms(phone, body))
        }
    }

    private suspend fun handleAsk(prompt: String) {
        if (prompt.isBlank() || store.isTrue("is_loading")) return

        val blocked = miniApp.safety.blockedKeywords.any { kw ->
            prompt.contains(kw, ignoreCase = true)
        }
        if (blocked) {
            store["ai_response"] = miniApp.safety.escalationMessage
                .ifEmpty { "This request has been blocked for safety." }
            return
        }

        // Safety passed — log this as a real ask event
        onAskLogged()

        store["ai_response"] = ""
        store["is_loading"] = "true"

        val engine = inferenceEngine
        if (engine == null || !engine.isReady) {
            store["ai_response"] = "**Model not loaded.** Push a `.litertlm` model to `/data/local/tmp/` to enable AI."
            store["is_loading"] = "false"
            return
        }

        val systemPrompt = buildSystemPrompt()

        try {
            val sb = StringBuilder()
            engine.generate(prompt, systemPrompt)
                .onEach { chunk ->
                    sb.append(chunk)
                    store["ai_response"] = sb.toString()
                }
                .catch { e ->
                    Logger.e(TAG, "Inference error", e)
                    store["ai_response"] = sb.toString().ifEmpty { "Error: ${e.message}" }
                }
                .collect()
            store["last_result"] = sb.toString()
        } finally {
            store["is_loading"] = "false"
        }
    }

    private suspend fun handleVisionAsk(prompt: String) {
        if (store.isTrue("is_loading")) return

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

        // Skip the native vision path entirely if the engine has already flagged it as broken.
        if (!engine.isVisionReady) {
            Logger.w(TAG, "Vision not ready; falling back to text-only inference")
            store["is_loading"] = "false"
            store["ai_response"] = "**Vision analysis unavailable** — answering with text only.\n\n"
            handleAsk(prompt)
            return
        }

        val systemPrompt = buildSystemPrompt()

        try {
            val sb = StringBuilder()
            engine.generateWithImage(prompt, photoPath, systemPrompt)
                .onEach { chunk ->
                    sb.append(chunk)
                    store["ai_response"] = sb.toString()
                }
                .catch { e ->
                    // Flow-level errors (e.g. image decode failure, SDK errors surfaced as
                    // Kotlin exceptions). Re-throw so the outer try/catch handles the fallback.
                    Logger.e(TAG, "Vision inference stream error", e)
                    throw e
                }
                .collect()
            store["last_result"] = sb.toString()
        } catch (e: Exception) {
            // Catch any exception that escapes the flow — including UnsupportedOperationException
            // thrown by LiteRtLmEngine when isVisionReady is false, and any JVM-visible crash
            // wrapping a native SIGSEGV. Fall back to text-only inference.
            Logger.e(TAG, "Vision inference failed, falling back to text-only", e)
            store["is_loading"] = "false"
            store["ai_response"] = "**Vision analysis unavailable** — answering with text only.\n\n"
            handleAsk(prompt)
            return
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
                store["user_location_time"] = com.bina.ai.platform.Clock.now().toString()
                Logger.d(TAG, "Got location: ${location.first}, ${location.second}")
            } else {
                store["user_location"] = DEFAULT_LOCATION
                store["user_location_time"] = com.bina.ai.platform.Clock.now().toString()
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

    private fun handleIncrement(variableName: String) {
        if (variableName.isBlank()) return
        val current = store[variableName].toIntOrNull() ?: 0
        val max = findChecklistMax(variableName)
        if (max != null && current >= max) return
        store[variableName] = (current + 1).toString()
    }

    private fun findChecklistMax(variableName: String): Int? {
        for (screen in miniApp.screens) {
            for (widget in screen.body) {
                if (widget is com.bina.ai.miniapp.model.Widget.ChecklistItems && widget.bind == variableName) {
                    return widget.items.size
                }
                if (widget is com.bina.ai.miniapp.model.Widget.ProgressBar && widget.bind == variableName) {
                    return widget.total
                }
            }
        }
        return null
    }

    private fun buildSystemPrompt(): String = buildString {
        appendLine("Be concise. Give a clear diagnosis or answer with actionable steps. Use short bullet points. No filler or preamble.")
        appendLine()
        val lang = store["active_language"]
        if (lang.isNotBlank()) {
            val langName = LANG_NAMES[lang] ?: lang
            appendLine("IMPORTANT: Reply in $langName ($lang).")
            appendLine()
        }
        if (miniApp.knowledge.alwaysLoaded.isNotBlank()) {
            appendLine("## Reference Knowledge")
            appendLine(miniApp.knowledge.alwaysLoaded)
            appendLine()
        }
        append(miniApp.model.systemPrompt)
    }

    companion object {
        private const val TAG = "ActionDispatcher"
        private const val DEFAULT_LOCATION = "3.139,101.687"
        private val LANG_NAMES = mapOf(
            "ms" to "Bahasa Melayu", "en" to "English",
            "id" to "Bahasa Indonesia", "zh" to "中文 (Chinese)",
            "ta" to "தமிழ் (Tamil)", "th" to "ไทย (Thai)",
            "vi" to "Tiếng Việt (Vietnamese)", "tl" to "Filipino",
            "my" to "မြန်မာ (Burmese)", "km" to "ខ្មែរ (Khmer)",
            "lo" to "ລາວ (Lao)", "ja" to "日本語 (Japanese)",
            "ko" to "한국어 (Korean)", "ar" to "العربية (Arabic)",
            "hi" to "हिन्दी (Hindi)", "bn" to "বাংলা (Bengali)",
            "fr" to "Français (French)", "es" to "Español (Spanish)",
            "pt" to "Português (Portuguese)", "sw" to "Kiswahili (Swahili)",
            "jv" to "Basa Jawa (Javanese)", "su" to "Basa Sunda (Sundanese)",
            "ceb" to "Cebuano", "ilo" to "Ilokano (Ilocano)",
            "ne" to "नेपाली (Nepali)", "si" to "සිංහල (Sinhala)",
            "ur" to "اردو (Urdu)", "ml" to "മലയാളം (Malayalam)",
            "mr" to "मराठी (Marathi)", "te" to "తెలుగు (Telugu)",
            "de" to "Deutsch (German)", "ru" to "Русский (Russian)",
            "am" to "አማርኛ (Amharic)", "gu" to "ગુજરાતી (Gujarati)",
            "kn" to "ಕನ್ನಡ (Kannada)",
        )
    }
}
