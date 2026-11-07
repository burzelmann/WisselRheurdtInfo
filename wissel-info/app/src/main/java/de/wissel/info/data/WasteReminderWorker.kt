package de.wissel.info.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

class WasteReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        if (!prefs.wasteNotificationEnabled) {
            return Result.success()
        }

        val tomorrow = LocalDate.now().plusDays(1)
        val repository = WisselRepository()
        val allWaste = repository.fetchWasteSchedule(prefs.street)
        val tomorrowWaste = allWaste.filter { it.date == tomorrow }

        if (tomorrowWaste.isNotEmpty()) {
            val names = tomorrowWaste.joinToString(" & ") { it.title }
            val formattedDate = "${tomorrow.dayOfMonth.toString().padStart(2, '0')}.${tomorrow.monthValue.toString().padStart(2, '0')}."
            NotificationHelper.sendWasteReminder(
                context = applicationContext,
                title = "Müllabfuhr Wissel (${prefs.street})",
                message = "Morgen ($formattedDate) Leerung: $names! Bitte heute Abend oder bis 06:00 Uhr rausstellen."
            )
        }

        // Reschedule for the next day at 19:00
        WasteReminderScheduler.scheduleDailyReminder(applicationContext)

        return Result.success()
    }
}
