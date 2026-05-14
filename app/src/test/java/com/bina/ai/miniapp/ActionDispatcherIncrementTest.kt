package com.bina.ai.miniapp

import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.miniapp.model.VariableDef
import com.bina.ai.miniapp.runtime.ActionDispatcher
import com.bina.ai.miniapp.runtime.FormulaEngine
import com.bina.ai.miniapp.runtime.VariableStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionDispatcherIncrementTest {

    private fun createDispatcher(store: VariableStore, miniApp: MiniApp): ActionDispatcher {
        return ActionDispatcher(
            store = store,
            miniApp = miniApp,
            formulaEngine = FormulaEngine(emptyMap()),
            onNavigate = {}
        )
    }

    @Test
    fun `increment action increases variable by 1`() = runBlocking {
        val store = VariableStore(
            mapOf("step" to VariableDef(type = "number", default = "0"))
        )
        val miniApp = MiniApp(id = "test", name = "Test")
        val dispatcher = createDispatcher(store, miniApp)

        assertEquals("0", store["step"])

        dispatcher.dispatch("increment:step")
        assertEquals("1", store["step"])

        dispatcher.dispatch("increment:step")
        assertEquals("2", store["step"])

        dispatcher.dispatch("increment:step")
        assertEquals("3", store["step"])
    }

    @Test
    fun `increment on unset variable starts from 0`() = runBlocking {
        val store = VariableStore(emptyMap())
        val miniApp = MiniApp(id = "test", name = "Test")
        val dispatcher = createDispatcher(store, miniApp)

        dispatcher.dispatch("increment:new_var")
        assertEquals("1", store["new_var"])
    }
}
