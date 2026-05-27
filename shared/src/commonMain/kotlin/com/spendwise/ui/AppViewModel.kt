package com.spendwise.ui

import com.spendwise.domain.ExpenseDraft
import com.spendwise.domain.Category
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.components.currencyDisplayFormats
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Clock

enum class SpendWiseTab {
    Expense,
    Calendar,
    Report,
    Settings
}

enum class AppLanguage(val label: String) {
    English("English"),
    Vietnamese("Tiếng Việt"),
    Chinese("中文");

    val code: String
        get() = when (this) {
            English -> "en"
            Vietnamese -> "vi"
            Chinese -> "zh"
        }

    companion object {
        fun fromCode(code: String): AppLanguage = when (code) {
            "vi" -> Vietnamese
            "zh" -> Chinese
            else -> English
        }
    }
}

enum class TagUsageSort {
    MostUsed,
    HighestSpending,
    RecentlyUsed,
    Alphabetical
}

val supportedCurrencies = currencyDisplayFormats.map { it.code }


fun centsToAmountText(cents: Long, currencyCode: String): String {
    val fractionDigits = currencyDisplayFormat(currencyCode).fractionDigits
    val whole = cents / 100
    val fraction = cents % 100
    if (fractionDigits == 0) return whole.toString()
    return if (fraction == 0L) whole.toString() else "$whole.${fraction.toString().padStart(2, '0')}"
}

internal fun sanitizeAmountTextForCurrency(value: String, currencyCode: String): String {
    val fractionDigits = currencyDisplayFormat(currencyCode).fractionDigits
    return value.filterCurrencyAmountInput(fractionDigits)
}

internal fun emptyDraft(
    baseCurrencyCode: String,
    categories: List<Category>
): ExpenseDraft =
    ExpenseDraft(
        currencyCode = baseCurrencyCode,
        categoryId = categories.firstOrNull()?.id,
        spentAtMillis = Clock.System.now().toEpochMilliseconds()
    )

internal fun String.filterCurrencyAmountInput(fractionDigits: Int): String {
    if (fractionDigits == 0) return filter { it.isDigit() }

    val value = filter { it.isDigit() || it == '.' }
    val firstDot = value.indexOf('.')
    if (firstDot < 0) return value

    val whole = value.take(firstDot + 1)
    val fraction = value.drop(firstDot + 1).replace(".", "").take(fractionDigits)
    return whole + fraction
}

internal fun String.filterDecimalInput(): String =
    filter { it.isDigit() || it == '.' }.let { value ->
        val firstDot = value.indexOf('.')
        if (firstDot < 0) value else value.take(firstDot + 1) + value.drop(firstDot + 1).replace(".", "")
    }

internal fun String.toCentsOrNull(): Long? {
    val amount = toDoubleOrNull() ?: return null
    return (amount * 100).roundToLong()
}

internal fun Double.toExchangeRateText(): String =
    toString().trimEnd('0').trimEnd('.').ifBlank { "1" }
