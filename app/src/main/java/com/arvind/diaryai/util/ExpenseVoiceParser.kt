package com.arvind.diaryai.util

import com.arvind.diaryai.data.Schema

data class ParsedExpense(
    val amount: String,
    val cashOnline: String,
    val whose: String,
    val category: String,
    val comments: String
)

/**
 * Turns a spoken phrase like:
 *   "minus 500 restaurant cash arv lunch with friends"
 *   "plus 30000 money in job earning online"
 * into structured fields. Numeric sign convention: '-'/'minus' = money out (expense),
 * '+'/'plus'/bare number = money in, matching the form's "InEx (-out, +In)" convention.
 * This is heuristic — the review screen always lets the user correct any field before saving.
 */
object ExpenseVoiceParser {

    fun parse(rawSpeech: String): ParsedExpense {
        var text = rawSpeech.lowercase().trim()
        text = text.replace("minus", "-").replace("plus", "+")

        val amountMatch = Regex("[-+]?\\d+(\\.\\d+)?").find(text)
        var amount = amountMatch?.value ?: "0"
        if (!amount.startsWith("-") && !amount.startsWith("+")) amount = amount // ambiguous sign left to user to confirm
        text = amountMatch?.let { text.removeRange(it.range) } ?: text

        val cashOnline = Schema.EXPENSE_CASH_ONLINE.firstOrNull { text.contains(it.lowercase()) } ?: "Cash"
        val whose = Schema.EXPENSE_WHOSE.firstOrNull { text.contains(it.lowercase()) } ?: "Arv"
        val category = Schema.EXPENSE_CATEGORIES
            .sortedByDescending { it.length } // match longest/most specific category phrase first
            .firstOrNull { text.contains(it.lowercase().substringBefore(" -")) } ?: "Other"

        var comments = text
        listOf(cashOnline, whose, category).forEach { comments = comments.replace(it.lowercase(), "") }
        comments = comments.trim().replace(Regex("\\s{2,}"), " ")

        return ParsedExpense(amount, cashOnline, whose, category, comments)
    }
}
