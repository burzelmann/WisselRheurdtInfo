package de.wissel.info.data

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object WasteReminderScheduler {
    private const val WORK_NAME = "wissel_daily_waste_reminder"

    fun scheduleDailyReminder(context: Context) {
        val now = LocalDateTime.now()
        // Target is today 19:00, or tomorrow 19:00 if 19:00 already passed
        var target = now.with(LocalTime.of(19, 0, 0))
        if (now.isAfter(target)) {
            target = target.plusDays(1)
        }
        val initialDelayMinutes = Duration.between(now, target).toMinutes().coerceAtLeast(1)

        val request = OneTimeWorkRequestBuilder<WasteReminderWorker>()
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
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
