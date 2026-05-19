package com.spendwise.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

object ReportCalculator {
    fun dailyTotals(expenses: List<Expense>, timeZone: TimeZone): List<DailyExpenseTotal> {
        return expenses
            .groupBy { it.localDate(timeZone) }
            .map { (date, rows) ->
                DailyExpenseTotal(
                    date = date,
                    totalBaseAmountCents = rows.sumOf { it.baseAmountCents },
                    expenseCount = rows.size
                )
            }
            .sortedBy { it.date }
    }

    fun categoryReport(
        expenses: List<Expense>,
        categories: List<Category>,
        selectedTags: Set<String>
    ): List<CategoryReportRow> {
        val filtered = expenses.filterByTags(selectedTags)
        val total = filtered.sumOf { it.baseAmountCents }.coerceAtLeast(1L)
        return filtered
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, rows) ->
                val category = categories.firstOrNull { it.id == categoryId } ?: return@mapNotNull null
                val amount = rows.sumOf { it.baseAmountCents }
                CategoryReportRow(
                    category = category,
                    totalBaseAmountCents = amount,
                    percentage = amount.toDouble() / total
                )
            }
            .sortedByDescending { it.totalBaseAmountCents }
    }

    fun monthOverMonth(
        expenses: List<Expense>,
        categories: List<Category>,
        selectedMonth: LocalDate,
        selectedTags: Set<String>,
        timeZone: TimeZone
    ): List<MonthComparisonRow> {
        val currentMonthStart = LocalDate(selectedMonth.year, selectedMonth.month, 1)
        val previousMonthStart = currentMonthStart.minus(1, DateTimeUnit.MONTH)

        val current = expenses
            .filterByTags(selectedTags)
            .filter { it.localDate(timeZone).year == currentMonthStart.year && it.localDate(timeZone).month == currentMonthStart.month }
            .groupBy { it.categoryId }
            .mapValues { (_, rows) -> rows.sumOf { it.baseAmountCents } }

        val previous = expenses
            .filterByTags(selectedTags)
            .filter { it.localDate(timeZone).year == previousMonthStart.year && it.localDate(timeZone).month == previousMonthStart.month }
            .groupBy { it.categoryId }
            .mapValues { (_, rows) -> rows.sumOf { it.baseAmountCents } }

        return (current.keys + previous.keys)
            .mapNotNull { categoryId ->
                val category = categories.firstOrNull { it.id == categoryId } ?: return@mapNotNull null
                MonthComparisonRow(
                    category = category,
                    currentMonthAmountCents = current[categoryId] ?: 0L,
                    previousMonthAmountCents = previous[categoryId] ?: 0L
                )
            }
            .filter { it.currentMonthAmountCents != 0L || it.previousMonthAmountCents != 0L }
            .sortedByDescending { kotlin.math.abs(it.changeAmountCents) }
    }

    fun Expense.localDate(timeZone: TimeZone): LocalDate =
        Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date

    fun List<Expense>.filterByTags(selectedTags: Set<String>): List<Expense> {
        if (selectedTags.isEmpty()) return this
        val normalized = selectedTags.map(TagParser::normalize).toSet()
        return filter { expense -> normalized.all { it in expense.tags.map(TagParser::normalize) } }
    }

    fun LocalDate.monthKey(): Int = year * 100 + month.number
}
