package com.spendwise.platform

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AndroidBackupScheduler(
    private val context: Context
) : BackupScheduler {

    override fun scheduleDailyBackup() {
        Log.d(TAG, "Scheduling daily backup...")
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .addTag(BACKUP_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        Log.d(TAG, "Daily backup scheduled with constraints: Wi-Fi, Charging, Idle")
    }

    override fun cancelDailyBackup() {
        Log.d(TAG, "Canceling daily backup...")
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(BACKUP_WORK_NAME)
    }

    override fun backupNow() {
        Log.d(TAG, "Triggering immediate backup...")
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .addTag(BACKUP_WORK_TAG)
            .build()
        
        workManager.enqueueUniqueWork(
            "immediate_backup",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private const val TAG = "BackupScheduler"
        private const val BACKUP_WORK_TAG = "spendwise_backup"
        private const val BACKUP_WORK_NAME = "daily_csv_backup"
    }
}
