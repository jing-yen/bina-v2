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
        COMPARISON_REGEX.matchEntire(key.trim())?.let { match ->
            val left = resolve(match.groupValues[1].trim())
            val op = match.groupValues[2].trim()
            val right = resolve(match.groupValues[3].trim())
            val ln = left.toDoubleOrNull()
            val rn = right.toDoubleOrNull()
            if (ln != null && rn != null) {
                return when (op) {
                    ">" -> ln > rn
                    ">=" -> ln >= rn
                    "<" -> ln < rn
                    "<=" -> ln <= rn
                    "==" -> ln == rn
                    "!=" -> ln != rn
                    else -> false
                }
            }
            return when (op) {
                "==" -> left == right
                "!=" -> left != right
                else -> false
            }
        }
        val v = state[key] ?: return false
        return v.isNotEmpty() && v != "0" && v != "false"
    }

    private fun resolve(token: String): String {
        if (token.startsWith("\"") && token.endsWith("\"")) return token.drop(1).dropLast(1)
        return state[token] ?: token
    }

    fun interpolate(template: String): String =
        VARIABLE_REGEX.replace(template) { match ->
            state[match.groupValues[1]] ?: match.value
        }

    companion object {
        private val VARIABLE_REGEX = Regex("\\{\\{(\\w+(?:\\.\\w+)?)\\}\\}")
        private val COMPARISON_REGEX = Regex("(.+?)\\s*(>=|<=|!=|==|>|<)\\s*(.+)")
    }
}
