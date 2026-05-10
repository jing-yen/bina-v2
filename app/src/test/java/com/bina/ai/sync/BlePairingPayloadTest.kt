package com.bina.ai.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BlePairingPayloadTest {

    private val sampleUuid = UUID.fromString("0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9")

    @Test fun `round trip preserves all fields`() {
        val offer = BlePairingPayload.Offer(
            serviceUuid = sampleUuid,
            recipeId = "farm_buddy",
            sizeBytes = 7116,
            recipeName = "Farm Buddy",
            authorName = "Universiti Putra Malaysia"
        )
        val encoded = BlePairingPayload.encode(offer)
        assertTrue("starts with BINA-BT:", encoded.startsWith("BINA-BT:"))
        val decoded = BlePairingPayload.decode(encoded).getOrThrow()
        assertEquals(offer, decoded)
    }

    @Test fun `decode rejects wrong magic header`() {
        val result = BlePairingPayload.decode("BINA2:abc")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects payload with missing fields`() {
        val result = BlePairingPayload.decode("BINA-BT:0a1b2c3d4e5f60718293a4b5c6d7e8f9:farm_buddy")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects malformed uuid`() {
        val result = BlePairingPayload.decode("BINA-BT:not-a-uuid:farm_buddy:1234:Rm9v")
        assertTrue(result.isFailure)
    }

    @Test fun `name and author with spaces and lowercase round trip cleanly`() {
        val offer = BlePairingPayload.Offer(
            serviceUuid = sampleUuid,
            recipeId = "weird_id",
            sizeBytes = 100,
            recipeName = "Mixed-Case Name with spaces",
            authorName = "lowercase author"
        )
        val decoded = BlePairingPayload.decode(BlePairingPayload.encode(offer)).getOrThrow()
        assertEquals(offer.recipeName, decoded.recipeName)
        assertEquals(offer.authorName, decoded.authorName)
    }

    @Test fun `encoded payload fits comfortably in a v10 byte mode QR`() {
        val offer = BlePairingPayload.Offer(
            serviceUuid = sampleUuid,
            recipeId = "long_recipe_id_for_testing",
            sizeBytes = 99999,
            recipeName = "A reasonably long recipe name",
            authorName = "An organization with a long name"
        )
        val encoded = BlePairingPayload.encode(offer)
        // QR v10 byte mode capacity is 271 bytes at level L. Pairing payloads
        // should sit well under that for any plausible name/author.
        assertTrue("encoded length ${encoded.length} should be under 200", encoded.length < 200)
    }
}
