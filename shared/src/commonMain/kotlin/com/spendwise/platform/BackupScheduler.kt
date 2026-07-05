package com.spendwise.platform

interface BackupScheduler {
    fun scheduleDailyBackup()
    fun cancelDailyBackup()
    fun backupNow()
}
