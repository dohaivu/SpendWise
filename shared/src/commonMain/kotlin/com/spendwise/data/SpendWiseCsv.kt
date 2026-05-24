package com.spendwise.data

import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.ui.components.currencyDisplayFormat
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

private val csvHeader = listOf("date", "amount", "currency", "category", "note")

data class CsvExpenseRow(
    val date: LocalDate,
    val amountCents: Long,
    val currencyCode: String,
    val categoryName: String,
    val note: String
)

data class CsvImportError(
    val lineNumber: Int,
    val message: String
)

data class CsvImportResult(
    val rows: List<CsvExpenseRow>,
    val errors: List<CsvImportError>
)

fun parseSpendWiseCsv(csvText: String): CsvImportResult {
    val rows = parseCsvRows(csvText)
        .dropWhile { row -> row.all { it.isBlank() } }
    if (rows.isEmpty()) {
        return CsvImportResult(emptyList(), listOf(CsvImportError(1, "CSV is empty")))
    }

    val header = rows.first().map { it.trim() }
    if (header != csvHeader) {
        return CsvImportResult(emptyList(), listOf(CsvImportError(1, "Header must be ${csvHeader.joinToString(",")}")))
    }

    val parsedRows = mutableListOf<CsvExpenseRow>()
    val errors = mutableListOf<CsvImportError>()
    rows.drop(1).forEachIndexed { index, row ->
        val lineNumber = index + 2
        if (row.all { it.isBlank() }) return@forEachIndexed
        if (row.size != csvHeader.size) {
            errors += CsvImportError(lineNumber, "Expected ${csvHeader.size} columns but found ${row.size}")
            return@forEachIndexed
        }

        val date = parseCsvDate(row[0].trim())
        val currency = row[2].trim().uppercase()
        val amount = parseCsvAmount(row[1].trim(), currency)
        val category = row[3].trim()
        val note = row[4]

        when {
            date == null -> errors += CsvImportError(lineNumber, "Invalid date")
            amount == null || amount <= 0L -> errors += CsvImportError(lineNumber, "Invalid amount")
            currency.isBlank() -> errors += CsvImportError(lineNumber, "Currency is required")
            category.isBlank() -> errors += CsvImportError(lineNumber, "Category is required")
            else -> parsedRows += CsvExpenseRow(
                date = date,
                amountCents = amount,
                currencyCode = currency,
                categoryName = category,
                note = note
            )
        }
    }

    return CsvImportResult(parsedRows, errors)
}

fun formatSpendWiseCsv(expenses: List<Expense>, categories: List<Category>): String {
    val categoryById = categories.associateBy { it.id }
    val timeZone = TimeZone.currentSystemDefault()
    val sortedExpenses = expenses.sortedWith(compareBy<Expense> { it.spentAtMillis }.thenBy { it.id })
    return buildString {
        append(csvHeader.joinToString(","))
        append('\n')
        sortedExpenses.forEach { expense ->
            val date = Instant.fromEpochMilliseconds(expense.spentAtMillis).toLocalDateTime(timeZone).date
            val row = listOf(
                formatCsvDate(date),
                formatCsvAmount(expense.originalAmountCents, expense.originalCurrencyCode),
                expense.originalCurrencyCode,
                categoryById[expense.categoryId]?.name ?: "Uncategorized",
                expense.note
            )
            append(row.joinToString(",") { it.escapeCsvField() })
            append('\n')
        }
    }
}

fun parseCsvDate(value: String): LocalDate? {
    if (value.isBlank()) return null

    val hyphenParts = value.split("-")
    if (hyphenParts.size == 3) {
        return parseLocalDate(
            year = hyphenParts[0].toIntOrNull(),
            month = hyphenParts[1].toIntOrNull(),
            day = hyphenParts[2].toIntOrNull()
        )
    }

    val slashParts = value.split("/")
    if (slashParts.size == 3) {
        val first = slashParts[0].toIntOrNull()
        val second = slashParts[1].toIntOrNull()
        val third = slashParts[2].toIntOrNull()
        return when {
            first != null && first > 31 -> parseLocalDate(year = first, month = second, day = third)
            first != null && second != null && third != null && first > 12 -> parseLocalDate(year = third, month = second, day = first)
            first != null && second != null && third != null -> parseLocalDate(year = third, month = first, day = second)
            else -> null
        }
    }

    return null
}

fun csvDuplicateKey(
    date: LocalDate,
    amountCents: Long,
    currencyCode: String,
    categoryName: String,
    note: String
): String = listOf(
    formatCsvDate(date),
    amountCents.toString(),
    currencyCode.uppercase(),
    categoryName.trim().lowercase(),
    note
).joinToString("|")

fun Expense.csvDuplicateKey(categories: List<Category>, timeZone: TimeZone): String {
    val date = Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date
    val categoryName = categories.firstOrNull { it.id == categoryId }?.name.orEmpty()
    return csvDuplicateKey(date, originalAmountCents, originalCurrencyCode, categoryName, note)
}

fun CsvExpenseRow.spentAtMillis(timeZone: TimeZone): Long =
    date.atStartOfDayIn(timeZone).toEpochMilliseconds()

private fun parseLocalDate(year: Int?, month: Int?, day: Int?): LocalDate? {
    if (year == null || month == null || day == null) return null
    return runCatching { LocalDate(year, month, day) }.getOrNull()
}

private fun parseCsvAmount(value: String, currencyCode: String): Long? {
    val normalized = value.replace(",", "").trim()
    val amount = normalized.toDoubleOrNull() ?: return null
    val fractionDigits = currencyDisplayFormat(currencyCode).fractionDigits
    return if (fractionDigits == 0) {
        (amount.roundToLong() * 100L)
    } else {
        (amount * 100).roundToLong()
    }
}

private fun formatCsvDate(date: LocalDate): String =
    "${date.year}-${date.month.number.toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"

private fun formatCsvAmount(cents: Long, currencyCode: String): String {
    val fractionDigits = currencyDisplayFormat(currencyCode).fractionDigits
    val whole = cents / 100
    val fraction = cents % 100
    return if (fractionDigits == 0) {
        whole.toString()
    } else {
        "$whole.${fraction.toString().padStart(2, '0').take(fractionDigits)}"
    }
}

private fun String.escapeCsvField(): String {
    val needsQuotes = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!needsQuotes) return this
    return "\"" + replace("\"", "\"\"") + "\""
}

private fun parseCsvRows(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var index = 0

    while (index < text.length) {
        val char = text[index]
        when {
            inQuotes && char == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                field.append('"')
                index++
            }
            char == '"' -> inQuotes = !inQuotes
            !inQuotes && char == ',' -> {
                row += field.toString()
                field.clear()
            }
            !inQuotes && (char == '\n' || char == '\r') -> {
                row += field.toString()
                field.clear()
                rows += row.toList()
                row.clear()
                if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') {
                    index++
                }
            }
            else -> field.append(char)
        }
        index++
    }

    if (field.isNotEmpty() || row.isNotEmpty()) {
        row += field.toString()
        rows += row.toList()
    }
    return rows
}
