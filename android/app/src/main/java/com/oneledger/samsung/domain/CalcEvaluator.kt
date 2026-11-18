package com.oneledger.samsung.domain

import kotlin.math.roundToLong

/**
 * 內建小計算機安全四則運算引擎（支援 +, -, ×, ÷ 與小數）
 */
object CalcEvaluator {
    private val tokenRegex = Regex("""(\d+(?:\.\d+)?|[+\-*/])""")

    fun evaluate(expr: String): Double? {
        val cleaned = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")
            .trimEnd('+', '-', '*', '/', '.')
        if (cleaned.isBlank()) return null

        val tokens = tokenRegex.findAll(cleaned).map { it.value }.toList()
        if (tokens.isEmpty() || tokens.joinToString("") != cleaned) return null

        val stack = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val tok = tokens[i]
            if (tok == "*" || tok == "/") {
                val prev = stack.removeLastOrNull()?.toDoubleOrNull() ?: return null
                val next = tokens.getOrNull(i + 1)?.toDoubleOrNull() ?: return null
                if (tok == "/" && next == 0.0) return null
                val res = if (tok == "*") prev * next else prev / next
                stack.add(res.toString())
                i += 2
            } else {
                stack.add(tok)
                i += 1
            }
        }

        var total = stack.firstOrNull()?.toDoubleOrNull() ?: return null
        var j = 1
        while (j + 1 < stack.size) {
            val op = stack[j]
            val value = stack[j + 1].toDoubleOrNull() ?: return null
            total = if (op == "+") total + value else total - value
            j += 2
        }

        if (!total.isFinite() || total < 0.0) return null
        return (total * 100.0).roundToLong() / 100.0
    }
}
