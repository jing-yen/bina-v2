package com.bina.ai.miniapp.runtime

import com.bina.ai.miniapp.model.VariableDef

class VariableStore(
    defs: Map<String, VariableDef>,
    private val state: MutableMap<String, String> = mutableMapOf()
) {

    var onChange: (() -> Unit)? = null

    init {
        defs.forEach { (name, def) -> state[name] = def.default }
        if ("ai_response" !in state) state["ai_response"] = ""
        if ("is_loading" !in state) state["is_loading"] = "false"
    }

    operator fun get(key: String): String = state[key] ?: ""

    operator fun set(key: String, value: String) {
        state[key] = value
        onChange?.invoke()
    }

    fun getNumber(key: String): Double = state[key]?.toDoubleOrNull() ?: 0.0

    fun isTrue(key: String): Boolean {
        val v = state[key] ?: return false
        return v.isNotEmpty() && v != "0" && v != "false"
    }

    fun interpolate(template: String): String =
        VARIABLE_REGEX.replace(template) { match ->
            state[match.groupValues[1]] ?: match.value
        }

    companion object {
        private val VARIABLE_REGEX = Regex("\\{\\{(\\w+(?:\\.\\w+)?)\\}\\}")
    }
}
