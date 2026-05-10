package com.bina.ai.sync

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipePayloadTest {

    @Test fun `round trip preserves yaml`() {
        val yaml = "id: foo\nname: Foo\ndescription: Hello\n"
        val encoded = RecipePayload.encode(yaml)
        assertTrue("encoded payload starts with BINA2:", encoded.startsWith("BINA2:"))
        val decoded = RecipePayload.decode(encoded).getOrThrow()
        assertEquals(yaml, decoded)
    }

    @Test fun `decode rejects wrong magic header`() {
        val result = RecipePayload.decode("OTHER:ABC234")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects old BINA1 magic header`() {
        // BINA1: was the previous format (base64). Reject so old QRs fail
        // loud rather than silently producing garbage.
        val result = RecipePayload.decode("BINA1:H4sIAAAAAAAAAA")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects payload with no magic header`() {
        val result = RecipePayload.decode("just-some-text")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects malformed base32 body`() {
        // '!' and '0' are not in the base32 alphabet (A-Z 2-7).
        val result = RecipePayload.decode("BINA2:!!!0000!!!")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects valid base32 that is not gzip`() {
        // base32 of "hello" — valid base32 alphabet, but the bytes aren't gzip-framed.
        val result = RecipePayload.decode("BINA2:NBSWY3DP")
        assertTrue(result.isFailure)
    }

    @Test fun `encode produces shorter payload than raw yaml for repetitive content`() {
        val yaml = "field: value\n".repeat(50)
        val encoded = RecipePayload.encode(yaml)
        assertTrue("encoded is shorter than raw", encoded.length < yaml.length)
    }

    @Test fun `base32 round trip is byte-exact for arbitrary data`() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 0xFF.toByte(), 0x80.toByte(), 0x7F)
        val encoded = RecipePayload.base32Encode(data)
        val decoded = RecipePayload.base32Decode(encoded)
        assertArrayEquals(data, decoded)
    }

    @Test fun `base32 encoding uses only QR alphanumeric chars`() {
        // QR alphanumeric mode supports: 0-9 A-Z $%*+-./: <space>
        // Our base32 alphabet (A-Z 2-7) is a subset, so the QR encoder should
        // pick alphanumeric mode (5.5 bits/char vs 8 bits in byte mode).
        val data = ByteArray(64) { it.toByte() }
        val encoded = RecipePayload.base32Encode(data)
        val allowed = ('A'..'Z').toSet() + ('2'..'7').toSet()
        encoded.forEach { c -> assertTrue("char '$c' should be QR-alphanumeric-safe", c in allowed) }
    }
}
