package com.spendwise.ui.calendar

import com.spendwise.domain.DailyExpenseTotal
import com.spendwise.domain.Expense
import com.spendwise.domain.ExpenseEntry
import com.spendwise.domain.ExpenseMatcher
import com.spendwise.domain.TransactionFilters
import com.spendwise.ui.CalendarData
import com.spendwise.ui.DateTransactionListItem
import com.spendwise.ui.components.ReportPeriod
import com.spendwise.ui.isSameMonth
import kotlinx.datetime.LocalDate

internal object CalendarDataBuilder {

    /**
     * Groups [entries] (sorted by `spentAtMillis` descending) into transaction items, daily totals
     * and header indexes in a single pass.
     */
    fun build(
        entries: List<ExpenseEntry>,
        period: ReportPeriod,
        month: LocalDate,
        filters: TransactionFilters
    ): CalendarData {
        val matcher = ExpenseMatcher.of(filters)

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
            if (!matcher.matches(entry)) continue

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
}

private fun LocalDate.matchesPeriod(period: ReportPeriod, month: LocalDate): Boolean =
    when (period) {
        ReportPeriod.Month -> isSameMonth(month)
        ReportPeriod.Annual -> year == month.year
    }
