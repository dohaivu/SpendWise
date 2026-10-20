package com.spendwise.ui.calendar

import com.spendwise.domain.Expense
import com.spendwise.domain.TransactionFilters
import com.spendwise.domain.prepareExpenseEntries
import com.spendwise.ui.CalendarData
import com.spendwise.ui.components.ReportPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarDataBuilderTest {

    @Test
    fun groupsExpensesByDateNewestFirst() {
        val data = build(
            month = LocalDate(2026, 3, 1),
            expenses = listOf(
                expense(id = 1, date = LocalDate(2026, 3, 10), baseAmountCents = 500),
                expense(id = 2, date = LocalDate(2026, 3, 12), baseAmountCents = 300),
                expense(id = 3, date = LocalDate(2026, 3, 12), baseAmountCents = 700)
            )
        )

        assertEquals(
            listOf(LocalDate(2026, 3, 12), LocalDate(2026, 3, 10)),
            data.transactionItems.map { it.date }
        )
        assertEquals(1000L, data.transactionItems.first().total)
        assertEquals(listOf(3L, 2L), data.transactionItems.first().expenses.map { it.id })
        assertEquals(0, data.headerIndexes[LocalDate(2026, 3, 12)])
        assertEquals(1, data.headerIndexes[LocalDate(2026, 3, 10)])
        assertEquals(1500L, data.filteredMonthTotal)
        assertEquals(3, data.monthTransactionCount)
        assertEquals(2, data.totalsByDate.getValue(LocalDate(2026, 3, 12)).expenseCount)
        assertEquals(1000L, data.totalsByDate.getValue(LocalDate(2026, 3, 12)).totalBaseAmountCents)
    }

    @Test
    fun monthPeriodExcludesOtherMonthsAndYears() {
        val data = build(
            month = LocalDate(2026, 3, 1),
            expenses = listOf(
                expense(id = 1, date = LocalDate(2026, 3, 31), baseAmountCents = 100),
                expense(id = 2, date = LocalDate(2026, 4, 1), baseAmountCents = 200),
                expense(id = 3, date = LocalDate(2025, 3, 15), baseAmountCents = 400)
            )
        )

        assertEquals(listOf(1L), data.transactionItems.single().expenses.map { it.id })
        assertEquals(100L, data.filteredMonthTotal)
    }

    @Test
    fun annualPeriodCoversEveryMonthOfTheYear() {
        val data = build(
            month = LocalDate(2026, 3, 1),
            period = ReportPeriod.Annual,
            expenses = listOf(
                expense(id = 1, date = LocalDate(2026, 1, 5), baseAmountCents = 100),
                expense(id = 2, date = LocalDate(2026, 12, 31), baseAmountCents = 200),
                expense(id = 3, date = LocalDate(2027, 1, 1), baseAmountCents = 400)
            )
        )

        assertEquals(2, data.monthTransactionCount)
        assertEquals(300L, data.filteredMonthTotal)
        assertEquals(listOf(2L, 1L), data.transactionItems.flatMap { it.expenses }.map { it.id })
    }

    @Test
    fun filtersApplyCategoryTagsAndQuery() {
        val expenses = listOf(
            expense(id = 1, date = LocalDate(2026, 3, 5), categoryId = 1, note = "Lunch", tags = listOf("work")),
            expense(id = 2, date = LocalDate(2026, 3, 5), categoryId = 2, note = "Lunch", tags = listOf("work")),
            expense(id = 3, date = LocalDate(2026, 3, 5), categoryId = 1, note = "Coffee", tags = listOf("personal"))
        )

        val byQuery = build(month = LocalDate(2026, 3, 1), expenses = expenses, filters = TransactionFilters(query = "lunch"))
        assertEquals(listOf(2L, 1L), byQuery.transactionItems.single().expenses.map { it.id })

        val byCategory = build(month = LocalDate(2026, 3, 1), expenses = expenses, filters = TransactionFilters(categoryId = 1))
        assertEquals(listOf(3L, 1L), byCategory.transactionItems.single().expenses.map { it.id })

        val byTag = build(month = LocalDate(2026, 3, 1), expenses = expenses, filters = TransactionFilters(selectedTags = setOf("work")))
        assertEquals(listOf(2L, 1L), byTag.transactionItems.single().expenses.map { it.id })
    }

    @Test
    fun queryIgnoresAccentsAndCase() {
        val data = build(
            month = LocalDate(2026, 3, 1),
            expenses = listOf(expense(id = 1, date = LocalDate(2026, 3, 5), note = "Cà phê sữa")),
            filters = TransactionFilters(query = "CA PHE")
        )

        assertEquals(1, data.monthTransactionCount)
        assertEquals(1L, data.transactionItems.single().expenses.single().id)
    }

    @Test
    fun prepareResolvesDateForTheGivenTimeZone() {
        val millis = LocalDateTime(2026, 3, 10, 18, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val expense = expense(
            id = 1,
            date = LocalDate(2026, 3, 10),
            note = "Cà phê #Work",
            tags = listOf("Work")
        ).copy(spentAtMillis = millis)

        val entries = prepareExpenseEntries(
            expenses = listOf(expense),
            timeZone = TimeZone.of("Asia/Ho_Chi_Minh")
        )

        assertEquals(LocalDate(2026, 3, 11), entries.single().date)
        assertEquals("ca phe #work", entries.single().searchText)
        assertEquals(listOf("work"), entries.single().normalizedTags)
    }

    @Test
    fun emptyEntriesProduceEmptyData() {
        val data = build(month = LocalDate(2026, 3, 1), expenses = emptyList())

        assertTrue(data.transactionItems.isEmpty())
        assertTrue(data.totalsByDate.isEmpty())
        assertTrue(data.headerIndexes.isEmpty())
        assertEquals(0, data.monthTransactionCount)
        assertEquals(0L, data.filteredMonthTotal)
    }

    private fun build(
        month: LocalDate,
        expenses: List<Expense>,
        period: ReportPeriod = ReportPeriod.Month,
        filters: TransactionFilters = TransactionFilters(),
        timeZone: TimeZone = TimeZone.UTC
    ): CalendarData = CalendarDataBuilder.build(
        entries = prepareExpenseEntries(expenses, timeZone),
        period = period,
        month = month,
        filters = filters
    )

    private fun expense(
        id: Long,
        date: LocalDate,
        baseAmountCents: Long = 100L,
        categoryId: Long = 1L,
        note: String = "",
        tags: List<String> = emptyList()
    ): Expense = Expense(
        id = id,
        originalAmountCents = baseAmountCents,
        originalCurrencyCode = "USD",
        baseAmountCents = baseAmountCents,
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
