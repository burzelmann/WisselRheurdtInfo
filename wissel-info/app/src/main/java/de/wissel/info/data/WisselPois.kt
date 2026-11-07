package de.wissel.info.data

enum class PoiCategory(val label: String, val icon: String, val colorHex: Long) {
    WASSER("Pegel & See", "🌊", 0xFF0288D1),
    HALTESTELLE("Haltestellen", "🚏", 0xFF2E7D32),
    NATUR("Natur & Freizeit", "🌳", 0xFFE65100),
    KULTUR("Ort & Kultur", "🏛️", 0xFF6A1B9A)
}

data class WisselPoi(
    val id: String,
    val title: String,
    val category: PoiCategory,
    val lat: Double,
    val lon: Double,
    val subtitle: String,
    val description: String,
    val extraValue: String? = null,
    val isRegional: Boolean = false // true for Rees/Emmerich outside Wissel
)

object WisselPois {
    fun getPois(currentPegelCm: Double? = null): List<WisselPoi> {
        val pegelStr = if (currentPegelCm != null) "$currentPegelCm cm" else "27 cm"
        return listOf(
            // --- Exakte Wisseler Dorf-POIs (OSM vermessen) ---
            WisselPoi(
                id = "kirche_clemens",
                title = "St. Clemens Basilika",
                category = PoiCategory.KULTUR,
                lat = 51.771487,
                lon = 6.286969,
                subtitle = "Köstersdick 40 / Kirchplatz",
                description = "Bedeutende romanische Pfeilerbasilika aus Tuffstein (12. Jahrhundert). Das historische Zentrum und Wahrzeichen von Wissel.",
                extraValue = "Denkmalschutz"
            ),
            WisselPoi(
                id = "tabakmuseum",
                title = "Stifts- & Tabakmuseum",
                category = PoiCategory.KULTUR,
                lat = 51.773594,
                lon = 6.289382,
                subtitle = "Dorfstraße 31",
                description = "Dokumentiert die traditionsreiche Tabakverarbeitung im Dünendorf Wissel sowie die Geschichte des ehemaligen Stifts.",
                extraValue = "Heimatmuseum"
            ),
            WisselPoi(
                id = "dorfplatz",
                title = "Dorfplatz Wissel",
                category = PoiCategory.KULTUR,
                lat = 51.773414,
                lon = 6.289570,
                subtitle = "Dorfstraße / Ortsmitte",
                description = "Zentraler Platz für Dorffeste, Kirmes und Vereinsleben der St. Johannes Schützenbruderschaft.",
                extraValue = "Dorfmitte"
            ),
            WisselPoi(
                id = "wisseler_duenen",
                title = "Wisseler Dünen",
                category = PoiCategory.NATUR,
                lat = 51.768345,
                lon = 6.304890,
                subtitle = "Naturschutzgebiet am Kaltenberg / Dünenweg",
                description = "Binnendünenkomplex mit seltenen Sandmagerrasen und Silbergrasfluren östlich des Dorfkerns. Einzigartiges Biotop am Niederrhein.",
                extraValue = "Natura 2000"
            ),
            WisselPoi(
                id = "camping_wisseler_see",
                title = "Freizeitpark Wisseler See",
                category = PoiCategory.NATUR,
                lat = 51.760912,
                lon = 6.285302,
                subtitle = "Zum Wisseler See 15 (Haupteingang)",
                description = "Campingpark, Ferienhäuser und Freizeitanlage am Wisseler See.",
                extraValue = "Campingpark"
            ),
            WisselPoi(
                id = "strand_wisseler_see",
                title = "Naturfreibad Wisseler See",
                category = PoiCategory.WASSER,
                lat = 51.762933,
                lon = 6.291579,
                subtitle = "Sandstrand & Badegewässer",
                description = "Freibad am Wisseler See mit Sandstrand, Liegewiese und Wassersport. Regelmäßige Güteüberwachung durch das LANUK NRW.",
                extraValue = "Badewasser: Gut"
            ),

            // --- Exakte Haltestellen (OSM vermessen) ---
            WisselPoi(
                id = "bus_schule",
                title = "Haltestelle Grundschule",
                category = PoiCategory.HALTESTELLE,
                lat = 51.773815,
                lon = 6.290409,
                subtitle = "Dorfstraße / Grundschule",
                description = "Haltestelle der Buslinie 44 (Kalkar Markt / Goch bzw. Rees / Emmerich) und Bürgerbus Kalkar.",
                extraValue = "VRR Linie 44"
            ),
            WisselPoi(
                id = "bus_molkerei",
                title = "Haltestelle Alte Molkerei",
                category = PoiCategory.HALTESTELLE,
                lat = 51.766364,
                lon = 6.285762,
                subtitle = "Alte Molkerei (Südort)",
                description = "Haltestelle am südlichen Ende von Wissel auf dem Weg zum Wisseler See.",
                extraValue = "VRR Linie 44"
            ),
            WisselPoi(
                id = "bus_ortseingang",
                title = "Haltestelle Wissel Ortseingang",
                category = PoiCategory.HALTESTELLE,
                lat = 51.769709,
                lon = 6.287580,
                subtitle = "Emmericher Straße (Nordwest)",
                description = "Ortseingang Haltestelle aus Richtung Grieth / Emmerich.",
                extraValue = "VRR Linie 44"
            ),

            // --- Überregionale Rhein-Messstationen (außerhalb Wissel) ---
            WisselPoi(
                id = "pegel_rees",
                title = "Rheinpegel Rees",
                category = PoiCategory.WASSER,
                lat = 51.756953,
                lon = 6.395692,
                subtitle = "Rheinkilometer 837,4 (ca. 8 km östlich von Wissel)",
                description = "Amtliche Wasserstandsmessung der WSV am Rheinufer in Rees. Maßgebender Richtwert für Grundwasserstände und Deichsicherheit der Wisseler Polder.",
                extraValue = "Stand: $pegelStr",
                isRegional = true
            ),
            WisselPoi(
                id = "pegel_emmerich",
                title = "Rheinpegel Emmerich",
                category = PoiCategory.WASSER,
                lat = 51.829252,
                lon = 6.245594,
                subtitle = "Rheinkilometer 851,9 (ca. 9 km nördlich von Wissel)",
                description = "Unterer Niederrhein-Pegel vor der Grenze zu den Niederlanden.",
                extraValue = "WSV Station",
                isRegional = true
            )
        )
    }
}
