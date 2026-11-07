package de.rheurdt.info.data

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
    val station: String = "Duisburg-Ruhrort (km 780,8)",
    val currentCm: Double,
    val timestamp: String,
    val trend: String,
    val diffToYesterdayCm: Double,
    val historicMinCm: Double = 110.0,
    val averageCm: Double = 330.0,
    val recentValues: List<Double> = emptyList(),
    val history30d: List<PegelPoint> = emptyList(),
    val characteristics: List<CharacteristicValue> = emptyList(),
    val weselData: PegelStationData? = null
)

data class MastrRecentUnit(
    val name: String,
    val energyType: String,
    val powerKw: Double,
    val commissioningDate: String,
    val locationOrStreet: String
)

data class MastrEnergyStats(
    val solarUnits: Int = 853,
    val batteryUnits: Int = 404,
    val windUnits: Int = 14,
    val biomassUnits: Int = 2,
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

enum class RheurdtBusStop(val id: String, val stopName: String, val shortName: String) {
    KIRCHE("20035435", "Rheurdt Kirche", "Kirche (Rathaus)"),
    SCHAEPHUYSEN_KIRCHE("20035431", "Rheurdt Schaephuysen Kirche", "Schaephuysen"),
    GRUNDSCHULE("20035008", "Rheurdt Grundschule", "Grundschule"),
    SPARKASSE("20035434", "Rheurdt Sparkasse", "Sparkasse"),
    FINKENBERG("20035013", "Rheurdt Finkenberg", "Finkenberg")
}

data class CouncilDecision(
    val id: String,
    val title: String,
    val committeeName: String,
    val date: LocalDate,
    val documentNumber: String,
    val decisionSummary: String,
    val isRheurdtRelevant: Boolean = true,
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
    val location: String = "Bürgerforum Rheurdt"
)

enum class VillageEventCategory(val label: String, val icon: String, val colorHex: Long) {
    OEKODORF("Ökodorf & Natur", "🌿", 0xFF2E7D32),
    VEREIN("Vereine & Brauchtum", "🎪", 0xFFE65100),
    SCHUETZEN("Schützen", "🎯", 0xFF1565C0),
    FEUERWEHR("Feuerwehr", "🚒", 0xFFD32F2F),
    KIRCHE("Kirche & Feste", "⛪", 0xFF6A1B9A),
    SPORT("Sport & Freizeit", "🏃", 0xFF00796B)
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
    WEATHER("weather", "Wetter & Photovoltaik", "☀️", "DWD Wetter, Sonnenstunden & PV-Ertrag Rheurdt"),
    PEGEL("pegel", "Rheinpegel & Gewässer", "🌊", "Duisburg-Ruhrort & Wesel mit 30-Tage-Verlauf"),
    ENERGY("energy", "Energie-Tracker Rheurdt", "⚡", "Marktstammdatenregister: Solar & Speicher (PLZ 47509)"),
    WASTE("waste", "Nächste Müllabfuhr", "🗑️", "Abfuhrtermine Gemeinde Rheurdt & Schaephuysen"),
    BUS("bus", "Bus-Abfahrten Rheurdt", "🚌", "VRR Live-Fahrplan, SB70 & NIAG Linien"),
    EVENTS("events", "Vereine & Dorfleben", "🎪", "Termine Rheurdt, Schaephuysen & Ökodorf"),
    POLITICS("politics", "Lokalpolitik & Rat", "🏛️", "Gemeinderat Rheurdt & Ausschüsse (ris.rheurdt.de)"),
    HOLIDAYS("holidays", "Ferien & Feiertage", "🏖️", "Schulferien & Feiertage in NRW");

    companion object {
        fun fromId(id: String): DashboardCardType? = values().firstOrNull { it.id == id }
        val DEFAULT_ORDER = listOf(WEATHER, PEGEL, ENERGY, WASTE, BUS, EVENTS, POLITICS, HOLIDAYS)
    }
}

data class RheurdtDashboardState(
    val activeCards: List<DashboardCardType> = DashboardCardType.DEFAULT_ORDER,
    val disabledCards: List<DashboardCardType> = emptyList(),
    val isLoading: Boolean = true,
    val lastUpdated: String = "",
    val errorMessage: String? = null,
    val weather: WeatherData? = null,
    val pegelData: PegelData? = null,
    val mastrEnergyStats: MastrEnergyStats? = null,
    val nextWaste: List<WasteItem> = emptyList(),
    val allWaste: List<WasteItem> = emptyList(),
    val activeWarnings: List<WarningItem> = emptyList(),
    val upcomingHolidays: List<HolidayItem> = emptyList(),
    val busDepartures: List<BusDeparture> = emptyList(),
    val selectedBusStop: RheurdtBusStop = RheurdtBusStop.KIRCHE,
    val isBusLoading: Boolean = false,
    val councilMeetings: List<CouncilMeeting> = emptyList(),
    val councilDecisions: List<CouncilDecision> = emptyList(),
    val villageEvents: List<VillageEvent> = emptyList(),
    val councilFactions: List<CouncilFaction> = emptyList(),
    val streetName: String = "Rathausstraße",
    val pvCapacityKwp: Double = 10.0,
    val wasteNotificationEnabled: Boolean = true,
    val politicsMeetingNotificationEnabled: Boolean = true,
    val politicsDecisionNotificationEnabled: Boolean = true,
    val severeAlertsNotificationEnabled: Boolean = true,
    val pegelAlertsNotificationEnabled: Boolean = true
)
