package com.bina.ai.ui.screens.configurator

import com.bina.ai.install.CapabilityChecker
import com.bina.ai.miniapp.model.Feature
import com.bina.ai.miniapp.model.MiniApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfiguratorViewModelTest {

    private val checker = CapabilityChecker.forTest(hardwareSupport = mapOf("gps" to true))

    private val recipe = MiniApp(
        id = "farm_buddy",
        name = "Farm Buddy",
        features = listOf(
            Feature(id = "cam", name = "Cam",   recommended = true,  sizeKb = 0.4f, requires = listOf("permission:camera")),
            Feature(id = "voice", name = "Voice", recommended = true,  sizeKb = 0.3f, requires = listOf("permission:microphone")),
            Feature(id = "sms", name = "SMS",   recommended = false, sizeKb = 0.2f, requires = listOf("service:sms_dispatch")),
            Feature(id = "p2p", name = "P2P",   recommended = true,  sizeKb = 0.2f, requires = listOf("service:p2p"))   // recommended-but-unavailable
        )
    )

    @Test fun `initial state turns on recommended-and-available, leaves others off`() {
        val state = ConfiguratorState.initial(recipe, checker)
        assertTrue(state.toggles["cam"] == true)
        assertTrue(state.toggles["voice"] == true)
        assertFalse(state.toggles["sms"] == true)
        assertFalse(state.toggles["p2p"] == true)   // recommended but unavailable
    }

    @Test fun `availability reflects checker for each feature`() {
        val state = ConfiguratorState.initial(recipe, checker)
        assertTrue(state.availability["cam"] == true)
        assertTrue(state.availability["voice"] == true)
        assertFalse(state.availability["sms"] == true)
        assertFalse(state.availability["p2p"] == true)
    }

    @Test fun `totalCount equals features size including unavailable ones`() {
        val state = ConfiguratorState.initial(recipe, checker)
        assertEquals(4, state.totalCount)
    }

    @Test fun `activeCount counts only enabled toggles`() {
        val state = ConfiguratorState.initial(recipe, checker)
        assertEquals(2, state.activeCount)
    }

    @Test fun `totalSizeKb sums base plus enabled-feature sizes`() {
        val state = ConfiguratorState.initial(recipe, checker)
        assertEquals(1.9f, state.totalSizeKb(baseSizeKb = 1.2f), 0.001f)
    }

    @Test fun `withToggle flips a feature on or off and returns a new state`() {
        val state = ConfiguratorState.initial(recipe, checker)
        val turnedOff = state.withToggle("cam", on = false)
        assertEquals(false, turnedOff.toggles["cam"])
        assertEquals(1, turnedOff.activeCount)
        val turnedBackOn = turnedOff.withToggle("cam", on = true)
        assertEquals(true, turnedBackOn.toggles["cam"])
        assertEquals(2, turnedBackOn.activeCount)
    }

    @Test fun `enabledFeatureIds returns the set of currently-on features`() {
        val state = ConfiguratorState.initial(recipe, checker)
        assertEquals(setOf("cam", "voice"), state.enabledFeatureIds)
    }
}
