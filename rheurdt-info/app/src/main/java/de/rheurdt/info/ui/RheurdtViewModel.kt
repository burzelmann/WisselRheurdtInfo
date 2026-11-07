package de.rheurdt.info.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.rheurdt.info.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class RheurdtViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RheurdtRepository()
    private val prefs = UserPreferences(application)

    private val _uiState = MutableStateFlow(
        RheurdtDashboardState(
            isLoading = true,
            selectedBusStop = prefs.selectedBusStop,
            streetName = prefs.streetName,
            pvCapacityKwp = prefs.pvCapacityKwp,
            wasteNotificationEnabled = prefs.wasteNotificationEnabled,
            politicsMeetingNotificationEnabled = prefs.politicsMeetingNotificationEnabled,
            politicsDecisionNotificationEnabled = prefs.politicsDecisionNotificationEnabled,
            severeAlertsNotificationEnabled = prefs.severeAlertsNotificationEnabled,
            pegelAlertsNotificationEnabled = prefs.pegelAlertsNotificationEnabled,
            activeCards = parseActiveCards(),
            disabledCards = parseDisabledCards()
        )
    )
    val uiState: StateFlow<RheurdtDashboardState> = _uiState.asStateFlow()

    init {
        val cached = LocalCacheManager.loadCache(application)
        if (prefs.politicsMeetingNotificationEnabled || prefs.politicsDecisionNotificationEnabled) {
            PoliticsReminderScheduler.scheduleDailyCheck(application)
        }
        if (prefs.severeAlertsNotificationEnabled || prefs.pegelAlertsNotificationEnabled) {
            AlertsAndPegelScheduler.schedulePeriodicCheck(application)
        }
        if (cached != null) {
            _uiState.value = cached.copy(
                isLoading = false,
                selectedBusStop = prefs.selectedBusStop,
                activeCards = parseActiveCards(),
                disabledCards = parseDisabledCards()
            )
        }
        refresh()
    }

    private fun parseActiveCards(): List<DashboardCardType> {
        val savedOrder = prefs.dashboardCardOrder
        val disabled = prefs.disabledDashboardCards
        val list = mutableListOf<DashboardCardType>()
        savedOrder.forEach { id ->
            val card = DashboardCardType.fromId(id)
            if (card != null && !disabled.contains(id) && !list.contains(card)) {
                list.add(card)
            }
        }
        DashboardCardType.values().forEach { card ->
            if (!disabled.contains(card.id) && !list.contains(card)) {
                list.add(card)
            }
        }
        return list
    }

    private fun parseDisabledCards(): List<DashboardCardType> {
        val disabled = prefs.disabledDashboardCards
        val savedOrder = prefs.dashboardCardOrder
        val list = mutableListOf<DashboardCardType>()
        savedOrder.forEach { id ->
            if (disabled.contains(id)) {
                val card = DashboardCardType.fromId(id)
                if (card != null && !list.contains(card)) {
                    list.add(card)
                }
            }
        }
        DashboardCardType.values().forEach { card ->
            if (disabled.contains(card.id) && !list.contains(card)) {
                list.add(card)
            }
        }
        return list
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val state = repository.fetchDashboard(
                    selectedStop = _uiState.value.selectedBusStop,
                    enabledCards = _uiState.value.activeCards.toSet(),
                    currentState = _uiState.value
                )
                _uiState.value = state.copy(
                    activeCards = _uiState.value.activeCards,
                    disabledCards = _uiState.value.disabledCards,
                    streetName = prefs.streetName,
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

    fun selectBusStop(stop: RheurdtBusStop) {
        if (_uiState.value.selectedBusStop == stop) return
        prefs.selectedBusStop = stop
        _uiState.value = _uiState.value.copy(selectedBusStop = stop, isBusLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deps = repository.fetchBusDepartures(currentStop)
                _uiState.value = _uiState.value.copy(busDepartures = deps, isBusLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBusLoading = false)
            }
        }
    }

    fun updateStreet(street: String) {
        prefs.streetName = street
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
            "Morgen Leerung in Rheurdt: $names! Bitte bis 06:00 Uhr rausstellen."
        } else {
            "Morgen werden die Tonnen in Rheurdt geleert. Bitte rechtzeitig rausstellen."
        }
        NotificationHelper.sendWasteReminderNotification(
            context = context,
            notificationId = 1001,
            title = "🔔 Müllabfuhr Rheurdt (${prefs.streetName})",
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
        val title = if (nextMeeting != null) "🏛️ Morgen: ${nextMeeting.committeeName}" else "🏛️ Morgen: Gemeinderat Rheurdt"
        val msg = if (nextMeeting != null) {
            "${nextMeeting.title}\nUhrzeit: ${nextMeeting.time} • Ort: ${nextMeeting.location}"
        } else {
            "Sitzung im Rathaus Rheurdt (Sitzungssaal). Öffentliche Tagesordnung einsehbar."
        }
        NotificationHelper.sendCouncilMeetingReminder(
            context = context,
            title = title,
            message = msg
        )
    }

    fun sendTestPoliticsDecisionNotification(context: Context) {
        val relevantDecision = _uiState.value.councilDecisions.firstOrNull { it.isRheurdtRelevant }
            ?: _uiState.value.councilDecisions.firstOrNull()
        val title = "📜 Neuer Ratsbeschluss mit Rheurdt-Bezug!"
        val msg = if (relevantDecision != null) {
            "${relevantDecision.documentNumber}: ${relevantDecision.title}\n${relevantDecision.decisionSummary}"
        } else {
            "Vorlage 1080: Neufassung der Entwässerungssatzung für Rheurdt einstimmig beschlossen."
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
            title = "⚠️ DWD Unwetterwarnung für Rheurdt",
            message = "Schwere Gewitter mit orkanartigen Böen bis 100 km/h und Starkregen ab 16:30 Uhr erwartet. Schutzmaßnahmen beachten!"
        )
    }

    fun sendTestPegelAlertNotification(context: Context) {
        NotificationHelper.sendPegelAlert(
            context = context,
            title = "🌊 Rheinpegel Wesel: Hochwassermarke I überschritten!",
            message = "Aktueller Wasserstand: 6,55 m (steigend). Schifffahrt fährt gedrosselt. Erste Rheinauen geflutet."
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

    fun disableCard(card: DashboardCardType) {
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

    fun enableCard(card: DashboardCardType) {
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

        viewModelScope.launch(Dispatchers.IO) {
            when (card) {
                DashboardCardType.WEATHER -> {
                    val w = runCatching { repository.fetchWeather() }.getOrNull()
                    if (w != null) _uiState.value = _uiState.value.copy(weather = w)
                }
                DashboardCardType.PEGEL -> {
                    val p = runCatching { repository.fetchPegel() }.getOrNull()
                    if (p != null) _uiState.value = _uiState.value.copy(pegelData = p)
                }
                DashboardCardType.ENERGY -> {
                    val m = runCatching { repository.fetchMastrStats() }.getOrNull()
                    if (m != null) _uiState.value = _uiState.value.copy(mastrEnergyStats = m)
                }
                DashboardCardType.BUS -> {
                    val b = runCatching { repository.fetchBusDepartures(_uiState.value.selectedBusStop) }.getOrNull()
                    if (b != null) _uiState.value = _uiState.value.copy(busDepartures = b)
                }
                DashboardCardType.WASTE -> {
                    val w = runCatching { repository.fetchWasteSchedule() }.getOrNull()
                    if (w != null) {
                        val today = LocalDate.now()
                        val upcoming = w.filter { !it.date.isBefore(today) }
                        _uiState.value = _uiState.value.copy(allWaste = upcoming, nextWaste = upcoming.take(2))
                    }
                }
                DashboardCardType.POLITICS -> {
                    val m = runCatching { repository.fetchCouncilMeetings() }.getOrNull()
                    val d = runCatching { repository.fetchCouncilDecisions() }.getOrNull()
                    val today = LocalDate.now()
                    _uiState.value = _uiState.value.copy(
                        councilMeetings = m?.filter { !it.date.isBefore(today) } ?: _uiState.value.councilMeetings,
                        councilDecisions = d ?: _uiState.value.councilDecisions
                    )
                }
                DashboardCardType.EVENTS -> {
                    val v = runCatching { repository.fetchVillageEvents() }.getOrNull()
                    val today = LocalDate.now()
                    if (v != null) _uiState.value = _uiState.value.copy(villageEvents = v.filter { !it.date.isBefore(today) })
                }
                DashboardCardType.HOLIDAYS -> {}
            }
        }
    }
}
