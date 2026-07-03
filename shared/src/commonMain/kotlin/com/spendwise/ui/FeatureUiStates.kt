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
import com.spendwise.ui.components.CurrencyDisplayFormat
import com.spendwise.ui.components.ReportPeriod
import com.spendwise.ui.components.currencyDisplayFormat
import kotlin.time.Clock

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val draft: ExpenseDraft = ExpenseDraft(spentAtMillis = Clock.System.now().toEpochMilliseconds()),
    val baseCurrency: CurrencyDisplayFormat = currencyDisplayFormat("USD"),
    val activeTagToken: ActiveTagToken? = null,
    val tagSuggestions: List<String> = emptyList(),
    val message: String? = null
)

data class CalendarUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val baseCurrency: CurrencyDisplayFormat = currencyDisplayFormat("USD"),
    val selectedPeriod: ReportPeriod = ReportPeriod.Month,
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
    val selectedDate: kotlinx.datetime.LocalDate = today(),
    val transactionFilters: TransactionFilters = TransactionFilters(),
    val calendarData: CalendarData = CalendarData()
)

data class AllTransactionsUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val baseCurrency: CurrencyDisplayFormat = currencyDisplayFormat("USD"),
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

data class DateTransactionListItem(
    val date: kotlinx.datetime.LocalDate,
    val total: Long,
    val expenses: List<Expense>
)

data class ReportUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val baseCurrency: CurrencyDisplayFormat = currencyDisplayFormat("USD"),
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
    val selectedReportCategoryId: Long? = null,
    val transactionFilters: TransactionFilters = TransactionFilters()
)

data class SettingsUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val categoryDraft: CategoryDraft = CategoryDraft(),
    val baseCurrency: CurrencyDisplayFormat = currencyDisplayFormat("USD"),
    val language: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val colorSchemeMode: AppColorSchemeMode = AppColorSchemeMode.Sunset,
    val reminders: List<ExpenseReminder> = emptyList(),
    val tagUsageSort: TagUsageSort = TagUsageSort.MostUsed,
    val backupFolderUri: String? = null,
    val backupFolderName: String? = null,
    val message: String? = null
)
