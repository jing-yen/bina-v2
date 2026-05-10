package com.bina.ai.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipePayloadTest {

    @Test fun `round trip preserves yaml`() {
        val yaml = "id: foo\nname: Foo\ndescription: Hello\n"
        val encoded = RecipePayload.encode(yaml)
        assertTrue("encoded payload starts with BINA1:", encoded.startsWith("BINA1:"))
        val decoded = RecipePayload.decode(encoded).getOrThrow()
        assertEquals(yaml, decoded)
    }

    @Test fun `decode rejects wrong magic header`() {
        val result = RecipePayload.decode("OTHER:abc123")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects payload with no magic header`() {
        val result = RecipePayload.decode("just-some-text")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects malformed base64 body`() {
        val result = RecipePayload.decode("BINA1:!!!not-valid-base64!!!")
        assertTrue(result.isFailure)
    }

    @Test fun `decode rejects valid base64 that is not gzip`() {
        // Base64 of "hello" — valid base64, but not gzipped
        val result = RecipePayload.decode("BINA1:aGVsbG8")
        assertTrue(result.isFailure)
    }

    @Test fun `encode produces shorter payload than raw yaml for repetitive content`() {
        val yaml = "field: value\n".repeat(50)
        val encoded = RecipePayload.encode(yaml)
        // Magic + base64(gzip(repetitive)) should compress well
        assertTrue("encoded is shorter than raw", encoded.length < yaml.length)
    }
}
