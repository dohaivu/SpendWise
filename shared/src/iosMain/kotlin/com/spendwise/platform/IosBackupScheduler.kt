package com.spendwise.platform

class IosBackupScheduler : BackupScheduler {
    override fun scheduleDailyBackup() = Unit
    override fun cancelDailyBackup() = Unit
    override fun backupNow() = Unit
}
