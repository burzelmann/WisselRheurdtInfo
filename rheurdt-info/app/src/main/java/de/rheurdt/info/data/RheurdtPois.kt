package de.rheurdt.info.data

enum class PoiCategory(val label: String, val icon: String, val colorHex: Long) {
    WASSER("Pegel & Gewässer", "🌊", 0xFF0288D1),
    HALTESTELLE("Haltestellen", "🚏", 0xFF2E7D32),
    NATUR("Natur & Ökodorf", "🌳", 0xFFE65100),
    KULTUR("Ort & Kultur", "🏛️", 0xFF6A1B9A)
}

data class RheurdtPoi(
    val id: String,
    val title: String,
    val category: PoiCategory,
    val lat: Double,
    val lon: Double,
    val subtitle: String,
    val description: String,
    val extraValue: String? = null,
    val isRegional: Boolean = false
)

object RheurdtPois {
    fun getPois(currentPegelCm: Double? = null): List<RheurdtPoi> {
        val pegelStr = if (currentPegelCm != null) "$currentPegelCm cm" else "144 cm"
        return listOf(
            RheurdtPoi(
                id = "buergerforum",
                title = "Rathaus & Bürgerforum Rheurdt",
                category = PoiCategory.KULTUR,
                lat = 51.4648,
                lon = 6.4715,
                subtitle = "Rathausstraße 35 (Ortsmitte)",
                description = "Verwaltungssitz der Gemeinde Rheurdt und Tagungsort des Gemeinderats sowie der Ratsausschüsse im modernen Bürgerforum.",
                extraValue = "Rathaus"
            ),
            RheurdtPoi(
                id = "kirche_nikolaus",
                title = "St. Nikolaus Kirche Rheurdt",
                category = PoiCategory.KULTUR,
                lat = 51.4658,
                lon = 6.4721,
                subtitle = "Kirchstraße / Ortskern",
                description = "Katholische Pfarrkirche im Herzen von Rheurdt. Schutzpatron St. Nikolaus ziert auch das offizielle Gemeindewappen.",
                extraValue = "Kirche"
            ),
            RheurdtPoi(
                id = "kirche_hubertus",
                title = "St. Hubertus Kirche Schaephuysen",
                category = PoiCategory.KULTUR,
                lat = 51.4532,
                lon = 6.5165,
                subtitle = "Hauptstraße 30 / Schaephuysen",
                description = "Neugotische Pfarrkirche in Schaephuysen, Treffpunkt für Gemeindeleben und Dorfbrauchtum am Fuße des Höhenzugs.",
                extraValue = "Ortsteil Schaephuysen"
            ),
            RheurdtPoi(
                id = "oekodorf_pfad",
                title = "Ökodorf Rheurdt & Kräutergarten",
                category = PoiCategory.NATUR,
                lat = 51.4665,
                lon = 6.4670,
                subtitle = "Aldekerker Straße / Grünes Rheurdt",
                description = "Rheurdt trägt seit 1991 den Beinamen Ökodorf. Naturlehrpfade, Streuobstwiesen und ökologische Projekte prägen den Ort.",
                extraValue = "Ökodorf NRW"
            ),
            RheurdtPoi(
                id = "littardkuhlen",
                title = "Naturschutzgebiet Littardkuhlen",
                category = PoiCategory.WASSER,
                lat = 51.4720,
                lon = 6.4880,
                subtitle = "Littardweg (LINEG-Feuchtgebiet)",
                description = "Idyllische Gewässerkette alter Torfgräben mit seltenen Wasservögeln, Seerosen und Feuchtbiotopen.",
                extraValue = "Naturschutzgebiet"
            ),
            RheurdtPoi(
                id = "schaephuysener_hoehen",
                title = "Schaephuysener Höhen & Windmühlenberg",
                category = PoiCategory.NATUR,
                lat = 51.4510,
                lon = 6.5240,
                subtitle = "Niederrheinischer Höhenzug",
                description = "Markante eiszeitliche Endmoräne mit ca. 80 m ü. NHN. Beliebtes Wander- und Laufrevier mit Weitblick bis ins Ruhrgebiet.",
                extraValue = "Aussichtspunkt"
            ),
            RheurdtPoi(
                id = "sport_meistersweg",
                title = "Sportanlage Am Meistersweg",
                category = PoiCategory.NATUR,
                lat = 51.4682,
                lon = 6.4630,
                subtitle = "SV Rheurdt 1945 e.V.",
                description = "Sportplatz, Vereinsheim und Sportanlagen für Fußball, Leichtathletik und Breitensport.",
                extraValue = "Sportstätte"
            ),
            RheurdtPoi(
                id = "feuerwehr_rheurdt",
                title = "Freiwillige Feuerwehr Rheurdt",
                category = PoiCategory.KULTUR,
                lat = 51.4635,
                lon = 6.4735,
                subtitle = "Bahnstraße (Löschzug Rheurdt)",
                description = "Moderne Wache des Löschzugs Rheurdt für Brand- und Katastrophenschutz in der Gemeinde.",
                extraValue = "Löschzug 1"
            ),
            RheurdtPoi(
                id = "bus_kirche",
                title = "Haltestelle Rheurdt Kirche",
                category = PoiCategory.HALTESTELLE,
                lat = 51.4655,
                lon = 6.4725,
                subtitle = "Kirchstraße / Rathausplatz",
                description = "Zentrale Bushaltestelle im Kernort. Verbindungen nach Krefeld, Kempen, Kamp-Lintfort und Neukirchen-Vluyn.",
                extraValue = "VRR / NIAG"
            ),
            RheurdtPoi(
                id = "bus_schaephuysen",
                title = "Haltestelle Schaephuysen Kirche",
                category = PoiCategory.HALTESTELLE,
                lat = 51.4530,
                lon = 6.5160,
                subtitle = "Hauptstraße (Ortsteil Schaephuysen)",
                description = "Zentraler Umsteigepunkt in Schaephuysen mit Verbindung Richtung Hüls / Krefeld und Moers.",
                extraValue = "VRR Linien 077, 079"
            ),
            RheurdtPoi(
                id = "pegel_duisburg",
                title = "Rheinpegel Duisburg-Ruhrort",
                category = PoiCategory.WASSER,
                lat = 51.4553,
                lon = 6.7279,
                subtitle = "Rheinkilometer 780,8 (südlicher Richtwert)",
                description = "Amtliche Wasserstandsmessung der Wasserstraßen- und Schifffahrtsverwaltung. Maßgeblicher Pegel für das südliche Kreisgebiet.",
                extraValue = "Stand: $pegelStr",
                isRegional = true
            ),
            RheurdtPoi(
                id = "pegel_wesel",
                title = "Rheinpegel Wesel",
                category = PoiCategory.WASSER,
                lat = 51.6461,
                lon = 6.6068,
                subtitle = "Rheinkilometer 814,0 (nördlicher Richtwert)",
                description = "Pegelstation der WSV oberhalb der Lippe-Mündung.",
                extraValue = "WSV Station",
                isRegional = true
            )
        )
    }
}
