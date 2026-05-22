package com.spendwise.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.DailyExpenseTotal
import com.spendwise.domain.Expense
import com.spendwise.domain.TagParser
import com.spendwise.domain.TransactionFilters
import com.spendwise.domain.usecase.SpendWiseUseCases
import com.spendwise.ui.CalendarUiState
import com.spendwise.ui.firstDayOfMonth
import com.spendwise.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class CalendarViewModel(
    repository: ExpenseRepository,
    private val useCases: SpendWiseUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSnapshot().collect { snapshot ->
                _uiState.update {
                    it.copy(
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
        _uiState.update { it.copy(selectedMonth = month.firstDayOfMonth()) }
    }

    fun previousMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.minus(1, DateTimeUnit.MONTH)) }
    }

    fun nextMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.plus(1, DateTimeUnit.MONTH)) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun resetToToday() {
        val today = today()
        _uiState.update {
            it.copy(
                selectedMonth = today.firstDayOfMonth(),
                selectedDate = today
            )
        }
    }

    fun toggleTagFilter(tag: String) {
        val normalized = TagParser.normalize(tag)
        _uiState.update {
            val next = if (normalized in it.selectedTags) it.selectedTags - normalized else it.selectedTags + normalized
            it.copy(selectedTags = next)
        }
    }

    fun updateTransactionQuery(value: String) {
        _uiState.update { it.copy(transactionFilters = it.transactionFilters.copy(query = value)) }
    }

    fun updateTransactionCategory(categoryId: Long?) {
        _uiState.update { it.copy(transactionFilters = it.transactionFilters.copy(categoryId = categoryId)) }
    }

    fun updateTransactionCurrency(currencyCode: String?) {
        _uiState.update { it.copy(transactionFilters = it.transactionFilters.copy(currencyCode = currencyCode)) }
    }

    fun clearTransactionFilters() {
        _uiState.update { it.copy(transactionFilters = TransactionFilters()) }
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
}
