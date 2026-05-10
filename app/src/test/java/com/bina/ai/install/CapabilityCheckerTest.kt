package com.bina.ai.install

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityCheckerTest {
    private val checker = CapabilityChecker.forTest(
        hardwareSupport = mapOf("gps" to true, "camera" to true)
    )

    @Test fun `permission tokens are always available`() {
        assertTrue(checker.isAvailable("permission:camera"))
        assertTrue(checker.isAvailable("permission:microphone"))
        assertTrue(checker.isAvailable("permission:location"))
        assertTrue(checker.isAvailable("permission:sms"))
        assertTrue(checker.isAvailable("permission:notifications"))
    }

    @Test fun `service tokens are always unavailable`() {
        assertFalse(checker.isAvailable("service:p2p"))
        assertFalse(checker.isAvailable("service:smart_notifications"))
        assertFalse(checker.isAvailable("service:sms_dispatch"))
        assertFalse(checker.isAvailable("service:anything_else"))
    }

    @Test fun `hardware tokens reflect supplied support map`() {
        assertTrue(checker.isAvailable("hardware:gps"))
        assertTrue(checker.isAvailable("hardware:camera"))
        assertFalse(checker.isAvailable("hardware:nfc"))
    }

    @Test fun `unknown tokens default to true (lenient)`() {
        assertTrue(checker.isAvailable("foo:bar"))
        assertTrue(checker.isAvailable("garbage"))
    }

    @Test fun `feature with all-available requires is available`() {
        val feature = listOf("permission:camera", "permission:microphone")
        assertTrue(feature.all { checker.isAvailable(it) })
    }

    @Test fun `feature with any unavailable require is NOT available`() {
        val feature = listOf("permission:camera", "service:p2p")
        assertFalse(feature.all { checker.isAvailable(it) })
    }
}
