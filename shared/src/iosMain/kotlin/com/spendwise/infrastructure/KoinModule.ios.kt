package com.spendwise.infrastructure

import com.spendwise.platform.BackupScheduler
import com.spendwise.platform.IosBackupScheduler
import com.spendwise.platform.IosReminderScheduler
import com.spendwise.platform.ReminderScheduler
import org.koin.dsl.module

actual fun platformModule() = module {
    single<ReminderScheduler> { IosReminderScheduler() }
    single<BackupScheduler> { IosBackupScheduler() }
}

