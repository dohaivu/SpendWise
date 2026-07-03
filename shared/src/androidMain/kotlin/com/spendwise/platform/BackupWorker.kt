package com.spendwise.platform

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spendwise.data.ExpenseRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: ExpenseRepository by inject()

    override suspend fun doWork(): Result {
        val snapshot = repository.observeSnapshot().first()
        val backupFolderUri = snapshot.settings.backupFolderUri ?: return Result.failure()
        
        return try {
            val csvContent = repository.getBackupCsv()
            val folderUri = Uri.parse(backupFolderUri)
            val folder = DocumentFile.fromTreeUri(applicationContext, folderUri)
                ?: return Result.failure()

            val fileName = "SpendWise_Backup.csv"
            var file = folder.findFile(fileName)
            if (file == null) {
                file = folder.createFile("text/csv", fileName)
            }

            if (file != null) {
                applicationContext.contentResolver.openOutputStream(file.uri)?.use { output ->
                    output.write(csvContent.toByteArray())
                }
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
