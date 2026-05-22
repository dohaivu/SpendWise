package com.spendwise.domain.usecase

data class SpendWiseUseCases(
    val addExpense: AddExpenseUseCase,
    val updateExpense: UpdateExpenseUseCase,
    val deleteExpense: DeleteExpenseUseCase,
    val getExpenses: GetExpensesUseCase,
    val getTransactionsByDate: GetTransactionsByDateUseCase,
    val getTransactionsByFilters: GetTransactionsByFiltersUseCase,
    val parseTagsFromNote: ParseTagsFromNoteUseCase,
    val getTagAutocompleteSuggestions: GetTagAutocompleteSuggestionsUseCase,
    val getTagUsageStats: GetTagUsageStatsUseCase,
    val getKnownTags: GetKnownTagsUseCase,
    val getDailyExpenseTotals: GetDailyExpenseTotalsUseCase,
    val getCategoryPieReport: GetCategoryPieReportUseCase,
    val getYearlyCategoryReport: GetYearlyCategoryReportUseCase,
    val getAnnualMonthlyReport: GetAnnualMonthlyReportUseCase,
    val convertToBaseCurrency: ConvertToBaseCurrencyUseCase,
    val getCurrencySettings: GetCurrencySettingsUseCase,
    val updateBaseCurrency: UpdateBaseCurrencyUseCase,
    val getExchangeRate: GetExchangeRateUseCase,
    val saveCategory: SaveCategoryUseCase,
    val deleteCategory: DeleteCategoryUseCase,
    val moveCategory: MoveCategoryUseCase
)
