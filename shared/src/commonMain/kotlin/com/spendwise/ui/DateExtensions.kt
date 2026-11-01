package com.spendwise.ui

import com.spendwise.domain.Expense
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

internal fun LocalDate.toUtcStartMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun Long.toUtcLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

internal fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).date

internal fun Expense.spentDate(timeZone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date

internal fun LocalDate.isSameMonth(month: LocalDate): Boolean =
    year == month.year && this.month == month.month

internal fun LocalDate.compactDateWithDayName(): String =
    "${month.number}.${day} (${dayOfWeek.shortName()})"

internal fun Map<LocalDate, Int>.firstHeaderIndexForMonth(month: LocalDate): Int? =
    entries
        .filter { (date, _) -> date.isSameMonth(month) }
        .maxByOrNull { (date, _) -> date }
        ?.value

internal fun Month.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

internal fun DayOfWeek.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

internal fun LocalDate.shortMonthName(): String =
    month.shortName()

internal fun LocalDate.monthAxisLabel(): String {
    val monthLabel = shortMonthName()
    return if (month.number == 1) "$monthLabel $year" else monthLabel
}

fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalDate.firstDayOfMonth(): LocalDate = LocalDate(year, month, 1)

internal fun Long.formatDateTime(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.day}/${dt.month.number}/${dt.year} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}

/**
 * Returns a distinct color for each month (1 = Jan, 12 = Dec),
 * arranged so that adjacent months alternate hues and contrasting tones.
 */
internal fun monthColor(monthNumber: Int): androidx.compose.ui.graphics.Color {
    return when ((((monthNumber - 1) % 12) + 12) % 12) {
        0 -> androidx.compose.ui.graphics.Color(0xFF0288D1)  // Jan: Ice Blue
        1 -> androidx.compose.ui.graphics.Color(0xFFD81B60)  // Feb: Rose Pink
        2 -> androidx.compose.ui.graphics.Color(0xFF43A047)  // Mar: Emerald Green
        3 -> androidx.compose.ui.graphics.Color(0xFFFB8C00)  // Apr: Vibrant Orange
        4 -> androidx.compose.ui.graphics.Color(0xFF5E35B1)  // May: Deep Violet
        5 -> androidx.compose.ui.graphics.Color(0xFF7CB342)  // Jun: Fresh Lime
        6 -> androidx.compose.ui.graphics.Color(0xFFE53935)  // Jul: Bright Red
        7 -> androidx.compose.ui.graphics.Color(0xFF00ACC1)  // Aug: Deep Teal
        8 -> androidx.compose.ui.graphics.Color(0xFFFDD835)  // Sep: Amber Gold
        9 -> androidx.compose.ui.graphics.Color(0xFF3949AB)  // Oct: Royal Indigo
        10 -> androidx.compose.ui.graphics.Color(0xFFD84315) // Nov: Burnt Terracotta
        11 -> androidx.compose.ui.graphics.Color(0xFF8E24AA) // Dec: Magenta Purple
        else -> androidx.compose.ui.graphics.Color(0xFF0288D1)
    }
}
