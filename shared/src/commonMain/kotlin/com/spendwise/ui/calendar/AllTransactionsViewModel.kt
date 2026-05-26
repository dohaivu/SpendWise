package com.spendwise.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.domain.TagParser
import com.spendwise.domain.TagUsage
import com.spendwise.domain.TransactionFilters
import com.spendwise.domain.usecase.filterByTransactionFilters
import com.spendwise.ui.AllTransactionsUiState
import com.spendwise.ui.CalendarData
import com.spendwise.ui.DateTransactionListItem
import com.spendwise.ui.components.CurrencyDisplayFormat
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.spentDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

class AllTransactionsViewModel(
    repository: ExpenseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AllTransactionsUiState())
    val uiState: StateFlow<AllTransactionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSnapshot().collect { snapshot ->
                _uiState.update { state ->
                    state.withTransactionData(
                        expenses = snapshot.expenses,
                        categories = snapshot.categories,
                        tagUsage = snapshot.tagUsage,
                        baseCurrencyCode = currencyDisplayFormat(snapshot.settings.baseCurrencyCode)
                    )
                }
            }
        }
    }

    fun toggleTagFilter(tag: String) {
        val normalized = TagParser.normalize(tag)
        _uiState.update {
            val currentTags = it.transactionFilters.selectedTags
            val next = if (normalized in currentTags) currentTags - normalized else currentTags + normalized
            it.withTransactionData(transactionFilters = it.transactionFilters.copy(selectedTags = next))
        }
    }

    fun updateTransactionQuery(value: String) {
        _uiState.update { it.withTransactionData(transactionFilters = it.transactionFilters.copy(query = value)) }
    }

    fun updateTransactionCategory(categoryId: Long?) {
        _uiState.update { it.withTransactionData(transactionFilters = it.transactionFilters.copy(categoryId = categoryId)) }
    }

    private fun AllTransactionsUiState.withTransactionData(
        expenses: List<Expense> = this.expenses,
        categories: List<Category> = this.categories,
        tagUsage: List<TagUsage> = this.tagUsage,
        baseCurrencyCode: CurrencyDisplayFormat = this.baseCurrencyCode,
        transactionFilters: TransactionFilters = this.transactionFilters
    ): AllTransactionsUiState {
        return copy(
            expenses = expenses,
            categories = categories,
            tagUsage = tagUsage,
            baseCurrencyCode = baseCurrencyCode,
            transactionFilters = transactionFilters,
            transactionData = buildAllTransactionsData(
                expenses = expenses,
                filters = transactionFilters,
                timeZone = TimeZone.currentSystemDefault()
            )
        )
    }

    private fun buildAllTransactionsData(
        expenses: List<Expense>,
        filters: TransactionFilters,
        timeZone: TimeZone
    ): CalendarData {
        val datedTransactions = expenses
            .filterByTransactionFilters(filters)
            .sortedByDescending { it.spentAtMillis }
            .map { expense -> expense to expense.spentDate(timeZone) }

        val groupedTransactions = datedTransactions
            .groupBy(keySelector = { (_, spentDate) -> spentDate }, valueTransform = { (expense, _) -> expense })
            .toList()
        val transactionItems = buildList {
            groupedTransactions.forEach { (date, dayExpenses) ->
                add(DateTransactionListItem.Header(date, dayExpenses.sumOf { it.baseAmountCents }))
                dayExpenses.forEach { expense ->
                    add(DateTransactionListItem.Transaction(expense))
                }
            }
        }

        return CalendarData(
            monthTransactionCount = datedTransactions.size,
            transactionItems = transactionItems,
            filteredMonthTotal = datedTransactions.sumOf { (expense, _) -> expense.baseAmountCents }
        )
    }
}
