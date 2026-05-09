package com.bina.ai.miniapp.runtime

import com.bina.ai.miniapp.model.FormulaDef
import kotlin.math.pow

class FormulaEngine(private val formulas: Map<String, FormulaDef>) {

    fun evaluate(formulaId: String, store: VariableStore) {
        val formula = formulas[formulaId] ?: return
        val expr = store.interpolate(formula.expression)
        try {
            val result = evalExpression(expr)
            store[formula.output] = result.toString()
        } catch (e: Exception) {
            store[formula.output] = "Error"
        }
    }

    private fun evalExpression(expr: String): Double {
        val tokens = tokenize(expr)
        val pos = intArrayOf(0)
        return parseExpr(tokens, pos)
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            when {
                expr[i].isWhitespace() -> i++
                expr[i] in "+-*/%()^" -> { tokens.add(expr[i].toString()); i++ }
                expr[i].isDigit() || expr[i] == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens.add(expr.substring(start, i))
                }
                else -> i++
            }
        }
        return tokens
    }

    private fun parseExpr(tokens: List<String>, pos: IntArray): Double {
        var left = parseTerm(tokens, pos)
        while (pos[0] < tokens.size && tokens[pos[0]] in listOf("+", "-")) {
            val op = tokens[pos[0]++]
            val right = parseTerm(tokens, pos)
            left = if (op == "+") left + right else left - right
        }
        return left
    }

    private fun parseTerm(tokens: List<String>, pos: IntArray): Double {
        var left = parsePower(tokens, pos)
        while (pos[0] < tokens.size && tokens[pos[0]] in listOf("*", "/", "%")) {
            val op = tokens[pos[0]++]
            val right = parsePower(tokens, pos)
            left = when (op) {
                "*" -> left * right
                "/" -> if (right != 0.0) left / right else 0.0
                "%" -> left % right
                else -> left
            }
        }
        return left
    }

    private fun parsePower(tokens: List<String>, pos: IntArray): Double {
        val base = parseUnary(tokens, pos)
        if (pos[0] < tokens.size && tokens[pos[0]] == "^") {
            pos[0]++
            val exp = parseUnary(tokens, pos)
            return base.pow(exp)
        }
        return base
    }

    private fun parseUnary(tokens: List<String>, pos: IntArray): Double {
        if (pos[0] < tokens.size && tokens[pos[0]] == "-") {
            pos[0]++
            return -parseAtom(tokens, pos)
        }
        return parseAtom(tokens, pos)
    }

    private fun parseAtom(tokens: List<String>, pos: IntArray): Double {
        if (pos[0] >= tokens.size) return 0.0
        val token = tokens[pos[0]]
        if (token == "(") {
            pos[0]++
            val value = parseExpr(tokens, pos)
            if (pos[0] < tokens.size && tokens[pos[0]] == ")") pos[0]++
            return value
        }
        pos[0]++
        return token.toDoubleOrNull() ?: 0.0
    }
}
