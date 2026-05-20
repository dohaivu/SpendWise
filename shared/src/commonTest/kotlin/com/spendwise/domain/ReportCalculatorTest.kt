package com.spendwise.domain

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
