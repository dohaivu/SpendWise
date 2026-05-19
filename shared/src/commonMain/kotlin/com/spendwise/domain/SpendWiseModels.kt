package com.spendwise.domain

import kotlinx.datetime.LocalDate

data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val sortOrder: Int,
    val archived: Boolean = false
)

data class Expense(
    val id: Long,
    val originalAmountCents: Long,
    val originalCurrencyCode: String,
    val baseAmountCents: Long,
    val baseCurrencyCode: String,
    val exchangeRate: Double,
    val categoryId: Long,
    val note: String,
    val tags: List<String>,
    val spentAtMillis: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class TagUsage(
    val name: String,
    val expenseCount: Int,
    val totalBaseAmountCents: Long,
    val lastUsedAtMillis: Long
)

data class DailyExpenseTotal(
    val date: LocalDate,
    val totalBaseAmountCents: Long,
    val expenseCount: Int
)

data class CategoryReportRow(
    val category: Category,
    val totalBaseAmountCents: Long,
    val percentage: Double
)

data class MonthComparisonRow(
    val category: Category,
    val currentMonthAmountCents: Long,
    val previousMonthAmountCents: Long
) {
    val changeAmountCents: Long = currentMonthAmountCents - previousMonthAmountCents
    val changePercent: Double? =
        if (previousMonthAmountCents == 0L) null else changeAmountCents.toDouble() / previousMonthAmountCents

    val status: String? = when {
        previousMonthAmountCents == 0L && currentMonthAmountCents > 0L -> "New"
        currentMonthAmountCents == 0L && previousMonthAmountCents > 0L -> "Stopped"
        else -> null
    }
}

data class SpendWiseSnapshot(
    val categories: List<Category> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList()
)

data class ExpenseDraft(
    val amountText: String = "",
    val currencyCode: String = "USD",
    val categoryId: Long? = null,
    val note: String = "",
    val spentAtMillis: Long,
    val exchangeRateText: String = "1.0"
)

data class AddExpenseInput(
    val originalAmountCents: Long,
    val originalCurrencyCode: String,
    val baseAmountCents: Long,
    val baseCurrencyCode: String,
    val exchangeRate: Double,
    val categoryId: Long,
    val note: String,
    val tags: List<String>,
    val spentAtMillis: Long
)

