package com.bina.ai.sync

import java.util.Base64
import java.util.UUID

/**
 * Wire format for the QR code that bootstraps a BLE recipe transfer.
 *
 * `BINA-BT:<uuid-hex>:<recipe-id>:<size>:<urlsafe-base64(name|author)>`
 *
 * The receiver decodes this, shows a confirmation sheet, then opens a BLE
 * connection to the advertised service UUID to fetch the actual recipe YAML.
 */
object BlePairingPayload {
    private const val MAGIC = "BINA-BT:"
    private const val FIELD_SEP = ":"
    private const val META_SEP = "|"

    data class Offer(
        val serviceUuid: UUID,
        val recipeId: String,
        val sizeBytes: Long,
        val recipeName: String,
        val authorName: String
    )

    fun encode(offer: Offer): String {
        val uuidHex = offer.serviceUuid.toString().replace("-", "")
        val nameAuthor = "${offer.recipeName}$META_SEP${offer.authorName}"
        val nameAuthorB64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(nameAuthor.toByteArray(Charsets.UTF_8))
        return buildString {
            append(MAGIC)
            append(uuidHex); append(FIELD_SEP)
            append(offer.recipeId); append(FIELD_SEP)
            append(offer.sizeBytes); append(FIELD_SEP)
            append(nameAuthorB64)
        }
    }

    fun decode(raw: String): Result<Offer> = runCatching {
        require(raw.startsWith(MAGIC)) { "Not a Bina pairing QR" }
        val body = raw.substring(MAGIC.length)
        val parts = body.split(FIELD_SEP, limit = 4)
        require(parts.size == 4) { "Pairing payload has wrong number of fields" }
        val (uuidHex, recipeId, sizeStr, nameAuthorB64) = parts

        val uuid = uuidFromHex(uuidHex)
        val sizeBytes = sizeStr.toLongOrNull() ?: error("Pairing payload size is not a number")
        val nameAuthor = String(
            Base64.getUrlDecoder().decode(nameAuthorB64),
            Charsets.UTF_8
        )
        val (name, author) = nameAuthor.split(META_SEP, limit = 2).let {
            require(it.size == 2) { "Pairing payload name/author missing delimiter" }
            it[0] to it[1]
        }
        Offer(
            serviceUuid = uuid,
            recipeId = recipeId,
            sizeBytes = sizeBytes,
            recipeName = name,
            authorName = author
        )
    }

    private fun uuidFromHex(hex: String): UUID {
        require(hex.length == 32) { "Service UUID must be 32 hex chars (got ${hex.length})" }
        val withDashes = buildString(36) {
            append(hex, 0, 8); append('-')
            append(hex, 8, 12); append('-')
            append(hex, 12, 16); append('-')
            append(hex, 16, 20); append('-')
            append(hex, 20, 32)
        }
        return UUID.fromString(withDashes)
    }
}
