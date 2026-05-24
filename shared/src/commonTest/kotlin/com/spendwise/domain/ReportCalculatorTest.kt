package com.spendwise.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportCalculatorTest {
    @Test
    fun categoryReportAppliesTagFilters() {
        val categories = listOf(foodCategory, travelCategory)
        val expenses = listOf(
            expense(id = 1, categoryId = 1, amount = 1_200, tags = listOf("work")),
            expense(id = 2, categoryId = 2, amount = 3_400, tags = listOf("trip")),
            expense(id = 3, categoryId = 1, amount = 800, tags = listOf("work", "trip"))
        )

        val rows = ReportCalculator.categoryReport(expenses, categories, selectedTags = setOf("work"))

        assertEquals(1, rows.size)
        assertEquals(2_000, rows.first().totalBaseAmountCents)
    }

    @Test
    fun filterByTagsMatchesAnySelectedTag() {
        val expenses = listOf(
            expense(id = 1, categoryId = 1, amount = 1_200, tags = listOf("work")),
            expense(id = 2, categoryId = 2, amount = 3_400, tags = listOf("trip")),
            expense(id = 3, categoryId = 1, amount = 800, tags = listOf("work", "trip")),
            expense(id = 4, categoryId = 1, amount = 600, tags = listOf("personal"))
        )

        val filtered = with(ReportCalculator) {
            expenses.filterByTags(setOf("work", "trip"))
        }

        assertEquals(listOf(1L, 2L, 3L), filtered.map { it.id })
    }

    @Test
    fun monthlyTotalsReturnsAllMonthsAndAppliesYearAndTagFilters() {
        val expenses = listOf(
            expense(id = 1, categoryId = 1, amount = 1_200, tags = listOf("work"), spentAtMillis = dateMillis(2026, 1, 10)),
            expense(id = 2, categoryId = 1, amount = 800, tags = listOf("work"), spentAtMillis = dateMillis(2026, 1, 20)),
            expense(id = 3, categoryId = 1, amount = 3_400, tags = listOf("trip"), spentAtMillis = dateMillis(2026, 2, 5)),
            expense(id = 4, categoryId = 1, amount = 5_600, tags = listOf("work"), spentAtMillis = dateMillis(2025, 1, 10))
        )

        val rows = ReportCalculator.monthlyTotals(
            expenses = expenses,
            year = 2026,
            selectedTags = setOf("work"),
            timeZone = TimeZone.UTC
        )

        assertEquals(12, rows.size)
        assertEquals(2_000, rows[0].totalBaseAmountCents)
        assertEquals(2, rows[0].expenseCount)
        assertEquals(0, rows[1].totalBaseAmountCents)
        assertEquals(12, rows.last().monthNumber)
    }

    private val foodCategory = Category(1, "Food", "Food", 0xFFE76F51, 0)
    private val travelCategory = Category(2, "Travel", "Travel", 0xFF277DA1, 1)

    private fun expense(
        id: Long,
        categoryId: Long,
        amount: Long,
        tags: List<String> = emptyList(),
        spentAtMillis: Long = 1_704_067_200_000
    ): Expense {
        return Expense(
            id = id,
            originalAmountCents = amount,
            originalCurrencyCode = "USD",
            baseAmountCents = amount,
            baseCurrencyCode = "USD",
            exchangeRate = 1.0,
            categoryId = categoryId,
            note = "",
            tags = tags,
            spentAtMillis = spentAtMillis,
            createdAtMillis = spentAtMillis,
            updatedAtMillis = spentAtMillis
        )
    }

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}
