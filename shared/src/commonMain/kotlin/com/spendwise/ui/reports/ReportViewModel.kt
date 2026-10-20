package com.spendwise.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.data.ExpenseWindow
import com.spendwise.domain.Expense
import com.spendwise.domain.ExpenseEntry
import com.spendwise.domain.ExpenseMatcher
import com.spendwise.domain.ReportAggregator
import com.spendwise.domain.ReportSummary
import com.spendwise.domain.TagParser
import com.spendwise.domain.TransactionFilters
import com.spendwise.domain.prepareExpenseEntries
import com.spendwise.ui.DateTransactionListItem
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.firstDayOfMonth
import com.spendwise.ui.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    repository: ExpenseRepository
) : ViewModel() {
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    /**
     * The selected year plus the previous year: the annual report needs the current year and the
     * category detail chart needs up to twelve trailing months.
     */
    private val requestedWindow = MutableStateFlow(windowFor(_uiState.value))

    /** Expenses for [requestedWindow], prepared once per emission. */
    private var entries: List<ExpenseEntry> = emptyList()

    private var queryRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            requestedWindow
                .flatMapLatest { window -> repository.observeSnapshot(window).map { window to it } }
                .collect { (window, snapshot) ->
                    val prepared = withContext(Dispatchers.Default) {
                        prepareExpenseEntries(snapshot.expenses, timeZone)
                    }
                    // A newer year superseded this one while the data was being prepared.
                    if (requestedWindow.value != window) return@collect
                    queryRefreshJob?.cancel()
                    entries = prepared
                    val baseCurrency = currencyDisplayFormat(snapshot.settings.baseCurrencyCode)
                    _uiState.update { state ->
                        val next = state.copy(
                            categories = snapshot.categories,
                            tagUsage = snapshot.tagUsage,
                            baseCurrency = baseCurrency
                        )
                        next.copy(report = summarize(next, prepared))
                    }
                }
        }
    }

    fun previousMonth() = changePeriod(_uiState.value.selectedMonth.minus(1, DateTimeUnit.MONTH))

    fun nextMonth() = changePeriod(_uiState.value.selectedMonth.plus(1, DateTimeUnit.MONTH))

    fun previousYear() = changePeriod(_uiState.value.selectedMonth.minus(1, DateTimeUnit.YEAR))

    fun nextYear() = changePeriod(_uiState.value.selectedMonth.plus(1, DateTimeUnit.YEAR))

    fun resetToCurrentPeriod() = changePeriod(today().firstDayOfMonth())

    fun selectMonth(month: LocalDate) = changePeriod(month.firstDayOfMonth())

    fun toggleTagFilter(tag: String) {
        val normalized = TagParser.normalize(tag)
        updateFilters { filters ->
            val next = if (normalized in filters.selectedTags) {
                filters.selectedTags - normalized
            } else {
                filters.selectedTags + normalized
            }
            filters.copy(selectedTags = next)
        }
    }

    fun updateTransactionQuery(value: String) {
        updateFilters(debounce = true) { it.copy(query = value) }
    }

    fun updateTransactionCategory(categoryId: Long?) {
        updateFilters { it.copy(categoryId = categoryId) }
    }

    fun openReportCategory(categoryId: Long) {
        _uiState.update { it.copy(selectedReportCategoryId = categoryId) }
    }

    fun closeReportCategory() {
        _uiState.update { it.copy(selectedReportCategoryId = null) }
    }

    /**
     * Transactions of one category for one month, backed by the cached entries. Callers should
     * memoize this against [ReportUiState.report] and the selected month.
     */
    fun getCategoryTransactions(categoryId: Long, month: LocalDate): List<DateTransactionListItem> {
        val matcher = ExpenseMatcher.of(_uiState.value.transactionFilters.copy(categoryId = null))
        val items = ArrayList<DateTransactionListItem>()

        var bucketDate: LocalDate? = null
        var bucketExpenses = ArrayList<Expense>()
        var bucketTotal = 0L

        fun flushBucket() {
            val date = bucketDate ?: return
            items += DateTransactionListItem(date, bucketTotal, bucketExpenses)
            bucketExpenses = ArrayList()
            bucketTotal = 0L
        }

        for (entry in entries) {
            if (entry.date.year != month.year || entry.date.month != month.month) continue
            if (entry.expense.categoryId != categoryId) continue
            if (!matcher.matches(entry)) continue

            if (entry.date != bucketDate) {
                flushBucket()
                bucketDate = entry.date
            }
            bucketExpenses += entry.expense
            bucketTotal += entry.expense.baseAmountCents
        }
        flushBucket()

        return items
    }

    private fun changePeriod(selectedMonth: LocalDate) {
        queryRefreshJob?.cancel()
        val state = _uiState.updateAndGet { current ->
            val next = current.copy(selectedMonth = selectedMonth)
            next.copy(report = summarize(next, entries))
        }
        requestedWindow.value = windowFor(state)
    }

    private fun updateFilters(
        debounce: Boolean = false,
        transform: (TransactionFilters) -> TransactionFilters
    ) {
        _uiState.update { state ->
            val filters = transform(state.transactionFilters)
            val next = state.copy(transactionFilters = filters)
            if (debounce) next else next.copy(report = summarize(next, entries))
        }
        queryRefreshJob?.cancel()
        if (!debounce) return

        queryRefreshJob = viewModelScope.launch {
            delay(QUERY_DEBOUNCE_MILLIS)
            val state = _uiState.value
            val snapshotEntries = entries
            val report = withContext(Dispatchers.Default) {
                summarize(state, snapshotEntries)
            }
            _uiState.update { current ->
                val isStillCurrent = entries === snapshotEntries &&
                    current.transactionFilters == state.transactionFilters &&
                    current.selectedMonth == state.selectedMonth
                if (isStillCurrent) current.copy(report = report) else current
            }
        }
    }

    private fun summarize(state: ReportUiState, entries: List<ExpenseEntry>): ReportSummary =
        ReportAggregator.summarize(
            entries = entries,
            categories = state.categories,
            selectedMonth = state.selectedMonth,
            filters = state.transactionFilters
        )

    private fun windowFor(state: ReportUiState): ExpenseWindow {
        val year = state.selectedMonth.year
        val start = LocalDate(year - 1, 1, 1)
        val end = LocalDate(year + 1, 1, 1)
        return ExpenseWindow(
            startMillis = start.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            endMillis = end.atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
    }

    private companion object {
        const val QUERY_DEBOUNCE_MILLIS = 120L
    }
}
