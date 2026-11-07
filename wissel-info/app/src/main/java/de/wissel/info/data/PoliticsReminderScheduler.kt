package de.wissel.info.data

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object PoliticsReminderScheduler {
    private const val WORK_NAME = "wissel_politics_reminder"

    fun scheduleDailyCheck(context: Context) {
        val now = LocalDateTime.now()
        // Check at 18:00 (24h before evening meetings)
        var target = now.with(LocalTime.of(18, 0, 0))
        if (now.isAfter(target)) {
            target = target.plusDays(1)
        }
        val initialDelayMinutes = Duration.between(now, target).toMinutes().coerceAtLeast(1)

        val request = OneTimeWorkRequestBuilder<PoliticsReminderWorker>()
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
