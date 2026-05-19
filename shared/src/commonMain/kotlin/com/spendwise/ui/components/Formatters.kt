package com.spendwise.ui

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue

internal fun monthTitle(date: LocalDate): String =
    "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.year}"

internal fun formatMoney(cents: Long, currencyCode: String): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = cents.absoluteValue
    return "$sign$currencyCode ${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

internal fun formatCompactMoney(cents: Long, currencyCode: String): String {
    val amount = cents / 100
    return when {
        amount >= 1_000_000 -> "$currencyCode ${amount / 1_000_000}m"
        amount >= 1_000 -> "$currencyCode ${amount / 1_000}k"
        else -> "$currencyCode $amount"
    }
}

internal fun formatCompactAmount(cents: Long): String {
    val amount = cents / 100
    return when {
        amount >= 1_000_000 -> "${amount / 1_000_000}m"
        amount >= 1_000 -> "${amount / 1_000}k"
        else -> amount.toString()
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
