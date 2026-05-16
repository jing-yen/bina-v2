package com.bina.ai.inference

import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    val isReady: Boolean
    suspend fun initialize()
    fun generate(prompt: String, systemPrompt: String = ""): Flow<String>
    fun generateWithImage(prompt: String, imagePath: String, systemPrompt: String = ""): Flow<String>
    fun generateWithAudio(prompt: String, audioPath: String, systemPrompt: String = ""): Flow<String>
    fun close()
}
