package com.spendwise.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.spendwise.domain.TagParser
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

@Dao
interface SpendWiseDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Insert
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE name = :name AND id != :excludeId ORDER BY sortOrder, id LIMIT 1")
    suspend fun getCategoryByNameExcludingId(name: String, excludeId: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun countExpensesForCategory(categoryId: Long): Int

    @Query("UPDATE expenses SET categoryId = :toCategoryId WHERE categoryId = :fromCategoryId")
    suspend fun moveExpensesToCategory(fromCategoryId: Long, toCategoryId: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Transaction
    suspend fun deleteCategory(id: Long) {
        val category = getCategory(id) ?: return
        if (countExpensesForCategory(id) > 0) {
            val otherCategoryId = getCategoryByNameExcludingId("Other", id)?.id
                ?: insertCategory(
                    CategoryEntity(
                        name = "Other",
                        icon = "other",
                        color = 0xFF6C757D,
                        sortOrder = countCategories()
                    )
                )
            moveExpensesToCategory(fromCategoryId = category.id, toCategoryId = otherCategoryId)
        }
        deleteCategoryById(category.id)
    }

    @Transaction
    suspend fun moveCategory(id: Long, direction: Int) {
        val categories = getAllCategoriesOnce()
        val index = categories.indexOfFirst { it.id == id }
        if (index < 0) return
        val swapIndex = (index + direction).coerceIn(categories.indices)
        if (index == swapIndex) return
        val first = categories[index]
        val second = categories[swapIndex]
        updateCategory(first.copy(sortOrder = second.sortOrder))
        updateCategory(second.copy(sortOrder = first.sortOrder))
    }

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: Long): CategoryEntity?

    @Query("SELECT * FROM expenses ORDER BY spentAtMillis DESC, id DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY spentAtMillis DESC, id DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpense(id: Long): ExpenseEntity?

    @Query(
        """
        SELECT expenses.* FROM expenses
        INNER JOIN expense_tags ON expense_tags.expenseId = expenses.id
        WHERE expense_tags.tagName = :tagName
        ORDER BY spentAtMillis DESC, id DESC
        """
    )
    suspend fun getExpensesForTag(tagName: String): List<ExpenseEntity>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expense_tags WHERE expenseId = :expenseId")
    suspend fun deleteTagsForExpense(expenseId: Long)

    @Query("DELETE FROM expense_tags WHERE tagName = :tagName")
    suspend fun deleteExpenseTagRefs(tagName: String)

    @Query("DELETE FROM expense_tags WHERE expenseId = :expenseId AND tagName = :tagName")
    suspend fun deleteExpenseTagRef(expenseId: Long, tagName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseTags(tags: List<ExpenseTagEntity>)

    @Upsert
    suspend fun upsertTags(tags: List<TagEntity>)

    @Query("SELECT * FROM tags WHERE normalizedName = :tagName")
    suspend fun getTag(tagName: String): TagEntity?

    @Query("DELETE FROM tags WHERE normalizedName = :tagName")
    suspend fun deleteTagEntity(tagName: String)

    @Query("SELECT * FROM tags ORDER BY lastUsedAtMillis DESC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM expense_tags")
    fun observeExpenseTags(): Flow<List<ExpenseTagEntity>>

    @Transaction
    suspend fun insertExpenseWithTags(expense: ExpenseEntity, tags: List<TagEntity>): Long {
        val expenseId = insertExpense(expense)
        if (tags.isNotEmpty()) {
            upsertTags(tags)
            insertExpenseTags(tags.map { ExpenseTagEntity(expenseId = expenseId, tagName = it.normalizedName) })
        }
        return expenseId
    }

    @Transaction
    suspend fun updateExpenseWithTags(expense: ExpenseEntity, tags: List<TagEntity>) {
        updateExpense(expense)
        deleteTagsForExpense(expense.id)
        if (tags.isNotEmpty()) {
            upsertTags(tags)
            insertExpenseTags(tags.map { ExpenseTagEntity(expenseId = expense.id, tagName = it.normalizedName) })
        }
    }

    @Transaction
    suspend fun renameTag(oldTag: String, newTag: String, nowMillis: Long) {
        val oldNormalized = TagParser.normalize(oldTag)
        val newNormalized = TagParser.normalize(newTag)
        if (oldNormalized.isBlank() || newNormalized.isBlank() || oldNormalized == newNormalized) return

        val oldEntity = getTag(oldNormalized)
        val existingNewEntity = getTag(newNormalized)
        val expenses = getExpensesForTag(oldNormalized)
        if (oldEntity == null && expenses.isEmpty()) return

        val nextTag = existingNewEntity?.copy(
            lastUsedAtMillis = max(existingNewEntity.lastUsedAtMillis, oldEntity?.lastUsedAtMillis ?: nowMillis)
        ) ?: TagEntity(
            normalizedName = newNormalized,
            displayName = newNormalized,
            createdAtMillis = oldEntity?.createdAtMillis ?: nowMillis,
            lastUsedAtMillis = oldEntity?.lastUsedAtMillis ?: nowMillis
        )
        upsertTags(listOf(nextTag))

        expenses.forEach { expense ->
            val renamedNote = TagParser.renameTagInNote(expense.note, oldNormalized, newNormalized)
            updateExpense(expense.copy(note = renamedNote, updatedAtMillis = nowMillis))
            insertExpenseTags(listOf(ExpenseTagEntity(expenseId = expense.id, tagName = newNormalized)))
            deleteExpenseTagRef(expense.id, oldNormalized)
        }
        deleteTagEntity(oldNormalized)
    }

    @Transaction
    suspend fun deleteTag(tag: String, nowMillis: Long) {
        val normalized = TagParser.normalize(tag)
        if (normalized.isBlank()) return

        getExpensesForTag(normalized).forEach { expense ->
            val nextNote = TagParser.removeTagFromNote(expense.note, normalized)
            updateExpense(expense.copy(note = nextNote, updatedAtMillis = nowMillis))
        }
        deleteExpenseTagRefs(normalized)
        deleteTagEntity(normalized)
    }

    @Query(
        """
        SELECT * FROM exchange_rates 
        WHERE fromCurrencyCode = :fromCurrencyCode AND toCurrencyCode = :toCurrencyCode 
        ORDER BY effectiveDateEpochDay DESC 
        LIMIT 1
        """
    )
    suspend fun getLatestExchangeRate(fromCurrencyCode: String, toCurrencyCode: String): ExchangeRateEntity?

    @Upsert
    suspend fun upsertExchangeRate(rate: ExchangeRateEntity)

    @Query("SELECT * FROM currency_settings WHERE id = 1")
    fun observeCurrencySettings(): Flow<CurrencySettingsEntity?>

    @Query("SELECT * FROM currency_settings WHERE id = 1")
    suspend fun getCurrencySettingsOnce(): CurrencySettingsEntity?

    @Upsert
    suspend fun upsertCurrencySettings(settings: CurrencySettingsEntity)

    @Query("SELECT * FROM expense_reminders ORDER BY hour, minute")
    fun observeExpenseReminders(): Flow<List<ExpenseReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpenseReminder(reminder: ExpenseReminderEntity): Long

    @Query("UPDATE expense_reminders SET enabled = :enabled WHERE id = :id")
    suspend fun setExpenseReminderEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM expense_reminders WHERE id = :id")
    suspend fun deleteExpenseReminder(id: Long)
}
