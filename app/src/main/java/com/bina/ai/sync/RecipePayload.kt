package com.bina.ai.sync

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Versioned payload format for transferring a recipe YAML over a single QR code.
 *
 * Wire format: "BINA2:" + RFC 4648 base32 (uppercase, no padding) of gzip(yaml-bytes).
 *
 * Why base32 over base64: base32's alphabet is a subset of QR's alphanumeric mode
 * alphabet, so ZXing auto-encodes the QR in alphanumeric mode (5.5 bits/char) instead
 * of byte mode (8 bits/char). That's a ~30% capacity gain — necessary for ~7 KB
 * recipes after gzip to fit in a single QR.
 *
 * The "BINA2:" magic header lets us reject non-Bina QR codes and bump format again later.
 */
object RecipePayload {
    private const val MAGIC = "BINA2:"
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(yaml: String): String {
        val gzipped = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { it.write(yaml.toByteArray(Charsets.UTF_8)) }
            baos.toByteArray()
        }
        return MAGIC + base32Encode(gzipped)
    }

    fun decode(raw: String): Result<String> = runCatching {
        require(raw.startsWith(MAGIC)) { "Not a Bina recipe QR" }
        val gzippedBytes = base32Decode(raw.substring(MAGIC.length))
        GZIPInputStream(ByteArrayInputStream(gzippedBytes))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    /** RFC 4648 base32 encode without padding. */
    internal fun base32Encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val sb = StringBuilder()
        var buffer = 0L
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF).toLong()
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                sb.append(ALPHABET[((buffer shr bitsLeft) and 0x1F).toInt()])
            }
        }
        if (bitsLeft > 0) {
            sb.append(ALPHABET[((buffer shl (5 - bitsLeft)) and 0x1F).toInt()])
        }
        return sb.toString()
    }

    /** RFC 4648 base32 decode (uppercase; trailing `=` padding tolerated). */
    internal fun base32Decode(input: String): ByteArray {
        val cleaned = input.uppercase().trimEnd('=')
        if (cleaned.isEmpty()) return ByteArray(0)
        val out = ByteArrayOutputStream()
        var buffer = 0L
        var bitsLeft = 0
        for (c in cleaned) {
            val v = ALPHABET.indexOf(c)
            require(v >= 0) { "Invalid base32 character: $c" }
            buffer = (buffer shl 5) or v.toLong()
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.write(((buffer shr bitsLeft) and 0xFF).toInt())
            }
        }
        return out.toByteArray()
    }
}
