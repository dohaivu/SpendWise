package com.spendwise.data

import com.spendwise.domain.AddExpenseInput
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryDraft
import com.spendwise.domain.Expense
import com.spendwise.domain.ExpenseReminder
import com.spendwise.domain.SpendWiseBackup
import com.spendwise.domain.SpendWiseSnapshot
import com.spendwise.domain.TagParser
import com.spendwise.domain.TagUsage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Epoch-millis window used to scope expense loading to the period a screen actually displays.
 */
data class ExpenseWindow(
    val startMillis: Long,
    val endMillis: Long
) {
    init {
        require(endMillis > startMillis) { "ExpenseWindow must have a positive duration." }
    }
}

/**
 * Reference data needed to create or edit an expense. Unlike [SpendWiseSnapshot] it carries no
 * expense rows, so screens that only need pickers and suggestions never load the history.
 */
data class ExpenseEditorSnapshot(
    val categories: List<Category> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val baseCurrencyCode: String = "VND"
)

interface ExpenseRepository {
    fun observeSnapshot(): Flow<SpendWiseSnapshot>

    /**
     * Observes a snapshot whose [SpendWiseSnapshot.expenses] only contains rows inside [window].
     * Categories, settings and tag usage stay global so filters keep working while browsing.
     */
    fun observeSnapshot(window: ExpenseWindow): Flow<SpendWiseSnapshot>

    /** Observes categories, tag usage and settings without loading any expense rows. */
    fun observeEditorSnapshot(): Flow<ExpenseEditorSnapshot>

    suspend fun seedDefaults()
    suspend fun saveExpense(input: AddExpenseInput): Long
    suspend fun deleteExpense(id: Long)
    suspend fun saveCategory(draft: CategoryDraft): Long
    suspend fun deleteCategory(id: Long)
    suspend fun moveCategory(id: Long, direction: Int)
    suspend fun saveReminder(reminder: ExpenseReminder): Long
    suspend fun setReminderEnabled(id: Long, enabled: Boolean)
    suspend fun deleteReminder(id: Long)
    suspend fun renameTag(oldTag: String, newTag: String)
    suspend fun deleteTag(tag: String)
    suspend fun getLatestExchangeRate(fromCurrencyCode: String, toCurrencyCode: String): Double?
    suspend fun getBackupCsv(): String
    suspend fun getBackupJson(): String
    suspend fun restoreFromJson(json: String)
}

class RoomExpenseRepository(
    private val dao: SpendWiseDao,
    private val settingsRepository: SettingsRepository,
    private val exchangeRateClient: FrankfurterExchangeRateClient,
    scope: CoroutineScope
) : ExpenseRepository {
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

    // Multiple screens observe the full snapshot. Sharing the upstream keeps the expense mapping
    // and tag aggregation from running once per collector.
    private val snapshot: Flow<SpendWiseSnapshot> = buildSnapshot(
        expensesFlow = dao.observeExpenses(),
        refsFlow = dao.observeExpenseTags()
    ).shareIn(scope, SharingStarted.WhileSubscribed(SNAPSHOT_STOP_TIMEOUT_MILLIS), replay = 1)

    override fun observeSnapshot(): Flow<SpendWiseSnapshot> = snapshot

    override fun observeSnapshot(window: ExpenseWindow): Flow<SpendWiseSnapshot> =
        buildSnapshot(
            expensesFlow = dao.observeExpensesInRange(window.startMillis, window.endMillis),
            refsFlow = dao.observeExpenseTagsInRange(window.startMillis, window.endMillis)
        )

    override fun observeEditorSnapshot(): Flow<ExpenseEditorSnapshot> =
        combine(
            dao.observeCategories(),
            observeTagUsage(),
            settingsRepository.settings
        ) { categoryEntities, tagUsageRows, settings ->
            ExpenseEditorSnapshot(
                categories = categoryEntities.map { it.toDomain() },
                tagUsage = tagUsageRows.map { it.toDomain() },
                baseCurrencyCode = settings.baseCurrencyCode
            )
        }

    private fun buildSnapshot(
        expensesFlow: Flow<List<ExpenseEntity>>,
        refsFlow: Flow<List<ExpenseTagEntity>>
    ): Flow<SpendWiseSnapshot> {
        val withoutReminders = combine(
            dao.observeCategories(),
            expensesFlow,
            refsFlow,
            observeTagUsage(),
            settingsRepository.settings
        ) { categoryEntities, expenseEntities, refs, tagUsageRows, settings ->
            val tagsByExpense = refs.groupBy({ it.expenseId }, { it.tagName })
            SpendWiseSnapshot(
                categories = categoryEntities.map { it.toDomain() },
                expenses = expenseEntities.map { entity ->
                    entity.toDomain(tagsByExpense[entity.id]?.sorted().orEmpty())
                },
                tagUsage = tagUsageRows.map { it.toDomain() },
                settings = settings
            )
        }
        return combine(withoutReminders, dao.observeExpenseReminders()) { snapshot, reminderEntities ->
            snapshot.copy(reminders = reminderEntities.map { it.toDomain() })
        }
    }

    private fun observeTagUsage(): Flow<List<TagUsageRow>> = flow {
        val currentMonth = Clock.System.now()
            .toLocalDateTime(timeZone)
            .date
            .let { LocalDate(it.year, it.month, 1) }
        val currentMonthStartMillis = currentMonth.atStartOfDayIn(timeZone).toEpochMilliseconds()
        emitAll(
            dao.observeTagUsage(
                currentMonthStartMillis = currentMonthStartMillis,
                nextMonthStartMillis = currentMonth
                    .plus(1, DateTimeUnit.MONTH)
                    .atStartOfDayIn(timeZone)
                    .toEpochMilliseconds(),
                previousMonthStartMillis = currentMonth
                    .minus(1, DateTimeUnit.MONTH)
                    .atStartOfDayIn(timeZone)
                    .toEpochMilliseconds()
            )
        )
    }

    override suspend fun seedDefaults() {
        if (dao.countCategories() == 0) {
            dao.insertCategories(defaultCategories.mapIndexed { index, category ->
                CategoryEntity(
                    name = category.name,
                    icon = category.icon,
                    color = category.color,
                    sortOrder = index
                )
            })
        }
    }

    override suspend fun saveExpense(input: AddExpenseInput): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val expense = ExpenseEntity(
            id = input.id ?: 0L,
            originalAmountCents = input.originalAmountCents,
            originalCurrencyCode = input.originalCurrencyCode,
            baseAmountCents = input.baseAmountCents,
            baseCurrencyCode = input.baseCurrencyCode,
            exchangeRate = input.exchangeRate,
            categoryId = input.categoryId,
            note = input.note,
            spentAtMillis = input.spentAtMillis,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        val tags = input.tags.map {
            TagEntity(
                normalizedName = TagParser.normalize(it),
                displayName = TagParser.normalize(it),
                createdAtMillis = now,
            lastUsedAtMillis = now
            )
        }
        if (input.originalCurrencyCode != input.baseCurrencyCode) {
            dao.upsertExchangeRate(
                ExchangeRateEntity(
                    fromCurrencyCode = input.originalCurrencyCode,
                    toCurrencyCode = input.baseCurrencyCode,
                    effectiveDateEpochDay = (input.spentAtMillis / 86_400_000L).toInt(),
                    rate = input.exchangeRate
                )
            )
        }
        return if (input.id == null) {
            dao.insertExpenseWithTags(expense, tags)
        } else {
            dao.updateExpenseWithTags(expense, tags)
            input.id
        }
    }

    override suspend fun deleteExpense(id: Long) {
        dao.getExpense(id)?.let { dao.deleteExpense(it) }
    }

    override suspend fun saveCategory(draft: CategoryDraft): Long {
        val existing = draft.editingCategoryId?.let { dao.getCategory(it) }
        val entity = CategoryEntity(
            id = draft.editingCategoryId ?: 0L,
            name = draft.name.trim().ifBlank { "Category" },
            icon = draft.icon.trim().ifBlank { "other" },
            color = draft.color,
            sortOrder = existing?.sortOrder ?: dao.countCategories()
        )
        return if (existing == null) {
            dao.insertCategory(entity)
        } else {
            dao.updateCategory(entity)
            entity.id
        }
    }

    override suspend fun deleteCategory(id: Long) {
        dao.deleteCategory(id)
    }

    override suspend fun moveCategory(id: Long, direction: Int) {
        dao.moveCategory(id, direction)
    }

    override suspend fun saveReminder(reminder: ExpenseReminder): Long {
        return dao.upsertExpenseReminder(
            ExpenseReminderEntity(
                id = reminder.id,
                hour = reminder.hour.coerceIn(0, 23),
                minute = reminder.minute.coerceIn(0, 59),
                enabled = reminder.enabled
            )
        )
    }

    override suspend fun setReminderEnabled(id: Long, enabled: Boolean) {
        dao.setExpenseReminderEnabled(id, enabled)
    }

    override suspend fun deleteReminder(id: Long) {
        dao.deleteExpenseReminder(id)
    }

    override suspend fun renameTag(oldTag: String, newTag: String) {
        dao.renameTag(oldTag, newTag, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun deleteTag(tag: String) {
        dao.deleteTag(tag, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun getLatestExchangeRate(fromCurrencyCode: String, toCurrencyCode: String): Double? {
        if (fromCurrencyCode == toCurrencyCode) return 1.0
        val fetched = runCatching {
            exchangeRateClient.getLatestRate(fromCurrencyCode, toCurrencyCode)
        }.getOrNull()
        if (fetched != null) {
            dao.upsertExchangeRate(
                ExchangeRateEntity(
                    fromCurrencyCode = fetched.fromCurrencyCode,
                    toCurrencyCode = fetched.toCurrencyCode,
                    effectiveDateEpochDay = fetched.effectiveDateEpochDay,
                    rate = fetched.rate
                )
            )
            return fetched.rate
        }
        return dao.getLatestExchangeRate(fromCurrencyCode, toCurrencyCode)?.rate
    }

    override suspend fun getBackupCsv(): String {
        val expenses = dao.getAllExpensesOnce().map { it.toDomain(emptyList()) }
        val categories = dao.getAllCategoriesOnce().map { it.toDomain() }
        return formatSpendWiseCsv(expenses, categories)
    }

    override suspend fun getBackupJson(): String {
        val expenses = dao.getAllExpensesOnce().map { it.toDomain(emptyList()) }
        val categories = dao.getAllCategoriesOnce().map { it.toDomain() }
        val settings = settingsRepository.settings.first()
        
        val backup = SpendWiseBackup(
            expenses = expenses,
            categories = categories,
            settings = settings
        )
        return kotlinx.serialization.json.Json { 
            prettyPrint = true 
            encodeDefaults = true
        }.encodeToString(SpendWiseBackup.serializer(), backup)
    }

    override suspend fun restoreFromJson(json: String) {
        val backup = kotlinx.serialization.json.Json { 
            ignoreUnknownKeys = true 
        }.decodeFromString(SpendWiseBackup.serializer(), json)

        val categories = backup.categories.map { 
            CategoryEntity(it.id, it.name, it.icon, it.color, it.sortOrder) 
        }
        val expenses = backup.expenses.map { 
            ExpenseEntity(
                id = it.id,
                originalAmountCents = it.originalAmountCents,
                originalCurrencyCode = it.originalCurrencyCode,
                baseAmountCents = it.baseAmountCents,
                baseCurrencyCode = it.baseCurrencyCode,
                exchangeRate = it.exchangeRate,
                categoryId = it.categoryId,
                note = it.note,
                spentAtMillis = it.spentAtMillis,
                createdAtMillis = it.createdAtMillis,
                updatedAtMillis = it.updatedAtMillis
            )
        }
        
        val tagMap = mutableMapOf<String, TagEntity>()
        val expenseTags = mutableListOf<ExpenseTagEntity>()
        
        backup.expenses.forEach { expense ->
            expense.tags.forEach { tag ->
                val normalized = TagParser.normalize(tag)
                tagMap.getOrPut(normalized) {
                    TagEntity(
                        normalizedName = normalized,
                        displayName = tag,
                        createdAtMillis = expense.createdAtMillis,
                        lastUsedAtMillis = expense.spentAtMillis
                    )
                }
                expenseTags.add(ExpenseTagEntity(expense.id, normalized))
            }
        }

        dao.restoreData(categories, expenses, tagMap.values.toList(), expenseTags)
        settingsRepository.saveSettings(backup.settings)
    }

    private fun CategoryEntity.toDomain(): Category =
        Category(
            id = id,
            name = name,
            icon = icon,
            color = color,
            sortOrder = sortOrder
        )

    private fun ExpenseEntity.toDomain(tags: List<String>): Expense =
        Expense(
            id = id,
            originalAmountCents = originalAmountCents,
            originalCurrencyCode = originalCurrencyCode,
            baseAmountCents = baseAmountCents,
            baseCurrencyCode = baseCurrencyCode,
            exchangeRate = exchangeRate,
            categoryId = categoryId,
            note = note,
            tags = tags,
            spentAtMillis = spentAtMillis,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis
        )

    private fun ExpenseReminderEntity.toDomain(): ExpenseReminder =
        ExpenseReminder(
            id = id,
            hour = hour,
            minute = minute,
            enabled = enabled
        )

    private fun TagUsageRow.toDomain(): TagUsage =
        TagUsage(
            name = name,
            expenseCount = (expenseCount ?: 0L).toInt(),
            totalBaseAmountCents = totalBaseAmountCents ?: 0L,
            lastUsedAtMillis = lastUsedAtMillis,
            currentMonthAmountCents = currentMonthAmountCents ?: 0L,
            previousMonthAmountCents = previousMonthAmountCents ?: 0L
        )

    private val defaultCategories = listOf(
        Category(0L, "Food", "restaurant", 0xFFE76F51, 0),
        Category(0L, "Coffee", "local_cafe", 0xFF8D6E63, 1),
        Category(0L, "Groceries", "shopping_cart", 0xFF2A9D8F, 2),
        Category(0L, "Transport", "directions_bus", 0xFF457B9D, 3),
        Category(0L, "Shopping", "shopping_bag", 0xFFE9C46A, 4),
        Category(0L, "Bills", "lightbulb", 0xFF6D597A, 5),
        Category(0L, "Health", "medication", 0xFF43AA8B, 6),
        Category(0L, "Travel", "flight", 0xFF277DA1, 7),
        Category(0L, "Family", "home", 0xFFF4A261, 8),
        Category(0L, "Other", "other", 0xFF6C757D, 9)
    )

    private companion object {
        const val SNAPSHOT_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
