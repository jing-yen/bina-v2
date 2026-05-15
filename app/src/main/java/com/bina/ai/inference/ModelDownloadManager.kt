package com.bina.ai.inference

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadManager(private val context: Context) {

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    private val targetFile: File
        get() = File(modelsDir, MODEL_FILENAME)

    private val partialFile: File
        get() = File(modelsDir, "$MODEL_FILENAME.part")

    fun getModelPath(): String? {
        val f = targetFile
        return if (f.exists() && f.length() > MIN_MODEL_SIZE) f.absolutePath else null
    }

    suspend fun download() {
        if (_state.value is DownloadState.Downloading) return

        withContext(Dispatchers.IO) {
            try {
                val existing = partialFile.length().takeIf { partialFile.exists() } ?: 0L
                Log.d(TAG, "Starting download, resuming from byte $existing")
                _state.value = DownloadState.Downloading(0f, existing, 0L)

                val conn = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 60_000
                    if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
                    connect()
                }

                val responseCode = conn.responseCode
                val totalBytes: Long
                val resume: Boolean

                if (responseCode == 206) {
                    val contentLength = conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
                    totalBytes = existing + contentLength
                    resume = true
                } else if (responseCode == 200) {
                    totalBytes = conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
                    if (existing > 0) partialFile.delete()
                    resume = false
                } else {
                    _state.value = DownloadState.Error("HTTP $responseCode")
                    conn.disconnect()
                    return@withContext
                }

                Log.d(TAG, "Response $responseCode, total=$totalBytes, resume=$resume")

                conn.inputStream.use { input ->
                    partialFile.outputStream(resume).use { output ->
                        val buffer = ByteArray(128 * 1024)
                        var downloaded = if (resume) existing else 0L
                        var lastEmitTime = System.currentTimeMillis()

                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime > 250) {
                                val progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else -1f
                                _state.value = DownloadState.Downloading(progress, downloaded, totalBytes)
                                lastEmitTime = now
                            }
                        }
                        output.flush()
                    }
                }
                conn.disconnect()

                if (partialFile.length() < MIN_MODEL_SIZE) {
                    _state.value = DownloadState.Error("Download incomplete (${partialFile.length()} bytes)")
                    return@withContext
                }

                partialFile.renameTo(targetFile)
                Log.d(TAG, "Download complete: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                _state.value = DownloadState.Done(targetFile.absolutePath)

            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _state.value = DownloadState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun cancel() {
        _state.value = DownloadState.Idle
    }

    fun deleteModel() {
        targetFile.delete()
        partialFile.delete()
        _state.value = DownloadState.Idle
    }

    companion object {
        private const val TAG = "ModelDownload"
        private const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"
        private const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
        private const val MIN_MODEL_SIZE = 100_000_000L // 100 MB sanity check
    }
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState
    data class Done(val modelPath: String) : DownloadState
    data class Error(val message: String) : DownloadState
}

private fun File.outputStream(append: Boolean): java.io.FileOutputStream =
    java.io.FileOutputStream(this, append)
