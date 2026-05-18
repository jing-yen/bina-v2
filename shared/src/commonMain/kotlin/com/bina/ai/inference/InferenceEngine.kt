package com.bina.ai.inference

import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    val isReady: Boolean

    /**
     * Whether the engine can handle multimodal (image) inference.
     * Defaults to true; implementations may set this to false after a vision
     * call fails so callers can skip the native path on subsequent requests.
     */
    val isVisionReady: Boolean get() = true

    suspend fun initialize()
    fun generate(prompt: String, systemPrompt: String = ""): Flow<String>
    fun generateWithImage(prompt: String, imagePath: String, systemPrompt: String = ""): Flow<String>
    fun generateWithAudio(prompt: String, audioPath: String, systemPrompt: String = ""): Flow<String>
    fun close()
}
