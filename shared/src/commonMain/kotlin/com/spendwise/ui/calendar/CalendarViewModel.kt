package com.spendwise.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.DailyExpenseTotal
import com.spendwise.domain.Expense
import com.spendwise.domain.TagParser
import com.spendwise.domain.TransactionFilters
import com.spendwise.domain.usecase.SpendWiseUseCases
import com.spendwise.domain.usecase.filterByTransactionFilters
import com.spendwise.ui.CalendarData
import com.spendwise.ui.CalendarTransactionListItem
import com.spendwise.ui.CalendarUiState
import com.spendwise.ui.firstDayOfMonth
import com.spendwise.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class CalendarViewModel(
    repository: ExpenseRepository,
    private val useCases: SpendWiseUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSnapshot().collect { snapshot ->
                _uiState.update { state ->
                    state.withCalendarData(
                        expenses = snapshot.expenses,
                        categories = snapshot.categories,
                        tagUsage = snapshot.tagUsage,
                        baseCurrencyCode = snapshot.settings.baseCurrencyCode
                    )
                }
            }
        }
    }

    fun selectMonth(month: LocalDate) {
        _uiState.update { it.withCalendarData(selectedMonth = month.firstDayOfMonth()) }
    }

    fun previousMonth() {
        _uiState.update { it.withCalendarData(selectedMonth = it.selectedMonth.minus(1, DateTimeUnit.MONTH)) }
    }

    fun nextMonth() {
        _uiState.update { it.withCalendarData(selectedMonth = it.selectedMonth.plus(1, DateTimeUnit.MONTH)) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun resetToToday() {
        val today = today()
        _uiState.update {
            it.withCalendarData(
                selectedMonth = today.firstDayOfMonth(),
                selectedDate = today
            )
        }
    }

    fun toggleTagFilter(tag: String) {
        val normalized = TagParser.normalize(tag)
        _uiState.update {
            val next = if (normalized in it.selectedTags) it.selectedTags - normalized else it.selectedTags + normalized
            it.withCalendarData(selectedTags = next)
        }
    }

    fun updateTransactionQuery(value: String) {
        _uiState.update { it.withCalendarData(transactionFilters = it.transactionFilters.copy(query = value)) }
    }

    fun updateTransactionCategory(categoryId: Long?) {
        _uiState.update { it.withCalendarData(transactionFilters = it.transactionFilters.copy(categoryId = categoryId)) }
    }

    fun updateTransactionCurrency(currencyCode: String?) {
        _uiState.update { it.withCalendarData(transactionFilters = it.transactionFilters.copy(currencyCode = currencyCode)) }
    }

    fun clearTransactionFilters() {
        _uiState.update { it.withCalendarData(transactionFilters = TransactionFilters()) }
    }

    fun getDailyExpenseTotals(timeZone: TimeZone): List<DailyExpenseTotal> {
        return useCases.getDailyExpenseTotals(_uiState.value.expenses, timeZone)
    }

    fun getTransactionsForSelectedDate(timeZone: TimeZone, ignoreCurrencyFilter: Boolean = false): List<Expense> {
        val state = _uiState.value
        val filters = if (ignoreCurrencyFilter) {
            state.transactionFilters.copy(currencyCode = null)
        } else {
            state.transactionFilters
        }
        return useCases.getTransactionsByDate(
            expenses = state.expenses,
            date = state.selectedDate,
            timeZone = timeZone,
            selectedTags = state.selectedTags,
            filters = filters
        )
    }

    fun getFilteredTransactions(): List<Expense> {
        val state = _uiState.value
        return useCases.getTransactionsByFilters(
            expenses = state.expenses,
            filters = state.transactionFilters,
            selectedTags = state.selectedTags
        )
    }

    private fun CalendarUiState.withCalendarData(
        expenses: List<Expense> = this.expenses,
        categories: List<com.spendwise.domain.Category> = this.categories,
        tagUsage: List<com.spendwise.domain.TagUsage> = this.tagUsage,
        baseCurrencyCode: String = this.baseCurrencyCode,
        selectedMonth: LocalDate = this.selectedMonth,
        selectedDate: LocalDate = this.selectedDate,
        selectedTags: Set<String> = this.selectedTags,
        transactionFilters: TransactionFilters = this.transactionFilters
    ): CalendarUiState {
        return copy(
            expenses = expenses,
            categories = categories,
            tagUsage = tagUsage,
            baseCurrencyCode = baseCurrencyCode,
            selectedMonth = selectedMonth,
            selectedDate = selectedDate,
            selectedTags = selectedTags,
            transactionFilters = transactionFilters,
            calendarData = buildCalendarData(
                expenses = expenses,
                month = selectedMonth,
                filters = transactionFilters.copy(currencyCode = null),
                selectedTags = selectedTags,
                timeZone = TimeZone.currentSystemDefault()
            )
        )
    }

    private fun buildCalendarData(
        expenses: List<Expense>,
        month: LocalDate,
        filters: TransactionFilters,
        selectedTags: Set<String>,
        timeZone: TimeZone
    ): CalendarData {
        val datedTransactions = expenses
            .asSequence()
            .map { expense -> expense to expense.spentDate(timeZone) }
            .filter { (_, spentDate) -> spentDate.isSameMonth(month) }
            .map { (expense, _) -> expense }
            .toList()
            .filterByTransactionFilters(filters, selectedTags)
            .sortedByDescending { it.spentAtMillis }
            .map { expense -> expense to expense.spentDate(timeZone) }

        val groupedTransactions = datedTransactions
            .groupBy(keySelector = { (_, spentDate) -> spentDate }, valueTransform = { (expense, _) -> expense })
            .toList()
        val totalsByDate = groupedTransactions.associate { (date, dayExpenses) ->
            date to DailyExpenseTotal(
                date = date,
                totalBaseAmountCents = dayExpenses.sumOf { it.baseAmountCents },
                expenseCount = dayExpenses.size
            )
        }
        val transactionItems = buildList {
            groupedTransactions.forEach { (date, dayExpenses) ->
                add(CalendarTransactionListItem.Header(date, dayExpenses.sumOf { it.baseAmountCents }))
                dayExpenses.forEach { expense ->
                    add(CalendarTransactionListItem.Transaction(expense))
                }
            }
        }
        val headerIndexes = buildMap {
            transactionItems.forEachIndexed { index, item ->
                if (item is CalendarTransactionListItem.Header) {
                    put(item.date, index)
                }
            }
        }
        val filteredMonthTotal = datedTransactions.sumOf { (expense, _) -> expense.baseAmountCents }

        return CalendarData(
            monthTransactionCount = datedTransactions.size,
            transactionItems = transactionItems,
            totalsByDate = totalsByDate,
            headerIndexes = headerIndexes,
            filteredMonthTotal = filteredMonthTotal
        )
    }
}

private fun Expense.spentDate(timeZone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date

private fun LocalDate.isSameMonth(month: LocalDate): Boolean =
    year == month.year && this.month == month.month
