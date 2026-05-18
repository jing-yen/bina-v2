package com.bina.ai.miniapp.runtime

import com.bina.ai.miniapp.model.VariableDef

class VariableStore(
    defs: Map<String, VariableDef>,
    private val state: MutableMap<String, String> = mutableMapOf()
) {

    var onChange: (() -> Unit)? = null

    init {
        defs.forEach { (name, def) ->
            // Preserve persisted values; only apply default for new or transient variables
            if (name !in state || name in TRANSIENT_VARS) {
                state[name] = def.default
            }
        }
        // Always reset transient runtime state regardless of YAML definitions
        state["ai_response"] = ""
        state["is_loading"] = "false"
        state.remove("photo_path")
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

        /**
         * Variables that are session-scoped and must always reset on re-open,
         * even if they were somehow persisted. These are also excluded from
         * SharedPreferences writes in MiniAppScreen's onChange handler.
         */
        val TRANSIENT_VARS = setOf("ai_response", "is_loading", "photo_path", "user_text")
    }
}
