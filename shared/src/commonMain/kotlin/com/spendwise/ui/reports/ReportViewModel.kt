package com.spendwise.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.CategoryReportRow
import com.spendwise.domain.Expense
import com.spendwise.domain.MonthlyExpenseTotal
import com.spendwise.domain.TagParser
import com.spendwise.domain.usecase.SpendWiseUseCases
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.currencyDisplayFormat
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

class ReportViewModel(
    repository: ExpenseRepository,
    private val useCases: SpendWiseUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSnapshot().collect { snapshot ->
                _uiState.update {
                    it.copy(
                        expenses = snapshot.expenses,
                        categories = snapshot.categories,
                        tagUsage = snapshot.tagUsage,
                        baseCurrency = currencyDisplayFormat(snapshot.settings.baseCurrencyCode)
                    )
                }
            }
        }
    }

    fun previousMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.minus(1, DateTimeUnit.Companion.MONTH)) }
    }

    fun nextMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.plus(1, DateTimeUnit.Companion.MONTH)) }
    }

    fun previousYear() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.minus(1, DateTimeUnit.Companion.YEAR)) }
    }

    fun nextYear() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.plus(1, DateTimeUnit.Companion.YEAR)) }
    }

    fun resetToCurrentPeriod() {
        _uiState.update { it.copy(selectedMonth = today().firstDayOfMonth()) }
    }

    fun toggleTagFilter(tag: String) {
        val normalized = TagParser.normalize(tag)
        _uiState.update {
            val currentTags = it.transactionFilters.selectedTags
            val next = if (normalized in currentTags) currentTags - normalized else currentTags + normalized
            it.copy(transactionFilters = it.transactionFilters.copy(selectedTags = next))
        }
    }

    fun openReportCategory(categoryId: Long) {
        _uiState.update { it.copy(selectedReportCategoryId = categoryId) }
    }

    fun closeReportCategory() {
        _uiState.update { it.copy(selectedReportCategoryId = null) }
    }

    fun updateTransactionQuery(value: String) {
        _uiState.update { it.copy(transactionFilters = it.transactionFilters.copy(query = value)) }
    }

    fun updateTransactionCategory(categoryId: Long?) {
        _uiState.update { it.copy(transactionFilters = it.transactionFilters.copy(categoryId = categoryId)) }
    }

    fun getCategoryReport(expenses: List<Expense>): List<CategoryReportRow> {
        val state = _uiState.value
        val filteredExpenses = useCases.getTransactionsByFilters(
            expenses = expenses,
            filters = state.transactionFilters
        )
        return useCases.getCategoryPieReport(filteredExpenses, state.categories)
    }

    fun getYearlyCategoryReport(year: Int, timeZone: TimeZone): List<CategoryReportRow> {
        val state = _uiState.value
        val filteredExpenses = useCases.getTransactionsByFilters(
            expenses = state.expenses,
            filters = state.transactionFilters
        )
        return useCases.getYearlyCategoryReport(
            expenses = filteredExpenses,
            categories = state.categories,
            year = year,
            timeZone = timeZone
        )
    }

    fun getAnnualMonthlyReport(year: Int, timeZone: TimeZone): List<MonthlyExpenseTotal> {
        val state = _uiState.value
        val filteredExpenses = useCases.getTransactionsByFilters(
            expenses = state.expenses,
            filters = state.transactionFilters
        )
        return useCases.getAnnualMonthlyReport(
            expenses = filteredExpenses,
            year = year,
            timeZone = timeZone
        )
    }

    fun selectMonth(month: LocalDate) {
        _uiState.update { it.copy(selectedMonth = month.firstDayOfMonth()) }
    }
}
