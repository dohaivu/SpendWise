package com.spendwise.infrastructure

import androidx.room.RoomDatabase
import com.spendwise.data.ExpenseRepository
import com.spendwise.data.FrankfurterExchangeRateClient
import com.spendwise.data.RoomExpenseRepository
import com.spendwise.data.SpendWiseDatabase
import com.spendwise.data.buildSpendWiseDatabase
import com.spendwise.data.provideSpendWiseDatabaseBuilder
import com.spendwise.domain.usecase.AddExpenseUseCase
import com.spendwise.domain.usecase.ConvertToBaseCurrencyUseCase
import com.spendwise.domain.usecase.DeleteCategoryUseCase
import com.spendwise.domain.usecase.DeleteExpenseUseCase
import com.spendwise.domain.usecase.GetCategoryPieReportUseCase
import com.spendwise.domain.usecase.GetCurrencySettingsUseCase
import com.spendwise.domain.usecase.GetDailyExpenseTotalsUseCase
import com.spendwise.domain.usecase.GetAnnualMonthlyReportUseCase
import com.spendwise.domain.usecase.GetExchangeRateUseCase
import com.spendwise.domain.usecase.GetExpensesUseCase
import com.spendwise.domain.usecase.GetKnownTagsUseCase
import com.spendwise.domain.usecase.GetTagAutocompleteSuggestionsUseCase
import com.spendwise.domain.usecase.GetTagUsageStatsUseCase
import com.spendwise.domain.usecase.GetTransactionsByDateUseCase
import com.spendwise.domain.usecase.GetTransactionsByFiltersUseCase
import com.spendwise.domain.usecase.GetYearlyCategoryReportUseCase
import com.spendwise.domain.usecase.MoveCategoryUseCase
import com.spendwise.domain.usecase.ParseTagsFromNoteUseCase
import com.spendwise.domain.usecase.SaveCategoryUseCase
import com.spendwise.domain.usecase.SpendWiseUseCases
import com.spendwise.domain.usecase.UpdateBaseCurrencyUseCase
import com.spendwise.domain.usecase.UpdateExpenseUseCase
import com.spendwise.ui.calendar.AllTransactionsViewModel
import com.spendwise.ui.calendar.CalendarViewModel
import com.spendwise.ui.expense.ExpenseViewModel
import com.spendwise.ui.reports.ReportViewModel
import com.spendwise.ui.settings.SettingsViewModel
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

expect fun platformModule(): Module

fun doInitKoin() {
    initKoin()
}

fun initKoin(config: KoinAppDeclaration? = null) =
    startKoin {
        config?.invoke(this)
        modules(
            platformModule(),
            provideDatabaseModule,
            provideInteractorModule,
            provideLocalServiceModule,
            provideViewModelModule
        )
    }

val provideInteractorModule = module {
    single { HttpClient() }
    single { FrankfurterExchangeRateClient(get()) }
    single<ExpenseRepository> { RoomExpenseRepository(get(), get()) }
    single { AddExpenseUseCase(get()) }
    single { UpdateExpenseUseCase(get()) }
    single { DeleteExpenseUseCase(get()) }
    single { GetExpensesUseCase(get()) }
    single { GetTransactionsByDateUseCase() }
    single { GetTransactionsByFiltersUseCase() }
    single { ParseTagsFromNoteUseCase() }
    single { GetTagAutocompleteSuggestionsUseCase() }
    single { GetTagUsageStatsUseCase(get()) }
    single { GetKnownTagsUseCase(get()) }
    single { GetDailyExpenseTotalsUseCase() }
    single { GetCategoryPieReportUseCase() }
    single { GetYearlyCategoryReportUseCase() }
    single { GetAnnualMonthlyReportUseCase() }
    single { ConvertToBaseCurrencyUseCase() }
    single { GetCurrencySettingsUseCase(get()) }
    single { UpdateBaseCurrencyUseCase(get()) }
    single { GetExchangeRateUseCase(get()) }
    single { SaveCategoryUseCase(get()) }
    single { DeleteCategoryUseCase(get()) }
    single { MoveCategoryUseCase(get()) }
    single {
        SpendWiseUseCases(
            addExpense = get(),
            updateExpense = get(),
            deleteExpense = get(),
            getExpenses = get(),
            getTransactionsByDate = get(),
            getTransactionsByFilters = get(),
            parseTagsFromNote = get(),
            getTagAutocompleteSuggestions = get(),
            getTagUsageStats = get(),
            getKnownTags = get(),
            getDailyExpenseTotals = get(),
            getCategoryPieReport = get(),
            getYearlyCategoryReport = get(),
            getAnnualMonthlyReport = get(),
            convertToBaseCurrency = get(),
            getCurrencySettings = get(),
            updateBaseCurrency = get(),
            getExchangeRate = get(),
            saveCategory = get(),
            deleteCategory = get(),
            moveCategory = get()
        )
    }
}

val provideDatabaseModule = module {
    single<RoomDatabase.Builder<SpendWiseDatabase>> { provideSpendWiseDatabaseBuilder() }
    single { buildSpendWiseDatabase(get()) }
    single { get<SpendWiseDatabase>().spendWiseDao() }
}

val provideLocalServiceModule = module {

}

val provideViewModelModule = module {
    viewModel { ExpenseViewModel(get(), get()) }
    viewModel { CalendarViewModel(get()) }
    viewModel { AllTransactionsViewModel(get()) }
    viewModel { ReportViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
