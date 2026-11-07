package de.rheurdt.info.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rheurdt_info_prefs", Context.MODE_PRIVATE)

    var streetName: String
        get() = prefs.getString(KEY_STREET_NAME, "Rathausstraße") ?: "Rathausstraße"
        set(value) = prefs.edit().putString(KEY_STREET_NAME, value).apply()

    var pvCapacityKwp: Double
        get() = prefs.getFloat(KEY_PV_CAPACITY, 10.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_PV_CAPACITY, value.toFloat()).apply()

    var selectedBusStop: RheurdtBusStop
        get() {
            val name = prefs.getString(KEY_BUS_STOP, RheurdtBusStop.KIRCHE.name)
            return runCatching { RheurdtBusStop.valueOf(name ?: RheurdtBusStop.KIRCHE.name) }
                .getOrDefault(RheurdtBusStop.KIRCHE)
        }
        set(value) = prefs.edit().putString(KEY_BUS_STOP, value.name).apply()

    var wasteNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_WASTE_NOTIF, true)
        set(value) = prefs.edit().putBoolean(KEY_WASTE_NOTIF, value).apply()

    var politicsMeetingNotificationEnabled: Boolean
        get() = prefs.getBoolean("politics_meeting_notification_enabled", true)
        set(value) { prefs.edit().putBoolean("politics_meeting_notification_enabled", value).commit() }

    var politicsDecisionNotificationEnabled: Boolean
        get() = prefs.getBoolean("politics_decision_notification_enabled", true)
        set(value) { prefs.edit().putBoolean("politics_decision_notification_enabled", value).commit() }

    var severeAlertsNotificationEnabled: Boolean
        get() = prefs.getBoolean("severe_alerts_notification_enabled", true)
        set(value) { prefs.edit().putBoolean("severe_alerts_notification_enabled", value).commit() }

    var pegelAlertsNotificationEnabled: Boolean
        get() = prefs.getBoolean("pegel_alerts_notification_enabled", true)
        set(value) { prefs.edit().putBoolean("pegel_alerts_notification_enabled", value).commit() }

    var lastNotifiedSevereAlertId: String
        get() = prefs.getString("last_notified_severe_alert_id", "") ?: ""
        set(value) { prefs.edit().putString("last_notified_severe_alert_id", value).commit() }

    var lastNotifiedPegelStage: Int
        get() = prefs.getInt("last_notified_pegel_stage", 0)
        set(value) { prefs.edit().putInt("last_notified_pegel_stage", value).commit() }

    var knownCouncilDecisionIds: Set<String>
        get() {
            val str = prefs.getString("known_council_decision_ids_str", null)
            if (str != null) {
                if (str.isEmpty()) return emptySet()
                return str.split(";;").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            }
            return prefs.getStringSet("known_council_decision_ids", emptySet()) ?: emptySet()
        }
        set(value) {
            prefs.edit()
                .putString("known_council_decision_ids_str", value.joinToString(";;"))
                .putStringSet("known_council_decision_ids", HashSet(value))
                .commit()
        }

    var notifiedCouncilMeetingIds: Set<String>
        get() {
            val str = prefs.getString("notified_council_meeting_ids_str", null)
            if (str != null) {
                if (str.isEmpty()) return emptySet()
                return str.split(";;").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            }
            return prefs.getStringSet("notified_council_meeting_ids", emptySet()) ?: emptySet()
        }
        set(value) {
            prefs.edit()
                .putString("notified_council_meeting_ids_str", value.joinToString(";;"))
                .putStringSet("notified_council_meeting_ids", HashSet(value))
                .commit()
        }

    var dashboardCardOrder: List<String>
        get() {
            val raw = prefs.getString(KEY_CARD_ORDER, null) ?: return DashboardCardType.DEFAULT_ORDER.map { it.id }
            return raw.split(",").filter { it.isNotBlank() }
        }
        set(value) { prefs.edit().putString(KEY_CARD_ORDER, value.joinToString(",")).commit() }

    var disabledDashboardCards: Set<String>
        get() {
            val str = prefs.getString("disabled_dashboard_cards_str", null)
            if (str != null) {
                if (str.isEmpty()) return emptySet()
                return str.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            }
            return prefs.getStringSet(KEY_DISABLED_CARDS, emptySet()) ?: emptySet()
        }
        set(value) {
            val joined = value.joinToString(",")
            prefs.edit()
                .putString("disabled_dashboard_cards_str", joined)
                .putStringSet(KEY_DISABLED_CARDS, HashSet(value))
                .commit()
        }

    companion object {
        private const val KEY_STREET_NAME = "street_name"
        private const val KEY_PV_CAPACITY = "pv_capacity_kwp"
        private const val KEY_BUS_STOP = "bus_stop"
        private const val KEY_WASTE_NOTIF = "waste_notif"
        private const val KEY_CARD_ORDER = "dashboard_card_order"
        private const val KEY_DISABLED_CARDS = "disabled_dashboard_cards"

        val RHEURDT_STREETS = listOf(
            "Rathausstraße",
            "Kirchstraße",
            "Aldekerker Straße",
            "Meistersweg",
            "Bahnstraße",
            "Kengen",
            "Finkenberg",
            "Saelhuysen",
            "Schaephuysener Straße",
            "Hauptstraße",
            "Tönisberger Straße",
            "Vluyner Straße",
            "Geldersche Straße",
            "Krefelder Straße",
            "Moerser Straße",
            "Neufeld",
            "Lind",
            "Wickrath",
            "St.-Nikolaus-Weg",
            "Pastoratsweg",
            "Am Sportplatz",
            "Bergdahlsweg",
            "Holzhauer",
            "Littardweg",
            "Oermterberg",
            "Sassenrath",
            "Schüttenhof",
            "Vennekelshof",
            "Wolfsbergweg",
            "Leursdyk",
            "Hochend",
            "Grenzweg",
            "Waldweg",
            "Bürgermeister-Driessen-Straße"
        )
    }
}
