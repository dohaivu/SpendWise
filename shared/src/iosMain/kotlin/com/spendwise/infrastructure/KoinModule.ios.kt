package com.spendwise.infrastructure

import com.spendwise.platform.IosReminderScheduler
import com.spendwise.platform.ReminderScheduler
import org.koin.dsl.module

actual fun platformModule() = module {
    single<ReminderScheduler> { IosReminderScheduler() }
}

