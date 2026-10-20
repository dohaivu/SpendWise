package com.spendwise.ui.calendar

import com.spendwise.domain.DailyExpenseTotal
import com.spendwise.domain.Expense
import com.spendwise.domain.TagParser
import com.spendwise.domain.TransactionFilters
import com.spendwise.domain.removeAccents
import com.spendwise.ui.CalendarData
import com.spendwise.ui.DateTransactionListItem
import com.spendwise.ui.components.ReportPeriod
import com.spendwise.ui.isSameMonth
import com.spendwise.ui.spentDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * An expense enriched with the values that are expensive to derive, computed once per snapshot
 * instead of once per filter change.
 */
internal class CalendarExpenseEntry(
    val expense: Expense,
    val date: LocalDate,
    val searchText: String,
    val normalizedTags: List<String>
)

internal object CalendarDataBuilder {

    /**
     * Derives the display date, normalized search text and normalized tags for every expense.
     * The result is sorted by `spentAtMillis` descending, which lets [build] group rows by date in
     * a single pass.
     */
    fun prepare(expenses: List<Expense>, timeZone: TimeZone): List<CalendarExpenseEntry> {
        val entries = ArrayList<CalendarExpenseEntry>(expenses.size)
        for (expense in expenses) {
            entries += CalendarExpenseEntry(
                expense = expense,
                date = expense.spentDate(timeZone),
                searchText = expense.note.removeAccents().lowercase(),
                normalizedTags = normalizeTags(expense.tags)
            )
        }
        entries.sortWith(
            compareByDescending<CalendarExpenseEntry> { it.expense.spentAtMillis }
                .thenByDescending { it.expense.id }
        )
        return entries
    }

    fun build(
        entries: List<CalendarExpenseEntry>,
        period: ReportPeriod,
        month: LocalDate,
        filters: TransactionFilters
    ): CalendarData {
        val query = if (filters.query.isBlank()) {
            ""
        } else {
            filters.query.removeAccents().lowercase()
        }
        val categoryId = filters.categoryId
        val selectedTags: Set<String> = if (filters.selectedTags.isEmpty()) {
            emptySet()
        } else {
            filters.selectedTags.mapNotNullTo(HashSet()) { tag ->
                TagParser.normalize(tag).ifEmpty { null }
            }
        }

        val items = ArrayList<DateTransactionListItem>()
        val totalsByDate = HashMap<LocalDate, DailyExpenseTotal>()
        val headerIndexes = HashMap<LocalDate, Int>()
        var transactionCount = 0
        var filteredMonthTotal = 0L

        var bucketDate: LocalDate? = null
        var bucketExpenses = ArrayList<Expense>()
        var bucketTotal = 0L

        fun flushBucket() {
            val date = bucketDate ?: return
            items += DateTransactionListItem(date, bucketTotal, bucketExpenses)
            totalsByDate[date] = DailyExpenseTotal(
                date = date,
                totalBaseAmountCents = bucketTotal,
                expenseCount = bucketExpenses.size
            )
            headerIndexes[date] = items.lastIndex
            bucketExpenses = ArrayList()
            bucketTotal = 0L
        }

        for (entry in entries) {
            if (!entry.date.matchesPeriod(period, month)) continue
            if (categoryId != null && entry.expense.categoryId != categoryId) continue
            if (query.isNotEmpty() && !entry.searchText.contains(query)) continue
            if (selectedTags.isNotEmpty() && entry.normalizedTags.none { it in selectedTags }) continue

            if (entry.date != bucketDate) {
                flushBucket()
                bucketDate = entry.date
            }
            bucketExpenses += entry.expense
            bucketTotal += entry.expense.baseAmountCents
            transactionCount++
            filteredMonthTotal += entry.expense.baseAmountCents
        }
        flushBucket()

        return CalendarData(
            monthTransactionCount = transactionCount,
            transactionItems = items,
            totalsByDate = totalsByDate,
            headerIndexes = headerIndexes,
            filteredMonthTotal = filteredMonthTotal
        )
    }

    private fun normalizeTags(tags: List<String>): List<String> {
        if (tags.isEmpty()) return emptyList()
        val normalized = ArrayList<String>(tags.size)
        for (tag in tags) {
            val value = TagParser.normalize(tag)
            if (value.isNotEmpty() && value !in normalized) normalized += value
        }
        return normalized
    }
}

private fun LocalDate.matchesPeriod(period: ReportPeriod, month: LocalDate): Boolean =
    when (period) {
        ReportPeriod.Month -> isSameMonth(month)
        ReportPeriod.Annual -> year == month.year
    }
