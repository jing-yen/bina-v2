package com.bina.ai.inference

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File

class LiteRtLmEngine(private val context: Context) : InferenceEngine {

    private var engine: Engine? = null
    override var isReady: Boolean = false
        private set

    /**
     * Flipped to false the first time a vision call crashes (SIGSEGV surfaces as a JVM
     * exception in the caller, or we detect it pre-emptively). Subsequent vision_ask
     * actions will skip the native path and fall back to text-only inference.
     */
    @Volatile
    override var isVisionReady: Boolean = true
        private set

    override suspend fun initialize() {
        val modelPath = findModelFile() ?: run {
            Log.w(TAG, "No .litertlm model found. Checked: $MODEL_SEARCH_PATHS")
            return
        }

        Log.d(TAG, "Loading model from: $modelPath")

        withContext(Dispatchers.IO) {
            try {
                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    visionBackend = Backend.GPU(),
                    audioBackend = Backend.CPU(),
                    maxNumImages = 1,
                    cacheDir = context.cacheDir.path
                )
                engine = Engine(config)
                engine!!.initialize()
                isReady = true
                Log.d(TAG, "Engine initialized with GPU vision + CPU audio backend")
            } catch (e: Exception) {
                Log.w(TAG, "GPU vision backend failed, retrying all CPU: ${e.message}")
                try {
                    val cpuConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        visionBackend = Backend.CPU(),
                        audioBackend = Backend.CPU(),
                        maxNumImages = 1,
                        cacheDir = context.cacheDir.path
                    )
                    engine = Engine(cpuConfig)
                    engine!!.initialize()
                    isReady = true
                    Log.d(TAG, "Engine initialized with all-CPU backends")
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to initialize engine", e2)
                    engine = null
                    isReady = false
                }
            }
        }
    }

    override fun generate(prompt: String, systemPrompt: String): Flow<String> {
        val eng = engine ?: return fallbackFlow(prompt)

        return flow {
            val conversation = createConversation(eng, systemPrompt)
            try {
                conversation.sendMessageAsync(prompt).collect { message ->
                    val chunk = extractText(message)
                    if (chunk.isNotEmpty()) emit(chunk)
                }
            } finally {
                conversation.close()
            }
        }.flowOn(Dispatchers.IO).catch { e ->
            Log.e(TAG, "Generate error", e)
            emit("Error: ${e.message}")
        }
    }

    override fun generateWithImage(
        prompt: String,
        imagePath: String,
        systemPrompt: String
    ): Flow<String> {
        val eng = engine ?: return fallbackFlow(prompt)

        // If a previous vision call crashed, skip the native path immediately so the
        // caller can fall back to text-only inference without risking another SIGSEGV.
        if (!isVisionReady) {
            Log.w(TAG, "Vision marked unavailable; skipping generateWithImage")
            throw UnsupportedOperationException("Vision inference is unavailable on this device/model")
        }

        return flow {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                emit("Error: Image file not found at $imagePath")
                return@flow
            }
            Log.d(TAG, "Vision: loading image from $imagePath (${imageFile.length()} bytes)")
            val conversation = createConversation(eng, systemPrompt)
            try {
                val originalBitmap = BitmapFactory.decodeFile(imagePath)
                if (originalBitmap == null) {
                    emit("Error: Could not decode image at $imagePath")
                    return@flow
                }
                val maxDim = 448
                val scale = minOf(maxDim.toFloat() / originalBitmap.width, maxDim.toFloat() / originalBitmap.height, 1f)
                val bitmap = if (scale < 1f) {
                    val w = (originalBitmap.width * scale).toInt()
                    val h = (originalBitmap.height * scale).toInt()
                    Bitmap.createScaledBitmap(originalBitmap, w, h, true).also { originalBitmap.recycle() }
                } else originalBitmap
                val pngStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngStream)
                val imageBytes = pngStream.toByteArray()
                bitmap.recycle()
                Log.d(TAG, "Vision: converted to PNG (${imageBytes.size} bytes)")
                val contents = Contents.of(
                    Content.ImageBytes(imageBytes),
                    Content.Text(prompt)
                )
                conversation.sendMessageAsync(contents).collect { message ->
                    val chunk = extractText(message)
                    if (chunk.isNotEmpty()) emit(chunk)
                }
            } finally {
                conversation.close()
            }
        }.flowOn(Dispatchers.IO).catch { e ->
            Log.e(TAG, "Vision generate error — disabling vision for this session", e)
            // Mark vision as broken so future calls skip the native path.
            isVisionReady = false
            // Re-throw so the caller (ActionDispatcher) can apply its own fallback.
            throw e
        }
    }

    /** Marks vision inference as unavailable. Called externally when a crash is detected. */
    fun disableVision() {
        Log.w(TAG, "Vision inference disabled by caller")
        isVisionReady = false
    }

    override fun generateWithAudio(
        prompt: String,
        audioPath: String,
        systemPrompt: String
    ): Flow<String> {
        val eng = engine ?: return fallbackFlow(prompt)

        return flow {
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                emit("Error: Audio file not found at $audioPath")
                return@flow
            }
            Log.d(TAG, "Audio: loading from $audioPath (${audioFile.length()} bytes)")
            val conversation = createConversation(eng, systemPrompt)
            try {
                val audioBytes = audioFile.readBytes()
                Log.d(TAG, "Audio: ${audioBytes.size} bytes")
                val contents = Contents.of(
                    Content.AudioBytes(audioBytes),
                    Content.Text(prompt)
                )
                conversation.sendMessageAsync(contents).collect { message ->
                    val chunk = extractText(message)
                    if (chunk.isNotEmpty()) emit(chunk)
                }
            } finally {
                conversation.close()
            }
        }.flowOn(Dispatchers.IO).catch { e ->
            Log.e(TAG, "Audio generate error", e)
            emit("Error: ${e.message}")
        }
    }

    /**
     * Extracts the text content from a streaming [Message] chunk.
     *
     * The LiteRT-LM SDK emits each [Message] as an incremental token chunk via
     * [Conversation.sendMessageAsync]. Each chunk's [Contents] holds a list of [Content] items;
     * we concatenate all [Content.Text] parts to get the raw token text for that chunk.
     */
    private fun extractText(message: Message): String =
        message.contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString("") { it.text }

    private fun createConversation(eng: Engine, systemPrompt: String): Conversation {
        val config = ConversationConfig(
            systemInstruction = if (systemPrompt.isNotBlank()) {
                Contents.of(systemPrompt)
            } else null,
            samplerConfig = SamplerConfig(
                temperature = 0.3,
                topK = 40,
                topP = 0.95
            )
        )
        return eng.createConversation(config)
    }

    private fun fallbackFlow(prompt: String): Flow<String> = flow {
        emit("**Model not loaded.** To enable on-device AI:\n\n" +
            "1. Download a `.litertlm` model (e.g. gemma-4-E2B-it) from HuggingFace\n" +
            "2. Push to device: `adb push model.litertlm /data/local/tmp/`\n\n" +
            "_Your question: \"${prompt.take(100)}\"_")
    }

    fun findModelFile(): String? {
        val appModelsDir = File(context.filesDir, "models")
        val searchPaths = listOf(appModelsDir.absolutePath) + MODEL_SEARCH_PATHS
        for (path in searchPaths) {
            val dir = File(path)
            val listed = dir.listFiles()
            if (listed != null) {
                val model = listed.firstOrNull { it.name.endsWith(".litertlm") }
                if (model != null) return model.absolutePath
            }
            for (name in MODEL_FILENAMES) {
                val f = File(path, name)
                if (f.exists() && f.canRead()) return f.absolutePath
            }
        }
        return null
    }

    override fun close() {
        engine?.close()
        engine = null
        isReady = false
    }

    companion object {
        private const val TAG = "LiteRtLmEngine"
        private val MODEL_SEARCH_PATHS = listOf(
            "/data/local/tmp",
            "/sdcard/Download",
            "/sdcard/Models"
        )
        private val MODEL_FILENAMES = listOf(
            "gemma-4-E2B-it.litertlm",
            "gemma-3-1B-it.litertlm",
            "model.litertlm"
        )
    }
}
