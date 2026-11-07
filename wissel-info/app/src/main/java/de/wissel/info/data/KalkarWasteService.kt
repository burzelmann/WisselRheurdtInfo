package de.wissel.info.data

import java.time.LocalDate

object KalkarWasteService {

    /**
     * Ermittelt den offiziellen Abfuhrbezirk (1 oder 2) für eine Straße in der Stadt Kalkar.
     * Basiert auf dem amtlichen Straßenverzeichnis der Stadt Kalkar (Stand 2026).
     * Standard für wisseler Straßen oder unbekannte Straßen ist Bezirk 1.
     */
    fun getDistrictForStreet(street: String, houseNumber: String = ""): Int {
        val cleanStreet = street.trim()
        if (cleanStreet.isEmpty()) return 1

        // Sonderregeln mit Hausnummern laut amtlichem Straßenverzeichnis
        if (cleanStreet.contains("Römerstraße", ignoreCase = true)) {
            val num = houseNumber.filter { it.isDigit() }.toIntOrNull()
            return if (num == 1 || num == 5) 1 else 2
        }
        if (cleanStreet.contains("Xantener Straße", ignoreCase = true)) {
            val num = houseNumber.filter { it.isDigit() }.toIntOrNull()
            return if (num != null && num in 290..506) 1 else 2
        }

        // Direkte Zuordnung aus Straßenverzeichnis
        val direct = STREET_DISTRICT_MAP[cleanStreet]
        if (direct != null) return direct

        // Fuzzy match: case-insensitive
        val match = STREET_DISTRICT_MAP.entries.firstOrNull { 
            it.key.equals(cleanStreet, ignoreCase = true) 
        }
        if (match != null) return match.value

        // Standard für Wissel (Bezirk 1)
        return 1
    }

    /**
     * Liefert alle amtlichen Abfuhrtermine für das Jahr 2026 (inkl. 2027 Projektion)
     * bezirksgenau für die hinterlegte Straße.
     */
    fun getWasteSchedule(street: String, houseNumber: String = ""): List<WasteItem> {
        val district = getDistrictForStreet(street, houseNumber)
        val items = mutableListOf<WasteItem>()

        // 1. Restabfall (Graue Tonne, 14-tägig)
        if (district == 1) {
            // Bezirk 1: Montags (KW ungerade)
            val restDatesB1 = listOf(
                LocalDate.of(2026, 1, 12),
                LocalDate.of(2026, 1, 26),
                LocalDate.of(2026, 2, 9),
                LocalDate.of(2026, 2, 23),
                LocalDate.of(2026, 3, 9),
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 4, 7), // Di (verlegt von Ostermontag)
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 18),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 29),
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 7, 27),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 12, 14),
                LocalDate.of(2026, 12, 28),
                // Projektion 2027
                LocalDate.of(2027, 1, 11),
                LocalDate.of(2027, 1, 25),
                LocalDate.of(2027, 2, 8),
                LocalDate.of(2027, 2, 22)
            )
            restDatesB1.forEach { items.add(WasteItem(it, "Restabfall (Graue Tonne)", WasteType.REST)) }
        } else {
            // Bezirk 2: Dienstags
            val restDatesB2 = listOf(
                LocalDate.of(2026, 1, 13),
                LocalDate.of(2026, 1, 27),
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 24),
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 4, 8), // Mi (verlegt)
                LocalDate.of(2026, 4, 21),
                LocalDate.of(2026, 5, 5),
                LocalDate.of(2026, 5, 19),
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 16),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 14),
                LocalDate.of(2026, 7, 28),
                LocalDate.of(2026, 8, 11),
                LocalDate.of(2026, 8, 25),
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 22),
                LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 3),
                LocalDate.of(2026, 11, 17),
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 15),
                LocalDate.of(2026, 12, 29),
                // Projektion 2027
                LocalDate.of(2027, 1, 12),
                LocalDate.of(2027, 1, 26),
                LocalDate.of(2027, 2, 9),
                LocalDate.of(2027, 2, 23)
            )
            restDatesB2.forEach { items.add(WasteItem(it, "Restabfall (Graue Tonne)", WasteType.REST)) }
        }

        // 2. Bioabfall (Braune Tonne, 14-tägig, für Bezirk 1 + 2)
        val bioDates = listOf(
            LocalDate.of(2026, 1, 2), // Fr (verlegt)
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 11), // Mi (verlegt)
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 12),
            LocalDate.of(2026, 3, 26),
            LocalDate.of(2026, 4, 10), // Fr (verlegt)
            LocalDate.of(2026, 4, 23),
            LocalDate.of(2026, 5, 7),
            LocalDate.of(2026, 5, 21),
            LocalDate.of(2026, 6, 5), // Fr (verlegt)
            LocalDate.of(2026, 6, 18),
            LocalDate.of(2026, 7, 2),
            LocalDate.of(2026, 7, 16),
            LocalDate.of(2026, 7, 30),
            LocalDate.of(2026, 8, 13),
            LocalDate.of(2026, 8, 27),
            LocalDate.of(2026, 9, 10),
            LocalDate.of(2026, 9, 24),
            LocalDate.of(2026, 10, 8),
            LocalDate.of(2026, 10, 22),
            LocalDate.of(2026, 11, 5),
            LocalDate.of(2026, 11, 19),
            LocalDate.of(2026, 12, 3),
            LocalDate.of(2026, 12, 17),
            LocalDate.of(2026, 12, 31),
            // Projektion 2027
            LocalDate.of(2027, 1, 14),
            LocalDate.of(2027, 1, 28),
            LocalDate.of(2027, 2, 11),
            LocalDate.of(2027, 2, 25)
        )
        bioDates.forEach { items.add(WasteItem(it, "Bioabfall (Braune Tonne)", WasteType.BIO)) }

        // 3. Altpapier (Grüne/Blaue Tonne, 4-wöchentlich, Bezirk 1 + 2)
        val paperDates = listOf(
            LocalDate.of(2026, 1, 26),
            LocalDate.of(2026, 2, 23),
            LocalDate.of(2026, 3, 23),
            LocalDate.of(2026, 4, 20),
            LocalDate.of(2026, 5, 18),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 7, 13),
            LocalDate.of(2026, 8, 10),
            LocalDate.of(2026, 9, 7),
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 12, 28),
            // Projektion 2027
            LocalDate.of(2027, 1, 25),
            LocalDate.of(2027, 2, 22)
        )
        paperDates.forEach { items.add(WasteItem(it, "Altpapier (Grüne Tonne)", WasteType.PAPIER)) }

        // 4. Verkaufsverpackungen (Gelbe Tonne / Gelber Sack, 4-wöchentlich, Bezirk 1 + 2)
        val yellowDates = listOf(
            LocalDate.of(2026, 1, 12),
            LocalDate.of(2026, 2, 9),
            LocalDate.of(2026, 3, 9),
            LocalDate.of(2026, 4, 7), // Di (verlegt)
            LocalDate.of(2026, 5, 4),
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 29),
            LocalDate.of(2026, 7, 27),
            LocalDate.of(2026, 8, 24),
            LocalDate.of(2026, 9, 21),
            LocalDate.of(2026, 10, 19),
            LocalDate.of(2026, 11, 16),
            LocalDate.of(2026, 12, 14),
            // Projektion 2027
            LocalDate.of(2027, 1, 11),
            LocalDate.of(2027, 2, 8)
        )
        yellowDates.forEach { items.add(WasteItem(it, "Gelbe Tonne (Wertstoffe)", WasteType.GELB)) }

        // 5. Glasabfall (Glaskörbe / 240l Tonnen, alle 8 Wochen)
        if (district == 1) {
            val glassDatesB1 = listOf(
                LocalDate.of(2026, 2, 19),
                LocalDate.of(2026, 4, 16),
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 26),
                // Projektion 2027
                LocalDate.of(2027, 1, 21)
            )
            glassDatesB1.forEach { items.add(WasteItem(it, "Glasabfall (Glaskörbe)", WasteType.GLAS)) }
        } else {
            val glassDatesB2 = listOf(
                LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 4, 17),
                LocalDate.of(2026, 6, 12),
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 11, 27),
                // Projektion 2027
                LocalDate.of(2027, 1, 22)
            )
            glassDatesB2.forEach { items.add(WasteItem(it, "Glasabfall (Glaskörbe)", WasteType.GLAS)) }
        }

        // 6. Grünschnitt (nach Anmeldung)
        val greenDates = listOf(
            LocalDate.of(2026, 3, 2),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 10, 9),
            LocalDate.of(2026, 11, 6)
        )
        greenDates.forEach { items.add(WasteItem(it, "Grünschnitt (nach Anmeldung)", WasteType.GRUEN)) }

        // 7. Schadstoffmobil
        val hazardDates = listOf(
            WasteItem(LocalDate.of(2026, 1, 16), "Schadstoffmobil (Kalkar 15:00-18:00)", WasteType.SCHADSTOFFE),
            WasteItem(LocalDate.of(2026, 3, 13), "Schadstoffmobil Wissel (Marktplatz 15:00-18:00)", WasteType.SCHADSTOFFE),
            WasteItem(LocalDate.of(2026, 5, 8), "Schadstoffmobil (Kalkar 15:00-18:00)", WasteType.SCHADSTOFFE),
            WasteItem(LocalDate.of(2026, 7, 10), "Schadstoffmobil (Kalkar 15:00-18:00)", WasteType.SCHADSTOFFE),
            WasteItem(LocalDate.of(2026, 9, 25), "Schadstoffmobil (Kalkar 15:00-18:00)", WasteType.SCHADSTOFFE),
            WasteItem(LocalDate.of(2026, 11, 27), "Schadstoffmobil (Kalkar 15:00-18:00)", WasteType.SCHADSTOFFE)
        )
        items.addAll(hazardDates)

        return items.sortedBy { it.date }
    }

    /**
     * Vollständiges amtliches Straßenverzeichnis der Stadt Kalkar mit Abfuhrbezirkeinteilung.
     */
    val STREET_DISTRICT_MAP: Map<String, Int> = mapOf(
        "Ackerstraße" to 1,
        "Ahornweg" to 2,
        "Alte Molkerei" to 1,
        "Alte Schmiede" to 1,
        "Alter Schulweg" to 1,
        "Altkalkarer Straße" to 2,
        "Am Anger" to 1,
        "Am Bahndamm" to 2,
        "Am Bahnhof" to 2,
        "Am Bolk" to 1,
        "Am Bollwerk" to 2,
        "Am Ehrenmal" to 1,
        "Am Golfplatz" to 1,
        "Am Hanselaer Tor" to 2,
        "Am Haus Veen" to 1,
        "Am Pappelwäldchen" to 1,
        "Am Patersdeich" to 2,
        "Am Rietegatt" to 2,
        "Am See" to 1,
        "Am Sportplatz" to 1,
        "Am Stadtpark" to 2,
        "Am Steg" to 1,
        "Am Tannenbusch" to 1,
        "Am Weiher" to 2,
        "Amselweg" to 1,
        "An de alde Scholl" to 2,
        "An der Gracht" to 1,
        "An der Kehre" to 1,
        "An der Steinmühle" to 2,
        "An der Woy" to 1,
        "Anemonenweg" to 1,
        "Anton-Heuken-Straße" to 1,
        "Appeldorner Straße" to 1,
        "Arnimstraße" to 2,
        "Auenweg" to 1,
        "Auf dem Großen Damm" to 2,
        "Bahnhofstraße" to 2,
        "Bahnweg" to 1,
        "Baukamp" to 2,
        "Beginenweg" to 2,
        "Begonienweg" to 1,
        "Behrnenweg" to 2,
        "Bennepstraße" to 1,
        "Berglandstraße" to 1,
        "Bergstraße" to 1,
        "Bienemannsweg" to 1,
        "Birgelfeld" to 1,
        "Birkenallee" to 2,
        "Bleichenstege" to 2,
        "Bockskamp" to 1,
        "Bollenkamp" to 1,
        "Bollwerkstege" to 2,
        "Bossendell" to 1,
        "Bovenholt" to 2,
        "Brentanostraße" to 2,
        "Bruchweg" to 1,
        "Brüggersweg" to 1,
        "Buchenweg" to 2,
        "Burggarten" to 2,
        "Burginatium H-Nrn. 3, 6" to 2,
        "Burginatium H-Nrn. 8 - 38" to 1,
        "Buschweg" to 1,
        "Chamissostraße" to 2,
        "Cäcilienhof" to 2,
        "Dahlienweg" to 1,
        "Dammweg" to 2,
        "Daveracker" to 1,
        "Dechant-Beckmann-Straße" to 2,
        "Deichweg" to 2,
        "Dergeltweg" to 1,
        "Dominikaner Bongert" to 2,
        "Dorfstraße" to 1,
        "Douvermannstege" to 2,
        "Dr.-Hugo-Mönnig-Straße" to 2,
        "Dr.-Karl-Bartels-Weg" to 2,
        "Driwtstraße" to 1,
        "Drißkamp" to 2,
        "Drosselweg" to 1,
        "Durchlaß" to 1,
        "Düffelsmühle" to 1,
        "Dünenweg" to 1,
        "Düstern-Bongert" to 1,
        "Eichendorffstraße" to 2,
        "Eichenweg" to 2,
        "Eligiusstraße" to 2,
        "Elsemannsweg" to 1,
        "Emmericher Straße" to 1,
        "Ensefeld" to 1,
        "Erlenstraße" to 2,
        "Eschenweg" to 2,
        "Eselsweg" to 1,
        "Ewald-Scholten-Straße" to 1,
        "Eyland" to 1,
        "Fackelkampsweg" to 1,
        "Fasanenweg" to 1,
        "Fatimaweg" to 1,
        "Feldhuysenweg" to 2,
        "Feldweg" to 1,
        "Fichtenweg" to 2,
        "Fingerhutshof" to 1,
        "Fischerwall" to 1,
        "Florenweg" to 1,
        "Freyendahl" to 2,
        "Friedrich-Ebert-Straße" to 1,
        "Fuhrenweg" to 1,
        "Galgensteg" to 1,
        "Gartenstraße" to 1,
        "Gasthausstege" to 2,
        "Gehmsweg" to 1,
        "Geranienstraße" to 1,
        "Gerd-Jansen-Platz" to 2,
        "Giltjesweg" to 1,
        "Ginsterweg" to 2,
        "Gocher Feld" to 2,
        "Gocher Straße H-Nrn. 151" to 1,
        "Gocher Straße H-Nrn. 3 - 145" to 2,
        "Goetendyk" to 1,
        "Goethestraße" to 2,
        "Grabenstraße" to 2,
        "Grafenhof" to 2,
        "Greilack" to 1,
        "Grenzacker" to 1,
        "Grenzstraße" to 1,
        "Grenzweg" to 2,
        "Griether Feld" to 1,
        "Griether Markt" to 1,
        "Griether Straße" to 1,
        "Grimmstraße" to 2,
        "Grüner Weg" to 1,
        "Gänseweide" to 1,
        "Görtze-Woy" to 1,
        "Hagedorn" to 2,
        "Hammelweg" to 2,
        "Hanselaerstraße" to 2,
        "Hansestraße" to 1,
        "Hasenkamp" to 2,
        "Hasenweg" to 1,
        "Heideweg" to 1,
        "Heienberg" to 1,
        "Heiligenberg" to 1,
        "Heinrich-Eger-Straße" to 1,
        "Heinrich-Heine-Straße" to 2,
        "Heinrich-Terhorst-Weg" to 2,
        "Heinz-Seesing-Straße" to 2,
        "Helenenweg" to 1,
        "Hellendornstraße" to 1,
        "Herderstraße" to 2,
        "Heseler Weg" to 2,
        "Himmelacker" to 1,
        "Hinter dem Markt" to 2,
        "Hochend" to 1,
        "Hochstraße" to 1,
        "Hohe Straße" to 2,
        "Holtmoelen" to 2,
        "Horster Weg" to 2,
        "Hortmannsweg" to 1,
        "Husenweg" to 1,
        "Hölderlinstraße" to 2,
        "Im Dahl" to 2,
        "Im Mühlenfeld" to 1,
        "Im Schwanenhorst" to 2,
        "Immermannstraße" to 2,
        "In den Vennen" to 1,
        "In der Aue" to 2,
        "Industriepark" to 1,
        "Inselring" to 1,
        "Jan-Joest-Straße" to 2,
        "Josef-Rottmann-Weg" to 2,
        "Jägerweg" to 1,
        "Kalflakstraße" to 1,
        "Kallo-van-Holt-Straße" to 1,
        "Kaltenberg" to 1,
        "Kanienendyk" to 1,
        "Karl-Leisner-Platz" to 2,
        "Kasernenweg" to 2,
        "Kastellstraße" to 2,
        "Katernstraße" to 1,
        "Kattenberg" to 1,
        "Kemkesweg" to 1,
        "Kemnadestraße" to 1,
        "Kerkend" to 1,
        "Kerkpad" to 1,
        "Kerßeweg" to 1,
        "Kesselstraße" to 2,
        "Kiefernweg" to 2,
        "Kilewardsweg" to 1,
        "Kirchdamm" to 1,
        "Kirchenacker" to 1,
        "Kirchfeld" to 1,
        "Kirchhofstraße" to 1,
        "Kirchplatz" to 2,
        "Kirchstraße" to 2,
        "Kiwittweg" to 1,
        "Kleiststraße" to 2,
        "Klever Straße" to 2,
        "Klompenstraße" to 1,
        "Klosterstege" to 2,
        "Konrad-Adenauer-Straße" to 1,
        "Kreuzstraße" to 1,
        "Kuhweg" to 1,
        "Kurfürstendamm" to 2,
        "Köstersdick" to 1,
        "Kückstege" to 2,
        "Küppstege" to 1,
        "Laubenweg" to 2,
        "Leegtal" to 1,
        "Legestraße" to 1,
        "Lenaustraße" to 2,
        "Leo-Klever-Straße" to 1,
        "Lessingstraße" to 2,
        "Leuthweg" to 1,
        "Leygräfte" to 2,
        "Leyweg" to 1,
        "Limmerstraße" to 1,
        "Lincolnstraße" to 2,
        "Lindenweg" to 2,
        "Loefsche Straße" to 1,
        "Lärchenstraße" to 2,
        "Marienbaumer Straße" to 1,
        "Marienblum" to 2,
        "Markt" to 2,
        "Meerweg" to 1,
        "Metzgerweg" to 1,
        "Michelsdick" to 1,
        "Mintenweg" to 1,
        "Mittelsandweg" to 1,
        "Molkereistraße" to 1,
        "Monreberg" to 2,
        "Monrestraße" to 2,
        "Möllepöttje" to 2,
        "Mörikestraße" to 2,
        "Mössekiep" to 2,
        "Mühlenberg" to 1,
        "Mühlenstege" to 2,
        "Mühlenstraße" to 1,
        "Mühlenweg" to 1,
        "Narzissenstraße" to 1,
        "Nauenstege" to 2,
        "Nejwittweg" to 1,
        "Nelkenstraße" to 1,
        "Neue Straße" to 1,
        "Neuenhof" to 1,
        "Neulouisendorfer Straße" to 1,
        "Nätelward" to 1,
        "Obermörmterer Straße" to 1,
        "Ochsenwiesen" to 1,
        "Op de Wacht" to 2,
        "Osterwyk" to 1,
        "Oyweg H-Nrn. 6 - 46" to 2,
        "Oyweg H-Nrn. 93 - 344" to 1,
        "Pastor-Sieverding-Straße" to 1,
        "Pastor-Smits-Weg" to 1,
        "Postweg" to 2,
        "Prof.Schmidt-Straße" to 2,
        "Prostewardsweg" to 1,
        "Rabenhorst" to 1,
        "Reeser Straße" to 1,
        "Reeserschanz" to 1,
        "Reiherstraße" to 1,
        "Rheinstraße" to 1,
        "Rheintorstraße" to 1,
        "Rheinuferstraße" to 1,
        "Richard-Birckman-Weg" to 2,
        "Risterweg" to 2,
        "Ritter-Elbert-Straße" to 1,
        "Rosenstraße" to 1,
        "Rotes Häuschen" to 1,
        "Roßkamp" to 2,
        "Rutger-Krop-Weg" to 2,
        "Römerstraße 1, 5" to 1,
        "Römerstraße Rest" to 2,
        "Sandkuhl" to 1,
        "Sandweg" to 1,
        "Schafweg" to 2,
        "Scheppenacker" to 1,
        "Schifferdamm" to 1,
        "Schillerstraße" to 2,
        "Schlenkstraße" to 1,
        "Schlingstraße" to 1,
        "Schloßstraße" to 1,
        "Schlüskesgraben" to 2,
        "Scholtenhof" to 1,
        "Scholtenweg" to 1,
        "Schuldamm" to 1,
        "Schusterweg" to 1,
        "Schwalbenweg" to 1,
        "Schwester-Walburga-Straße" to 1,
        "Schwäwelsweg" to 1,
        "Schüttschott" to 1,
        "Schützenweg" to 1,
        "Seeweg" to 1,
        "Servitenstege" to 2,
        "Seydlitzstege" to 2,
        "Sießstraße" to 1,
        "Sommerdyck" to 2,
        "Sonnenstraße" to 1,
        "Spickstraße" to 1,
        "Spiegelstege" to 2,
        "Spierheide" to 1,
        "Spillenweg" to 1,
        "St. Hubertus Weg" to 1,
        "St. Lambertus-Straße" to 1,
        "Stadtwall" to 1,
        "Starenweg" to 1,
        "Steckkuhl" to 1,
        "Steenowense Weg" to 1,
        "Stefan-Paeßens-Straße" to 2,
        "Steinacker" to 1,
        "Steinbruch" to 1,
        "Steinchensweg" to 1,
        "Steinweg" to 1,
        "Sternenweg" to 1,
        "Stiller Winkel" to 1,
        "Stockshof" to 1,
        "Stormstraße" to 2,
        "Swartkopweg" to 1,
        "Tabaksweg" to 1,
        "Talacker" to 1,
        "Talstraße" to 2,
        "Taubensterz" to 1,
        "Taubenweg" to 1,
        "Terwelpweg" to 1,
        "Theodor-Franken-Straße" to 2,
        "Theodor-Heuss-Straße" to 1,
        "Theodor-Kuypers-Straße" to 2,
        "Tiller Straße" to 2,
        "Totenhügel" to 1,
        "Trebbelin" to 2,
        "Treidelweg" to 1,
        "Trompetweg" to 2,
        "Tulpenweg" to 1,
        "Uedemer Straße" to 1,
        "Uferallee" to 1,
        "Uhlandstraße" to 2,
        "Van-Dornick-Straße" to 2,
        "Van-Remmen-Weg" to 1,
        "Veenweg" to 1,
        "Viehstege" to 2,
        "Von-Lauff-Weg" to 2,
        "Vossegattweg" to 2,
        "Vossekuhl" to 1,
        "Vynener Straße" to 1,
        "Wallstraße" to 2,
        "Washingtonstraße" to 2,
        "Waysche Straße" to 1,
        "Weidenweg" to 1,
        "Wesselsbruch" to 1,
        "Wielandstraße" to 2,
        "Wildhagen" to 1,
        "Wöhrmannstraße" to 1,
        "Wüldersweg" to 1,
        "Wüllschlag" to 1,
        "Xantener Straße 290 – 506" to 1,
        "Xantener Straße 3 – 195" to 2,
        "Zum Wisseler See" to 1,
        "Zur ehemaligen Gärtnerei" to 2,
        "Ärmen Düwel" to 1
    )
}
