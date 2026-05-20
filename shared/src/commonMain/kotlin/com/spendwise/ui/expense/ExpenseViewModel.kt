package com.spendwise.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.ActiveTagToken
import com.spendwise.domain.AddExpenseInput
import com.spendwise.domain.Expense
import com.spendwise.domain.ExpenseDraft
import com.spendwise.domain.SpendWiseSnapshot
import com.spendwise.domain.TagParser
import com.spendwise.domain.usecase.SpendWiseUseCases
import com.spendwise.ui.ExpenseUiState
import com.spendwise.ui.centsToAmountText
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.emptyDraft
import com.spendwise.ui.filterCurrencyAmountInput
import com.spendwise.ui.filterDecimalInput
import com.spendwise.ui.sanitizeAmountTextForCurrency
import com.spendwise.ui.toCentsOrNull
import com.spendwise.ui.toExchangeRateText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Clock

class ExpenseViewModel(
    repository: ExpenseRepository,
    private val useCases: SpendWiseUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaults()
            repository.observeSnapshot().collect { snapshot ->
                _uiState.update { state -> state.mergeSnapshot(snapshot) }
            }
        }
    }

    fun updateAmount(value: String) {
        _uiState.update { state ->
            state.copy(
                draft = state.draft.copy(
                    amountText = sanitizeAmountTextForCurrency(value, state.draft.currencyCode)
                )
            )
        }
    }

    fun updateCurrency(value: String) {
        _uiState.update { state ->
            val rate = if (value == state.baseCurrencyCode) "1.0" else state.draft.exchangeRateText
            val fractionDigits = currencyDisplayFormat(value).fractionDigits
            state.copy(
                draft = state.draft.copy(
                    currencyCode = value,
                    amountText = state.draft.amountText.filterCurrencyAmountInput(fractionDigits),
                    exchangeRateText = rate
                )
            )
        }
        refreshExchangeRate()
    }

    fun updateCategory(id: Long) {
        _uiState.update { it.copy(draft = it.draft.copy(categoryId = id)) }
    }

    fun updateNote(value: String, cursor: Int = value.length) {
        _uiState.update { state ->
            val token = TagParser.activeToken(value, cursor)
            state.copy(
                draft = state.draft.copy(note = value),
                activeTagToken = token,
                tagSuggestions = tagSuggestions(token, state)
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
        _uiState.update { it.copy(draft = it.draft.copy(exchangeRateText = value.filterDecimalInput())) }
    }

    fun selectTodayForDraft() {
        _uiState.update { it.copy(draft = it.draft.copy(spentAtMillis = Clock.System.now().toEpochMilliseconds())) }
    }

    fun selectYesterdayForDraft() {
        _uiState.update { it.copy(draft = it.draft.copy(spentAtMillis = Clock.System.now().toEpochMilliseconds() - 86_400_000L)) }
    }

    fun selectDateForDraft(date: LocalDate) {
        val millis = date.atStartOfDayIn(TimeZone.Companion.currentSystemDefault()).toEpochMilliseconds()
        _uiState.update { it.copy(draft = it.draft.copy(spentAtMillis = millis)) }
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
        val baseAmountCents = useCases.convertToBaseCurrency(
            amountCents = amountCents,
            sourceCurrencyCode = state.draft.currencyCode,
            baseCurrencyCode = state.baseCurrencyCode,
            exchangeRate = rate
        )
        val tags = useCases.parseTagsFromNote(state.draft.note)

        viewModelScope.launch {
            val input = AddExpenseInput(
                id = state.draft.editingExpenseId,
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
            if (state.draft.editingExpenseId == null) {
                useCases.addExpense(input)
            } else {
                useCases.updateExpense(input)
            }
            _uiState.update {
                it.copy(
                    draft = emptyDraft(it.baseCurrencyCode, it.categories),
                    activeTagToken = null,
                    tagSuggestions = emptyList(),
                    message = if (state.draft.editingExpenseId == null) "Expense saved" else "Expense updated"
                )
            }
        }
    }

    fun editExpense(expense: Expense) {
        _uiState.update {
            it.copy(
                draft = ExpenseDraft(
                    editingExpenseId = expense.id,
                    amountText = centsToAmountText(expense.originalAmountCents, expense.originalCurrencyCode),
                    currencyCode = expense.originalCurrencyCode,
                    categoryId = expense.categoryId,
                    note = expense.note,
                    spentAtMillis = expense.spentAtMillis,
                    exchangeRateText = expense.exchangeRate.toString()
                ),
                activeTagToken = null,
                tagSuggestions = emptyList()
            )
        }
    }

    fun cancelExpenseEdit() {
        _uiState.update {
            it.copy(
                draft = emptyDraft(it.baseCurrencyCode, it.categories),
                activeTagToken = null,
                tagSuggestions = emptyList()
            )
        }
    }

    fun deleteEditingExpense() {
        val id = _uiState.value.draft.editingExpenseId ?: return
        viewModelScope.launch {
            useCases.deleteExpense(id)
            _uiState.update {
                it.copy(
                    draft = emptyDraft(it.baseCurrencyCode, it.categories),
                    message = "Expense deleted"
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun refreshExchangeRate() {
        val state = _uiState.value
        if (state.draft.currencyCode == state.baseCurrencyCode) return
        viewModelScope.launch {
            val rate = useCases.getExchangeRate(state.draft.currencyCode, state.baseCurrencyCode)
            if (rate != null) {
                _uiState.update { current ->
                    if (current.draft.currencyCode == state.draft.currencyCode &&
                        current.baseCurrencyCode == state.baseCurrencyCode
                    ) {
                        current.copy(draft = current.draft.copy(exchangeRateText = rate.toExchangeRateText()))
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun ExpenseUiState.mergeSnapshot(snapshot: SpendWiseSnapshot): ExpenseUiState {
        val categoryId = draft.categoryId ?: snapshot.categories.firstOrNull { !it.archived }?.id
        val nextDraft = if (draft.editingExpenseId == null && draft.amountText.isBlank() && draft.note.isBlank()) {
            draft.copy(
                categoryId = categoryId,
                currencyCode = snapshot.settings.baseCurrencyCode,
                exchangeRateText = "1.0"
            )
        } else {
            draft.copy(categoryId = categoryId)
        }
        return copy(
            expenses = snapshot.expenses,
            categories = snapshot.categories,
            tagUsage = snapshot.tagUsage,
            draft = nextDraft,
            baseCurrencyCode = snapshot.settings.baseCurrencyCode
        )
    }

    private fun tagSuggestions(token: ActiveTagToken?, state: ExpenseUiState): List<String> {
        if (token == null) return emptyList()
        return state.tagUsage
            .asSequence()
            .filter { it.name.startsWith(token.query, ignoreCase = true) }
            .sortedByDescending { it.expenseCount }
            .map { it.name }
            .take(5)
            .toList()
    }
}
