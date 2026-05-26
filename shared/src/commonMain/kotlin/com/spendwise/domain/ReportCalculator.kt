package com.spendwise.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
        categories: List<Category>
    ): List<CategoryReportRow> {
        val total = expenses.sumOf { it.baseAmountCents }.coerceAtLeast(1L)
        return expenses
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

    fun monthlyTotals(
        expenses: List<Expense>,
        year: Int,
        timeZone: TimeZone
    ): List<MonthlyExpenseTotal> {
        val filtered = expenses
            .filter { expense -> expense.localDate(timeZone).year == year }
        return (1..12).map { month ->
            val monthExpenses = filtered.filter { expense -> expense.localDate(timeZone).month.number == month }
            MonthlyExpenseTotal(
                monthNumber = month,
                totalBaseAmountCents = monthExpenses.sumOf { it.baseAmountCents },
                expenseCount = monthExpenses.size
            )
        }
    }

    fun Expense.localDate(timeZone: TimeZone): LocalDate =
        Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date

    fun List<Expense>.filterByTags(selectedTags: Set<String>): List<Expense> {
        if (selectedTags.isEmpty()) return this
        val normalized = selectedTags.map(TagParser::normalize).filter { it.isNotBlank() }.toSet()
        if (normalized.isEmpty()) return this
        return filter { expense -> expense.tags.map(TagParser::normalize).any { it in normalized } }
    }

    fun LocalDate.monthKey(): Int = year * 100 + month.number
}
