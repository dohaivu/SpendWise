package com.spendwise.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/**
 * Report data precomputed once per snapshot/filter change, so screens can render it without
 * filtering the whole expense history during composition.
 */
data class ReportSummary(
    val monthCategoryRows: List<CategoryReportRow> = emptyList(),
    val annualCategoryRows: List<CategoryReportRow> = emptyList(),
    val annualMonthlyTotals: List<MonthlyExpenseTotal> =
        List(12) { index -> MonthlyExpenseTotal(monthNumber = index + 1, totalBaseAmountCents = 0L, expenseCount = 0) },
    /** category id -> (`year * 100 + month`) -> base amount in cents. */
    val categoryMonthlyTotals: Map<Long, Map<Int, Long>> = emptyMap()
) {
    fun categoryMonthTotal(categoryId: Long, month: LocalDate): Long =
        categoryMonthlyTotals[categoryId]?.get(month.monthKey()) ?: 0L
}

/**
 * Builds every report aggregate in a single pass over precomputed [ExpenseEntry] values.
 */
internal object ReportAggregator {

    fun summarize(
        entries: List<ExpenseEntry>,
        categories: List<Category>,
        selectedMonth: LocalDate,
        filters: TransactionFilters
    ): ReportSummary {
        val matcher = ExpenseMatcher.of(filters)
        val year = selectedMonth.year
        val monthNumber = selectedMonth.month.number
        val categoryById = categories.associateBy { it.id }

        val monthTotals = LinkedHashMap<Long, Long>()
        val yearTotals = LinkedHashMap<Long, Long>()
        val monthAmounts = LongArray(12)
        val monthCounts = IntArray(12)
        val categoryMonthlyTotals = HashMap<Long, HashMap<Int, Long>>()
        var monthAllTotal = 0L
        var yearAllTotal = 0L

        for (entry in entries) {
            if (!matcher.matches(entry)) continue

            val expense = entry.expense
            val amount = expense.baseAmountCents
            val date = entry.date

            if (date.year == year) {
                yearAllTotal += amount
                yearTotals[expense.categoryId] = (yearTotals[expense.categoryId] ?: 0L) + amount
                val monthIndex = date.month.number - 1
                monthAmounts[monthIndex] += amount
                monthCounts[monthIndex]++

                if (date.month.number == monthNumber) {
                    monthAllTotal += amount
                    monthTotals[expense.categoryId] = (monthTotals[expense.categoryId] ?: 0L) + amount
                }
            }

            val monthKey = date.monthKey()
            val perCategory = categoryMonthlyTotals.getOrPut(expense.categoryId) { HashMap() }
            perCategory[monthKey] = (perCategory[monthKey] ?: 0L) + amount
        }

        return ReportSummary(
            monthCategoryRows = buildRows(monthTotals, categoryById, monthAllTotal.coerceAtLeast(1L)),
            annualCategoryRows = buildRows(yearTotals, categoryById, yearAllTotal.coerceAtLeast(1L)),
            annualMonthlyTotals = List(12) { index ->
                MonthlyExpenseTotal(
                    monthNumber = index + 1,
                    totalBaseAmountCents = monthAmounts[index],
                    expenseCount = monthCounts[index]
                )
            },
            categoryMonthlyTotals = categoryMonthlyTotals
        )
    }

    private fun buildRows(
        totals: Map<Long, Long>,
        categoryById: Map<Long, Category>,
        denominator: Long
    ): List<CategoryReportRow> =
        totals.mapNotNull { (categoryId, amount) ->
            val category = categoryById[categoryId] ?: return@mapNotNull null
            CategoryReportRow(
                category = category,
                totalBaseAmountCents = amount,
                percentage = amount.toDouble() / denominator
            )
        }.sortedByDescending { it.totalBaseAmountCents }
}

internal fun LocalDate.monthKey(): Int = year * 100 + month.number
