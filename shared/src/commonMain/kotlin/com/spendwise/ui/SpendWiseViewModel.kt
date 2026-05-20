package com.spendwise.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.ActiveTagToken
import com.spendwise.domain.AddExpenseInput
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryReportRow
import com.spendwise.domain.CategoryDraft
import com.spendwise.domain.DailyExpenseTotal
import com.spendwise.domain.Expense
import com.spendwise.domain.ExpenseDraft
import com.spendwise.domain.SpendWiseSnapshot
import com.spendwise.domain.TagParser
import com.spendwise.domain.TagUsage
import com.spendwise.domain.TransactionFilters
import com.spendwise.domain.UserSettings
import com.spendwise.domain.usecase.SpendWiseUseCases
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.components.currencyDisplayFormats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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

enum class AppLanguage(val label: String) {
    English("English"),
    Vietnamese("Tiếng Việt"),
    Chinese("中文");

    val code: String
        get() = when (this) {
            English -> "en"
            Vietnamese -> "vi"
            Chinese -> "zh"
        }

    companion object {
        fun fromCode(code: String): AppLanguage = when (code) {
            "vi" -> Vietnamese
            "zh" -> Chinese
            else -> English
        }
    }
}

enum class TagUsageSort {
    MostUsed,
    HighestSpending,
    RecentlyUsed,
    Alphabetical
}

data class SpendWiseUiState(
    val selectedTab: SpendWiseTab = SpendWiseTab.Input,
    val snapshot: SpendWiseSnapshot = SpendWiseSnapshot(),
    val draft: ExpenseDraft = ExpenseDraft(spentAtMillis = Clock.System.now().toEpochMilliseconds()),
    val categoryDraft: CategoryDraft = CategoryDraft(),
    val transactionFilters: TransactionFilters = TransactionFilters(),
    val baseCurrencyCode: String = "USD",
    val selectedMonth: LocalDate = today().firstDayOfMonth(),
    val selectedDate: LocalDate = today(),
    val selectedReportCategoryId: Long? = null,
    val selectedTags: Set<String> = emptySet(),
    val tagUsageSort: TagUsageSort = TagUsageSort.MostUsed,
    val activeTagToken: ActiveTagToken? = null,
    val tagSuggestions: List<String> = emptyList(),
    val language: AppLanguage = AppLanguage.English,
    val message: String? = null
)

class SpendWiseViewModel(
    private val repository: ExpenseRepository,
    private val useCases: SpendWiseUseCases
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
                    val draft = if (state.draft.editingExpenseId == null &&
                        state.draft.amountText.isBlank() &&
                        state.draft.note.isBlank()
                    ) {
                        state.draft.copy(
                            categoryId = categoryId,
                            currencyCode = snapshot.settings.baseCurrencyCode,
                            exchangeRateText = "1.0"
                        )
                    } else {
                        state.draft.copy(categoryId = categoryId)
                    }
                    state.copy(
                        snapshot = snapshot,
                        draft = draft,
                        baseCurrencyCode = snapshot.settings.baseCurrencyCode,
                        language = AppLanguage.fromCode(snapshot.settings.languageCode)
                    )
                }
            }
        }
    }

    fun selectTab(tab: SpendWiseTab) {
        _uiState.update { it.copy(selectedTab = tab, selectedReportCategoryId = null) }
    }

    fun handleBackNavigation() {
        _uiState.update { state ->
            if (state.selectedTab == SpendWiseTab.Report && state.selectedReportCategoryId != null) {
                return@update state.copy(selectedReportCategoryId = null)
            }
            state.selectedTab.backDestination()?.let { state.copy(selectedTab = it) } ?: state
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
                tagSuggestions = useCases.getTagAutocompleteSuggestions(token, state.snapshot)
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
        val millis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        _uiState.update { it.copy(draft = it.draft.copy(spentAtMillis = millis)) }
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

    fun toggleTagFilter(tag: String) {
        val normalized = TagParser.normalize(tag)
        _uiState.update {
            val next = if (normalized in it.selectedTags) it.selectedTags - normalized else it.selectedTags + normalized
            it.copy(selectedTags = next)
        }
    }

    fun openReportCategory(categoryId: Long) {
        _uiState.update { it.copy(selectedReportCategoryId = categoryId) }
    }

    fun closeReportCategory() {
        _uiState.update { it.copy(selectedReportCategoryId = null) }
    }

    fun clearTagFilters() {
        _uiState.update { it.copy(selectedTags = emptySet()) }
    }

    fun setTagUsageSort(sort: TagUsageSort) {
        _uiState.update { it.copy(tagUsageSort = sort) }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
        persistSettings()
    }

    fun setBaseCurrency(currency: String) {
        _uiState.update {
            it.copy(
                baseCurrencyCode = currency,
                draft = it.draft.copy(currencyCode = currency, exchangeRateText = "1.0")
            )
        }
        viewModelScope.launch { useCases.updateBaseCurrency(currency) }
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
                    draft = emptyDraft(it),
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
                selectedTab = SpendWiseTab.Input,
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
        _uiState.update { it.copy(draft = emptyDraft(it), activeTagToken = null, tagSuggestions = emptyList()) }
    }

    fun deleteEditingExpense() {
        val id = _uiState.value.draft.editingExpenseId ?: return
        viewModelScope.launch {
            useCases.deleteExpense(id)
            _uiState.update { it.copy(draft = emptyDraft(it), message = "Expense deleted") }
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

    fun editCategory(category: Category) {
        _uiState.update {
            it.copy(
                categoryDraft = CategoryDraft(
                    editingCategoryId = category.id,
                    name = category.name,
                    icon = category.icon,
                    color = category.color
                )
            )
        }
    }

    fun updateCategoryName(value: String) {
        _uiState.update { it.copy(categoryDraft = it.categoryDraft.copy(name = value)) }
    }

    fun updateCategoryIcon(value: String) {
        _uiState.update { it.copy(categoryDraft = it.categoryDraft.copy(icon = value.take(4))) }
    }

    fun updateCategoryColor(value: Long) {
        _uiState.update { it.copy(categoryDraft = it.categoryDraft.copy(color = value)) }
    }

    fun saveCategory() {
        val draft = _uiState.value.categoryDraft
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(message = "Category name is required") }
            return
        }
        viewModelScope.launch {
            useCases.saveCategory(draft)
            _uiState.update { it.copy(categoryDraft = CategoryDraft(), message = "Category saved") }
        }
    }

    fun cancelCategoryEdit() {
        _uiState.update { it.copy(categoryDraft = CategoryDraft()) }
    }

    fun archiveCategory(id: Long) {
        viewModelScope.launch {
            useCases.archiveCategory(id)
            _uiState.update { it.copy(message = "Category archived") }
        }
    }

    fun moveCategoryUp(id: Long) {
        viewModelScope.launch { useCases.moveCategory(id, -1) }
    }

    fun moveCategoryDown(id: Long) {
        viewModelScope.launch { useCases.moveCategory(id, 1) }
    }

    fun getDailyExpenseTotals(timeZone: TimeZone): List<DailyExpenseTotal> {
        return useCases.getDailyExpenseTotals(_uiState.value.snapshot.expenses, timeZone)
    }

    fun getTransactionsForSelectedDate(timeZone: TimeZone, ignoreCurrencyFilter: Boolean = false): List<Expense> {
        val state = _uiState.value
        val filters = if (ignoreCurrencyFilter) {
            state.transactionFilters.copy(currencyCode = null)
        } else {
            state.transactionFilters
        }
        return useCases.getTransactionsByDate(
            expenses = state.snapshot.expenses,
            date = state.selectedDate,
            timeZone = timeZone,
            selectedTags = state.selectedTags,
            filters = filters
        )
    }

    fun getFilteredTransactions(): List<Expense> {
        val state = _uiState.value
        return useCases.getTransactionsByFilters(
            expenses = state.snapshot.expenses,
            filters = state.transactionFilters,
            selectedTags = state.selectedTags
        )
    }

    fun getCategoryReport(expenses: List<Expense>): List<CategoryReportRow> {
        val state = _uiState.value
        return useCases.getCategoryPieReport(expenses, state.snapshot.categories, state.selectedTags)
    }

    fun getYearlyCategoryReport(year: Int, timeZone: TimeZone): List<CategoryReportRow> {
        val state = _uiState.value
        return useCases.getYearlyCategoryReport(
            expenses = state.snapshot.expenses,
            categories = state.snapshot.categories,
            year = year,
            selectedTags = state.selectedTags,
            timeZone = timeZone
        )
    }

    fun getSortedTagUsage(): List<TagUsage> {
        val state = _uiState.value
        return when (state.tagUsageSort) {
            TagUsageSort.MostUsed -> state.snapshot.tagUsage.sortedWith(compareByDescending<TagUsage> { it.expenseCount }.thenBy { it.name })
            TagUsageSort.HighestSpending -> state.snapshot.tagUsage.sortedByDescending { it.totalBaseAmountCents }
            TagUsageSort.RecentlyUsed -> state.snapshot.tagUsage.sortedByDescending { it.lastUsedAtMillis }
            TagUsageSort.Alphabetical -> state.snapshot.tagUsage.sortedBy { it.name }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun persistSettings() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveSettings(
                UserSettings(
                    baseCurrencyCode = state.baseCurrencyCode,
                    languageCode = state.language.code
                )
            )
        }
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
}

val supportedCurrencies = currencyDisplayFormats.map { it.code }

internal fun SpendWiseTab.backDestination(): SpendWiseTab? =
    if (this == SpendWiseTab.Input) null else SpendWiseTab.Input

fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalDate.firstDayOfMonth(): LocalDate = LocalDate(year, month, 1)

fun centsToAmountText(cents: Long, currencyCode: String): String {
    val fractionDigits = currencyDisplayFormat(currencyCode).fractionDigits
    val whole = cents / 100
    val fraction = cents % 100
    if (fractionDigits == 0) return whole.toString()
    return if (fraction == 0L) whole.toString() else "$whole.${fraction.toString().padStart(2, '0')}"
}

internal fun sanitizeAmountTextForCurrency(value: String, currencyCode: String): String {
    val fractionDigits = currencyDisplayFormat(currencyCode).fractionDigits
    return value.filterCurrencyAmountInput(fractionDigits)
}

private fun emptyDraft(state: SpendWiseUiState): ExpenseDraft =
    ExpenseDraft(
        currencyCode = state.baseCurrencyCode,
        categoryId = state.snapshot.categories.firstOrNull { category -> !category.archived }?.id,
        spentAtMillis = Clock.System.now().toEpochMilliseconds()
    )

private fun String.filterCurrencyAmountInput(fractionDigits: Int): String {
    if (fractionDigits == 0) return filter { it.isDigit() }

    val value = filter { it.isDigit() || it == '.' }
    val firstDot = value.indexOf('.')
    if (firstDot < 0) return value

    val whole = value.take(firstDot + 1)
    val fraction = value.drop(firstDot + 1).replace(".", "").take(fractionDigits)
    return whole + fraction
}

private fun String.filterDecimalInput(): String =
    filter { it.isDigit() || it == '.' }.let { value ->
        val firstDot = value.indexOf('.')
        if (firstDot < 0) value else value.take(firstDot + 1) + value.drop(firstDot + 1).replace(".", "")
    }

private fun String.toCentsOrNull(): Long? {
    val amount = toDoubleOrNull() ?: return null
    return (amount * 100).roundToLong()
}

private fun Double.toExchangeRateText(): String =
    toString().trimEnd('0').trimEnd('.').ifBlank { "1" }
