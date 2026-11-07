package de.rheurdt.info.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

class PoliticsReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        val repo = RheurdtRepository()

        // 1. Check upcoming meetings (announcement 24h prior)
        if (prefs.politicsMeetingNotificationEnabled) {
            val tomorrow = LocalDate.now().plusDays(1)
            val meetings = runCatching { repo.fetchCouncilMeetings() }.getOrDefault(emptyList())
            val tomorrowMeetings = meetings.filter { it.date == tomorrow }
            val notifiedMeetings = prefs.notifiedCouncilMeetingIds.toMutableSet()

            for (meeting in tomorrowMeetings) {
                if (!notifiedMeetings.contains(meeting.id)) {
                    val title = "🏛️ Morgen: ${meeting.committeeName}"
                    val message = "${meeting.title}\nUhrzeit: ${meeting.time} • Ort: ${meeting.location}"
                    NotificationHelper.sendCouncilMeetingReminder(
                        context = applicationContext,
                        title = title,
                        message = message,
                        notificationId = 2000 + (meeting.id.hashCode() % 1000)
                    )
                    notifiedMeetings.add(meeting.id)
                }
            }
            prefs.notifiedCouncilMeetingIds = notifiedMeetings
        }

        // 2. Check new decisions with local Rheurdt relevance
        if (prefs.politicsDecisionNotificationEnabled) {
            val decisions = runCatching { repo.fetchCouncilDecisions() }.getOrDefault(emptyList())
            val knownIds = prefs.knownCouncilDecisionIds.toMutableSet()

            if (knownIds.isEmpty()) {
                // First run: save existing IDs so we do not spam past decisions
                knownIds.addAll(decisions.map { it.id })
                prefs.knownCouncilDecisionIds = knownIds
            } else {
                val newRelevant = decisions.filter { it.isRheurdtRelevant && !knownIds.contains(it.id) }
                for (decision in newRelevant) {
                    val title = "📜 Neuer Ratsbeschluss mit Rheurdt-Bezug!"
                    val message = "${decision.documentNumber}: ${decision.title}\n${decision.decisionSummary}"
                    NotificationHelper.sendCouncilDecisionAlert(
                        context = applicationContext,
                        title = title,
                        message = message,
                        notificationId = 3000 + (decision.id.hashCode() % 1000)
                    )
                    knownIds.add(decision.id)
                }
                knownIds.addAll(decisions.map { it.id })
                prefs.knownCouncilDecisionIds = knownIds
            }
        }

        // Reschedule for next check
        PoliticsReminderScheduler.scheduleDailyCheck(applicationContext)

        return Result.success()
    }
}
