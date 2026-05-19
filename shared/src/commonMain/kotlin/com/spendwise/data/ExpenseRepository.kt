package com.spendwise.data

import com.spendwise.domain.AddExpenseInput
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryDraft
import com.spendwise.domain.Expense
import com.spendwise.domain.SpendWiseSnapshot
import com.spendwise.domain.TagParser
import com.spendwise.domain.TagUsage
import com.spendwise.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

interface ExpenseRepository {
    fun observeSnapshot(): Flow<SpendWiseSnapshot>
    suspend fun seedDefaults()
    suspend fun saveExpense(input: AddExpenseInput): Long
    suspend fun deleteExpense(id: Long)
    suspend fun saveCategory(draft: CategoryDraft): Long
    suspend fun archiveCategory(id: Long)
    suspend fun moveCategory(id: Long, direction: Int)
    suspend fun saveSettings(settings: UserSettings)
    suspend fun getLatestExchangeRate(fromCurrencyCode: String, toCurrencyCode: String): Double?
}

class RoomExpenseRepository(
    private val dao: SpendWiseDao
) : ExpenseRepository {
    override fun observeSnapshot(): Flow<SpendWiseSnapshot> {
        return combine(
            dao.observeCategories(),
            dao.observeExpenses(),
            dao.observeTags(),
            dao.observeExpenseTags(),
            dao.observeCurrencySettings()
        ) { categoryEntities, expenseEntities, tagEntities, refs, settings ->
            val tagsByExpense = refs.groupBy { it.expenseId }
                .mapValues { (_, value) -> value.map { it.tagName }.sorted() }
            val expenses = expenseEntities.map { it.toDomain(tagsByExpense[it.id].orEmpty()) }
            val timeZone = TimeZone.currentSystemDefault()
            val currentMonth = Clock.System.now().toLocalDateTime(timeZone).date.let { date ->
                kotlinx.datetime.LocalDate(date.year, date.month, 1)
            }
            val previousMonth = currentMonth.minus(1, DateTimeUnit.MONTH)
            val usage = tagEntities.map { tag ->
                val taggedExpenseIds = refs.filter { it.tagName == tag.normalizedName }.map { it.expenseId }.toSet()
                val taggedExpenses = expenses.filter { it.id in taggedExpenseIds }
                TagUsage(
                    name = tag.displayName,
                    expenseCount = taggedExpenses.size,
                    totalBaseAmountCents = taggedExpenses.sumOf { it.baseAmountCents },
                    lastUsedAtMillis = tag.lastUsedAtMillis,
                    currentMonthAmountCents = taggedExpenses
                        .filter { it.spentAtMillis.monthMatches(currentMonth, timeZone) }
                        .sumOf { it.baseAmountCents },
                    previousMonthAmountCents = taggedExpenses
                        .filter { it.spentAtMillis.monthMatches(previousMonth, timeZone) }
                        .sumOf { it.baseAmountCents }
                )
            }.sortedWith(compareByDescending<TagUsage> { it.expenseCount }.thenBy { it.name })

            SpendWiseSnapshot(
                categories = categoryEntities.map { it.toDomain() },
                expenses = expenses,
                tagUsage = usage,
                settings = UserSettings(
                    baseCurrencyCode = settings?.baseCurrencyCode ?: "USD",
                    languageCode = settings?.languageCode ?: "en"
                )
            )
        }
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
        if (dao.getCurrencySettingsOnce() == null) {
            dao.upsertCurrencySettings(CurrencySettingsEntity())
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
            icon = draft.icon.trim().ifBlank { "•" },
            color = draft.color,
            sortOrder = existing?.sortOrder ?: dao.countCategories(),
            archived = existing?.archived ?: false
        )
        return if (existing == null) {
            dao.insertCategory(entity)
        } else {
            dao.updateCategory(entity)
            entity.id
        }
    }

    override suspend fun archiveCategory(id: Long) {
        dao.archiveCategory(id)
    }

    override suspend fun moveCategory(id: Long, direction: Int) {
        val categories = dao.getAllCategoriesOnce()
        val index = categories.indexOfFirst { it.id == id }
        val swapIndex = (index + direction).coerceIn(categories.indices)
        if (index < 0 || index == swapIndex) return
        val first = categories[index]
        val second = categories[swapIndex]
        dao.updateCategory(first.copy(sortOrder = second.sortOrder))
        dao.updateCategory(second.copy(sortOrder = first.sortOrder))
    }

    override suspend fun saveSettings(settings: UserSettings) {
        dao.upsertCurrencySettings(
            CurrencySettingsEntity(
                baseCurrencyCode = settings.baseCurrencyCode,
                languageCode = settings.languageCode
            )
        )
    }

    override suspend fun getLatestExchangeRate(fromCurrencyCode: String, toCurrencyCode: String): Double? {
        if (fromCurrencyCode == toCurrencyCode) return 1.0
        return dao.getLatestExchangeRate(fromCurrencyCode, toCurrencyCode)?.rate
    }

    private fun CategoryEntity.toDomain(): Category =
        Category(
            id = id,
            name = name,
            icon = icon,
            color = color,
            sortOrder = sortOrder,
            archived = archived
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

    private val defaultCategories = listOf(
        Category(0L, "Food", "🍜", 0xFFE76F51, 0),
        Category(0L, "Coffee", "☕", 0xFF8D6E63, 1),
        Category(0L, "Groceries", "🛒", 0xFF2A9D8F, 2),
        Category(0L, "Transport", "🚌", 0xFF457B9D, 3),
        Category(0L, "Shopping", "🛍", 0xFFE9C46A, 4),
        Category(0L, "Bills", "💡", 0xFF6D597A, 5),
        Category(0L, "Health", "💊", 0xFF43AA8B, 6),
        Category(0L, "Travel", "✈", 0xFF277DA1, 7),
        Category(0L, "Family", "🏠", 0xFFF4A261, 8),
        Category(0L, "Other", "•••", 0xFF6C757D, 9)
    )
}

private fun Long.monthMatches(monthStart: kotlinx.datetime.LocalDate, timeZone: TimeZone): Boolean {
    val date = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date
    return date.year == monthStart.year && date.month == monthStart.month
}
