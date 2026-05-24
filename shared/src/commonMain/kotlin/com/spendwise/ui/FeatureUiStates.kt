package com.spendwise.ui

import com.spendwise.domain.ActiveTagToken
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryDraft
import com.spendwise.domain.DailyExpenseTotal
import com.spendwise.domain.Expense
import com.spendwise.domain.ExpenseDraft
import com.spendwise.domain.ExpenseReminder
import com.spendwise.domain.TagUsage
import com.spendwise.domain.TransactionFilters
import com.spendwise.ui.components.ReportPeriod
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
    val selectedPeriod: ReportPeriod = ReportPeriod.Month,
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
    val selectedDate: kotlinx.datetime.LocalDate = today(),
    val selectedTags: Set<String> = emptySet(),
    val transactionFilters: TransactionFilters = TransactionFilters(),
    val calendarData: CalendarData = CalendarData()
)

data class AllTransactionsUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val baseCurrencyCode: String = "USD",
    val selectedTags: Set<String> = emptySet(),
    val transactionFilters: TransactionFilters = TransactionFilters(),
    val transactionData: CalendarData = CalendarData()
)

data class CalendarData(
    val monthTransactionCount: Int = 0,
    val transactionItems: List<DateTransactionListItem> = emptyList(),
    val totalsByDate: Map<kotlinx.datetime.LocalDate, DailyExpenseTotal> = emptyMap(),
    val headerIndexes: Map<kotlinx.datetime.LocalDate, Int> = emptyMap(),
    val filteredMonthTotal: Long = 0L
)

sealed interface DateTransactionListItem {
    data class Header(
        val date: kotlinx.datetime.LocalDate,
        val total: Long
    ) : DateTransactionListItem

    data class Transaction(
        val expense: Expense
    ) : DateTransactionListItem
}

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
    val reminders: List<ExpenseReminder> = emptyList(),
    val tagUsageSort: TagUsageSort = TagUsageSort.MostUsed,
    val message: String? = null
)
