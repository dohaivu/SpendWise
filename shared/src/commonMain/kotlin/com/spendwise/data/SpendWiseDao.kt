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

    @Query("UPDATE categories SET archived = 1 WHERE id = :id")
    suspend fun archiveCategory(id: Long)

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
