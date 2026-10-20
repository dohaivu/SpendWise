package com.spendwise.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * An expense enriched with the values that are expensive to derive: its display date in the
 * current time zone, its accent-free search text and its normalized tags.
 *
 * Preparing these once per snapshot keeps filtering and grouping off the hot path (every
 * keystroke, every recomposition) and avoids repeated time zone conversions.
 */
class ExpenseEntry(
    val expense: Expense,
    val date: LocalDate,
    val searchText: String,
    val normalizedTags: List<String>
)

/**
 * Prepares [expenses] for filtering and grouping. The result is sorted by `spentAtMillis`
 * descending so callers can group consecutive entries by date in a single pass.
 */
fun prepareExpenseEntries(expenses: List<Expense>, timeZone: TimeZone): List<ExpenseEntry> {
    val entries = ArrayList<ExpenseEntry>(expenses.size)
    for (expense in expenses) {
        entries += ExpenseEntry(
            expense = expense,
            date = expense.localDate(timeZone),
            searchText = expense.note.removeAccents().lowercase(),
            normalizedTags = normalizeTags(expense.tags)
        )
    }
    entries.sortWith(
        compareByDescending<ExpenseEntry> { it.expense.spentAtMillis }
            .thenByDescending { it.expense.id }
    )
    return entries
}

/**
 * Pre-normalized [TransactionFilters] so a filter can be applied to many entries without
 * re-normalizing the query or the selected tags each time.
 */
internal class ExpenseMatcher private constructor(
    private val query: String,
    private val categoryId: Long?,
    private val tags: Set<String>
) {
    fun matches(entry: ExpenseEntry): Boolean {
        if (categoryId != null && entry.expense.categoryId != categoryId) return false
        if (query.isNotEmpty() && !entry.searchText.contains(query)) return false
        if (tags.isNotEmpty() && entry.normalizedTags.none { it in tags }) return false
        return true
    }

    companion object {
        fun of(filters: TransactionFilters): ExpenseMatcher {
            val query = if (filters.query.isBlank()) "" else filters.query.removeAccents().lowercase()
            val tags: Set<String> = if (filters.selectedTags.isEmpty()) {
                emptySet()
            } else {
                filters.selectedTags.mapNotNullTo(HashSet()) { tag ->
                    TagParser.normalize(tag).ifEmpty { null }
                }
            }
            return ExpenseMatcher(query = query, categoryId = filters.categoryId, tags = tags)
        }
    }
}

private fun Expense.localDate(timeZone: TimeZone): LocalDate =
    kotlin.time.Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date

private fun normalizeTags(tags: List<String>): List<String> {
    if (tags.isEmpty()) return emptyList()
    val normalized = ArrayList<String>(tags.size)
    for (tag in tags) {
        val value = TagParser.normalize(tag)
        if (value.isNotEmpty() && value !in normalized) normalized += value
    }
    return normalized
}
