package de.wissel.info.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wissel_user_prefs", Context.MODE_PRIVATE)

    var street: String
        get() = prefs.getString("selected_street", "Zum Wisseler See") ?: "Zum Wisseler See"
        set(value) { prefs.edit().putString("selected_street", value).commit() }

    var houseNumber: String
        get() = prefs.getString("selected_house_number", "10") ?: "10"
        set(value) { prefs.edit().putString("selected_house_number", value).commit() }

    var wasteNotificationEnabled: Boolean
        get() = prefs.getBoolean("waste_notification_enabled", true)
        set(value) { prefs.edit().putBoolean("waste_notification_enabled", value).commit() }

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
            val saved = prefs.getString("dashboard_card_order", null)
            if (!saved.isNullOrEmpty()) {
                val list = saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (list.isNotEmpty()) return list
            }
            return listOf("weather", "pegel", "energy", "waste", "bus", "events", "politics", "holidays")
        }
        set(value) { prefs.edit().putString("dashboard_card_order", value.joinToString(",")).commit() }

    var disabledDashboardCards: Set<String>
        get() {
            val str = prefs.getString("disabled_dashboard_cards_str", null)
            if (str != null) {
                if (str.isEmpty()) return emptySet()
                return str.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            }
            return prefs.getStringSet("disabled_dashboard_cards", emptySet()) ?: emptySet()
        }
        set(value) {
            val joined = value.joinToString(",")
            prefs.edit()
                .putString("disabled_dashboard_cards_str", joined)
                .putStringSet("disabled_dashboard_cards", HashSet(value))
                .commit()
        }

    var pvCapacityKwp: Double
        get() = prefs.getFloat("pv_capacity_kwp", 10.0f).toDouble()
        set(value) { prefs.edit().putFloat("pv_capacity_kwp", value.toFloat()).commit() }

    companion object {
        val WISSEL_KNOWN_STREETS = setOf(
            "Ackerstraße",
            "Alter Schulweg",
            "Am Bolk",
            "Am Pappelwäldchen",
            "Am Rietegatt",
            "Am See",
            "Am Tannenbusch",
            "Amselweg",
            "Anton-Heuken-Straße",
            "Berglandstraße",
            "Bienemannsweg",
            "Dorfstraße",
            "Drosselweg",
            "Dünenweg",
            "Emmericher Straße",
            "Fackelkampsweg",
            "Fingerhutshof",
            "Friedrich-Ebert-Straße",
            "Giltjesweg",
            "Hagelkreuzstraße",
            "Heienberg",
            "Helenenweg",
            "Hellendornstraße",
            "Hortmannsweg",
            "Kaltenberg",
            "Kemnadestraße",
            "Kirchdamm",
            "Kirchenacker",
            "Kirchhofstraße",
            "Kirchplatz",
            "Kirchstraße",
            "Köstersdick",
            "Michelsdick",
            "Molkereistraße",
            "Nejwittweg",
            "Pastor-Smits-Weg",
            "Prostewardsweg",
            "Rabenhorst",
            "Scholtenweg",
            "Schützenweg",
            "Seeweg",
            "Swartkopweg",
            "Terwelpweg",
            "Weidenweg",
            "Zum Wisseler See",
            "Ärmen Düwel",
        )

        val ALL_STREETS = listOf(
            "Ackerstraße",
            "Ahornweg",
            "Alte Molkerei",
            "Alte Schmiede",
            "Alter Schulweg",
            "Altkalkarer Straße",
            "Am Anger",
            "Am Bahndamm",
            "Am Bolk",
            "Am Bollwerk",
            "Am Ehrenmal",
            "Am Golfplatz",
            "Am Hanselaer Tor",
            "Am Haus Veen",
            "Am Pappelwäldchen",
            "Am Patersdeich",
            "Am Rietegatt",
            "Am See",
            "Am Sportplatz",
            "Am Stadtpark",
            "Am Steg",
            "Am Tannenbusch",
            "Am Weiher",
            "Amselweg",
            "An der Gracht",
            "An der Kehre",
            "An der Steinmühle",
            "An der Woy",
            "Anemonenweg",
            "Anton-Heuken-Straße",
            "Appeldorner Straße",
            "Arnimstraße",
            "Auenweg",
            "Auf dem Großen Damm",
            "Bahnhofstraße",
            "Bahnweg",
            "Baukamp",
            "Beginenweg",
            "Begonienweg",
            "Behrnenweg",
            "Bennepstraße",
            "Berglandstraße",
            "Bergstraße",
            "Bienemannsweg",
            "Birgelfeld",
            "Birkenallee",
            "Bockskamp",
            "Bollenkamp",
            "Bollwerkstege",
            "Bossendell",
            "Bovenholt",
            "Brentanostraße",
            "Bruchweg",
            "Brüggersweg",
            "Buchenweg",
            "Burggarten",
            "Burginatium",
            "Buschweg",
            "Chamissostraße",
            "Cäcilienhof",
            "Dahlienweg",
            "Dammweg",
            "Daveracker",
            "Dechant-Beckmann-Straße",
            "Deichweg",
            "Dellbrückstraße",
            "Dergeltweg",
            "Dominikaner-Bongert",
            "Dorfstraße",
            "Douvermannstege",
            "Douvermanstege",
            "Dr.-Hugo-Mönnig-Straße",
            "Dr.-Karl-Bartels-Weg",
            "Driwtstraße",
            "Drißkamp",
            "Drosselweg",
            "Durchlaß",
            "Düffelsmühle",
            "Dünenweg",
            "Düstern-Bongert",
            "Eichendorffstraße",
            "Eichenweg",
            "Eligiusstraße",
            "Elsemannsweg",
            "Emmericher Straße",
            "Ensefeld",
            "Erlenstraße",
            "Eschenweg",
            "Eselsweg",
            "Ewald-Scholten-Straße",
            "Eyland",
            "Fackelkampsweg",
            "Fasanenweg",
            "Fatimaweg",
            "Feldhuysenweg",
            "Feldweg",
            "Fichtenweg",
            "Fingerhutshof",
            "Fischerwall",
            "Florenweg",
            "Freyendahl",
            "Friedrich-Ebert-Straße",
            "Fuhrenweg",
            "Galgensteg",
            "Gartenstraße",
            "Gasthausstege",
            "Gehmsweg",
            "Geranienstraße",
            "Gerd-Jansen-Platz",
            "Giltjesweg",
            "Ginsterweg",
            "Gocher Feld",
            "Gocher Straße",
            "Goetendyk",
            "Goethestraße",
            "Grabenstraße",
            "Grafenhof",
            "Greilack",
            "Grenzacker",
            "Grenzstraße",
            "Grenzweg",
            "Griether Feld",
            "Griether Markt",
            "Griether Straße",
            "Grimmstraße",
            "Grüner Weg",
            "Gänseweide",
            "Görtze Woy",
            "Hagedorn",
            "Hagelkreuzstraße",
            "Hammelweg",
            "Hanselaerstraße",
            "Hansestraße",
            "Hasenkamp",
            "Hasenweg",
            "Heideweg",
            "Heienberg",
            "Heiligenberg",
            "Heinrich-Eger-Straße",
            "Heinrich-Heine-Straße",
            "Heinrich-Terhorst-Weg",
            "Heinz-Seesing-Straße",
            "Helenenweg",
            "Helenweg",
            "Hellendornstraße",
            "Herderstraße",
            "Heseler Weg",
            "Himmelacker",
            "Hinter dem Markt",
            "Hochend",
            "Hochstraße",
            "Hohe Straße",
            "Holtmoelen",
            "Horster Weg",
            "Hortmannsweg",
            "Husenweg",
            "Hölderlinstraße",
            "Im Dahl",
            "Im Mühlenfeld",
            "Im Schwanenhorst",
            "Immermannstraße",
            "In den Vennen",
            "In der Aue",
            "Industriepark",
            "Inselring",
            "Jan-Joest-Straße",
            "Josef-Rottmann-Weg",
            "Jägerweg",
            "Kalflakstraße",
            "Kallo-van-Holt-Straße",
            "Kaltenberg",
            "Kanienendyk",
            "Karl-Leisner-Platz",
            "Kasernenweg",
            "Kastellstraße",
            "Katernstraße",
            "Kattenberg",
            "Kemkesweg",
            "Kemnadestraße",
            "Kempkesweg",
            "Kerkend",
            "Kerkpad",
            "Kerßeweg",
            "Kesselstraße",
            "Kiefernweg",
            "Kilewardsweg",
            "Kirchdamm",
            "Kirchenacker",
            "Kirchfeld",
            "Kirchhofstraße",
            "Kirchplatz",
            "Kirchstraße",
            "Kiwittweg",
            "Kleiststraße",
            "Klever Straße",
            "Klompenstraße",
            "Klosterstege",
            "Konrad-Adenauer-Straße",
            "Kreuzstraße",
            "Kuhweg",
            "Kurfürstendamm",
            "Köstersdick",
            "Kückstege",
            "Laubenweg",
            "Leegtal",
            "Legestraße",
            "Lenaustraße",
            "Leo-Klever-Straße",
            "Lessingstraße",
            "Leuthweg",
            "Leygräfte",
            "Leyweg",
            "Limmerstraße",
            "Lincolnstraße",
            "Lindenweg",
            "Loefsche Straße",
            "Lärchenstraße",
            "Marienbaumer Straße",
            "Marienblum",
            "Markt",
            "Meerweg",
            "Metzgerweg",
            "Michelsdick",
            "Mintenweg",
            "Mittelsandweg",
            "Molkereistraße",
            "Monreberg",
            "Monrestraße",
            "Möllepöttje",
            "Mörikestraße",
            "Mössekiep",
            "Mühlenberg",
            "Mühlenstege",
            "Mühlenstraße",
            "Mühlenweg",
            "Narzissenstraße",
            "Nauenstege",
            "Nejwittweg",
            "Nelkenstraße",
            "Neue Straße",
            "Neuenhof",
            "Neulouisendorfer Straße",
            "Nätelward",
            "Obermörmterer Straße",
            "Op de Wacht",
            "Osterwyck",
            "Oyweg",
            "Pastor-Sieverding-Straße",
            "Pastor-Smits-Weg",
            "Postweg",
            "Prof. Schmidt Straße",
            "Prof.-Schmidt-Straße",
            "Prostewardsweg",
            "Rabenhorst",
            "Reeser Straße",
            "Reeserschanz",
            "Reiherstraße",
            "Rheinstraße",
            "Rheintorstraße",
            "Rheinuferstraße",
            "Richard-Birkmann-Weg",
            "Risterweg",
            "Ritter-Elbert-Straße",
            "Rosenstraße",
            "Rotes Häuschen",
            "Roßkamp",
            "Römerstraße",
            "Sandkuhl",
            "Sandweg",
            "Schafweg",
            "Scheppenacker",
            "Schifferdamm",
            "Schillerstraße",
            "Schlenkstraße",
            "Schlingstraße",
            "Schloßstraße",
            "Schlüskesgraben",
            "Scholtenhof",
            "Scholtenweg",
            "Schuldamm",
            "Schusterweg",
            "Schwalbenweg",
            "Schwester-Walburga-Straße",
            "Schwäwelsweg",
            "Schüttschott",
            "Schützenweg",
            "Seeweg",
            "Servitenstege",
            "Seydlitzstege",
            "Sießstraße",
            "Sommerdyck",
            "Sonnenstraße",
            "Spickstraße",
            "Spiegelstege",
            "Spierheide",
            "Spillenweg",
            "St.-Hubertus-Weg",
            "St.-Lambertus-Straße",
            "Stadtwall",
            "Starenweg",
            "Steckkuhl",
            "Stefan-Paeßens-Straße",
            "Steinacker",
            "Steinbruch",
            "Steinchensweg",
            "Steinweg",
            "Sternenweg",
            "Stiller Winkel",
            "Stockshof",
            "Stormstraße",
            "Swartkopweg",
            "Tabaksweg",
            "Talacker",
            "Talstraße",
            "Taubensterz",
            "Taubenweg",
            "Terwelpweg",
            "Theodor-Franken-Straße",
            "Theodor-Heuss-Straße",
            "Theodor-Kuypers-Straße",
            "Tiller Straße",
            "Totenhügel",
            "Trebbelin",
            "Treidelweg",
            "Trompetweg",
            "Tulpenweg",
            "Uedemer Straße",
            "Uferallee",
            "Uhlandstraße",
            "Van-Remmen-Weg",
            "Veenweg",
            "Viehstege",
            "Von-Lauff-Weg",
            "Vossegattweg",
            "Vossekuhl",
            "Vynener Straße",
            "Wallstraße",
            "Washingtonstraße",
            "Waysche Straße",
            "Weidenweg",
            "Wielandstraße",
            "Wildhagen",
            "Wöhrmannstraße",
            "Wüldersweg",
            "Wüllschlag",
            "Xantener Straße",
            "Zum Wisseler See",
            "Ärmen Düwel",
        )
    }
}
