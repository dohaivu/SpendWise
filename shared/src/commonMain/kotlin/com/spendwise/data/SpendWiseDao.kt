package com.spendwise.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpendWiseDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
                        icon = "•••",
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

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpense(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expense_tags WHERE expenseId = :expenseId")
    suspend fun deleteTagsForExpense(expenseId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseTags(tags: List<ExpenseTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTags(tags: List<TagEntity>)

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

    @Query(
        """
        SELECT * FROM exchange_rates 
        WHERE fromCurrencyCode = :fromCurrencyCode AND toCurrencyCode = :toCurrencyCode 
        ORDER BY effectiveDateEpochDay DESC 
        LIMIT 1
        """
    )
    suspend fun getLatestExchangeRate(fromCurrencyCode: String, toCurrencyCode: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExchangeRate(rate: ExchangeRateEntity)

    @Query("SELECT * FROM currency_settings WHERE id = 1")
    fun observeCurrencySettings(): Flow<CurrencySettingsEntity?>

    @Query("SELECT * FROM currency_settings WHERE id = 1")
    suspend fun getCurrencySettingsOnce(): CurrencySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrencySettings(settings: CurrencySettingsEntity)
}
