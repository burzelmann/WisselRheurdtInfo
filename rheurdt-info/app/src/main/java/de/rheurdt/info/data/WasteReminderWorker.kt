package de.rheurdt.info.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

class WasteReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(context)
        if (!prefs.wasteNotificationEnabled) {
            return Result.success()
        }

        val repository = RheurdtRepository()
        val allWaste = repository.fetchWasteSchedule()
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowItems = allWaste.filter { it.date == tomorrow }

        if (tomorrowItems.isNotEmpty()) {
            val names = tomorrowItems.joinToString(", ") { it.title }
            val title = "Morgen Müllabfuhr in Rheurdt!"
            val msg = "Bitte bereitstellen: $names"
            NotificationHelper.sendWasteReminderNotification(
                context = context,
                notificationId = tomorrow.dayOfYear,
                title = title,
                message = msg
            )
        }

        return Result.success()
    }
}
