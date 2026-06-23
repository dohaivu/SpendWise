package com.spendwise.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
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
    val languageCode: String = "en",
    val themeModeCode: String = "system",
    val colorSchemeModeCode: String = "sky_blue"
)

@Entity(tableName = "expense_reminders", indices = [Index(value = ["hour", "minute"], unique = true)])
data class ExpenseReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true
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
        ExpenseReminderEntity::class,
        ExchangeRateEntity::class
    ],
    version = 4,
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
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_4)
        .fallbackToDestructiveMigration(false)
        .fallbackToDestructiveMigrationOnDowngrade(false)
        .setQueryCoroutineContext(Dispatchers.IO)
        .setDriver(BundledSQLiteDriver())
        .build()
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS expense_reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                enabled INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_expense_reminders_hour_minute ON expense_reminders(hour, minute)")
    }
}

private val MIGRATION_2_4 = object : Migration(2, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE currency_settings ADD COLUMN themeModeCode TEXT NOT NULL DEFAULT 'system'")
        connection.execSQL("ALTER TABLE currency_settings ADD COLUMN colorSchemeModeCode TEXT NOT NULL DEFAULT 'sunset'")
    }
}
