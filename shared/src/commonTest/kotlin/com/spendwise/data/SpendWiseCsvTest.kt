package com.spendwise.data

import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpendWiseCsvTest {
    @Test
    fun parseMinimalCsvImportsValidRows() {
        val csv = """
            date,amount,currency,category,note
            2026-05-24,125000,VND,Food,lunch
            2026-05-25,12.50,USD,Coffee,latte #work
        """.trimIndent()

        val result = parseSpendWiseCsv(csv)

        assertEquals(emptyList(), result.errors)
        assertEquals(2, result.rows.size)
        assertEquals(LocalDate(2026, 5, 24), result.rows[0].date)
        assertEquals(12_500_000, result.rows[0].amountCents)
        assertEquals("VND", result.rows[0].currencyCode)
        assertEquals("Food", result.rows[0].categoryName)
        assertEquals(1_250, result.rows[1].amountCents)
    }

    @Test
    fun parseCsvDateSupportsExpectedFormats() {
        assertEquals(LocalDate(2026, 5, 24), parseCsvDate("2026-05-24"))
        assertEquals(LocalDate(2026, 5, 24), parseCsvDate("2026/5/24"))
        assertEquals(LocalDate(2026, 5, 24), parseCsvDate("5/24/2026"))
        assertEquals(LocalDate(2026, 5, 24), parseCsvDate("24/5/2026"))
    }

    @Test
    fun ambiguousSlashDatesDefaultToMonthDayYear() {
        assertEquals(LocalDate(2026, 3, 4), parseCsvDate("03/04/2026"))
    }

    @Test
    fun parserAcceptsCrLfAndUtf8Notes() {
        val result = parseSpendWiseCsv("date,amount,currency,category,note\r\n2026-05-24,45000,VND,Living,điện\r\n")

        assertEquals(emptyList(), result.errors)
        assertEquals("điện", result.rows.single().note)
    }

    @Test
    fun quotedCsvFieldsRoundTrip() {
        val categories = listOf(Category(1, "Food, Cafe", "restaurant", 0xFFE76F51, 0))
        val expenses = listOf(
            expense(
                note = "lunch, \"special\"\nwith team",
                categoryId = 1
            )
        )

        val csv = formatSpendWiseCsv(expenses, categories)
        val parsed = parseSpendWiseCsv(csv)

        assertEquals(emptyList(), parsed.errors)
        assertEquals("Food, Cafe", parsed.rows.single().categoryName)
        assertEquals("lunch, \"special\"\nwith team", parsed.rows.single().note)
    }

    @Test
    fun invalidRowsAreReported() {
        val csv = """
            date,amount,currency,category,note
            nope,100,VND,Food,bad date
            2026-05-24,abc,VND,Food,bad amount
            2026-05-24,100,,Food,bad currency
            2026-05-24,100,VND,,bad category
        """.trimIndent()

        val result = parseSpendWiseCsv(csv)

        assertEquals(0, result.rows.size)
        assertEquals(4, result.errors.size)
    }

    @Test
    fun invalidHeaderIsReported() {
        val result = parseSpendWiseCsv("inputDate,baseAmount,baseCurrency,note,type,categoryName\n")

        assertEquals(0, result.rows.size)
        assertTrue(result.errors.single().message.contains("Header must be"))
    }

    @Test
    fun exportUsesCanonicalHeaderDatesAndAmounts() {
        val categories = listOf(Category(1, "Food", "restaurant", 0xFFE76F51, 0))
        val expenses = listOf(
            expense(id = 2, amount = 1_250, currency = "USD", categoryId = 1, note = "lunch"),
            expense(id = 1, amount = 12_500_000, currency = "VND", categoryId = 1, note = "breakfast")
        )

        val csv = formatSpendWiseCsv(expenses, categories)

        assertEquals(
            """
            date,amount,currency,category,note
            2024-01-01,125000,VND,Food,breakfast
            2024-01-01,12.50,USD,Food,lunch

            """.trimIndent(),
            csv
        )
    }

    private fun expense(
        id: Long = 1,
        amount: Long = 1_250,
        currency: String = "USD",
        categoryId: Long = 1,
        note: String = ""
    ): Expense {
        return Expense(
            id = id,
            originalAmountCents = amount,
            originalCurrencyCode = currency,
            baseAmountCents = amount,
            baseCurrencyCode = currency,
            exchangeRate = 1.0,
            categoryId = categoryId,
            note = note,
            tags = emptyList(),
            spentAtMillis = 1_704_067_200_000,
            createdAtMillis = 1_704_067_200_000,
            updatedAtMillis = 1_704_067_200_000
        )
    }
}
