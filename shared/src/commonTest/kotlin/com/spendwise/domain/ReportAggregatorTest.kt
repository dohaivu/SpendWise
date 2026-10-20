package com.spendwise.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportAggregatorTest {

    @Test
    fun summarySplitsMonthAndYearTotals() {
        val summary = summarize(selectedMonth = LocalDate(2026, 3, 1))

        assertEquals(listOf("Travel", "Food"), summary.monthCategoryRows.map { it.category.name })
        assertEquals(listOf(3_000L, 2_000L), summary.monthCategoryRows.map { it.totalBaseAmountCents })
        assertEquals(0.6, summary.monthCategoryRows[0].percentage, 1e-9)
        assertEquals(0.4, summary.monthCategoryRows[1].percentage, 1e-9)

        assertEquals(listOf(8_000L, 2_000L), summary.annualCategoryRows.map { it.totalBaseAmountCents })
    }

    @Test
    fun summaryBucketsEveryMonthOfTheYear() {
        val summary = summarize(selectedMonth = LocalDate(2026, 3, 1))

        assertEquals(12, summary.annualMonthlyTotals.size)
        assertEquals(5_000L, summary.annualMonthlyTotals[1].totalBaseAmountCents)
        assertEquals(1, summary.annualMonthlyTotals[1].expenseCount)
        assertEquals(5_000L, summary.annualMonthlyTotals[2].totalBaseAmountCents)
        assertEquals(3, summary.annualMonthlyTotals[2].expenseCount)
        assertEquals(0L, summary.annualMonthlyTotals[0].totalBaseAmountCents)
        assertEquals(12, summary.annualMonthlyTotals.last().monthNumber)
    }

    @Test
    fun summaryKeepsCategoryMonthlyTotalsForTheTrailingYear() {
        val summary = summarize(selectedMonth = LocalDate(2026, 3, 1))

        assertEquals(7_000L, summary.categoryMonthTotal(categoryId = 1, month = LocalDate(2025, 12, 1)))
        assertEquals(2_000L, summary.categoryMonthTotal(categoryId = 1, month = LocalDate(2026, 3, 1)))
        assertEquals(5_000L, summary.categoryMonthTotal(categoryId = 2, month = LocalDate(2026, 2, 1)))
        assertEquals(0L, summary.categoryMonthTotal(categoryId = 2, month = LocalDate(2026, 4, 1)))
    }

    @Test
    fun summaryAppliesTransactionFilters() {
        val byTags = summarize(
            selectedMonth = LocalDate(2026, 3, 1),
            filters = TransactionFilters(selectedTags = setOf("work"))
        )

        assertEquals(listOf(1L), byTags.monthCategoryRows.map { it.category.id })
        assertEquals(2_000L, byTags.monthCategoryRows.single().totalBaseAmountCents)

        val byQuery = summarize(
            selectedMonth = LocalDate(2026, 3, 1),
            filters = TransactionFilters(query = "CA PHE")
        )

        assertEquals(listOf(1L), byQuery.monthCategoryRows.map { it.category.id })
    }

    @Test
    fun unknownCategoriesAreCountedInTheDenominatorButNotListed() {
        val entries = prepareExpenseEntries(
            expenses = listOf(
                expense(id = 1, categoryId = 1, date = LocalDate(2026, 3, 5), amount = 1_000),
                expense(id = 2, categoryId = 99, date = LocalDate(2026, 3, 6), amount = 1_000)
            ),
            timeZone = TimeZone.UTC
        )

        val summary = ReportAggregator.summarize(
            entries = entries,
            categories = listOf(foodCategory),
            selectedMonth = LocalDate(2026, 3, 1),
            filters = TransactionFilters()
        )

        assertEquals(1, summary.monthCategoryRows.size)
        assertEquals(0.5, summary.monthCategoryRows.single().percentage, 1e-9)
    }

    @Test
    fun emptyEntriesProduceEmptyRowsAndZeroMonths() {
        val summary = ReportAggregator.summarize(
            entries = emptyList(),
            categories = listOf(foodCategory),
            selectedMonth = LocalDate(2026, 3, 1),
            filters = TransactionFilters()
        )

        assertTrue(summary.monthCategoryRows.isEmpty())
        assertTrue(summary.annualCategoryRows.isEmpty())
        assertTrue(summary.categoryMonthlyTotals.isEmpty())
        assertEquals(12, summary.annualMonthlyTotals.size)
        assertTrue(summary.annualMonthlyTotals.all { it.totalBaseAmountCents == 0L })
    }

    private fun summarize(
        selectedMonth: LocalDate,
        filters: TransactionFilters = TransactionFilters()
    ): ReportSummary = ReportAggregator.summarize(
        entries = prepareExpenseEntries(expenses, TimeZone.UTC),
        categories = listOf(foodCategory, travelCategory),
        selectedMonth = selectedMonth,
        filters = filters
    )

    private val expenses = listOf(
        expense(id = 1, categoryId = 1, date = LocalDate(2026, 3, 5), amount = 1_000, tags = listOf("work"), note = "Lunch"),
        expense(id = 2, categoryId = 2, date = LocalDate(2026, 3, 6), amount = 3_000, tags = listOf("trip"), note = "Flight"),
        expense(id = 3, categoryId = 1, date = LocalDate(2026, 3, 7), amount = 1_000, tags = listOf("work"), note = "Cà phê"),
        expense(id = 4, categoryId = 2, date = LocalDate(2026, 2, 10), amount = 5_000, tags = listOf("trip"), note = "Hotel"),
        expense(id = 5, categoryId = 1, date = LocalDate(2025, 12, 1), amount = 7_000, tags = listOf("work"), note = "Gift")
    )

    private val foodCategory = Category(1, "Food", "restaurant", 0xFFE76F51, 0)
    private val travelCategory = Category(2, "Travel", "flight", 0xFF277DA1, 1)

    private fun expense(
        id: Long,
        categoryId: Long,
        date: LocalDate,
        amount: Long,
        tags: List<String> = emptyList(),
        note: String = ""
    ): Expense = Expense(
        id = id,
        originalAmountCents = amount,
        originalCurrencyCode = "USD",
        baseAmountCents = amount,
        baseCurrencyCode = "USD",
        exchangeRate = 1.0,
        categoryId = categoryId,
        note = note,
        tags = tags,
        spentAtMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )
}
