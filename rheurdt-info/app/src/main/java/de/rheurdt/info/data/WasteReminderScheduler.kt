package de.rheurdt.info.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object WasteReminderScheduler {
    private const val WORK_TAG = "RheurdtWasteReminderPeriodic"

    fun scheduleDailyReminder(context: Context) {
        val now = LocalDateTime.now()
        var targetTime = now.withHour(18).withMinute(0).withSecond(0)
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }
        val initialDelayMinutes = Duration.between(now, targetTime).toMinutes().coerceAtLeast(1)

        val request = PeriodicWorkRequestBuilder<WasteReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
    }
}
