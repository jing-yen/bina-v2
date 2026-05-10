package com.bina.ai.ui.screens.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.sync.BlePairingPayload
import com.bina.ai.sync.RecipeImporter
import com.bina.ai.sync.RecipePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SyncViewModelTest {

    @get:Rule val tmp = TemporaryFolder()

    private val sampleYaml = """
        id: t1
        name: T1
        description: test
        category: Test
        version: "1.0"
    """.trimIndent()

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun newVm(): SyncViewModel {
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { tmp.newFile("ds.preferences_pb") })
        val installStore = InstallStore(dataStore)
        val repo = MiniAppRepository(loadYamlFiles = { emptyList() })
        val importer = RecipeImporter(filesDir = tmp.root, miniAppRepository = repo)
        return SyncViewModel(repo, installStore, importer)
    }

    @Test fun `incoming starts Idle`() = runTest {
        val vm = newVm()
        assertEquals(IncomingState.Idle, vm.incoming.value)
    }

    @Test fun `handlePastedYaml with valid yaml transitions to Ready`() = runTest {
        val vm = newVm()
        vm.handlePastedYaml(sampleYaml)
        val state = vm.incoming.value
        assertTrue("got $state", state is IncomingState.Ready)
        assertEquals("t1", (state as IncomingState.Ready).miniApp.id)
    }

    @Test fun `handlePastedYaml with garbage transitions to Error`() = runTest {
        val vm = newVm()
        vm.handlePastedYaml("not yaml { ")
        val state = vm.incoming.value
        assertTrue("got $state", state is IncomingState.Error)
    }

    @Test fun `handleScannedQr with valid encoded payload transitions to Ready`() = runTest {
        val vm = newVm()
        val encoded = RecipePayload.encode(sampleYaml)
        vm.handleScannedQr(encoded)
        assertTrue(vm.incoming.value is IncomingState.Ready)
    }

    @Test fun `handleScannedQr with non-Bina QR transitions to Error`() = runTest {
        val vm = newVm()
        vm.handleScannedQr("https://example.com/some-qr")
        assertTrue(vm.incoming.value is IncomingState.Error)
    }

    @Test fun `dismissPreview returns to Idle`() = runTest {
        val vm = newVm()
        vm.handlePastedYaml(sampleYaml)
        vm.dismissPreview()
        assertEquals(IncomingState.Idle, vm.incoming.value)
    }

    @Test fun `handleScannedQr with BINA-BT pairing payload sets pairing offer`() = runTest {
        val vm = newVm()
        val offer = BlePairingPayload.Offer(
            serviceUuid = java.util.UUID.randomUUID(),
            recipeId = "t1",
            sizeBytes = 100,
            recipeName = "Test",
            authorName = "Test Author"
        )
        vm.handleScannedQr(BlePairingPayload.encode(offer))
        val pairing = vm.pairing.value
        org.junit.Assert.assertNotNull(pairing)
        org.junit.Assert.assertEquals("t1", pairing!!.recipeId)
    }
}
