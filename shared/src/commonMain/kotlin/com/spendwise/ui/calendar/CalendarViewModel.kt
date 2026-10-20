package com.spendwise.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.data.ExpenseWindow
import com.spendwise.domain.TagParser
import com.spendwise.domain.TransactionFilters
import com.spendwise.ui.CalendarData
import com.spendwise.ui.CalendarUiState
import com.spendwise.ui.components.ReportPeriod
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
class CalendarViewModel(
    repository: ExpenseRepository
) : ViewModel() {
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    /** The period currently displayed. Changing it re-subscribes to a narrower database query. */
    private val requestedWindow = MutableStateFlow(windowFor(_uiState.value))

    /** Expenses for [requestedWindow], prepared once per emission so filtering is allocation-free. */
    private var entries: List<CalendarExpenseEntry> = emptyList()

    private var queryRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            requestedWindow
                .flatMapLatest { window -> repository.observeSnapshot(window).map { window to it } }
                .collect { (window, snapshot) ->
                    val prepared = withContext(Dispatchers.Default) {
                        CalendarDataBuilder.prepare(snapshot.expenses, timeZone)
                    }
                    // A newer period superseded this one while the data was being prepared.
                    if (requestedWindow.value != window) return@collect
                    queryRefreshJob?.cancel()
                    entries = prepared
                    val baseCurrency = currencyDisplayFormat(snapshot.settings.baseCurrencyCode)
                    _uiState.update { state ->
                        state.copy(
                            categories = snapshot.categories,
                            tagUsage = snapshot.tagUsage,
                            baseCurrency = baseCurrency,
                            calendarData = CalendarDataBuilder.build(
                                entries = prepared,
                                period = state.selectedPeriod,
                                month = state.selectedMonth,
                                filters = state.transactionFilters
                            )
                        )
                    }
                }
        }
    }

    fun selectMonth(month: LocalDate) = changePeriod(selectedMonth = month.firstDayOfMonth())

    fun previousMonth() = changePeriod(selectedMonth = _uiState.value.selectedMonth.minus(1, DateTimeUnit.MONTH))

    fun nextMonth() = changePeriod(selectedMonth = _uiState.value.selectedMonth.plus(1, DateTimeUnit.MONTH))

    fun previousYear() = changePeriod(selectedMonth = _uiState.value.selectedMonth.minus(1, DateTimeUnit.YEAR))

    fun nextYear() = changePeriod(selectedMonth = _uiState.value.selectedMonth.plus(1, DateTimeUnit.YEAR))

    fun selectPeriod(period: ReportPeriod) = changePeriod(selectedPeriod = period)

    fun resetToToday() {
        val today = today()
        changePeriod(selectedMonth = today.firstDayOfMonth(), selectedDate = today)
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

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

    fun updateTransactionCategory(categoryId: Long?) {
        updateFilters { it.copy(categoryId = categoryId) }
    }

    fun updateTransactionQuery(value: String) {
        updateFilters(debounce = true) { it.copy(query = value) }
    }

    private fun changePeriod(
        selectedMonth: LocalDate = _uiState.value.selectedMonth,
        selectedDate: LocalDate = _uiState.value.selectedDate,
        selectedPeriod: ReportPeriod = _uiState.value.selectedPeriod
    ) {
        queryRefreshJob?.cancel()
        val state = _uiState.updateAndGet { current ->
            val next = current.copy(
                selectedMonth = selectedMonth,
                selectedDate = selectedDate,
                selectedPeriod = selectedPeriod
            )
            next.copy(calendarData = buildFrom(next))
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
            if (debounce) {
                next
            } else {
                next.copy(calendarData = CalendarDataBuilder.build(entries, next.selectedPeriod, next.selectedMonth, filters))
            }
        }
        queryRefreshJob?.cancel()
        if (!debounce) return

        queryRefreshJob = viewModelScope.launch {
            delay(QUERY_DEBOUNCE_MILLIS)
            val state = _uiState.value
            val snapshotEntries = entries
            val data = withContext(Dispatchers.Default) {
                CalendarDataBuilder.build(
                    entries = snapshotEntries,
                    period = state.selectedPeriod,
                    month = state.selectedMonth,
                    filters = state.transactionFilters
                )
            }
            _uiState.update { current ->
                val isStillCurrent = entries === snapshotEntries &&
                    current.transactionFilters == state.transactionFilters &&
                    current.selectedMonth == state.selectedMonth &&
                    current.selectedPeriod == state.selectedPeriod
                if (isStillCurrent) current.copy(calendarData = data) else current
            }
        }
    }

    private fun buildFrom(state: CalendarUiState): CalendarData =
        CalendarDataBuilder.build(
            entries = entries,
            period = state.selectedPeriod,
            month = state.selectedMonth,
            filters = state.transactionFilters
        )

    private fun windowFor(state: CalendarUiState): ExpenseWindow =
        windowFor(state.selectedPeriod, state.selectedMonth)

    private fun windowFor(period: ReportPeriod, month: LocalDate): ExpenseWindow {
        val start = when (period) {
            ReportPeriod.Month -> month.firstDayOfMonth()
            ReportPeriod.Annual -> LocalDate(month.year, 1, 1)
        }
        val end = when (period) {
            ReportPeriod.Month -> start.plus(1, DateTimeUnit.MONTH)
            ReportPeriod.Annual -> start.plus(1, DateTimeUnit.YEAR)
        }
        return ExpenseWindow(
            startMillis = start.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            endMillis = end.atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
    }

    private companion object {
        const val QUERY_DEBOUNCE_MILLIS = 120L
    }
}
