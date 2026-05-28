package com.spendwise.ui.components

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue

enum class CurrencySymbolPosition {
    Prefix,
    Suffix
}

data class CurrencyDisplayFormat(
    val code: String,
    val name: String,
    val symbol: String,
    val fractionDigits: Int,
    val symbolPosition: CurrencySymbolPosition,
    val groupSeparator: Char = ',',
    val decimalSeparator: Char = '.',
    val spaceBetweenSymbolAndAmount: Boolean = false
) {
    fun format(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val amount = cents.absoluteValue
        val whole = amount / 100
        val fraction = amount % 100
        val number = buildString {
            append(groupWholePart(whole, groupSeparator))
            if (fractionDigits > 0) {
                append(decimalSeparator)
                append(fraction.toString().padStart(2, '0').take(fractionDigits))
            }
        }
        val gap = if (spaceBetweenSymbolAndAmount) " " else ""
        return when (symbolPosition) {
            CurrencySymbolPosition.Prefix -> "$sign$symbol$gap$number"
            CurrencySymbolPosition.Suffix -> "$sign$number$gap$symbol"
        }
    }

    fun formatCompact(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val whole = cents.absoluteValue / 100
        val compact = when {
            whole >= 1_000_000 -> "${whole / 1_000_000}m"
            whole >= 1_000 -> "${whole / 1_000}k"
            else -> whole.toString()
        }
        val gap = if (spaceBetweenSymbolAndAmount) " " else ""
        return when (symbolPosition) {
            CurrencySymbolPosition.Prefix -> "$sign$symbol$gap$compact"
            CurrencySymbolPosition.Suffix -> "$sign$compact$gap$symbol"
        }
    }
}

internal val currencyDisplayFormats = listOf(
    CurrencyDisplayFormat(
        code = "USD",
        name = "US Dollar",
        symbol = "$",
        fractionDigits = 2,
        symbolPosition = CurrencySymbolPosition.Prefix,
        groupSeparator = ',',
        decimalSeparator = '.'
    ),
    CurrencyDisplayFormat(
        code = "VND",
        name = "Vietnamese Dong",
        symbol = "₫",
        fractionDigits = 0,
        symbolPosition = CurrencySymbolPosition.Suffix,
        groupSeparator = '.',
        decimalSeparator = ','
    ),
    CurrencyDisplayFormat(
        code = "CNY",
        name = "Chinese Yuan",
        symbol = "¥",
        fractionDigits = 2,
        symbolPosition = CurrencySymbolPosition.Prefix,
        groupSeparator = ',',
        decimalSeparator = '.'
    ),
    CurrencyDisplayFormat(
        code = "EUR",
        name = "Euro",
        symbol = "€",
        fractionDigits = 2,
        symbolPosition = CurrencySymbolPosition.Suffix,
        groupSeparator = '.',
        decimalSeparator = ',',
        spaceBetweenSymbolAndAmount = true
    ),
    CurrencyDisplayFormat(
        code = "JPY",
        name = "Japanese Yen",
        symbol = "¥",
        fractionDigits = 0,
        symbolPosition = CurrencySymbolPosition.Prefix,
        groupSeparator = ',',
        decimalSeparator = '.'
    ),
    CurrencyDisplayFormat(
        code = "SGD",
        name = "Singapore Dollar",
        symbol = "S$",
        fractionDigits = 2,
        symbolPosition = CurrencySymbolPosition.Prefix,
        groupSeparator = ',',
        decimalSeparator = '.'
    )
)

internal fun currencyDisplayFormat(currencyCode: String): CurrencyDisplayFormat =
    currencyDisplayFormats.firstOrNull { it.code == currencyCode }
        ?: CurrencyDisplayFormat(
            code = currencyCode,
            name = currencyCode,
            symbol = currencyCode,
            fractionDigits = 2,
            symbolPosition = CurrencySymbolPosition.Prefix,
            spaceBetweenSymbolAndAmount = true
        )

internal fun monthTitle(date: LocalDate): String =
    "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.year}"

internal fun formatMoney(cents: Long, currencyCode: String): String {
    return formatMoney(cents, currencyDisplayFormat(currencyCode))
}

internal fun formatMoney(cents: Long, format: CurrencyDisplayFormat): String {
    return format.format(cents)
}

internal fun formatMoneyValue(cents: Long, currencyCode: String): String {
    return formatMoneyValue(cents, currencyDisplayFormat(currencyCode))
}

internal fun formatMoneyValue(cents: Long, format: CurrencyDisplayFormat): String {
    val sign = if (cents < 0) "-" else ""
    val amount = cents.absoluteValue
    val whole = amount / 100
    val fraction = amount % 100
    return buildString {
        append(sign)
        append(groupWholePart(whole, format.groupSeparator))
        if (format.fractionDigits > 0) {
            append(format.decimalSeparator)
            append(fraction.toString().padStart(2, '0').take(format.fractionDigits))
        }
    }
}

internal fun formatCompactMoney(cents: Long, currencyCode: String): String {
    return formatCompactMoney(cents, currencyDisplayFormat(currencyCode))
}

internal fun formatCompactMoney(cents: Long, format: CurrencyDisplayFormat): String {
    return format.formatCompact(cents)
}

internal fun formatCompactAmount(cents: Long, displayMillions: Boolean = true): String {
    val amount = cents / 100
    return when {
        displayMillions && amount >= 1_000_000 -> "${amount / 1_000_000}m"
        amount >= 1_000 -> "${amount / 1_000}k"
        else -> amount.toString()
    }
}

internal fun formatAmount(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = cents.absoluteValue
    val whole = absolute / 100
    val fraction = absolute % 100
    return if (fraction == 0L) {
        "$sign$whole"
    } else {
        "$sign$whole.${fraction.toString().padStart(2, '0')}"
    }
}

internal fun signedMoney(cents: Long, currencyCode: String): String {
    val prefix = if (cents >= 0) "+" else "-"
    return prefix + formatMoney(cents.absoluteValue, currencyCode)
}

internal fun formatDate(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${date.year}-${date.month.number.toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
}

private fun groupWholePart(value: Long, separator: Char): String {
    val raw = value.toString()
    return raw.reversed()
        .chunked(3)
        .joinToString(separator.toString())
        .reversed()
}
