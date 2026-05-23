package com.spendwise.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.spendwise.MainActivity
import com.spendwise.domain.ExpenseReminder
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AndroidReminderScheduler(
    private val context: Context
) : ReminderScheduler {
    override fun schedule(reminders: List<ExpenseReminder>) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(EXPENSE_REMINDER_TAG)

        reminders.filter { it.enabled }.forEach { reminder ->
            val inputData = Data.Builder()
                .putLong(ReminderWorker.KEY_REMINDER_ID, reminder.id)
                .putInt(ReminderWorker.KEY_HOUR, reminder.hour)
                .putInt(ReminderWorker.KEY_MINUTE, reminder.minute)
                .build()
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInputData(inputData)
                .setInitialDelay(reminder.initialDelayMillis(), TimeUnit.MILLISECONDS)
                .addTag(EXPENSE_REMINDER_TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "$EXPENSE_REMINDER_WORK_PREFIX${reminder.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    private fun ExpenseReminder.initialDelayMillis(): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return next.timeInMillis - now.timeInMillis
    }
}

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureReminderChannel(notificationManager)

        val reminderId = inputData.getLong(KEY_REMINDER_ID, System.currentTimeMillis())
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = android.app.Notification.Builder(applicationContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(applicationContext.applicationInfo.icon)
            .setContentTitle("Enter your expense")
            .setContentText("Add today's spending while it is still fresh.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)
        return Result.success()
    }

    private fun ensureReminderChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Expense reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to enter expenses"
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
    }
}

private const val EXPENSE_REMINDER_TAG = "expense_reminder"
private const val EXPENSE_REMINDER_WORK_PREFIX = "expense_reminder_"
private const val REMINDER_CHANNEL_ID = "expense_reminders"
