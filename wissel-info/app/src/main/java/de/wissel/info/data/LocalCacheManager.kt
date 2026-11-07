package de.wissel.info.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

object LocalCacheManager {
    private const val FILE_NAME = "dashboard_cache.json"

    fun saveCache(context: Context, state: WisselDashboardState) {
        runCatching {
            val root = JSONObject()
            root.put("lastUpdated", state.lastUpdated)
            root.put("streetName", state.streetName)
            root.put("pvCapacityKwp", state.pvCapacityKwp)

            val activeArr = JSONArray()
            state.activeCards.forEach { activeArr.put(it.id) }
            root.put("activeCards", activeArr)

            val disabledArr = JSONArray()
            state.disabledCards.forEach { disabledArr.put(it.id) }
            root.put("disabledCards", disabledArr)

            state.weather?.let { w ->
                val wObj = JSONObject()
                wObj.put("temperature", w.temperature)
                wObj.put("windSpeedKmh", w.windSpeedKmh)
                wObj.put("condition", w.condition)
                wObj.put("icon", w.icon)
                wObj.put("humidity", w.humidity)
                wObj.put("solarSumKwh", w.solarSumKwh)

                w.todayForecast?.let { tf ->
                    val tfObj = JSONObject()
                    tfObj.put("date", tf.date.toString())
                    tfObj.put("isTomorrow", tf.isTomorrow)
                    tfObj.put("solarSumKwh", tf.solarSumKwh)
                    tfObj.put("pvYieldKwh", tf.pvYieldKwh)
                    tfObj.put("minTemp", tf.minTemp)
                    tfObj.put("maxTemp", tf.maxTemp)
                    tfObj.put("condition", tf.condition)
                    val hArr = JSONArray()
                    tf.hourlySolar.forEach { (h, v) ->
                        val item = JSONObject()
                        item.put("h", h)
                        item.put("v", v)
                        hArr.put(item)
                    }
                    tfObj.put("hourly", hArr)
                    wObj.put("todayForecast", tfObj)
                }

                w.tomorrowForecast?.let { tf ->
                    val tfObj = JSONObject()
                    tfObj.put("date", tf.date.toString())
                    tfObj.put("isTomorrow", tf.isTomorrow)
                    tfObj.put("solarSumKwh", tf.solarSumKwh)
                    tfObj.put("pvYieldKwh", tf.pvYieldKwh)
                    tfObj.put("minTemp", tf.minTemp)
                    tfObj.put("maxTemp", tf.maxTemp)
                    tfObj.put("condition", tf.condition)
                    val hArr = JSONArray()
                    tf.hourlySolar.forEach { (h, v) ->
                        val item = JSONObject()
                        item.put("h", h)
                        item.put("v", v)
                        hArr.put(item)
                    }
                    tfObj.put("hourly", hArr)
                    wObj.put("tomorrowForecast", tfObj)
                }

                root.put("weather", wObj)
            }

            state.pegelRees?.let { p ->
                val pObj = JSONObject()
                pObj.put("station", p.station)
                pObj.put("currentCm", p.currentCm)
                pObj.put("timestamp", p.timestamp)
                pObj.put("trend", p.trend)
                pObj.put("diffToYesterdayCm", p.diffToYesterdayCm)
                pObj.put("historicMinCm", p.historicMinCm)
                pObj.put("averageCm", p.averageCm)

                val histArr = JSONArray()
                p.history30d.forEach { pt ->
                    val o = JSONObject()
                    o.put("ts", pt.timestamp)
                    o.put("sec", pt.epochSeconds)
                    o.put("v", pt.value)
                    histArr.put(o)
                }
                pObj.put("history30d", histArr)

                p.emmerichData?.let { emm ->
                    val eObj = JSONObject()
                    eObj.put("stationShortname", emm.stationShortname)
                    eObj.put("stationName", emm.stationName)
                    eObj.put("km", emm.km)
                    eObj.put("currentCm", emm.currentCm)
                    eObj.put("timestamp", emm.timestamp)
                    eObj.put("trend", emm.trend)
                    eObj.put("diffToYesterdayCm", emm.diffToYesterdayCm)

                    val eHistArr = JSONArray()
                    emm.history30d.forEach { pt ->
                        val o = JSONObject()
                        o.put("ts", pt.timestamp)
                        o.put("sec", pt.epochSeconds)
                        o.put("v", pt.value)
                        eHistArr.put(o)
                    }
                    eObj.put("history30d", eHistArr)
                    pObj.put("emmerich", eObj)
                }

                root.put("pegel", pObj)
            }

            state.mastrEnergyStats?.let { m ->
                val mObj = JSONObject()
                mObj.put("solar", m.solarUnits)
                mObj.put("battery", m.batteryUnits)
                mObj.put("wind", m.windUnits)
                mObj.put("biomass", m.biomassUnits)

                val recArr = JSONArray()
                m.recentUnits.forEach { u ->
                    val uObj = JSONObject()
                    uObj.put("name", u.name)
                    uObj.put("type", u.energyType)
                    uObj.put("kw", u.powerKw)
                    uObj.put("date", u.commissioningDate)
                    uObj.put("loc", u.locationOrStreet)
                    recArr.put(uObj)
                }
                mObj.put("recent", recArr)
                root.put("mastr", mObj)
            }

            val file = File(context.filesDir, FILE_NAME)
            file.writeText(root.toString())
        }
    }

    fun loadCache(context: Context): WisselDashboardState? {
        return runCatching {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return null
            val root = JSONObject(file.readText())

            val wObj = root.optJSONObject("weather")
            val weather = if (wObj != null) {
                fun parseForecast(obj: JSONObject?): SolarDayForecast? {
                    if (obj == null) return null
                    val d = runCatching { LocalDate.parse(obj.optString("date")) }.getOrDefault(LocalDate.now())
                    val hArr = obj.optJSONArray("hourly") ?: JSONArray()
                    val hList = mutableListOf<Pair<Int, Double>>()
                    for (i in 0 until hArr.length()) {
                        val itm = hArr.getJSONObject(i)
                        hList.add(itm.optInt("h") to itm.optDouble("v"))
                    }
                    return SolarDayForecast(
                        date = d,
                        isTomorrow = obj.optBoolean("isTomorrow", false),
                        solarSumKwh = obj.optDouble("solarSumKwh", 1.8),
                        pvYieldKwh = obj.optDouble("pvYieldKwh", 14.4),
                        hourlySolar = hList,
                        minTemp = obj.optDouble("minTemp", 10.0),
                        maxTemp = obj.optDouble("maxTemp", 18.0),
                        condition = obj.optString("condition", "dry")
                    )
                }
                val tf = parseForecast(wObj.optJSONObject("todayForecast"))
                val tmf = parseForecast(wObj.optJSONObject("tomorrowForecast"))
                WeatherData(
                    temperature = wObj.optDouble("temperature", 15.0),
                    windSpeedKmh = wObj.optDouble("windSpeedKmh", 10.0),
                    condition = wObj.optString("condition", "cloudy"),
                    icon = wObj.optString("icon", "cloudy"),
                    humidity = wObj.optInt("humidity", 70),
                    solarSumKwh = wObj.optDouble("solarSumKwh", tf?.solarSumKwh ?: 1.8),
                    precipitationMm = 0.0,
                    hourlySolar = tf?.hourlySolar ?: emptyList(),
                    todayForecast = tf,
                    tomorrowForecast = tmf
                )
            } else null

            val pObj = root.optJSONObject("pegel")
            val pegel = if (pObj != null) {
                val histArr = pObj.optJSONArray("history30d") ?: JSONArray()
                val histList = mutableListOf<PegelPoint>()
                for (i in 0 until histArr.length()) {
                    val o = histArr.getJSONObject(i)
                    histList.add(PegelPoint(o.optString("ts"), o.optLong("sec"), o.optDouble("v")))
                }
                if (histList.isEmpty()) {
                    histList.addAll(WisselRepository().getFallbackPegel().history30d)
                }

                val emmObj = pObj.optJSONObject("emmerich")
                val emmData = if (emmObj != null) {
                    val eHistArr = emmObj.optJSONArray("history30d") ?: JSONArray()
                    val eHistList = mutableListOf<PegelPoint>()
                    for (i in 0 until eHistArr.length()) {
                        val o = eHistArr.getJSONObject(i)
                        eHistList.add(PegelPoint(o.optString("ts"), o.optLong("sec"), o.optDouble("v")))
                    }
                    if (eHistList.isEmpty()) {
                        eHistList.addAll(WisselRepository().getFallbackPegel().emmerichData?.history30d ?: emptyList())
                    }
                    PegelStationData(
                        stationShortname = emmObj.optString("stationShortname", "EMMERICH"),
                        stationName = emmObj.optString("stationName", "Emmerich (km 851,9)"),
                        km = emmObj.optDouble("km", 851.9),
                        currentCm = emmObj.optDouble("currentCm", -4.0),
                        timestamp = emmObj.optString("timestamp", "07:00 Uhr"),
                        trend = emmObj.optString("trend", "gleichbleibend"),
                        diffToYesterdayCm = emmObj.optDouble("diffToYesterdayCm", 0.0),
                        history30d = eHistList
                    )
                } else null

                PegelData(
                    station = pObj.optString("station", "Rees (km 837,4)"),
                    currentCm = pObj.optDouble("currentCm", 27.0),
                    timestamp = pObj.optString("timestamp", "Vorhin"),
                    trend = pObj.optString("trend", "gleichbleibend"),
                    diffToYesterdayCm = pObj.optDouble("diffToYesterdayCm", 0.0),
                    historicMinCm = pObj.optDouble("historicMinCm", 38.0),
                    averageCm = pObj.optDouble("averageCm", 293.0),
                    history30d = histList,
                    emmerichData = emmData
                )
            } else null

            val mObj = root.optJSONObject("mastr")
            val mastr = if (mObj != null) {
                val recArr = mObj.optJSONArray("recent") ?: JSONArray()
                val recList = mutableListOf<MastrRecentUnit>()
                for (i in 0 until recArr.length()) {
                    val o = recArr.getJSONObject(i)
                    recList.add(
                        MastrRecentUnit(
                            name = o.optString("name"),
                            energyType = o.optString("type"),
                            powerKw = o.optDouble("kw"),
                            commissioningDate = o.optString("date"),
                            locationOrStreet = o.optString("loc")
                        )
                    )
                }
                MastrEnergyStats(
                    solarUnits = mObj.optInt("solar", 1778),
                    batteryUnits = mObj.optInt("battery", 805),
                    windUnits = mObj.optInt("wind", 13),
                    biomassUnits = mObj.optInt("biomass", 6),
                    recentUnits = recList
                )
            } else null

            val street = root.optString("streetName", "Zum Wisseler See")
            val pvKwp = root.optDouble("pvCapacityKwp", 10.0)
            val updated = root.optString("lastUpdated", "")

            val activeArr = root.optJSONArray("activeCards")
            val activeList = if (activeArr != null) {
                val list = mutableListOf<DashboardCardType>()
                for (i in 0 until activeArr.length()) {
                    DashboardCardType.fromId(activeArr.getString(i))?.let { list.add(it) }
                }
                list
            } else null

            val disabledArr = root.optJSONArray("disabledCards")
            val disabledList = if (disabledArr != null) {
                val list = mutableListOf<DashboardCardType>()
                for (i in 0 until disabledArr.length()) {
                    DashboardCardType.fromId(disabledArr.getString(i))?.let { list.add(it) }
                }
                list
            } else null

            val repo = WisselRepository()
            val waste = repo.fetchWasteSchedule(street)
            val today = LocalDate.now()
            val upcoming = waste.filter { !it.date.isBefore(today) }

            WisselDashboardState(
                activeCards = activeList ?: DashboardCardType.DEFAULT_ORDER,
                disabledCards = disabledList ?: emptyList(),
                isLoading = false,
                lastUpdated = updated,
                weather = weather,
                pegelRees = pegel,
                mastrEnergyStats = mastr,
                streetName = street,
                pvCapacityKwp = pvKwp,
                allWaste = upcoming,
                nextWaste = upcoming.take(2),
                councilMeetings = repo.getFallbackCouncilMeetings().filter { !it.date.isBefore(today) },
                councilDecisions = repo.getFallbackCouncilDecisions(),
                villageEvents = repo.fetchVillageEvents().filter { !it.date.isBefore(today) },
                councilFactions = repo.getCouncilFactions()
            )
        }.getOrNull()
    }
}
