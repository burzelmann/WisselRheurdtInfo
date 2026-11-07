package de.rheurdt.info.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AlertsAndPegelWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        val repo = RheurdtRepository()

        // 1. Check Severe Weather & MoWaS Alerts
        if (prefs.severeAlertsNotificationEnabled) {
            val dashboard = runCatching { repo.fetchDashboard(enabledCards = setOf(DashboardCardType.WEATHER)) }.getOrNull()
            val warnings = dashboard?.activeWarnings ?: emptyList()

            if (warnings.isNotEmpty()) {
                val mostSevere = warnings.first()
                if (mostSevere.id != prefs.lastNotifiedSevereAlertId) {
                    val title = "⚠️ Amtliche Warnung: ${mostSevere.headline}"
                    val message = if (mostSevere.description.isNotBlank()) mostSevere.description else "Bitte Schutzmaßnahmen beachten."
                    NotificationHelper.sendSevereAlert(
                        context = applicationContext,
                        title = title,
                        message = message
                    )
                    prefs.lastNotifiedSevereAlertId = mostSevere.id
                }
            } else {
                prefs.lastNotifiedSevereAlertId = ""
            }
        }

        // 2. Check Rheinpegel Wesel (Marke I = 640 cm, Marke II = 800 cm)
        if (prefs.pegelAlertsNotificationEnabled) {
            val pegel = runCatching { repo.fetchPegel() }.getOrNull()
            if (pegel != null) {
                val currentCm = pegel.currentCm
                val currentStage = when {
                    currentCm >= 800.0 -> 2
                    currentCm >= 640.0 -> 1
                    else -> 0
                }
                val lastStage = prefs.lastNotifiedPegelStage

                if (currentStage > lastStage) {
                    val meters = String.format("%.2f", currentCm / 100.0)
                    if (currentStage == 1) {
                        NotificationHelper.sendPegelAlert(
                            context = applicationContext,
                            title = "🌊 Rheinpegel Wesel: Hochwassermarke I überschritten!",
                            message = "Aktueller Wasserstand: $meters m (${pegel.trend}). Schifffahrt fährt gedrosselt. Erste Rheinauen geflutet."
                        )
                    } else if (currentStage == 2) {
                        NotificationHelper.sendPegelAlert(
                            context = applicationContext,
                            title = "🚨 Rheinpegel Wesel: Hochwassermarke II erreicht!",
                            message = "Aktueller Wasserstand: $meters m (${pegel.trend}). Schifffahrt auf dem Rhein offiziell gesperrt!"
                        )
                    }
                    prefs.lastNotifiedPegelStage = currentStage
                } else if (currentStage == 0 && lastStage > 0) {
                    // Entwarnung
                    val meters = String.format("%.2f", currentCm / 100.0)
                    NotificationHelper.sendPegelAlert(
                        context = applicationContext,
                        title = "🌊 Rheinpegel Wesel: Entwarnung",
                        message = "Der Wasserstand ist wieder unter die Hochwassermarke I gesunken (aktuell $meters m)."
                    )
                    prefs.lastNotifiedPegelStage = 0
                }
            }
        }

        return Result.success()
    }
}
