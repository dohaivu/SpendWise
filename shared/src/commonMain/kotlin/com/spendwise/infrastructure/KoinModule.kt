package com.spendwise.infrastructure

import androidx.room.RoomDatabase
import com.spendwise.data.ExpenseRepository
import com.spendwise.data.RoomExpenseRepository
import com.spendwise.data.SpendWiseDatabase
import com.spendwise.data.buildSpendWiseDatabase
import com.spendwise.data.provideSpendWiseDatabaseBuilder
import com.spendwise.ui.SpendWiseViewModel
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
    single<ExpenseRepository> { RoomExpenseRepository(get()) }
}

val provideDatabaseModule = module {
    single<RoomDatabase.Builder<SpendWiseDatabase>> { provideSpendWiseDatabaseBuilder() }
    single { buildSpendWiseDatabase(get()) }
    single { get<SpendWiseDatabase>().spendWiseDao() }
}

val provideLocalServiceModule = module {

}

val provideViewModelModule = module {
    viewModel { SpendWiseViewModel(get()) }
}
