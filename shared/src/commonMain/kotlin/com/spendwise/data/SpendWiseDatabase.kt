package com.spendwise.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
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
        ExpenseReminderEntity::class,
        ExchangeRateEntity::class
    ],
    version = 7,
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
        .addMigrations(MIGRATION_4_5)
        .addMigrations(MIGRATION_5_6)
        .addMigrations(MIGRATION_6_7)
        .fallbackToDestructiveMigration(false)
        .fallbackToDestructiveMigrationOnDowngrade(false)
        .setQueryCoroutineContext(Dispatchers.IO)
        .setDriver(BundledSQLiteDriver())
        .build()
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
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
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE currency_settings ADD COLUMN themeModeCode TEXT NOT NULL DEFAULT 'system'")
        connection.execSQL("ALTER TABLE currency_settings ADD COLUMN colorSchemeModeCode TEXT NOT NULL DEFAULT 'sunset'")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE currency_settings ADD COLUMN backupFolderUri TEXT")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE currency_settings ADD COLUMN backupFolderName TEXT")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS currency_settings")
    }
}
