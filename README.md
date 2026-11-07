# Wissel Info & Rheurdt Info (v2.2.0)

Maßgeschneiderte, datenschutzfreundliche Android-Bürger-Apps für **Wissel am Niederrhein** (Stadt Kalkar) und die **Gemeinde Rheurdt** (inkl. Schaephuysen).

Entwickelt mit modernem **Kotlin**, **Jetpack Compose** (Material 3), **MVVM**, **Coroutines / StateFlow** und **Android WorkManager** für batterieschonende Hintergrunddienste.

---

## 📱 Projektübersicht

Dieses Repository bündelt die Quellcodes der beiden Bürger-Informations-Apps für den Niederrhein:

1. **`wissel-info/`**: Bürger-App für Wissel (Stadt Kalkar, Kreis Kleve)
2. **`rheurdt-info/`**: Bürger-App für Rheurdt & Schaephuysen (Kreis Kleve)

---

## ✨ Kernfunktionen

* **Dashboard & Live-Wetter:**
  * DWD-Wetterdaten (über Bright Sky API) mit Globalstrahlung, Temperatur, Wind und Feuchte.
  * Stündliche PV-Ertragsprognose basierend auf der hinterlegten Solar-Anlagengröße (kWp).
* **Rheinpegel & Hochwasserschutz:**
  * Live-Pegelstände via WSV PegelOnline API v2 (Rees für Wissel, Duisburg & Wesel für Rheurdt).
  * 30-Tage-Historienverlauf, 24h-Trend und Schwellenwert-Meldungen (Hochwassermarke I & II).
* **Müllabfuhr & Straßenkalender:**
  * Amtliche Abfuhrtermine für alle Straßen der Kommunen.
  * Automatische Erinnerungen am Vorabend um 19:00 Uhr mit genauer Benachrichtigung, welche Tonnen (Gelb, Restmüll, Biomüll, Papier) herausgestellt werden müssen.
* **Lokalpolitik & Ratsinformationen (RIS):**
  * Live-Sitzungskalender aller Ausschüsse und des Gemeinderats / Stadtrats.
  * Übersicht aller amtlichen Beschlussvorlagen und Drucksachen.
  * Schlagwortbasierte Filterung auf Beschlüsse mit lokalem Ortsbezug (z. B. Satzungen, Baumaßnahmen, Feuerwehr, Tourismus).
* **Interaktive Karte (OpenStreetMap & Geodaten NRW):**
  * Offline-fähige Vektorkarten (osmdroid) mit Sehenswürdigkeiten (POIs), Nahverkehrshaltestellen und Kulturstätten.
  * Zuschaltbare WMS-Themenebenen: ALKIS-Kataster, BORIS-Bodenrichtwerte, Hochwassergefahrenkarten (HWGK) und DWD-Niederschlagsradar.
* **ÖPNV-Abfahrtsmonitor:**
  * Live-Abfahrtszeiten lokaler Buslinien in Echtzeit über die Elektronische Fahrplanauskunft (VRR EFA).

---

## 🔔 Umfassendes Benachrichtigungssystem (5 Kategorien)

Jede App verfügt über ein feingranulares Benachrichtigungszentrum im Setup-Reiter mit getrennten Android Notification Channels:
1. **Müllabfuhr:** Erinnerung am Vorabend um 19:00 Uhr.
2. **Ratssitzungen:** Ankündigung 24 Stunden vor Sitzungsbeginn (18:00 Uhr am Vortag).
3. **Neue Beschlüsse mit Ortsbezug:** Sofortige Push-Benachrichtigung bei neuen RIS-Drucksachen mit lokaler Relevanz.
4. **Unwetter & Katastrophenschutz:** DWD-Unwetterwarnungen & BBK MoWaS / NINA Gefahrendurchsagen für den Kreis Kleve.
5. **Rheinpegel-Hochwassermarken:** Automatische Alarmierung beim Überschreiten von Marke I und Marke II sowie offizielle Entwarnung.

---

## ⚖️ Rechtliche Compliance & Datenschutz (DSGVO)

* **100 % Open Data:** Alle angebundenen Behördenschnittstellen sind lizenzkonform attribuiert:
  * Deutscher Wetterdienst (DWD): `dl-de/by-2-0`
  * WSV PEGELONLINE: `dl-de/by-2-0`
  * Marktstammdatenregister (MaStR) Bundesnetzagentur: `dl-de/by-2-0`
  * BBK MoWaS / NINA: `dl-de/by-2-0`
  * Geobasis NRW: `dl-de/zero-2-0` (ALKIS) & `dl-de/by-2-0` (BORIS / HWGK)
  * OpenStreetMap: `ODbL 1.0` (inkl. sichtbarem On-Map-Badge)
  * Ratsinformationen: `§ 5 UrhG` (Gemeinfreie amtliche Werke)
* **Privacy by Design:** Keine Server-Datenbank, keine Nutzerkonten, keine Werbenetzwerke, kein Google Analytics / Tracking. Alle Einstellungen verbleiben rein lokal auf dem Gerät (`SharedPreferences`).

---

## 🛠️ Bauen & Ausführen

Voraussetzungen: Android SDK (API 34+ / Android 14+), Java 17+ / JBR.

```bash
# Wissel Info bauen
cd wissel-info
./gradlew assembleDebug

# Rheurdt Info bauen
cd ../rheurdt-info
./gradlew assembleDebug
```
