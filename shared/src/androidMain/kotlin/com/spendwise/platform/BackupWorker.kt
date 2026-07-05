package com.spendwise.platform

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spendwise.data.ExpenseRepository
import com.spendwise.data.SettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: ExpenseRepository by inject()
    private val settingsRepository: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        Log.d(TAG, "BackupWorker starting...")
        val settings = settingsRepository.settings.first()
        val backupFolderUri = settings.backupFolderUri 
        
        if (backupFolderUri == null) {
            Log.w(TAG, "Backup failed: No backup folder selected")
            return Result.failure()
        }

        val lastBackup = settings.lastBackupAtMillis ?: 0L
        val now = System.currentTimeMillis()
        
        // Skip if last backup was successful within the last 23 hours to avoid redundancy
        // (using 23h instead of 24h to allow for small scheduling drifts)
        if (settings.lastBackupAtMillis != null && (now - lastBackup) < 23 * 60 * 60 * 1000L) {
            Log.d(TAG, "Backup skipped: Last backup was less than 23h ago")
            return Result.success()
        }
        
        return try {
            Log.d(TAG, "Generating backup JSON...")
            val jsonContent = repository.getBackupJson()
            val folderUri = Uri.parse(backupFolderUri)
            val folder = DocumentFile.fromTreeUri(applicationContext, folderUri)
            
            if (folder == null || !folder.exists()) {
                Log.e(TAG, "Backup failed: Could not access folder or folder doesn't exist")
                return Result.failure()
            }

            val fileName = "SpendWise_Backup.json"
            var file = folder.findFile(fileName)
            if (file == null) {
                Log.d(TAG, "Creating new backup file: $fileName")
                file = folder.createFile("application/json", fileName)
            }

            if (file != null) {
                Log.d(TAG, "Writing data to file: ${file.uri}")
                applicationContext.contentResolver.openOutputStream(file.uri)?.use { output ->
                    output.write(jsonContent.toByteArray())
                }
                
                // Update last backup time
                settingsRepository.saveSettings(settings.copy(lastBackupAtMillis = System.currentTimeMillis()))
                Log.i(TAG, "Backup successful!")
                Result.success()
            } else {
                Log.e(TAG, "Backup failed: Could not create file")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed with exception", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BackupWorker"
    }
}
