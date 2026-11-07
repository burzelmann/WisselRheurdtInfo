package de.wissel.info.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.wissel.info.data.LocalCacheManager
import de.wissel.info.data.NotificationHelper
import de.wissel.info.data.UserPreferences
import de.wissel.info.data.WasteReminderScheduler
import de.wissel.info.data.PoliticsReminderScheduler
import de.wissel.info.data.AlertsAndPegelScheduler
import de.wissel.info.data.WisselBusStop
import de.wissel.info.data.WisselDashboardState
import de.wissel.info.data.WisselRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WisselViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WisselRepository()
    private val prefs = UserPreferences(application)

    private val _uiState: MutableStateFlow<WisselDashboardState>

    init {
        val activeList = parseActiveCards()
        val disabledList = parseDisabledCards()

        val cached = LocalCacheManager.loadCache(application)
        val initial = cached ?: WisselDashboardState(
            streetName = prefs.street,
            pvCapacityKwp = prefs.pvCapacityKwp,
            wasteNotificationEnabled = prefs.wasteNotificationEnabled,
            politicsMeetingNotificationEnabled = prefs.politicsMeetingNotificationEnabled,
            politicsDecisionNotificationEnabled = prefs.politicsDecisionNotificationEnabled,
            severeAlertsNotificationEnabled = prefs.severeAlertsNotificationEnabled,
            pegelAlertsNotificationEnabled = prefs.pegelAlertsNotificationEnabled
        )
        _uiState = MutableStateFlow(
            initial.copy(
                activeCards = activeList,
                disabledCards = disabledList,
                streetName = prefs.street,
                pvCapacityKwp = prefs.pvCapacityKwp,
                wasteNotificationEnabled = prefs.wasteNotificationEnabled,
                politicsMeetingNotificationEnabled = prefs.politicsMeetingNotificationEnabled,
                politicsDecisionNotificationEnabled = prefs.politicsDecisionNotificationEnabled,
                severeAlertsNotificationEnabled = prefs.severeAlertsNotificationEnabled,
                pegelAlertsNotificationEnabled = prefs.pegelAlertsNotificationEnabled
            )
        )

        if (prefs.wasteNotificationEnabled) {
            WasteReminderScheduler.scheduleDailyReminder(application)
        }
        if (prefs.politicsMeetingNotificationEnabled || prefs.politicsDecisionNotificationEnabled) {
            PoliticsReminderScheduler.scheduleDailyCheck(application)
        }
        if (prefs.severeAlertsNotificationEnabled || prefs.pegelAlertsNotificationEnabled) {
            AlertsAndPegelScheduler.schedulePeriodicCheck(application)
        }
        refresh()
    }

    private fun parseActiveCards(): List<de.wissel.info.data.DashboardCardType> {
        val savedOrder = prefs.dashboardCardOrder
        val disabled = prefs.disabledDashboardCards
        val list = mutableListOf<de.wissel.info.data.DashboardCardType>()
        savedOrder.forEach { id ->
            val card = de.wissel.info.data.DashboardCardType.fromId(id)
            if (card != null && !disabled.contains(id) && !list.contains(card)) {
                list.add(card)
            }
        }
        de.wissel.info.data.DashboardCardType.values().forEach { card ->
            if (!disabled.contains(card.id) && !list.contains(card)) {
                list.add(card)
            }
        }
        return list
    }

    private fun parseDisabledCards(): List<de.wissel.info.data.DashboardCardType> {
        val disabled = prefs.disabledDashboardCards
        val savedOrder = prefs.dashboardCardOrder
        val list = mutableListOf<de.wissel.info.data.DashboardCardType>()
        savedOrder.forEach { id ->
            if (disabled.contains(id)) {
                val card = de.wissel.info.data.DashboardCardType.fromId(id)
                if (card != null && !list.contains(card)) {
                    list.add(card)
                }
            }
        }
        de.wissel.info.data.DashboardCardType.values().forEach { card ->
            if (disabled.contains(card.id) && !list.contains(card)) {
                list.add(card)
            }
        }
        return list
    }
    val uiState: StateFlow<WisselDashboardState> = _uiState.asStateFlow()



    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val state = repository.fetchDashboard(
                    currentStreet = prefs.street,
                    pvKwp = prefs.pvCapacityKwp,
                    notifEnabled = prefs.wasteNotificationEnabled,
                    selectedStop = _uiState.value.selectedBusStop,
                    enabledCards = _uiState.value.activeCards.toSet(),
                    currentState = _uiState.value
                )
                _uiState.value = state.copy(
                    activeCards = _uiState.value.activeCards,
                    disabledCards = _uiState.value.disabledCards,
                    streetName = prefs.street,
                    pvCapacityKwp = prefs.pvCapacityKwp,
                    wasteNotificationEnabled = prefs.wasteNotificationEnabled
                )
                LocalCacheManager.saveCache(getApplication(), _uiState.value)
            } catch (e: Exception) {
                val current = _uiState.value
                val fallbackTime = current.lastUpdated.ifEmpty { "zuletzt" }
                _uiState.value = current.copy(
                    isLoading = false,
                    errorMessage = "Offline-Modus • Datenstand: $fallbackTime"
                )
            }
        }
    }

    fun selectBusStop(stop: WisselBusStop) {
        if (_uiState.value.selectedBusStop == stop) return
        _uiState.value = _uiState.value.copy(selectedBusStop = stop, isBusLoading = true)
        viewModelScope.launch {
            try {
                val deps = repository.fetchBusDepartures(stop)
                _uiState.value = _uiState.value.copy(busDepartures = deps, isBusLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBusLoading = false)
            }
        }
    }

    fun refreshBus() {
        val currentStop = _uiState.value.selectedBusStop
        _uiState.value = _uiState.value.copy(isBusLoading = true)
        viewModelScope.launch {
            try {
                val deps = repository.fetchBusDepartures(currentStop)
                _uiState.value = _uiState.value.copy(busDepartures = deps, isBusLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBusLoading = false)
            }
        }
    }

    fun updateStreet(street: String) {
        prefs.street = street
        _uiState.value = _uiState.value.copy(streetName = street)
        refresh()
    }

    fun updatePvCapacity(kwp: Double) {
        prefs.pvCapacityKwp = kwp
        _uiState.value = _uiState.value.copy(pvCapacityKwp = kwp)
    }

    fun toggleWasteNotification(enabled: Boolean) {
        prefs.wasteNotificationEnabled = enabled
        _uiState.value = _uiState.value.copy(wasteNotificationEnabled = enabled)
        if (enabled) {
            WasteReminderScheduler.scheduleDailyReminder(getApplication())
        } else {
            WasteReminderScheduler.cancelReminder(getApplication())
        }
    }

    fun sendTestNotification(context: Context) {
        val nextItems = _uiState.value.nextWaste
        val msg = if (nextItems.isNotEmpty()) {
            val names = nextItems.joinToString(" & ") { it.title }
            "Morgen Leerung in Wissel: $names! Bitte bis 06:00 Uhr rausstellen."
        } else {
            "Morgen werden die Tonnen in Wissel geleert. Bitte rechtzeitig rausstellen."
        }
        NotificationHelper.sendWasteReminder(
            context = context,
            title = "🔔 Müllabfuhr Wissel (${prefs.street})",
            message = msg
        )
    }


    fun togglePoliticsMeetingNotification(enabled: Boolean) {
        prefs.politicsMeetingNotificationEnabled = enabled
        _uiState.value = _uiState.value.copy(politicsMeetingNotificationEnabled = enabled)
        if (enabled || prefs.politicsDecisionNotificationEnabled) {
            PoliticsReminderScheduler.scheduleDailyCheck(getApplication())
        } else {
            PoliticsReminderScheduler.cancelReminder(getApplication())
        }
    }

    fun togglePoliticsDecisionNotification(enabled: Boolean) {
        prefs.politicsDecisionNotificationEnabled = enabled
        _uiState.value = _uiState.value.copy(politicsDecisionNotificationEnabled = enabled)
        if (enabled || prefs.politicsMeetingNotificationEnabled) {
            PoliticsReminderScheduler.scheduleDailyCheck(getApplication())
        } else {
            PoliticsReminderScheduler.cancelReminder(getApplication())
        }
    }

    fun sendTestPoliticsMeetingNotification(context: Context) {
        val nextMeeting = _uiState.value.councilMeetings.firstOrNull { !it.date.isBefore(java.time.LocalDate.now()) }
            ?: _uiState.value.councilMeetings.firstOrNull()
        val title = if (nextMeeting != null) "🏛️ Morgen: ${nextMeeting.committeeName}" else "🏛️ Morgen: Rat der Stadt Kalkar"
        val msg = if (nextMeeting != null) {
            "${nextMeeting.title}\nUhrzeit: ${nextMeeting.time} • Ort: ${nextMeeting.location}"
        } else {
            "11. Sitzung im Historischen Rathaus Kalkar (Ratssaal). Öffentliche Tagesordnung einsehbar."
        }
        NotificationHelper.sendCouncilMeetingReminder(
            context = context,
            title = title,
            message = msg
        )
    }

    fun sendTestPoliticsDecisionNotification(context: Context) {
        val relevantDecision = _uiState.value.councilDecisions.firstOrNull { it.isWisselRelevant }
            ?: _uiState.value.councilDecisions.firstOrNull()
        val title = "📜 Neuer Ratsbeschluss mit Wissel-Bezug!"
        val msg = if (relevantDecision != null) {
            "${relevantDecision.documentNumber}: ${relevantDecision.title}\n${relevantDecision.decisionSummary}"
        } else {
            "Drucksache 12-177: Gestaltungssatzung für Alt-Wissel einstimmig beschlossen."
        }
        NotificationHelper.sendCouncilDecisionAlert(
            context = context,
            title = title,
            message = msg
        )
    }


    fun toggleSevereAlertsNotification(enabled: Boolean) {
        prefs.severeAlertsNotificationEnabled = enabled
        _uiState.value = _uiState.value.copy(severeAlertsNotificationEnabled = enabled)
        if (enabled || prefs.pegelAlertsNotificationEnabled) {
            AlertsAndPegelScheduler.schedulePeriodicCheck(getApplication())
        } else {
            AlertsAndPegelScheduler.cancelChecks(getApplication())
        }
    }

    fun togglePegelAlertsNotification(enabled: Boolean) {
        prefs.pegelAlertsNotificationEnabled = enabled
        _uiState.value = _uiState.value.copy(pegelAlertsNotificationEnabled = enabled)
        if (enabled || prefs.severeAlertsNotificationEnabled) {
            AlertsAndPegelScheduler.schedulePeriodicCheck(getApplication())
        } else {
            AlertsAndPegelScheduler.cancelChecks(getApplication())
        }
    }

    fun sendTestSevereAlertNotification(context: Context) {
        NotificationHelper.sendSevereAlert(
            context = context,
            title = "⚠️ DWD Unwetterwarnung für Kalkar/Wissel",
            message = "Schwere Gewitter mit orkanartigen Böen bis 105 km/h und Starkregen ab 16:30 Uhr erwartet. Schutzmaßnahmen beachten!"
        )
    }

    fun sendTestPegelAlertNotification(context: Context) {
        NotificationHelper.sendPegelAlert(
            context = context,
            title = "🌊 Rheinpegel Rees: Hochwassermarke I überschritten!",
            message = "Aktueller Wasserstand: 6,74 m (steigend). Schifffahrt fährt gedrosselt. Erste Deichvorländer im Polder geflutet."
        )
    }

    fun moveCard(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.activeCards.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            val disabledIds = _uiState.value.disabledCards.map { it.id }
            val fullOrder = current.map { it.id } + disabledIds
            prefs.dashboardCardOrder = fullOrder
            _uiState.value = _uiState.value.copy(activeCards = current)
            LocalCacheManager.saveCache(getApplication(), _uiState.value)
        }
    }

    fun disableCard(card: de.wissel.info.data.DashboardCardType) {
        val currentActive = _uiState.value.activeCards.toMutableList()
        currentActive.remove(card)
        val currentDisabled = _uiState.value.disabledCards.toMutableList()
        if (!currentDisabled.contains(card)) {
            currentDisabled.add(card)
        }
        val newDisabledSet = (prefs.disabledDashboardCards + card.id).toSet()
        prefs.disabledDashboardCards = newDisabledSet

        val fullOrder = currentActive.map { it.id } + currentDisabled.map { it.id }
        prefs.dashboardCardOrder = fullOrder

        _uiState.value = _uiState.value.copy(
            activeCards = currentActive,
            disabledCards = currentDisabled
        )
        LocalCacheManager.saveCache(getApplication(), _uiState.value)
    }

    fun enableCard(card: de.wissel.info.data.DashboardCardType) {
        val currentActive = _uiState.value.activeCards.toMutableList()
        if (!currentActive.contains(card)) {
            currentActive.add(card)
        }
        val currentDisabled = _uiState.value.disabledCards.toMutableList()
        currentDisabled.remove(card)

        val newDisabledSet = (prefs.disabledDashboardCards - card.id).toSet()
        prefs.disabledDashboardCards = newDisabledSet

        val fullOrder = currentActive.map { it.id } + currentDisabled.map { it.id }
        prefs.dashboardCardOrder = fullOrder

        _uiState.value = _uiState.value.copy(
            activeCards = currentActive,
            disabledCards = currentDisabled
        )
        LocalCacheManager.saveCache(getApplication(), _uiState.value)

        viewModelScope.launch {
            when (card) {
                de.wissel.info.data.DashboardCardType.WEATHER -> {
                    val w = runCatching { repository.fetchWeather(prefs.pvCapacityKwp) }.getOrNull()
                    if (w != null) _uiState.value = _uiState.value.copy(weather = w)
                }
                de.wissel.info.data.DashboardCardType.PEGEL -> {
                    val p = runCatching { repository.fetchPegel() }.getOrNull()
                    if (p != null) _uiState.value = _uiState.value.copy(pegelRees = p)
                }
                de.wissel.info.data.DashboardCardType.ENERGY -> {
                    val m = runCatching { repository.fetchMastrStats() }.getOrNull()
                    if (m != null) _uiState.value = _uiState.value.copy(mastrEnergyStats = m)
                }
                de.wissel.info.data.DashboardCardType.BUS -> {
                    val b = runCatching { repository.fetchBusDepartures(_uiState.value.selectedBusStop) }.getOrNull()
                    if (b != null) _uiState.value = _uiState.value.copy(busDepartures = b)
                }
                de.wissel.info.data.DashboardCardType.WASTE -> {
                    val w = runCatching { repository.fetchWasteSchedule(prefs.street) }.getOrNull()
                    if (w != null) {
                        val today = java.time.LocalDate.now()
                        val upcoming = w.filter { !it.date.isBefore(today) }
                        _uiState.value = _uiState.value.copy(allWaste = upcoming, nextWaste = upcoming.take(2))
                    }
                }
                de.wissel.info.data.DashboardCardType.POLITICS -> {
                    val m = runCatching { repository.fetchCouncilMeetings() }.getOrNull()
                    val d = runCatching { repository.fetchCouncilDecisions() }.getOrNull()
                    val today = java.time.LocalDate.now()
                    _uiState.value = _uiState.value.copy(
                        councilMeetings = m?.filter { !it.date.isBefore(today) } ?: _uiState.value.councilMeetings,
                        councilDecisions = d ?: _uiState.value.councilDecisions
                    )
                }
                de.wissel.info.data.DashboardCardType.EVENTS -> {
                    val v = runCatching { repository.fetchVillageEvents() }.getOrNull()
                    val today = java.time.LocalDate.now()
                    if (v != null) _uiState.value = _uiState.value.copy(villageEvents = v.filter { !it.date.isBefore(today) })
                }
                de.wissel.info.data.DashboardCardType.HOLIDAYS -> {
                    val h = runCatching { repository.fetchHolidays() }.getOrNull()
                    val today = java.time.LocalDate.now()
                    if (h != null) _uiState.value = _uiState.value.copy(upcomingHolidays = h.filter { !it.endDate.isBefore(today) }.take(4))
                }
            }
        }
    }
}
