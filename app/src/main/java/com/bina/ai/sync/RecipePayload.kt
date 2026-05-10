package com.bina.ai.sync

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Versioned payload format for transferring a recipe YAML over a single QR code.
 *
 * Wire format: "BINA1:" + URL-safe base64 (no padding) of gzip(yaml-bytes).
 * The "BINA1:" magic header lets us reject non-Bina QR codes and bump format later.
 */
object RecipePayload {
    private const val MAGIC = "BINA1:"

    fun encode(yaml: String): String {
        val gzipped = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { it.write(yaml.toByteArray(Charsets.UTF_8)) }
            baos.toByteArray()
        }
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(gzipped)
        return MAGIC + b64
    }

    fun decode(raw: String): Result<String> = runCatching {
        require(raw.startsWith(MAGIC)) { "Not a Bina recipe QR" }
        val gzippedBytes = Base64.getUrlDecoder().decode(raw.substring(MAGIC.length))
        GZIPInputStream(ByteArrayInputStream(gzippedBytes))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }
}
