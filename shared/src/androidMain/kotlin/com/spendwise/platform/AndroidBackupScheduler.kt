package com.spendwise.platform

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AndroidBackupScheduler(
    private val context: Context
) : BackupScheduler {

    override fun scheduleDailyBackup() {
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .addTag(BACKUP_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun cancelDailyBackup() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(BACKUP_WORK_NAME)
    }

    companion object {
        private const val BACKUP_WORK_TAG = "spendwise_backup"
        private const val BACKUP_WORK_NAME = "daily_csv_backup"
    }
}
