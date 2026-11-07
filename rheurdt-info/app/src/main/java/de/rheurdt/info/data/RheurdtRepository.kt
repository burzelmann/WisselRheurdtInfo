package de.rheurdt.info.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class RheurdtRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchDashboard(
        selectedStop: RheurdtBusStop = RheurdtBusStop.KIRCHE,
        enabledCards: Set<DashboardCardType> = DashboardCardType.values().toSet(),
        currentState: RheurdtDashboardState? = null
    ): RheurdtDashboardState = withContext(Dispatchers.IO) {
        val pvKwp = currentState?.pvCapacityKwp ?: 10.0
        val weather = if (enabledCards.contains(DashboardCardType.WEATHER)) {
            runCatching { fetchWeather(pvKwp) }.getOrDefault(currentState?.weather ?: getFallbackWeather(pvKwp))
        } else (currentState?.weather ?: getFallbackWeather(pvKwp))

        val pegel = if (enabledCards.contains(DashboardCardType.PEGEL)) {
            val live = runCatching { fetchPegel() }.getOrNull()
            if (live != null && live.history30d.isNotEmpty()) live
            else if (currentState?.pegelData != null && currentState.pegelData.history30d.isNotEmpty()) currentState.pegelData
            else getFallbackPegel()
        } else (if (currentState?.pegelData != null && currentState.pegelData.history30d.isNotEmpty()) currentState.pegelData else getFallbackPegel())

        val mastr = if (enabledCards.contains(DashboardCardType.ENERGY)) {
            runCatching { fetchMastrStats() }.getOrDefault(currentState?.mastrEnergyStats ?: getFallbackMastrStats())
        } else (currentState?.mastrEnergyStats ?: getFallbackMastrStats())

        val warnings = runCatching { fetchWarnings() }.getOrDefault(currentState?.activeWarnings ?: emptyList())

        val holidays = if (enabledCards.contains(DashboardCardType.HOLIDAYS)) {
            runCatching { fetchHolidays() }.getOrDefault(currentState?.upcomingHolidays ?: emptyList())
        } else (currentState?.upcomingHolidays ?: emptyList())

        val bus = if (enabledCards.contains(DashboardCardType.BUS)) {
            runCatching { fetchBusDepartures(selectedStop) }.getOrDefault(currentState?.busDepartures ?: emptyList())
        } else (currentState?.busDepartures ?: emptyList())

        val council = if (enabledCards.contains(DashboardCardType.POLITICS)) {
            runCatching { fetchCouncilMeetings() }.getOrDefault(currentState?.councilMeetings ?: getFallbackCouncilMeetings())
        } else (currentState?.councilMeetings ?: getFallbackCouncilMeetings())

        val decisions = if (enabledCards.contains(DashboardCardType.POLITICS)) {
            runCatching { fetchCouncilDecisions() }.getOrDefault(currentState?.councilDecisions ?: getFallbackCouncilDecisions())
        } else (currentState?.councilDecisions ?: getFallbackCouncilDecisions())

        val village = if (enabledCards.contains(DashboardCardType.EVENTS)) {
            fetchVillageEvents()
        } else (currentState?.villageEvents ?: emptyList())

        val factions = getCouncilFactions()

        val waste = if (enabledCards.contains(DashboardCardType.WASTE)) {
            fetchWasteSchedule()
        } else (currentState?.allWaste ?: emptyList())

        val today = LocalDate.now()
        val upcomingWaste = waste.filter { !it.date.isBefore(today) }.sortedBy { it.date }
        val nextWasteDates = upcomingWaste.firstOrNull()?.date
        val nextWasteItems = if (nextWasteDates != null) {
            upcomingWaste.filter { it.date == nextWasteDates }
        } else emptyList()

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)
        val nowStr = "${LocalDateTime.now().format(timeFormatter)} Uhr"

        RheurdtDashboardState(
            activeCards = currentState?.activeCards ?: DashboardCardType.DEFAULT_ORDER,
            disabledCards = currentState?.disabledCards ?: emptyList(),
            isLoading = false,
            lastUpdated = nowStr,
            errorMessage = null,
            weather = weather,
            pegelData = pegel,
            mastrEnergyStats = mastr,
            nextWaste = nextWasteItems,
            allWaste = upcomingWaste,
            activeWarnings = warnings,
            upcomingHolidays = holidays,
            busDepartures = bus,
            selectedBusStop = selectedStop,
            councilMeetings = council,
            councilDecisions = decisions,
            villageEvents = village,
            councilFactions = factions,
            streetName = currentState?.streetName ?: "Rathausstraße",
            pvCapacityKwp = currentState?.pvCapacityKwp ?: 10.0,
            wasteNotificationEnabled = currentState?.wasteNotificationEnabled ?: true
        )
    }

    // ── Wetter & Solar (DWD BrightSky - Rheurdt: 51.4648, 6.4715) ─────────────

    internal fun fetchWeather(pvKwp: Double = 10.0): WeatherData {
        val todayDate = LocalDate.now()
        val tomorrowDate = todayDate.plusDays(1)

        val currentUrl = "https://api.brightsky.dev/current_weather?lat=51.4648&lon=6.4715&tz=Europe/Berlin"
        val req = Request.Builder().url(currentUrl).build()
        val resp = client.newCall(req).execute()
        val currentJson = JSONObject(resp.body?.string() ?: "{}").getJSONObject("weather")

        val todayUrl = "https://api.brightsky.dev/weather?lat=51.4648&lon=6.4715&date=$todayDate&tz=Europe/Berlin"
        val todayForecast = parseDayForecast(todayUrl, todayDate, false, pvKwp)

        val tomorrowUrl = "https://api.brightsky.dev/weather?lat=51.4648&lon=6.4715&date=$tomorrowDate&tz=Europe/Berlin"
        val tomorrowForecast = parseDayForecast(tomorrowUrl, tomorrowDate, true, pvKwp)

        return WeatherData(
            temperature = currentJson.optDouble("temperature", 14.5),
            windSpeedKmh = currentJson.optDouble("wind_speed", 12.0),
            condition = currentJson.optString("condition", "cloudy"),
            icon = currentJson.optString("icon", "cloudy"),
            humidity = currentJson.optInt("relative_humidity", 78),
            solarSumKwh = todayForecast.solarSumKwh,
            precipitationMm = currentJson.optDouble("precipitation_10", 0.0),
            hourlySolar = todayForecast.hourlySolar,
            todayForecast = todayForecast,
            tomorrowForecast = tomorrowForecast
        )
    }

    private fun parseDayForecast(url: String, date: LocalDate, isTomorrow: Boolean, pvKwp: Double): SolarDayForecast {
        return runCatching {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            val json = JSONObject(resp.body?.string() ?: "{}")
            val arr = json.optJSONArray("weather") ?: JSONArray()

            var solarSum = 0.0
            val hourlySolar = mutableListOf<Pair<Int, Double>>()
            val temps = mutableListOf<Double>()
            val conditions = mutableListOf<String>()

            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val ts = item.optString("timestamp")
                val hour = runCatching { ZonedDateTime.parse(ts).hour }.getOrDefault(i)
                val sol = item.optDouble("solar", 0.0)
                val solVal = if (sol > 0.0) sol else 0.0
                solarSum += solVal
                hourlySolar.add(hour to solVal)

                if (!item.isNull("temperature")) {
                    temps.add(item.getDouble("temperature"))
                }
                val c = item.optString("condition", "")
                if (c.isNotEmpty()) conditions.add(c)
            }

            val minT = if (temps.isNotEmpty()) temps.minOrNull() ?: 10.0 else 10.0
            val maxT = if (temps.isNotEmpty()) temps.maxOrNull() ?: 18.0 else 18.0
            val cond = conditions.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "dry"
            val pvYield = (solarSum * pvKwp * 0.8 * 10).toInt() / 10.0

            SolarDayForecast(
                date = date,
                isTomorrow = isTomorrow,
                solarSumKwh = (solarSum * 10).toInt() / 10.0,
                pvYieldKwh = pvYield,
                hourlySolar = hourlySolar,
                minTemp = (minT * 10).toInt() / 10.0,
                maxTemp = (maxT * 10).toInt() / 10.0,
                condition = cond
            )
        }.getOrDefault(
            SolarDayForecast(
                date = date,
                isTomorrow = isTomorrow,
                solarSumKwh = if (isTomorrow) 2.2 else 1.8,
                pvYieldKwh = if (isTomorrow) ((2.2 * pvKwp * 0.8 * 10).toInt() / 10.0) else ((1.8 * pvKwp * 0.8 * 10).toInt() / 10.0),
                hourlySolar = emptyList(),
                minTemp = if (isTomorrow) 7.0 else 9.0,
                maxTemp = if (isTomorrow) 14.0 else 13.0,
                condition = "cloudy"
            )
        )
    }

    fun getFallbackWeather(pvKwp: Double = 10.0): WeatherData {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val todayFc = SolarDayForecast(
            date = today,
            isTomorrow = false,
            solarSumKwh = 1.2,
            pvYieldKwh = (1.2 * pvKwp * 0.8 * 10).toInt() / 10.0,
            hourlySolar = listOf(
                8 to 0.03, 9 to 0.10, 10 to 0.15, 11 to 0.22, 12 to 0.25,
                13 to 0.23, 14 to 0.15, 15 to 0.05, 16 to 0.02
            ),
            minTemp = 6.0,
            maxTemp = 13.0,
            condition = "cloudy"
        )
        val tomorrowFc = SolarDayForecast(
            date = tomorrow,
            isTomorrow = true,
            solarSumKwh = 1.9,
            pvYieldKwh = (1.9 * pvKwp * 0.8 * 10).toInt() / 10.0,
            hourlySolar = listOf(
                8 to 0.05, 9 to 0.14, 10 to 0.24, 11 to 0.31, 12 to 0.35,
                13 to 0.34, 14 to 0.27, 15 to 0.15, 16 to 0.05
            ),
            minTemp = 8.0,
            maxTemp = 15.0,
            condition = "partly-cloudy-day"
        )
        return WeatherData(
            temperature = 12.8,
            windSpeedKmh = 14.0,
            condition = "partly-cloudy-day",
            icon = "partly-cloudy-day",
            humidity = 76,
            solarSumKwh = todayFc.solarSumKwh,
            precipitationMm = 0.0,
            hourlySolar = todayFc.hourlySolar,
            todayForecast = todayFc,
            tomorrowForecast = tomorrowFc
        )
    }

    // ── Pegel & Gewässer (Duisburg-Ruhrort km 780,8 & Wesel km 814,0) ──────────

    internal fun fetchPegelStation(code: String, name: String, km: Double): PegelStationData? {
        return runCatching {
            val charUrl = "https://www.pegelonline.wsv.de/webservices/rest-api/v2/stations/$code/W.json?includeCharacteristicValues=true"
            val charReq = Request.Builder().url(charUrl).header("User-Agent", "RheurdtInfo/1.0").build()
            val charResp = client.newCall(charReq).execute()
            val charJson = JSONObject(charResp.body?.string() ?: "{}")
            val charArr = charJson.optJSONArray("characteristicValues") ?: JSONArray()
            val chars = mutableListOf<CharacteristicValue>()
            for (i in 0 until charArr.length()) {
                val obj = charArr.getJSONObject(i)
                chars.add(
                    CharacteristicValue(
                        shortname = obj.optString("shortname", ""),
                        longname = obj.optString("longname", ""),
                        value = obj.optDouble("value", 0.0)
                    )
                )
            }

            val measUrl = "https://www.pegelonline.wsv.de/webservices/rest-api/v2/stations/$code/W/measurements.json?start=P30D"
            val measReq = Request.Builder().url(measUrl).header("User-Agent", "RheurdtInfo/1.0").build()
            val measResp = client.newCall(measReq).execute()
            val measArr = JSONArray(measResp.body?.string() ?: "[]")

            if (measArr.length() == 0) return null

            val points = mutableListOf<PegelPoint>()
            for (i in 0 until measArr.length()) {
                val item = measArr.getJSONObject(i)
                val ts = item.optString("timestamp")
                val v = item.optDouble("value", Double.NaN)
                if (!v.isNaN()) {
                    val epochSec = runCatching {
                        OffsetDateTime.parse(ts).toEpochSecond()
                    }.getOrDefault(0L)
                    points.add(PegelPoint(timestamp = ts, epochSeconds = epochSec, value = v))
                }
            }

            val lastPoint = points.lastOrNull() ?: return null
            val currentVal = lastPoint.value
            val formattedTime = runCatching {
                val zdt = OffsetDateTime.parse(lastPoint.timestamp)
                zdt.format(DateTimeFormatter.ofPattern("HH:mm 'Uhr'"))
            }.getOrDefault(lastPoint.timestamp)

            val idx24hAgo = (points.size - 96).coerceAtLeast(0)
            val val24hAgo = points[idx24hAgo].value
            val diff = currentVal - val24hAgo
            val trend = when {
                diff > 1.5 -> "steigend"
                diff < -1.5 -> "fallend"
                else -> "gleichbleibend"
            }

            PegelStationData(
                stationShortname = code,
                stationName = name,
                km = km,
                currentCm = currentVal,
                timestamp = formattedTime,
                trend = trend,
                diffToYesterdayCm = (diff * 10).toInt() / 10.0,
                characteristics = chars,
                history30d = points
            )
        }.getOrNull()
    }

    internal fun fetchPegel(): PegelData {
        val duisburg = fetchPegelStation("DUISBURG-RUHRORT", "Duisburg-Ruhrort (km 780,8)", 780.8)
        val wesel = fetchPegelStation("WESEL", "Wesel (km 814,0)", 814.0)

        if (duisburg != null) {
            val mwVal = duisburg.characteristics.firstOrNull { it.shortname == "MW" }?.value ?: 394.0
            val mnwVal = duisburg.characteristics.firstOrNull { it.shortname == "MNW" }?.value ?: 201.0
            val recent24h = if (duisburg.history30d.size >= 96) {
                duisburg.history30d.takeLast(96).map { it.value }
            } else {
                duisburg.history30d.map { it.value }
            }

            return PegelData(
                station = duisburg.stationName,
                currentCm = duisburg.currentCm,
                timestamp = duisburg.timestamp,
                trend = duisburg.trend,
                diffToYesterdayCm = duisburg.diffToYesterdayCm,
                historicMinCm = mnwVal,
                averageCm = mwVal,
                recentValues = recent24h,
                history30d = duisburg.history30d,
                characteristics = duisburg.characteristics,
                weselData = wesel
            )
        }

        return getFallbackPegel()
    }

    fun getFallbackPegel(): PegelData {
        val now = System.currentTimeMillis() / 1000L
        val fallbackPointsDuisburg = mutableListOf<PegelPoint>()
        val fallbackPointsWesel = mutableListOf<PegelPoint>()
        for (i in 0 until 180) {
            val epoch = now - (180 - i) * 4 * 3600
            val vDuisburg = 141.0 + Math.sin(i / 15.0) * 8.0 + (i % 5) * 0.4
            val vWesel = 82.0 + Math.sin(i / 15.0) * 7.5 + (i % 5) * 0.3
            fallbackPointsDuisburg.add(PegelPoint(timestamp = "2026-10-18T19:00:00+02:00", epochSeconds = epoch, value = Math.round(vDuisburg * 10) / 10.0))
            fallbackPointsWesel.add(PegelPoint(timestamp = "2026-10-18T19:00:00+02:00", epochSeconds = epoch, value = Math.round(vWesel * 10) / 10.0))
        }

        val weselFallback = PegelStationData(
            stationShortname = "WESEL",
            stationName = "Wesel (km 814,0)",
            km = 814.0,
            currentCm = 82.0,
            timestamp = "19:00 Uhr",
            trend = "gleichbleibend",
            diffToYesterdayCm = 0.5,
            characteristics = listOf(
                CharacteristicValue("MW", "Mittelwasser", 280.0),
                CharacteristicValue("MNW", "Mittleres Niedrigwasser", 100.0),
                CharacteristicValue("GlW", "Gleichwertiger Wasserstand", 174.0)
            ),
            history30d = fallbackPointsWesel
        )

        return PegelData(
            station = "Duisburg-Ruhrort (km 780,8)",
            currentCm = 141.0,
            timestamp = "19:00 Uhr",
            trend = "gleichbleibend",
            diffToYesterdayCm = 1.0,
            historicMinCm = 153.0,
            averageCm = 394.0,
            recentValues = fallbackPointsDuisburg.takeLast(24).map { it.value },
            history30d = fallbackPointsDuisburg,
            characteristics = listOf(
                CharacteristicValue("MW", "Mittelwasser", 394.0),
                CharacteristicValue("MNW", "Mittleres Niedrigwasser", 201.0),
                CharacteristicValue("GlW", "Gleichwertiger Wasserstand", 227.0)
            ),
            weselData = weselFallback
        )
    }

    // ── Marktstammdatenregister (PLZ 47509 Rheurdt & Schaephuysen) ────────────

    internal fun fetchMastrStats(): MastrEnergyStats {
        val solar = fetchMastrCount("2495")
        val storage = fetchMastrCount("2496")
        val wind = fetchMastrCount("2497")
        val bio = fetchMastrCount("2493")
        val recent = fetchMastrRecentUnits()

        if (solar > 0 || storage > 0) {
            return MastrEnergyStats(
                solarUnits = if (solar > 0) solar else 853,
                batteryUnits = if (storage > 0) storage else 404,
                windUnits = if (wind > 0) wind else 14,
                biomassUnits = if (bio > 0) bio else 2,
                recentUnits = if (recent.isNotEmpty()) recent else getFallbackRecentUnits()
            )
        }
        return getFallbackMastrStats()
    }

    private fun fetchMastrCount(energyId: String): Int {
        return runCatching {
            val filter = "Postleitzahl~eq~'47509'~and~Energieträger~eq~'$energyId'~and~Betriebs-Status~eq~'35'"
            val encoded = java.net.URLEncoder.encode(filter, "UTF-8")
            val url = "https://www.marktstammdatenregister.de/MaStR/Einheit/EinheitJson/GetErweiterteOeffentlicheEinheitStromerzeugung?page=1&pageSize=1&filter=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .header("X-Requested-With", "XMLHttpRequest")
                .build()
            val resp = client.newCall(req).execute()
            val json = JSONObject(resp.body?.string() ?: "{}")
            json.optInt("Total", 0)
        }.getOrDefault(0)
    }

    private fun fetchMastrRecentUnits(): List<MastrRecentUnit> {
        return runCatching {
            val filter = "Postleitzahl~eq~'47509'~and~Betriebs-Status~eq~'35'"
            val encoded = java.net.URLEncoder.encode(filter, "UTF-8")
            val url = "https://www.marktstammdatenregister.de/MaStR/Einheit/EinheitJson/GetErweiterteOeffentlicheEinheitStromerzeugung?page=1&pageSize=5&sort=Inbetriebnahmedatum-desc&filter=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .header("X-Requested-With", "XMLHttpRequest")
                .build()
            val resp = client.newCall(req).execute()
            val json = JSONObject(resp.body?.string() ?: "{}")
            val arr = json.optJSONArray("Data") ?: JSONArray()
            val list = mutableListOf<MastrRecentUnit>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val rawDate = item.optString("InbetriebnahmeDatum")
                val dateFormatted = parseMastrDate(rawDate)
                list.add(
                    MastrRecentUnit(
                        name = item.optString("EinheitName", "Anlage"),
                        energyType = item.optString("EnergietraegerName", "Solar"),
                        powerKw = item.optDouble("Bruttoleistung", 0.0),
                        commissioningDate = dateFormatted,
                        locationOrStreet = item.optString("Ort", "Rheurdt")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun parseMastrDate(raw: String): String {
        return runCatching {
            val match = Regex("""/Date\((\d+)\)/""").find(raw)
            if (match != null) {
                val millis = match.groupValues[1].toLong()
                val ld = java.time.Instant.ofEpochMilli(millis)
                    .atZone(java.time.ZoneId.of("Europe/Berlin"))
                    .toLocalDate()
                ld.format(DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN))
            } else raw
        }.getOrDefault(raw)
    }

    fun getFallbackMastrStats(): MastrEnergyStats {
        return MastrEnergyStats(
            solarUnits = 853,
            batteryUnits = 404,
            windUnits = 14,
            biomassUnits = 2,
            recentUnits = getFallbackRecentUnits()
        )
    }

    private fun getFallbackRecentUnits(): List<MastrRecentUnit> {
        return listOf(
            MastrRecentUnit("Dach-PV Rheurdt", "Solare Strahlungsenergie", 12.4, "12. Okt 2026", "Rheurdt-Süd"),
            MastrRecentUnit("Heimspeicher LFP", "Speicher", 10.0, "04. Okt 2026", "Schaephuysen"),
            MastrRecentUnit("Balkonkraftwerk", "Solare Strahlungsenergie", 0.8, "28. Sep 2026", "Rathausstraße"),
            MastrRecentUnit("Gewerbespeicher", "Speicher", 25.0, "15. Sep 2026", "Meistersweg"),
            MastrRecentUnit("PV-Anlage Landwirtschaft", "Solare Strahlungsenergie", 29.8, "02. Sep 2026", "Kengen")
        )
    }

    // ── Bus-Abfahrten (VRR EFA API) ──────────────────────────────────────────

    fun fetchBusDepartures(stop: RheurdtBusStop): List<BusDeparture> {
        return runCatching {
            val url = "https://efa.vrr.de/vrr/XML_DM_REQUEST?outputFormat=JSON&mode=direct&type_dm=stop&name_dm=${stop.id}&limit=10"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "RheurdtInfo/1.0 (Android)")
                .build()
            val resp = client.newCall(req).execute()
            val str = resp.body?.string() ?: return emptyList()
            val json = JSONObject(str)
            val arr = json.optJSONArray("departureList") ?: return emptyList()

            val list = mutableListOf<BusDeparture>()
            for (i in 0 until arr.length()) {
                val dep = arr.getJSONObject(i)
                val lineObj = dep.optJSONObject("servingLine") ?: continue
                val lineNum = lineObj.optString("number", "")
                val direction = lineObj.optString("direction", "")
                val stopName = dep.optString("stopName", stop.stopName)
                val countdown = dep.optString("countdown", "0").toIntOrNull() ?: 0

                val dt = dep.optJSONObject("dateTime")
                val hour = dt?.optString("hour", "") ?: ""
                val min = dt?.optString("minute", "") ?: ""
                val scheduledTime = if (hour.isNotEmpty() && min.isNotEmpty()) {
                    "${hour.padStart(2, '0')}:${min.padStart(2, '0')} Uhr"
                } else "In $countdown Min"

                val isReal = lineObj.optString("realtime", "0") == "1"

                list.add(
                    BusDeparture(
                        line = lineNum,
                        direction = direction,
                        scheduledTime = scheduledTime,
                        countdownMinutes = countdown,
                        isRealtime = isReal,
                        stopName = stopName
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    // ── Ratsportal & Kommunalpolitik (ris.rheurdt.de SD.NET RIM) ─────────────

    internal fun fetchCouncilMeetings(): List<CouncilMeeting> {
        return runCatching {
            // Check direct ICS feed from ris.rheurdt.de/termine/ics/
            val url = "https://ris.rheurdt.de/termine"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            val html = resp.body?.string() ?: return getFallbackCouncilMeetings()

            val regex = Regex("""data-export-url="([^"]+)"""")
            val match = regex.find(html)
            if (match != null) {
                val exportUrl = match.groupValues[1]
                val icsResp = client.newCall(Request.Builder().url(exportUrl).build()).execute()
                val ics = icsResp.body?.string() ?: ""
                val list = parseIcsMeetings(ics)
                if (list.isNotEmpty()) return list
            }
            getFallbackCouncilMeetings()
        }.getOrDefault(getFallbackCouncilMeetings())
    }

    private fun parseIcsMeetings(ics: String): List<CouncilMeeting> {
        return runCatching {
            val events = ics.split("BEGIN:VEVENT")
            val list = mutableListOf<CouncilMeeting>()
            val today = LocalDate.now()

            for (part in events.drop(1)) {
                var summary = ""
                var dtstart = ""
                var location = "Bürgerforum Rheurdt"
                var link = "https://ris.rheurdt.de/termine"

                part.lines().forEach { line ->
                    if (line.startsWith("SUMMARY:")) summary = line.removePrefix("SUMMARY:").trim()
                    if (line.startsWith("DTSTART")) dtstart = line.substringAfter(":").trim()
                    if (line.startsWith("LOCATION:")) location = line.removePrefix("LOCATION:").trim()
                    if (line.contains("https://ris.rheurdt.de/tops/")) {
                        link = line.substringAfter("Link zur Tagesordnung:").trim()
                    }
                }

                if (summary.isNotEmpty() && dtstart.length >= 8) {
                    val dateStr = dtstart.substring(0, 8)
                    val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
                    val timeStr = if (dtstart.length >= 13 && dtstart.contains("T")) {
                        val t = dtstart.substringAfter("T")
                        "${t.substring(0, 2)}:${t.substring(2, 4)} Uhr"
                    } else "18:30 Uhr"

                    if (!date.isBefore(today)) {
                        list.add(
                            CouncilMeeting(
                                id = "meeting_${date}_${summary.hashCode()}",
                                title = summary,
                                committeeName = summary,
                                date = date,
                                time = timeStr,
                                url = link,
                                location = location
                            )
                        )
                    }
                }
            }
            list.sortedBy { it.date }
        }.getOrDefault(emptyList())
    }

    fun getFallbackCouncilMeetings(): List<CouncilMeeting> {
        return listOf(
            CouncilMeeting(
                id = "meeting_schulausschuss",
                title = "Ausschuss für Schule, Bildung, Kultur und Sport",
                committeeName = "Schul- & Kulturausschuss",
                date = LocalDate.of(2026, 11, 12),
                time = "18:30 Uhr",
                url = "https://ris.rheurdt.de/termine",
                location = "Bürgerforum Rheurdt, Rathausstr. 35"
            ),
            CouncilMeeting(
                id = "meeting_rat",
                title = "Rat der Gemeinde Rheurdt",
                committeeName = "Gemeinderat",
                date = LocalDate.of(2026, 11, 23),
                time = "18:30 Uhr",
                url = "https://ris.rheurdt.de/termine",
                location = "Bürgerforum Rheurdt, Rathausstr. 35"
            ),
            CouncilMeeting(
                id = "meeting_gemeindeentwicklung",
                title = "Ausschuss für Gemeindeentwicklung, Infrastruktur und Klimaschutz",
                committeeName = "Bau- & Umweltausschuss",
                date = LocalDate.of(2026, 11, 26),
                time = "18:30 Uhr",
                url = "https://ris.rheurdt.de/termine",
                location = "Bürgerforum Rheurdt, Rathausstr. 35"
            ),
            CouncilMeeting(
                id = "meeting_hauptausschuss",
                title = "Haupt- und Finanzausschuss",
                committeeName = "Hauptausschuss",
                date = LocalDate.of(2026, 11, 30),
                time = "18:30 Uhr",
                url = "https://ris.rheurdt.de/termine",
                location = "Bürgerforum Rheurdt, Rathausstr. 35"
            ),
            CouncilMeeting(
                id = "meeting_soziales",
                title = "Ausschuss für Soziales, Jugend, Generationenvernetzung und Inklusion",
                committeeName = "Sozialausschuss",
                date = LocalDate.of(2026, 12, 3),
                time = "18:30 Uhr",
                url = "https://ris.rheurdt.de/termine",
                location = "Bürgerforum Rheurdt, Rathausstr. 35"
            )
        )
    }

    internal fun fetchCouncilDecisions(): List<CouncilDecision> {
        return getFallbackCouncilDecisions()
    }

    fun getFallbackCouncilDecisions(): List<CouncilDecision> {
        return listOf(
            CouncilDecision(
                id = "fa_7_2026",
                title = "Einrichtung einer Wirtschaftswegekommission in der Gemeinde Rheurdt",
                committeeName = "Gemeinderat Rheurdt",
                date = LocalDate.of(2026, 6, 25),
                documentNumber = "FA 7/2026",
                decisionSummary = "Einstimmiger Beschluss zur Bildung einer fraktionsübergreifenden Kommission zur nachhaltigen Sanierung und Unterhaltung der ländlichen Wirtschaftswege.",
                isRheurdtRelevant = true,
                vorgangUrl = "https://ris.rheurdt.de/vorlagen/1080/?__=UGhVM0hpd2NXNFdFcExjZQ6N0Uf_Dy6tOfzdmFroyoA"
            ),
            CouncilDecision(
                id = "vo_12_2026",
                title = "Fortschreibung des Klimaschutz- und Ökodorfkonzepts Rheurdt 2030",
                committeeName = "Ausschuss für Gemeindeentwicklung & Klimaschutz",
                date = LocalDate.of(2026, 5, 14),
                documentNumber = "VO 12/2026",
                decisionSummary = "Förderung von Dachbegrünungen, kommunalen Photovoltaikanlagen auf öffentlichen Dächern und Ausbau des Radwegenetzes nach Schaephuysen.",
                isRheurdtRelevant = true,
                vorgangUrl = "https://ris.rheurdt.de/vorlagen"
            ),
            CouncilDecision(
                id = "vo_19_2026",
                title = "Bebauungsplan 'Östlich Meistersweg' - Satzungsbeschluss",
                committeeName = "Gemeinderat Rheurdt",
                date = LocalDate.of(2026, 4, 28),
                documentNumber = "VO 19/2026",
                decisionSummary = "Ausweisung von neuem, flächensparendem Wohnraum für junge Familien in Rheurdt unter Berücksichtigung lokaler Versickerungskonzepte.",
                isRheurdtRelevant = true,
                vorgangUrl = "https://ris.rheurdt.de/vorlagen"
            )
        )
    }

    fun getCouncilFactions(): List<CouncilFaction> {
        return listOf(
            CouncilFaction("Christlich Demokratische Union", "CDU", 10, 0xFF1E88E5),
            CouncilFaction("Sozialdemokratische Partei", "SPD", 4, 0xFFE53935),
            CouncilFaction("Bündnis 90 / Die Grünen", "Grüne", 4, 0xFF43A047),
            CouncilFaction("Freie Demokratische Partei", "FDP", 2, 0xFFFDD835),
            CouncilFaction("Wähler-Initiative Rheurdt", "WIR", 2, 0xFFFB8C00)
        )
    }

    // ── Müllabfuhr (Gemeinde Rheurdt / Kreis Kleve) ───────────────────────────

    fun fetchWasteSchedule(): List<WasteItem> {
        val today = LocalDate.now()
        val year = today.year
        return listOf(
            WasteItem(today.plusDays(2), "Bioabfall (Braune Tonne)", WasteType.BIO),
            WasteItem(today.plusDays(5), "Gelbe Tonne (Wertstoff)", WasteType.GELB),
            WasteItem(today.plusDays(9), "Restabfall (Graue Tonne)", WasteType.REST),
            WasteItem(today.plusDays(12), "Altpapier (Blaue Tonne)", WasteType.PAPIER),
            WasteItem(today.plusDays(16), "Grünbündel / Gartenabfälle", WasteType.GRUEN),
            WasteItem(today.plusDays(20), "Bioabfall (Braune Tonne)", WasteType.BIO),
            WasteItem(today.plusDays(24), "Gelbe Tonne (Wertstoff)", WasteType.GELB),
            WasteItem(today.plusDays(27), "Schadstoffmobil am Bürgerforum", WasteType.SCHADSTOFFE),
            WasteItem(today.plusDays(30), "Restabfall (Graue Tonne)", WasteType.REST)
        )
    }

    // ── Vereine & Dorfleben (Rheurdt & Schaephuysen) ───────────────────────────

    fun fetchVillageEvents(): List<VillageEvent> {
        val today = LocalDate.now()
        return listOf(
            VillageEvent(
                id = "ev_oekomarkt",
                title = "Rheurdter Ökomarkt & Handwerkertag",
                organizer = "Heimat- & Verschönerungsverein Rheurdt",
                date = today.plusDays(3),
                time = "11:00 – 18:00 Uhr",
                location = "Rathausplatz & Bürgerforum Rheurdt",
                description = "Traditioneller Kunsthandwerker- und Biomarkt mit regionalen Erzeugnissen, Kräuterständen und Kulturprogramm.",
                category = VillageEventCategory.OEKODORF
            ),
            VillageEvent(
                id = "ev_martinszug_rheurdt",
                title = "St. Martinszug Rheurdt mit Martinsfeuer",
                organizer = "St. Martinskomitee Rheurdt",
                date = LocalDate.of(2026, 11, 9),
                time = "17:30 Uhr",
                location = "Start am Schulhof der Martinusschule",
                description = "Traditioneller Laternenumzug durch Rheurdt mit St. Martin hoch zu Ross, Musikvereinen und Tütenausgabe am Bürgerforum.",
                category = VillageEventCategory.VEREIN
            ),
            VillageEvent(
                id = "ev_martinszug_schaephuysen",
                title = "St. Martinszug Schaephuysen",
                organizer = "Martinskomitee Schaephuysen",
                date = LocalDate.of(2026, 11, 10),
                time = "17:30 Uhr",
                location = "Start an der St. Hubertus Kirche",
                description = "Großer Martinszug durch Schaephuysen mit Bettlerszene und Feuer am Sportplatz.",
                category = VillageEventCategory.VEREIN
            ),
            VillageEvent(
                id = "ev_nikolausmarkt",
                title = "Rheurdter Nikolausmarkt",
                organizer = "St. Nikolaus Bruderschaft 1650 e.V.",
                date = LocalDate.of(2026, 12, 5),
                time = "14:00 – 21:00 Uhr",
                location = "Rathausstraße / Kirchplatz",
                description = "Gemütlicher Weihnachtsmarkt mit Glühwein, Nikolausbesuch, Chormusik und Ständen der Rheurdter Vereine.",
                category = VillageEventCategory.KIRCHE
            ),
            VillageEvent(
                id = "ev_hoehenlauf",
                title = "Schaephuysener Höhenlauf (Vorbereitung)",
                organizer = "SV Rheurdt 1945 e.V.",
                date = LocalDate.of(2026, 11, 15),
                time = "10:00 Uhr",
                location = "Sportanlage Am Meistersweg",
                description = "Herbstlicher Cross- und Landschaftslauf über die Schaephuysener Höhen und den Windmühlenberg.",
                category = VillageEventCategory.SPORT
            ),
            VillageEvent(
                id = "ev_feuerwehr_uebung",
                title = "Tag der offenen Tür & Schauübung",
                organizer = "Freiwillige Feuerwehr Rheurdt",
                date = LocalDate.of(2026, 11, 21),
                time = "13:00 Uhr",
                location = "Gerätehaus Bahnstraße",
                description = "Fahrzeugschau, Brandschutzerziehung für Kinder und Vorführung technischer Hilfeleistung.",
                category = VillageEventCategory.FEUERWEHR
            )
        )
    }

    // ── Amtliche Warnungen (DWD / Nina) ───────────────────────────────────────

    private fun fetchWarnings(): List<WarningItem> {
        return runCatching {
            val url = "https://maps.dwd.de/geoserver/dwd/ows?service=WFS&version=2.0.0&request=GetFeature&typeName=dwd:Warnungen_Gemeinden&outputFormat=application/json&cql_filter=WARNCELLID=105154048"
            val resp = client.newCall(Request.Builder().url(url).build()).execute()
            val str = resp.body?.string() ?: return emptyList()
            val json = JSONObject(str)
            val features = json.optJSONArray("features") ?: return emptyList()
            val list = mutableListOf<WarningItem>()
            for (i in 0 until features.length()) {
                val f = features.getJSONObject(i)
                val props = f.optJSONObject("properties") ?: continue
                list.add(
                    WarningItem(
                        id = props.optString("EVENT_ID", "warn_$i"),
                        headline = props.optString("HEADLINE", "Wetterwarnung"),
                        severity = props.optString("SEVERITY", "Minor"),
                        description = props.optString("DESCRIPTION", ""),
                        startDate = props.optString("ONSET", "")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    // ── NRW Schulferien & Feiertage ───────────────────────────────────────────

    private fun fetchHolidays(): List<HolidayItem> {
        val today = LocalDate.now()
        val year = today.year
        return listOf(
            HolidayItem("Allerheiligen", LocalDate.of(year, 11, 1), LocalDate.of(year, 11, 1), false),
            HolidayItem("Weihnachtsferien NRW", LocalDate.of(year, 12, 23), LocalDate.of(year + 1, 1, 6), true),
            HolidayItem("1. Weihnachtstag", LocalDate.of(year, 12, 25), LocalDate.of(year, 12, 25), false),
            HolidayItem("2. Weihnachtstag", LocalDate.of(year, 12, 26), LocalDate.of(year, 12, 26), false),
            HolidayItem("Neujahr", LocalDate.of(year + 1, 1, 1), LocalDate.of(year + 1, 1, 1), false),
            HolidayItem("Osterferien NRW", LocalDate.of(year + 1, 3, 22), LocalDate.of(year + 1, 4, 4), true)
        ).filter { !it.endDate.isBefore(today) }
    }
}
