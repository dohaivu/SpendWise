package com.spendwise.domain.usecase

import com.spendwise.domain.ActiveTagToken
import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.domain.ExpenseReminder
import com.spendwise.domain.SpendWiseSnapshot
import com.spendwise.domain.TagUsage
import com.spendwise.domain.TransactionFilters
import kotlin.test.Test
import kotlin.test.assertEquals

class UseCaseTest {
    @Test
    fun parseTagsUseCaseDelegatesToTagParserRules() {
        val useCase = ParseTagsFromNoteUseCase()

        assertEquals(listOf("work", "中文"), useCase("Lunch #Work #中文"))
    }

    @Test
    fun autocompleteUseCaseSortsMatchingTagsByUsage() {
        val useCase = GetTagAutocompleteSuggestionsUseCase()
        val snapshot = SpendWiseSnapshot(
            tagUsage = listOf(
                TagUsage("food", expenseCount = 1, totalBaseAmountCents = 100, lastUsedAtMillis = 1),
                TagUsage("focus", expenseCount = 4, totalBaseAmountCents = 200, lastUsedAtMillis = 2),
                TagUsage("work", expenseCount = 9, totalBaseAmountCents = 300, lastUsedAtMillis = 3)
            )
        )

        val suggestions = useCase(ActiveTagToken(query = "fo", startIndex = 0, endIndex = 3), snapshot)

        assertEquals(listOf("focus", "food"), suggestions)
    }

    @Test
    fun convertToBaseCurrencyHandlesSameAndDifferentCurrencies() {
        val useCase = ConvertToBaseCurrencyUseCase()

        assertEquals(1_000, useCase(1_000, "USD", "USD", 24_000.0))
        assertEquals(24_000_000, useCase(1_000, "USD", "VND", 24_000.0))
    }

    @Test
    fun transactionFilterUseCaseAppliesTagsCategoryAndText() {
        val useCase = GetTransactionsByFiltersUseCase()
        val expenses = listOf(
            expense(id = 1, categoryId = 1, currency = "USD", note = "Lunch #work", tags = listOf("work")),
            expense(id = 2, categoryId = 2, currency = "VND", note = "Dinner #work", tags = listOf("work")),
            expense(id = 3, categoryId = 1, currency = "USD", note = "Coffee #personal", tags = listOf("personal"))
        )

        val result = useCase(
            expenses = expenses,
            filters = TransactionFilters(query = "lunch", categoryId = 1, selectedTags = setOf("work"))
        )

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun transactionFilterUseCaseMatchesNoteWithoutAccents() {
        val useCase = GetTransactionsByFiltersUseCase()
        val expenses = listOf(
            expense(id = 1, categoryId = 1, note = "Đi chợ mua cà phê"),
            expense(id = 2, categoryId = 1, note = "Mua sach")
        )

        val result = useCase(
            expenses = expenses,
            filters = TransactionFilters(query = "di cho")
        )

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun categoryReportUseCaseCalculatesRows() {
        val useCase = GetCategoryPieReportUseCase()
        val categories = listOf(Category(1, "Food", "F", 0xFFE76F51, 0))
        val expenses = listOf(
            expense(id = 1, categoryId = 1, amount = 1_000, tags = listOf("work")),
            expense(id = 2, categoryId = 1, amount = 2_000, tags = listOf("personal"))
        )

        val rows = useCase(expenses, categories)

        assertEquals(1, rows.size)
        assertEquals(3_000, rows.first().totalBaseAmountCents)
    }

    @Test
    fun expenseReminderCalculatesMinutesSinceMidnight() {
        val reminder = ExpenseReminder(id = 1, hour = 18, minute = 30, enabled = true)

        assertEquals(1_110, reminder.minutesSinceMidnight)
    }

    private fun expense(
        id: Long,
        categoryId: Long,
        amount: Long = 1_000,
        currency: String = "USD",
        note: String = "",
        tags: List<String> = emptyList()
    ): Expense {
        return Expense(
            id = id,
            originalAmountCents = amount,
            originalCurrencyCode = currency,
            baseAmountCents = amount,
            baseCurrencyCode = "USD",
            exchangeRate = 1.0,
            categoryId = categoryId,
            note = note,
            tags = tags,
            spentAtMillis = 1_704_067_200_000,
            createdAtMillis = 1_704_067_200_000,
            updatedAtMillis = 1_704_067_200_000
        )
    }
}
