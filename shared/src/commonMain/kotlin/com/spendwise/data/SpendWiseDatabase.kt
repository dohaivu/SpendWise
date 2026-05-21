package com.spendwise.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Entity(tableName = "categories", indices = [Index(value = ["sortOrder"])])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val icon: String,
    val color: Long,
    val sortOrder: Int
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["spentAtMillis"]),
        Index(value = ["categoryId"]),
        Index(value = ["baseCurrencyCode"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val originalAmountCents: Long,
    val originalCurrencyCode: String,
    val baseAmountCents: Long,
    val baseCurrencyCode: String,
    val exchangeRate: Double,
    val categoryId: Long,
    val note: String,
    val spentAtMillis: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val normalizedName: String,
    val displayName: String,
    val createdAtMillis: Long,
    val lastUsedAtMillis: Long
)

@Entity(
    tableName = "expense_tags",
    primaryKeys = ["expenseId", "tagName"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["normalizedName"],
            childColumns = ["tagName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["expenseId"]), Index(value = ["tagName"])]
)
data class ExpenseTagEntity(
    val expenseId: Long,
    val tagName: String
)

@Entity(tableName = "currency_settings")
data class CurrencySettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val baseCurrencyCode: String = "USD",
    val languageCode: String = "en"
)

@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["fromCurrencyCode", "toCurrencyCode", "effectiveDateEpochDay"]
)
data class ExchangeRateEntity(
    val fromCurrencyCode: String,
    val toCurrencyCode: String,
    val effectiveDateEpochDay: Int,
    val rate: Double
)

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        TagEntity::class,
        ExpenseTagEntity::class,
        CurrencySettingsEntity::class,
        ExchangeRateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@ConstructedBy(SpendWiseDatabaseConstructor::class)
abstract class SpendWiseDatabase : RoomDatabase() {
    abstract fun spendWiseDao(): SpendWiseDao
}

@Suppress("KotlinNoActualForExpect")
expect object SpendWiseDatabaseConstructor : RoomDatabaseConstructor<SpendWiseDatabase> {
    override fun initialize(): SpendWiseDatabase
}

fun buildSpendWiseDatabase(
    builder: RoomDatabase.Builder<SpendWiseDatabase>
): SpendWiseDatabase {
    return builder
        .fallbackToDestructiveMigration(false)
        .fallbackToDestructiveMigrationOnDowngrade(false)
        .setQueryCoroutineContext(Dispatchers.IO)
        .setDriver(BundledSQLiteDriver())
        .build()
}
