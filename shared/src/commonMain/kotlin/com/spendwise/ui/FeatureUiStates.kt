package com.spendwise.ui

import com.spendwise.domain.ActiveTagToken
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryDraft
import com.spendwise.domain.Expense
import com.spendwise.domain.ExpenseDraft
import com.spendwise.domain.TagUsage
import com.spendwise.domain.TransactionFilters
import kotlin.time.Clock

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val draft: ExpenseDraft = ExpenseDraft(spentAtMillis = Clock.System.now().toEpochMilliseconds()),
    val baseCurrencyCode: String = "USD",
    val activeTagToken: ActiveTagToken? = null,
    val tagSuggestions: List<String> = emptyList(),
    val message: String? = null
)

data class CalendarUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val baseCurrencyCode: String = "USD",
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
    val selectedDate: kotlinx.datetime.LocalDate = today(),
    val selectedTags: Set<String> = emptySet(),
    val transactionFilters: TransactionFilters = TransactionFilters()
)

data class ReportUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val baseCurrencyCode: String = "USD",
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
    val selectedReportCategoryId: Long? = null,
    val selectedTags: Set<String> = emptySet()
)

data class SettingsUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val categoryDraft: CategoryDraft = CategoryDraft(),
    val baseCurrencyCode: String = "USD",
    val language: AppLanguage = AppLanguage.English,
    val tagUsageSort: TagUsageSort = TagUsageSort.MostUsed,
    val message: String? = null
)
