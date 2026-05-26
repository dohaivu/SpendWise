package com.spendwise.domain.usecase

import com.spendwise.domain.Category
import com.spendwise.domain.CategoryReportRow
import com.spendwise.domain.DailyExpenseTotal
import com.spendwise.domain.Expense
import com.spendwise.domain.MonthlyExpenseTotal
import com.spendwise.domain.ReportCalculator
import kotlinx.datetime.TimeZone

class GetDailyExpenseTotalsUseCase {
    operator fun invoke(expenses: List<Expense>, timeZone: TimeZone): List<DailyExpenseTotal> =
        ReportCalculator.dailyTotals(expenses, timeZone)
}

class GetCategoryPieReportUseCase {
    operator fun invoke(
        expenses: List<Expense>,
        categories: List<Category>
    ): List<CategoryReportRow> =
        ReportCalculator.categoryReport(expenses, categories)
}

class GetYearlyCategoryReportUseCase {
    operator fun invoke(
        expenses: List<Expense>,
        categories: List<Category>,
        year: Int,
        timeZone: TimeZone
    ): List<CategoryReportRow> {
        val filtered = expenses.filter { expense -> with(ReportCalculator) { expense.localDate(timeZone).year == year } }
        return ReportCalculator.categoryReport(filtered, categories)
    }
}

class GetAnnualMonthlyReportUseCase {
    operator fun invoke(
        expenses: List<Expense>,
        year: Int,
        timeZone: TimeZone
    ): List<MonthlyExpenseTotal> =
        ReportCalculator.monthlyTotals(expenses, year, timeZone)
}
