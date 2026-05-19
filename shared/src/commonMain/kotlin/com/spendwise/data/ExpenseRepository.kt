package com.spendwise.data

import com.spendwise.domain.AddExpenseInput
import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.domain.SpendWiseSnapshot
import com.spendwise.domain.TagParser
import com.spendwise.domain.TagUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock

interface ExpenseRepository {
    fun observeSnapshot(): Flow<SpendWiseSnapshot>
    suspend fun seedDefaults()
    suspend fun addExpense(input: AddExpenseInput): Long
    suspend fun archiveCategory(id: Long)
}

class RoomExpenseRepository(
    private val dao: SpendWiseDao
) : ExpenseRepository {
    override fun observeSnapshot(): Flow<SpendWiseSnapshot> {
        return combine(
            dao.observeCategories(),
            dao.observeExpenses(),
            dao.observeTags(),
            dao.observeExpenseTags()
        ) { categoryEntities, expenseEntities, tagEntities, refs ->
            val tagsByExpense = refs.groupBy { it.expenseId }
                .mapValues { (_, value) -> value.map { it.tagName }.sorted() }
            val expenses = expenseEntities.map { it.toDomain(tagsByExpense[it.id].orEmpty()) }
            val usage = tagEntities.map { tag ->
                val taggedExpenseIds = refs.filter { it.tagName == tag.normalizedName }.map { it.expenseId }.toSet()
                val taggedExpenses = expenses.filter { it.id in taggedExpenseIds }
                TagUsage(
                    name = tag.displayName,
                    expenseCount = taggedExpenses.size,
                    totalBaseAmountCents = taggedExpenses.sumOf { it.baseAmountCents },
                    lastUsedAtMillis = tag.lastUsedAtMillis
                )
            }.sortedWith(compareByDescending<TagUsage> { it.expenseCount }.thenBy { it.name })

            SpendWiseSnapshot(
                categories = categoryEntities.map { it.toDomain() },
                expenses = expenses,
                tagUsage = usage
            )
        }
    }

    override suspend fun seedDefaults() {
        if (dao.countCategories() > 0) return
        dao.insertCategories(defaultCategories.mapIndexed { index, category ->
            CategoryEntity(
                name = category.name,
                icon = category.icon,
                color = category.color,
                sortOrder = index
            )
        })
    }

    override suspend fun addExpense(input: AddExpenseInput): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val expense = ExpenseEntity(
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
        return dao.insertExpenseWithTags(expense, tags)
    }

    override suspend fun archiveCategory(id: Long) {
        dao.archiveCategory(id)
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
