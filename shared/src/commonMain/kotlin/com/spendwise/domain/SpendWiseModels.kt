package com.spendwise.domain

import kotlinx.datetime.LocalDate

data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val sortOrder: Int
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
    val lastUsedAtMillis: Long,
    val currentMonthAmountCents: Long = 0L,
    val previousMonthAmountCents: Long = 0L
)

data class DailyExpenseTotal(
    val date: LocalDate,
    val totalBaseAmountCents: Long,
    val expenseCount: Int
)

data class MonthlyExpenseTotal(
    val monthNumber: Int,
    val totalBaseAmountCents: Long,
    val expenseCount: Int
)

data class CategoryReportRow(
    val category: Category,
    val totalBaseAmountCents: Long,
    val percentage: Double
)

data class SpendWiseSnapshot(
    val categories: List<Category> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val settings: UserSettings = UserSettings()
)

data class ExpenseDraft(
    val editingExpenseId: Long? = null,
    val amountText: String = "",
    val currencyCode: String = "USD",
    val categoryId: Long? = null,
    val note: String = "",
    val spentAtMillis: Long,
    val exchangeRateText: String = "1.0"
)

data class CategoryDraft(
    val editingCategoryId: Long? = null,
    val name: String = "",
    val icon: String = "other",
    val color: Long = 0xFF457B9D
)

data class TransactionFilters(
    val query: String = "",
    val categoryId: Long? = null,
    val currencyCode: String? = null
)

data class UserSettings(
    val baseCurrencyCode: String = "USD",
    val languageCode: String = "en"
)

data class AddExpenseInput(
    val id: Long? = null,
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
