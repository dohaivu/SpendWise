package com.spendwise.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.data.CsvExpenseRow
import com.spendwise.data.ExpenseRepository
import com.spendwise.data.csvDuplicateKey
import com.spendwise.data.formatSpendWiseCsv
import com.spendwise.data.parseSpendWiseCsv
import com.spendwise.data.spentAtMillis
import com.spendwise.domain.AddExpenseInput
import com.spendwise.domain.AppConfig
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryDraft
import com.spendwise.domain.ExpenseReminder
import com.spendwise.domain.TagUsage
import com.spendwise.domain.UserSettings
import com.spendwise.domain.usecase.SpendWiseUseCases
import com.spendwise.platform.BackupScheduler
import com.spendwise.platform.ReminderScheduler
import com.spendwise.ui.AppLanguage
import com.spendwise.ui.AppColorSchemeMode
import com.spendwise.ui.AppThemeMode
import com.spendwise.ui.SettingsUiState
import com.spendwise.ui.TagUsageSort
import com.spendwise.ui.components.currencyDisplayFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val useCases: SpendWiseUseCases,
    private val reminderScheduler: ReminderScheduler,
    private val backupScheduler: BackupScheduler,
    private val appConfig: AppConfig,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    val versionName: String = appConfig.versionName

    init {
        viewModelScope.launch {
            var scheduledReminders = emptyList<ExpenseReminder>()
            var lastBackupFolderUri: String? = null
            repository.observeSnapshot().collect { snapshot ->
                val reminders = snapshot.reminders.sortedBy { reminder -> reminder.minutesSinceMidnight }
                _uiState.update {
                    it.copy(
                        expenses = snapshot.expenses,
                        categories = snapshot.categories,
                        tagUsage = snapshot.tagUsage,
                        baseCurrency = currencyDisplayFormat(snapshot.settings.baseCurrencyCode),
                        language = AppLanguage.Companion.fromCode(snapshot.settings.languageCode),
                        themeMode = AppThemeMode.Companion.fromCode(snapshot.settings.themeModeCode),
                        colorSchemeMode = AppColorSchemeMode.Companion.fromCode(snapshot.settings.colorSchemeModeCode),
                        reminders = reminders,
                        backupFolderUri = snapshot.settings.backupFolderUri,
                        backupFolderName = snapshot.settings.backupFolderName
                    )
                }
                if (scheduledReminders != reminders) {
                    scheduledReminders = reminders
                    reminderScheduler.schedule(reminders)
                }
                if (lastBackupFolderUri != snapshot.settings.backupFolderUri) {
                    lastBackupFolderUri = snapshot.settings.backupFolderUri
                    if (lastBackupFolderUri != null) {
                        backupScheduler.scheduleDailyBackup()
                    } else {
                        backupScheduler.cancelDailyBackup()
                    }
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

    fun setThemeMode(themeMode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = themeMode) }
        persistSettings()
    }

    fun setColorSchemeMode(colorSchemeMode: AppColorSchemeMode) {
        _uiState.update { it.copy(colorSchemeMode = colorSchemeMode) }
        persistSettings()
    }

    fun setBaseCurrency(currency: String) {
        _uiState.update {
            it.copy(
                baseCurrency = currencyDisplayFormat(currency)
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

    fun exportCsv(): String {
        val state = _uiState.value
        return formatSpendWiseCsv(state.expenses, state.categories)
    }

    fun importCsv(csvText: String) {
        val result = parseSpendWiseCsv(csvText)
        if (result.rows.isEmpty()) {
            val errorSummary = result.errors.firstOrNull()?.message ?: "No valid rows found"
            _uiState.update { it.copy(message = "CSV import failed: $errorSummary") }
            return
        }

        viewModelScope.launch {
            val initialState = _uiState.value
            val timeZone = TimeZone.currentSystemDefault()
            val categoryByName = initialState.categories.associateBy { it.name.trim().lowercase() }.toMutableMap()
            val duplicateKeys = initialState.expenses
                .map { expense -> expense.csvDuplicateKey(initialState.categories, timeZone) }
                .toMutableSet()
            var imported = 0
            var skippedDuplicates = 0

            result.rows.forEach { row ->
                val key = row.csvDuplicateKey()
                if (!duplicateKeys.add(key)) {
                    skippedDuplicates++
                    return@forEach
                }

                val category = categoryByName.getOrPut(row.categoryName.trim().lowercase()) {
                    val id = useCases.saveCategory(
                        CategoryDraft(
                            name = row.categoryName,
                            icon = "other",
                            color = 0xFF457B9D
                        )
                    )
                    Category(
                        id = id,
                        name = row.categoryName,
                        icon = "other",
                        color = 0xFF457B9D,
                        sortOrder = categoryByName.size
                    )
                }

                useCases.addExpense(
                    AddExpenseInput(
                        originalAmountCents = row.amountCents,
                        originalCurrencyCode = row.currencyCode,
                        baseAmountCents = row.amountCents,
                        baseCurrencyCode = row.currencyCode,
                        exchangeRate = 1.0,
                        categoryId = category.id,
                        note = row.note,
                        tags = useCases.parseTagsFromNote(row.note),
                        spentAtMillis = row.spentAtMillis(timeZone)
                    )
                )
                imported++
            }

            result.rows.map { it.currencyCode }.distinct().singleOrNull()?.let { currency ->
                useCases.updateBaseCurrency(currency)
            }

            _uiState.update {
                it.copy(message = importSummary(imported, skippedDuplicates, result.errors.size))
            }
        }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
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

    fun renameTag(oldTag: String, newTag: String) {
        if (newTag.isBlank()) {
            _uiState.update { it.copy(message = "Tag name is required") }
            return
        }
        viewModelScope.launch {
            useCases.renameTag(oldTag, newTag)
            _uiState.update { it.copy(message = "Tag renamed") }
        }
    }

    fun deleteTag(tag: String) {
        viewModelScope.launch {
            useCases.deleteTag(tag)
            _uiState.update { it.copy(message = "Tag deleted") }
        }
    }

    fun setBackupFolderUri(uri: String?, name: String?) {
        _uiState.update { it.copy(backupFolderUri = uri, backupFolderName = name) }
        persistSettings()
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun persistSettings() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveSettings(
                UserSettings(
                    baseCurrencyCode = state.baseCurrency.code,
                    languageCode = state.language.code,
                    themeModeCode = state.themeMode.code,
                    colorSchemeModeCode = state.colorSchemeMode.code,
                    backupFolderUri = state.backupFolderUri,
                    backupFolderName = state.backupFolderName
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

    private fun CsvExpenseRow.csvDuplicateKey(): String =
        csvDuplicateKey(date, amountCents, currencyCode, categoryName, note)

    private fun importSummary(imported: Int, skippedDuplicates: Int, invalidRows: Int): String {
        val parts = mutableListOf<String>()
        parts += "Imported $imported ${if (imported == 1) "expense" else "expenses"}"
        if (skippedDuplicates > 0) {
            parts += "skipped $skippedDuplicates duplicates"
        }
        if (invalidRows > 0) {
            parts += "ignored $invalidRows invalid rows"
        }
        return parts.joinToString(", ")
    }
}
