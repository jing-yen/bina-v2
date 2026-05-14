package com.bina.ai.miniapp.runtime

import com.bina.ai.inference.InferenceEngine
import com.bina.ai.miniapp.model.ScreenCatalogEntry
import com.bina.ai.miniapp.model.TriageConfig
import com.bina.ai.platform.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList

class TriageEngine(
    private val catalog: List<ScreenCatalogEntry>,
    private val questions: Map<String, List<String>>,
    private val config: TriageConfig,
    private val inferenceEngine: InferenceEngine?
) {
    private var clarificationCount = 0
    private var lastClarifyScreenId: String? = null

    val isChatMode: Boolean get() = config.homeMode == "chat" && catalog.isNotEmpty()

    fun reset() {
        clarificationCount = 0
        lastClarifyScreenId = null
    }

    suspend fun route(userInput: String): TriageResult {
        val engine = inferenceEngine
        if (engine == null || !engine.isReady || catalog.isEmpty()) {
            return TriageResult.Fallback
        }

        val routingPrompt = buildRoutingPrompt(userInput)

        try {
            val chunks = engine.generate(routingPrompt, ROUTING_SYSTEM_PROMPT)
                .catch { e ->
                    Logger.e(TAG, "Triage inference error", e)
                }
                .toList()
            val response = chunks.joinToString("").trim()

            val matchedId = catalog.firstOrNull { entry ->
                response.equals(entry.id, ignoreCase = true) ||
                    response.contains(entry.id, ignoreCase = true)
            }?.id

            if (matchedId != null) {
                val entry = catalog.first { it.id == matchedId }
                return TriageResult.Navigate(matchedId, entry.prefillHints)
            }

            if (response.contains("CLARIFY", ignoreCase = true) &&
                clarificationCount < config.maxClarifications
            ) {
                clarificationCount++
                val question = pickClarificationQuestion()
                if (question != null) {
                    return TriageResult.Clarify(question)
                }
            }

            return when (config.fallback) {
                "show_all" -> TriageResult.Fallback
                else -> TriageResult.Fallback
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Triage routing failed", e)
            return TriageResult.Fallback
        }
    }

    private fun buildRoutingPrompt(userInput: String): String = buildString {
        appendLine("User says: \"$userInput\"")
        appendLine()
        appendLine("Available screens:")
        catalog.forEach { entry ->
            appendLine("- ${entry.id}: ${entry.description.ifEmpty { entry.title }}")
        }
        appendLine()
        appendLine("Reply with ONLY the screen id that best matches, or CLARIFY if you need more info.")
    }

    private fun pickClarificationQuestion(): String? {
        if (questions.isEmpty()) return "Could you tell me more about what you need?"

        val allQuestions = questions.values.flatten()
        if (allQuestions.isEmpty()) return "Could you tell me more about what you need?"

        val idx = (clarificationCount - 1).coerceIn(0, allQuestions.size - 1)
        return allQuestions[idx]
    }

    companion object {
        private const val TAG = "TriageEngine"
        private const val ROUTING_SYSTEM_PROMPT =
            "You are a routing assistant. Given a user's request and a list of available screens, " +
            "respond with ONLY the screen id that best matches their need. " +
            "If the request is unclear, respond with CLARIFY. " +
            "Do not add any other text or explanation."
    }
}

sealed class TriageResult {
    data class Navigate(val screenId: String, val prefillHints: Map<String, String> = emptyMap()) : TriageResult()
    data class Clarify(val question: String) : TriageResult()
    data object Fallback : TriageResult()
}
