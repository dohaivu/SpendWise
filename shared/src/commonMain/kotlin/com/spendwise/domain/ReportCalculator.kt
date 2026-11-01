package com.spendwise.domain

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
        val categoryById = categories.associateBy { it.id }
        return expenses
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, rows) ->
                val category = categoryById[categoryId] ?: return@mapNotNull null
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
        val amounts = LongArray(12)
        val counts = IntArray(12)
        for (expense in expenses) {
            val date = expense.localDate(timeZone)
            if (date.year != year) continue
            val index = date.month.number - 1
            amounts[index] += expense.baseAmountCents
            counts[index]++
        }
        return List(12) { index ->
            MonthlyExpenseTotal(
                monthNumber = index + 1,
                totalBaseAmountCents = amounts[index],
                expenseCount = counts[index]
            )
        }
    }

    fun Expense.localDate(timeZone: TimeZone): LocalDate = kotlin.time.Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date
}
