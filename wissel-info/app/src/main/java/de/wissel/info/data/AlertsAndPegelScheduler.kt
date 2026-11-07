package de.wissel.info.data

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object AlertsAndPegelScheduler {
    private const val WORK_NAME = "wissel_alerts_and_pegel_worker"

    fun schedulePeriodicCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<AlertsAndPegelWorker>(2, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelChecks(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
