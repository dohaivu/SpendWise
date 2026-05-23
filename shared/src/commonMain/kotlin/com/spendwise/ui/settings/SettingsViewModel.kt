package com.spendwise.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryDraft
import com.spendwise.domain.ExpenseReminder
import com.spendwise.domain.TagUsage
import com.spendwise.domain.UserSettings
import com.spendwise.domain.usecase.SpendWiseUseCases
import com.spendwise.platform.ReminderScheduler
import com.spendwise.ui.AppLanguage
import com.spendwise.ui.SettingsUiState
import com.spendwise.ui.TagUsageSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val useCases: SpendWiseUseCases,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var scheduledReminders = emptyList<ExpenseReminder>()
            repository.observeSnapshot().collect { snapshot ->
                val reminders = snapshot.reminders.sortedBy { reminder -> reminder.minutesSinceMidnight }
                _uiState.update {
                    it.copy(
                        expenses = snapshot.expenses,
                        categories = snapshot.categories,
                        tagUsage = snapshot.tagUsage,
                        baseCurrencyCode = snapshot.settings.baseCurrencyCode,
                        language = AppLanguage.Companion.fromCode(snapshot.settings.languageCode),
                        reminders = reminders
                    )
                }
                if (scheduledReminders != reminders) {
                    scheduledReminders = reminders
                    reminderScheduler.schedule(reminders)
                }
            }
        }
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
                baseCurrencyCode = currency
            )
        }
        viewModelScope.launch { useCases.updateBaseCurrency(currency) }
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
        _uiState.update { it.copy(categoryDraft = it.categoryDraft.copy(icon = value)) }
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

    fun deleteCategory(id: Long) {
        _uiState.update { state ->
            state.copy(categories = state.categories.filterNot { it.id == id })
        }
        viewModelScope.launch {
            useCases.deleteCategory(id)
            _uiState.update { it.copy(message = "Category deleted") }
        }
    }

    fun addReminder(hour: Int, minute: Int) {
        val state = _uiState.value
        if (state.reminders.any { it.hour == hour && it.minute == minute }) {
            _uiState.update { it.copy(message = "Reminder already exists") }
            return
        }
        viewModelScope.launch {
            repository.saveReminder(ExpenseReminder(id = 0L, hour = hour, minute = minute, enabled = true))
            _uiState.update { it.copy(message = "Reminder added") }
        }
    }

    fun setReminderEnabled(id: Long, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(reminders = state.reminders.map { if (it.id == id) it.copy(enabled = enabled) else it })
        }
        viewModelScope.launch { repository.setReminderEnabled(id, enabled) }
    }

    fun deleteReminder(id: Long) {
        _uiState.update { state ->
            state.copy(reminders = state.reminders.filterNot { it.id == id })
        }
        viewModelScope.launch {
            repository.deleteReminder(id)
            _uiState.update { it.copy(message = "Reminder deleted") }
        }
    }

    fun moveCategoryUp(id: Long) {
        moveCategory(id, -1)
    }

    fun moveCategoryDown(id: Long) {
        moveCategory(id, 1)
    }

    fun getSortedTagUsage(): List<TagUsage> {
        val state = _uiState.value
        return when (state.tagUsageSort) {
            TagUsageSort.MostUsed -> state.tagUsage.sortedWith(compareByDescending<TagUsage> { it.expenseCount }.thenBy { it.name })
            TagUsageSort.HighestSpending -> state.tagUsage.sortedByDescending { it.totalBaseAmountCents }
            TagUsageSort.RecentlyUsed -> state.tagUsage.sortedByDescending { it.lastUsedAtMillis }
            TagUsageSort.Alphabetical -> state.tagUsage.sortedBy { it.name }
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

    private fun moveCategory(id: Long, direction: Int) {
        _uiState.update { state ->
            state.copy(categories = state.categories.moveCategory(id, direction))
        }
        viewModelScope.launch { useCases.moveCategory(id, direction) }
    }

    private fun List<Category>.moveCategory(id: Long, direction: Int): List<Category> {
        val index = indexOfFirst { it.id == id }
        if (index < 0) return this
        val swapIndex = (index + direction).coerceIn(indices)
        if (index == swapIndex) return this

        val reordered = toMutableList().apply {
            val moved = removeAt(index)
            add(swapIndex, moved)
        }
        val updatedById = reordered
            .mapIndexed { sortOrder, category -> category.copy(sortOrder = sortOrder) }
            .associateBy { it.id }

        return map { category -> updatedById[category.id] ?: category }
            .sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.name })
    }
}
