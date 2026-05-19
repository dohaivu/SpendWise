package com.spendwise.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.ActiveTagToken
import com.spendwise.domain.AddExpenseInput
import com.spendwise.domain.ExpenseDraft
import com.spendwise.domain.SpendWiseSnapshot
import com.spendwise.domain.TagParser
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
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Clock

enum class SpendWiseTab {
    Input,
    Calendar,
    Report,
    Others
}

enum class ReportPeriod {
    Month,
    Year
}

enum class AppLanguage(val label: String) {
    English("English"),
    Vietnamese("Tiếng Việt"),
    Chinese("中文")
}

data class SpendWiseUiState(
    val selectedTab: SpendWiseTab = SpendWiseTab.Input,
    val snapshot: SpendWiseSnapshot = SpendWiseSnapshot(),
    val draft: ExpenseDraft = ExpenseDraft(spentAtMillis = Clock.System.now().toEpochMilliseconds()),
    val baseCurrencyCode: String = "USD",
    val selectedMonth: LocalDate = today().firstDayOfMonth(),
    val selectedDate: LocalDate = today(),
    val selectedReportPeriod: ReportPeriod = ReportPeriod.Month,
    val selectedTags: Set<String> = emptySet(),
    val activeTagToken: ActiveTagToken? = null,
    val tagSuggestions: List<String> = emptyList(),
    val language: AppLanguage = AppLanguage.English,
    val message: String? = null
)

class SpendWiseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpendWiseUiState())
    val uiState: StateFlow<SpendWiseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaults()
            repository.observeSnapshot().collect { snapshot ->
                _uiState.update { state ->
                    val categoryId = state.draft.categoryId
                        ?: snapshot.categories.firstOrNull { !it.archived }?.id
                    state.copy(
                        snapshot = snapshot,
                        draft = state.draft.copy(categoryId = categoryId)
                    )
                }
            }
        }
    }

    fun selectTab(tab: SpendWiseTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(draft = it.draft.copy(amountText = value.filterAmountInput())) }
    }

    fun updateCurrency(value: String) {
        _uiState.update { state ->
            val rate = if (value == state.baseCurrencyCode) "1.0" else state.draft.exchangeRateText
            state.copy(draft = state.draft.copy(currencyCode = value, exchangeRateText = rate))
        }
    }

    fun updateCategory(id: Long) {
        _uiState.update { it.copy(draft = it.draft.copy(categoryId = id)) }
    }

    fun updateNote(value: String) {
        _uiState.update { state ->
            val token = TagParser.activeToken(value, value.length)
            state.copy(
                draft = state.draft.copy(note = value),
                activeTagToken = token,
                tagSuggestions = suggestionsFor(token, state.snapshot)
            )
        }
    }

    fun selectTagSuggestion(tag: String) {
        _uiState.update { state ->
            val token = state.activeTagToken ?: return@update state
            val note = TagParser.replaceActiveToken(state.draft.note, token, tag)
            state.copy(
                draft = state.draft.copy(note = note),
                activeTagToken = null,
                tagSuggestions = emptyList()
            )
        }
    }

    fun updateExchangeRate(value: String) {
        _uiState.update { it.copy(draft = it.draft.copy(exchangeRateText = value.filterAmountInput())) }
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

    fun toggleReportPeriod() {
        _uiState.update {
            it.copy(
                selectedReportPeriod = if (it.selectedReportPeriod == ReportPeriod.Month) ReportPeriod.Year else ReportPeriod.Month
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

    fun clearTagFilters() {
        _uiState.update { it.copy(selectedTags = emptySet()) }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
    }

    fun setBaseCurrency(currency: String) {
        _uiState.update {
            it.copy(
                baseCurrencyCode = currency,
                draft = it.draft.copy(currencyCode = currency, exchangeRateText = "1.0")
            )
        }
    }

    fun saveExpense() {
        val state = _uiState.value
        val amountCents = state.draft.amountText.toCentsOrNull()
        val categoryId = state.draft.categoryId
        if (amountCents == null || amountCents <= 0L || categoryId == null) {
            _uiState.update { it.copy(message = "Enter an amount and category") }
            return
        }
        val rate = if (state.draft.currencyCode == state.baseCurrencyCode) {
            1.0
        } else {
            state.draft.exchangeRateText.toDoubleOrNull() ?: 1.0
        }
        val baseAmountCents = (amountCents * rate).roundToLong()
        val tags = TagParser.parse(state.draft.note)

        viewModelScope.launch {
            repository.addExpense(
                AddExpenseInput(
                    originalAmountCents = amountCents,
                    originalCurrencyCode = state.draft.currencyCode,
                    baseAmountCents = baseAmountCents,
                    baseCurrencyCode = state.baseCurrencyCode,
                    exchangeRate = rate,
                    categoryId = categoryId,
                    note = state.draft.note,
                    tags = tags,
                    spentAtMillis = state.draft.spentAtMillis
                )
            )
            _uiState.update {
                it.copy(
                    draft = ExpenseDraft(
                        currencyCode = it.baseCurrencyCode,
                        categoryId = it.snapshot.categories.firstOrNull { category -> !category.archived }?.id,
                        spentAtMillis = Clock.System.now().toEpochMilliseconds()
                    ),
                    activeTagToken = null,
                    tagSuggestions = emptyList(),
                    message = "Expense saved"
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun suggestionsFor(token: ActiveTagToken?, snapshot: SpendWiseSnapshot): List<String> {
        if (token == null) return emptyList()
        return snapshot.tagUsage
            .asSequence()
            .filter { it.name.startsWith(token.query, ignoreCase = true) }
            .sortedByDescending { it.expenseCount }
            .map { it.name }
            .take(5)
            .toList()
    }
}

val supportedCurrencies = listOf("USD", "VND", "CNY", "EUR", "JPY", "SGD")

fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalDate.firstDayOfMonth(): LocalDate = LocalDate(year, month, 1)

private fun String.filterAmountInput(): String =
    filter { it.isDigit() || it == '.' }.let { value ->
        val firstDot = value.indexOf('.')
        if (firstDot < 0) value else value.take(firstDot + 1) + value.drop(firstDot + 1).replace(".", "")
    }

private fun String.toCentsOrNull(): Long? {
    val amount = toDoubleOrNull() ?: return null
    return (amount * 100).roundToLong()
}
