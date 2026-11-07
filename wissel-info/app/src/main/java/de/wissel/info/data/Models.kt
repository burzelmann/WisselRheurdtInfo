package de.wissel.info.data

import java.time.LocalDate

data class SolarDayForecast(
    val date: LocalDate,
    val isTomorrow: Boolean,
    val solarSumKwh: Double,
    val pvYieldKwh: Double,
    val hourlySolar: List<Pair<Int, Double>> = emptyList(),
    val minTemp: Double = 10.0,
    val maxTemp: Double = 18.0,
    val condition: String = "dry"
)

data class WeatherData(
    val temperature: Double,
    val windSpeedKmh: Double,
    val condition: String,
    val icon: String,
    val humidity: Int,
    val solarSumKwh: Double,
    val precipitationMm: Double,
    val hourlySolar: List<Pair<Int, Double>> = emptyList(),
    val todayForecast: SolarDayForecast? = null,
    val tomorrowForecast: SolarDayForecast? = null
)

data class PegelPoint(
    val timestamp: String,
    val epochSeconds: Long,
    val value: Double
)

data class CharacteristicValue(
    val shortname: String,
    val longname: String,
    val value: Double
)

data class PegelStationData(
    val stationShortname: String,
    val stationName: String,
    val km: Double,
    val currentCm: Double,
    val timestamp: String,
    val trend: String,
    val diffToYesterdayCm: Double,
    val characteristics: List<CharacteristicValue> = emptyList(),
    val history30d: List<PegelPoint> = emptyList()
)

data class PegelData(
    val station: String = "Rees (km 837,4)",
    val currentCm: Double,
    val timestamp: String,
    val trend: String,
    val diffToYesterdayCm: Double,
    val historicMinCm: Double = 38.0,
    val averageCm: Double = 293.0,
    val recentValues: List<Double> = emptyList(),
    val history30d: List<PegelPoint> = emptyList(),
    val characteristics: List<CharacteristicValue> = emptyList(),
    val emmerichData: PegelStationData? = null
)

data class MastrRecentUnit(
    val name: String,
    val energyType: String,
    val powerKw: Double,
    val commissioningDate: String,
    val locationOrStreet: String
)

data class MastrEnergyStats(
    val solarUnits: Int = 1778,
    val batteryUnits: Int = 805,
    val windUnits: Int = 13,
    val biomassUnits: Int = 6,
    val recentUnits: List<MastrRecentUnit> = emptyList()
)

enum class WasteType(val label: String, val colorHex: Long) {
    REST("Restabfall", 0xFF424242),
    BIO("Bioabfall", 0xFF5D4037),
    GELB("Gelbe Tonne", 0xFFFBC02D),
    PAPIER("Papier", 0xFF1976D2),
    GLAS("Altglas", 0xFF00796B),
    SCHADSTOFFE("Schadstoffe", 0xFFE64A19),
    GRUEN("Grünbündel", 0xFF388E3C),
    SONSTIGES("Sonstiges", 0xFF757575)
}

data class WasteItem(
    val date: LocalDate,
    val title: String,
    val type: WasteType
)

data class WarningItem(
    val id: String,
    val headline: String,
    val severity: String,
    val description: String,
    val startDate: String
)

data class HolidayItem(
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isSchoolHoliday: Boolean
)

data class BusDeparture(
    val line: String,
    val direction: String,
    val scheduledTime: String,
    val countdownMinutes: Int,
    val isRealtime: Boolean = false,
    val stopName: String,
    val hint: String? = null
)

enum class WisselBusStop(val id: String, val stopName: String, val shortName: String) {
    GRUNDSCHULE("20035920", "Wissel Grundschule", "Grundschule"),
    MOLKEREI("20035660", "Wissel Molkerei", "Molkerei"),
    ORTSEINGANG("20035661", "Wissel Ortseingang", "Ortseingang"),
    CAMPINGPLATZ("20037333", "Campingplatz Wisseler See", "Campingplatz")
}

data class CouncilDecision(
    val id: String,
    val title: String,
    val committeeName: String,
    val date: LocalDate,
    val documentNumber: String,
    val decisionSummary: String,
    val isWisselRelevant: Boolean = false,
    val pdfUrl: String? = null,
    val vorgangUrl: String? = null,
    val topsUrl: String? = null,
    val status: String = "Beschlossen"
)

data class CouncilMeeting(
    val id: String,
    val title: String,
    val committeeName: String,
    val date: LocalDate,
    val time: String,
    val url: String,
    val location: String = "Historisches Rathaus Kalkar (Ratssaal)"
)

enum class VillageEventCategory(val label: String, val icon: String, val colorHex: Long) {
    VEREIN("Vereine", "🎪", 0xFFE65100),
    SCHUETZEN("Schützen", "🎯", 0xFF2E7D32),
    MUSEUM("Museum", "🏛️", 0xFF6A1B9A),
    FEUERWEHR("Feuerwehr", "🚒", 0xFFD32F2F),
    KIRCHE("Kirche & Feste", "⛪", 0xFF0288D1),
    SPORT("Sport", "⚽", 0xFF00796B)
}

data class VillageEvent(
    val id: String,
    val title: String,
    val organizer: String,
    val date: LocalDate,
    val time: String? = null,
    val location: String,
    val description: String,
    val category: VillageEventCategory
)

data class CouncilFaction(
    val name: String,
    val shortName: String,
    val seats: Int,
    val colorHex: Long
)


enum class DashboardCardType(
    val id: String,
    val title: String,
    val icon: String,
    val subtitle: String
) {
    WEATHER("weather", "Wetter & Photovoltaik", "☀️", "DWD Wetter, Sonnenstunden & PV-Ertrag"),
    PEGEL("pegel", "Rheinpegel & Zeitreihen", "🌊", "Pegel Rees & Emmerich mit 30-Tage-Verlauf"),
    ENERGY("energy", "Energie-Tracker Wissel", "⚡", "Marktstammdatenregister: Solar, Speicher, Wind"),
    WASTE("waste", "Nächste Müllabfuhr", "🗑️", "Abfuhrtermine für deinen Straßenbezirk"),
    BUS("bus", "Bus-Abfahrten Wissel", "🚌", "VRR Live-Fahrplan & TaxiBus"),
    EVENTS("events", "Vereine & Dorfleben", "🎪", "Termine Wisseler Vereine & Schützen"),
    POLITICS("politics", "Lokalpolitik & Rat", "🏛️", "Ratssitzungen & Ratsbeschlüsse Kalkar"),
    HOLIDAYS("holidays", "Ferien & Feiertage", "🏖️", "Schulferien & Feiertage in NRW");

    companion object {
        fun fromId(id: String): DashboardCardType? = values().firstOrNull { it.id == id }
        val DEFAULT_ORDER = listOf(WEATHER, PEGEL, ENERGY, WASTE, BUS, EVENTS, POLITICS, HOLIDAYS)
    }
}

data class WisselDashboardState(
    val activeCards: List<DashboardCardType> = DashboardCardType.DEFAULT_ORDER,
    val disabledCards: List<DashboardCardType> = emptyList(),
    val isLoading: Boolean = true,
    val lastUpdated: String = "",
    val errorMessage: String? = null,
    val weather: WeatherData? = null,
    val pegelRees: PegelData? = null,
    val mastrEnergyStats: MastrEnergyStats? = null,
    val nextWaste: List<WasteItem> = emptyList(),
    val allWaste: List<WasteItem> = emptyList(),
    val activeWarnings: List<WarningItem> = emptyList(),
    val upcomingHolidays: List<HolidayItem> = emptyList(),
    val busDepartures: List<BusDeparture> = emptyList(),
    val selectedBusStop: WisselBusStop = WisselBusStop.GRUNDSCHULE,
    val isBusLoading: Boolean = false,
    val councilMeetings: List<CouncilMeeting> = emptyList(),
    val councilDecisions: List<CouncilDecision> = emptyList(),
    val villageEvents: List<VillageEvent> = emptyList(),
    val councilFactions: List<CouncilFaction> = emptyList(),
    val streetName: String = "Zum Wisseler See",
    val pvCapacityKwp: Double = 10.0,
    val wasteNotificationEnabled: Boolean = true,
    val politicsMeetingNotificationEnabled: Boolean = true,
    val politicsDecisionNotificationEnabled: Boolean = true,
    val severeAlertsNotificationEnabled: Boolean = true,
    val pegelAlertsNotificationEnabled: Boolean = true
)
