package com.bina.ai.install

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.bina.ai.install.model.InstallRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class InstallStoreTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: InstallStore

    @Before fun setUp() {
        val testFile = tempFolder.newFile("test_installs.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create { testFile }
        store = InstallStore(dataStore)
    }

    @Test fun `installs flow starts empty`() = runTest {
        assertTrue(store.installs.first().isEmpty())
    }

    @Test fun `install records survive a round-trip`() = runTest {
        val record = InstallRecord(
            recipeId = "farm_buddy",
            installedAt = 1_700_000_000_000L,
            enabledFeatureIds = setOf("camera_scanner", "offline_storage")
        )
        store.install(record)
        val out = store.installs.first()
        assertEquals(1, out.size)
        assertEquals(record, out["farm_buddy"])
    }

    @Test fun `installing the same recipe overwrites the previous record`() = runTest {
        val first = InstallRecord("farm_buddy", 1L, setOf("a"))
        val second = InstallRecord("farm_buddy", 2L, setOf("a", "b"))
        store.install(first)
        store.install(second)
        val out = store.installs.first()
        assertEquals(1, out.size)
        assertEquals(second, out["farm_buddy"])
    }

    @Test fun `installing different recipes keeps both`() = runTest {
        store.install(InstallRecord("farm_buddy", 1L, emptySet()))
        store.install(InstallRecord("bidan_pintar", 2L, setOf("voice_assistant")))
        val out = store.installs.first()
        assertEquals(2, out.size)
        assertTrue("farm_buddy" in out)
        assertTrue("bidan_pintar" in out)
    }

    @Test fun `uninstall removes the record`() = runTest {
        store.install(InstallRecord("farm_buddy", 1L, setOf("a")))
        store.install(InstallRecord("bidan_pintar", 2L, emptySet()))
        store.uninstall("farm_buddy")
        val out = store.installs.first()
        assertEquals(1, out.size)
        assertNull(out["farm_buddy"])
        assertTrue("bidan_pintar" in out)
    }

    @Test fun `uninstalling a recipe that was never installed is a no-op`() = runTest {
        store.install(InstallRecord("bidan_pintar", 1L, emptySet()))
        store.uninstall("not_there")
        val out = store.installs.first()
        assertEquals(1, out.size)
        assertTrue("bidan_pintar" in out)
    }

    @Test fun `isInstalled reflects current state`() = runTest {
        assertEquals(false, store.isInstalled("farm_buddy").first())
        store.install(InstallRecord("farm_buddy", 1L, emptySet()))
        assertEquals(true, store.isInstalled("farm_buddy").first())
        store.uninstall("farm_buddy")
        assertEquals(false, store.isInstalled("farm_buddy").first())
    }
}
