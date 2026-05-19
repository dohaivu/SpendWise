package com.spendwise.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
    fun monthOverMonthComparesCurrentAndPreviousMonth() {
        val categories = listOf(foodCategory)
        val expenses = listOf(
            expense(id = 1, categoryId = 1, amount = 3_000, spentAtMillis = 1_704_067_200_000),
            expense(id = 2, categoryId = 1, amount = 1_000, spentAtMillis = 1_701_388_800_000)
        )

        val rows = ReportCalculator.monthOverMonth(
            expenses = expenses,
            categories = categories,
            selectedMonth = LocalDate(2024, 1, 1),
            selectedTags = emptySet(),
            timeZone = TimeZone.UTC
        )

        assertEquals(1, rows.size)
        assertEquals(3_000, rows.first().currentMonthAmountCents)
        assertEquals(1_000, rows.first().previousMonthAmountCents)
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
}
