package de.rheurdt.info.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rheurdt.info.data.DashboardCardType
import de.rheurdt.info.data.HolidayItem
import de.rheurdt.info.data.VillageEvent
import de.rheurdt.info.data.RheurdtBusStop
import de.rheurdt.info.data.RheurdtDashboardState
import de.rheurdt.info.ui.components.MastrEnergyCard
import de.rheurdt.info.ui.components.PegelChartCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    state: RheurdtDashboardState,
    onRefresh: () -> Unit,
    onSelectBusStop: (RheurdtBusStop) -> Unit = {},
    onRefreshBus: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToPolitics: () -> Unit = {},
    onMoveCard: (Int, Int) -> Unit = { _, _ -> },
    onDisableCard: (DashboardCardType) -> Unit = {},
    onEnableCard: (DashboardCardType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isEditMode by remember { mutableStateOf(false) }
    val todayStr = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Title, Date, Customize Mode, and Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Rheurdt Info",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = todayStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Toggle Edit / Organize Mode
                IconButton(onClick = { isEditMode = !isEditMode }) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Tune,
                        contentDescription = if (isEditMode) "Fertig" else "Kacheln anpassen",
                        tint = if (isEditMode) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onRefresh) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Aktualisieren",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Edit Mode Information Banner
        AnimatedVisibility(
            visible = isEditMode,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kacheln anpassen & sortieren",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Nutze ⬆️ / ⬇️ zum Verschieben oder ✕ zum Deaktivieren. Deaktivierte Kacheln pausieren den Datenabruf.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { isEditMode = false },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Fertig")
                    }
                }
            }
        }

        // Active warnings banner (always on top for safety)
        if (state.activeWarnings.isNotEmpty()) {
            state.activeWarnings.forEach { warn ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Column {
                            Text(
                                text = warn.headline,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            if (warn.description.isNotBlank()) {
                                Text(
                                    text = warn.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Keine amtlichen Gefahrenmeldungen für Rheurdt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Empty State Banner if all cards are deactivated
        if (state.activeCards.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✨", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Alle Infokacheln sind deaktiviert",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tippe unten auf die Minimalicons, um gewünschte Module wieder einzublenden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Dynamic Cards rendered in user-configured order
        state.activeCards.forEachIndexed { index, cardType ->
            DashboardCardWrapper(
                cardType = cardType,
                index = index,
                totalCards = state.activeCards.size,
                isEditMode = isEditMode,
                onMoveUp = { onMoveCard(index, index - 1) },
                onMoveDown = { onMoveCard(index, index + 1) },
                onDisable = { onDisableCard(cardType) }
            ) {
                when (cardType) {
                    DashboardCardType.WEATHER -> WeatherSolarCard(state = state)
                    DashboardCardType.PEGEL -> PegelChartCard(pegel = state.pegelData)
                    DashboardCardType.ENERGY -> MastrEnergyCard(stats = state.mastrEnergyStats)
                    DashboardCardType.WASTE -> WasteCard(state = state)
                    DashboardCardType.BUS -> BusDeparturesCard(
                        state = state,
                        onSelectBusStop = onSelectBusStop,
                        onRefreshBus = onRefreshBus
                    )
                    DashboardCardType.EVENTS -> VillageEventsCard(
                        events = state.villageEvents,
                        onNavigateToCalendar = onNavigateToCalendar
                    )
                    DashboardCardType.POLITICS -> PoliticsQuickCard(
                        state = state,
                        onNavigateToPolitics = onNavigateToPolitics
                    )
                    DashboardCardType.HOLIDAYS -> HolidaysCard(
                        holidays = state.upcomingHolidays
                    )
                }
            }
        }

        // ── Deactivated Cards Section (Minimal Icons) ────────────────
        if (state.disabledCards.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Ausgeblendete Module",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tippen zum Reaktivieren • Datenabruf ist pausiert",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Minimal Icons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.disabledCards.forEach { card ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { onEnableCard(card) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Aktivieren",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = card.icon + " " + card.title.split(" ")[0],
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick footer button to customize layout
        if (!isEditMode) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = { isEditMode = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.activeCards.size} von ${DashboardCardType.values().size} Modulen aktiv • Kacheln anpassen"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DashboardCardWrapper(
    cardType: DashboardCardType,
    index: Int,
    totalCards: Int,
    isEditMode: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDisable: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isEditMode) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(cardType.icon, fontSize = 18.sp)
                        Text(
                            text = cardType.title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Move Up
                        IconButton(
                            onClick = onMoveUp,
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Nach oben",
                                modifier = Modifier.size(18.dp),
                                tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        }

                        // Move Down
                        IconButton(
                            onClick = onMoveDown,
                            enabled = index < totalCards - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Nach unten",
                                modifier = Modifier.size(18.dp),
                                tint = if (index < totalCards - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        }

                        // Disable / Hide button
                        FilledTonalIconButton(
                            onClick = onDisable,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Ausblenden",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        content()
    }
}

// ── Individual Card Implementations ──────────────────────────────────────────

@Composable
private fun WeatherSolarCard(state: RheurdtDashboardState) {
        val w = state.weather
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Wetter & Photovoltaik Rheurdt",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${w?.temperature ?: "--"} °C",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Wind: ${w?.windSpeedKmh ?: "--"} km/h • Feuchte: ${w?.humidity ?: "--"}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                val currentHour = remember { java.time.LocalTime.now().hour }
                var selectedDayTab by remember { mutableStateOf(if (currentHour >= 19) 1 else 0) }

                val activeForecast = if (selectedDayTab == 1) {
                    w?.tomorrowForecast ?: de.rheurdt.info.data.SolarDayForecast(
                        date = LocalDate.now().plusDays(1),
                        isTomorrow = true,
                        solarSumKwh = 2.3,
                        pvYieldKwh = 18.6,
                        minTemp = 6.0,
                        maxTemp = 13.7
                    )
                } else {
                    w?.todayForecast ?: de.rheurdt.info.data.SolarDayForecast(
                        date = LocalDate.now(),
                        isTomorrow = false,
                        solarSumKwh = w?.solarSumKwh ?: 1.8,
                        pvYieldKwh = 14.4,
                        minTemp = 12.0,
                        maxTemp = 21.0
                    )
                }

                // Day Selection Tabs: Heute (Rückblick) vs Morgen (Prognose)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDayTab == 0,
                        onClick = { selectedDayTab = 0 },
                        label = { Text("Heute (Rückblick)") },
                        leadingIcon = if (selectedDayTab == 0) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    FilterChip(
                        selected = selectedDayTab == 1,
                        onClick = { selectedDayTab = 1 },
                        label = {
                            Text(if (currentHour >= 19) "Morgen (Prognose) 🌙" else "Morgen (Prognose)")
                        },
                        leadingIcon = if (selectedDayTab == 1) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFB300).copy(alpha = 0.28f),
                            selectedLabelColor = Color(0xFFFFD54F),
                            selectedLeadingIconColor = Color(0xFFFFD54F)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Calculated PV Yield for user's plant
                val solarSum = activeForecast.solarSumKwh
                val estKwh = activeForecast.pvYieldKwh

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (selectedDayTab == 1) Color(0xFFFFB300) else MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        val titleText = if (selectedDayTab == 1) {
                            "Erwarteter PV-Ertrag morgen: ~${estKwh} kWh"
                        } else {
                            "PV-Tagesertrag heute: ~${estKwh} kWh"
                        }
                        Text(
                            text = "$titleText (${state.pvCapacityKwp} kWp Anlage)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        val subText = if (selectedDayTab == 1) {
                            "Globalstrahlung: $solarSum kWh/m² • Temp: ${activeForecast.minTemp}–${activeForecast.maxTemp} °C"
                        } else {
                            "Globalstrahlung: $solarSum kWh/m² • Gemessene Werte (Tagesbilanz)"
                        }
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }

                // Hourly Solar Curve Chart
                val hourly = activeForecast.hourlySolar
                if (hourly.size >= 4) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (selectedDayTab == 1) {
                            "Erwarteter Sonnenverlauf morgen (06:00 – 20:00 Uhr):"
                        } else {
                            "Stündlicher Sonnenverlauf heute (06:00 – 20:00 Uhr):"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        val maxSolar = (hourly.maxOfOrNull { it.second } ?: 0.5).coerceAtLeast(0.1)
                        val barCount = hourly.size
                        val barSpacing = 4.dp.toPx()
                        val totalSpacing = barSpacing * (barCount - 1)
                        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(4f)

                        hourly.forEachIndexed { index, (hour, solarVal) ->
                            val heightRatio = (solarVal / maxSolar).toFloat().coerceIn(0.08f, 1f)
                            val barHeight = size.height * heightRatio
                            val left = index * (barWidth + barSpacing)
                            val top = size.height - barHeight

                            val barColor = if (solarVal > 0.05) {
                                if (selectedDayTab == 1) Color(0xFFFFB300) else Color(0xFFFFCA28)
                            } else {
                                Color(0xFF616161)
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("06:00", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("13:00 (Peak)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("20:00", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }
}

@Composable
private fun WasteCard(state: RheurdtDashboardState) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Nächste Müllabfuhr",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Bezirk: ${state.streetName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.nextWaste.isNotEmpty()) {
                    val firstDate = state.nextWaste.first().date
                    val dateLabel = firstDate.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN))
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.nextWaste.forEach { item ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(item.type.colorHex).copy(alpha = 0.2f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, Color(item.type.colorHex))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bereitstellung am Vorabend oder bis 06:00 Uhr morgens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Text("Keine anstehenden Termine geladen.")
                }
            }
        }
}

@Composable
private fun BusDeparturesCard(
    state: RheurdtDashboardState,
    onSelectBusStop: (RheurdtBusStop) -> Unit,
    onRefreshBus: () -> Unit
) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Bus-Abfahrten Rheurdt",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "VRR Live-Fahrplan",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshBus,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (state.isBusLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2E7D32)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Bus-Abfahrten aktualisieren",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stop Selection Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RheurdtBusStop.values().forEach { stop ->
                        val isSelected = stop == state.selectedBusStop
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectBusStop(stop) },
                            label = { Text(stop.shortName) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2E7D32).copy(alpha = 0.35f),
                                selectedLabelColor = Color(0xFF81C784),
                                selectedLeadingIconColor = Color(0xFF81C784)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.busDepartures.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.busDepartures.take(5).forEach { dep ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF1565C0),
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = dep.line.ifEmpty { "BUS" },
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = dep.direction,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = dep.scheduledTime,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                            val countdownStr = when {
                                                dep.countdownMinutes <= 0 -> "Jetzt"
                                                dep.countdownMinutes < 60 -> "in ${dep.countdownMinutes} Min"
                                                else -> ""
                                            }
                                            if (countdownStr.isNotEmpty()) {
                                                Text(
                                                    text = countdownStr,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (dep.countdownMinutes <= 5) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }

                                    if (!dep.hint.isNullOrEmpty() && (dep.hint.contains("TaxiBus", ignoreCase = true) || dep.hint.contains("Voranmeldung", ignoreCase = true))) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = Color(0xFFE65100),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "TaxiBus: 30 Min. Voranmeldung nötig (02841 / 88 22 444)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFE65100),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keine Abfahrten in den nächsten Stunden gemeldet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
}

@Composable
private fun VillageEventsCard(
    events: List<VillageEvent>,
    onNavigateToCalendar: () -> Unit
) {
    if (events.isEmpty()) return
    val nextEvents = events.take(3)
    Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎪", fontSize = 18.sp)
                            Text(
                                text = "Rheurdter Vereine & Dorfleben",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = onNavigateToCalendar,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Alle Termine", style = MaterialTheme.typography.labelMedium)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    nextEvents.forEach { ev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCalendar() }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(ev.category.colorHex).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = ev.category.icon,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ev.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val dateStr = ev.date.format(DateTimeFormatter.ofPattern("EEE, d. MMMM", Locale.GERMAN))
                                val timeStr = if (!ev.time.isNullOrEmpty()) "$dateStr • ${ev.time}" else dateStr
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
}

@Composable
private fun PoliticsQuickCard(
    state: RheurdtDashboardState,
    onNavigateToPolitics: () -> Unit
) {
    val nextMeeting = state.councilMeetings.firstOrNull() ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Nächste Ratssitzung Rheurdt",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = onNavigateToPolitics,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Ratsportal", style = MaterialTheme.typography.labelMedium)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPolitics() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = nextMeeting.date.format(DateTimeFormatter.ofPattern("EEE", Locale.GERMAN)).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = nextMeeting.date.format(DateTimeFormatter.ofPattern("d", Locale.GERMAN)),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nextMeeting.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${nextMeeting.time} • ${nextMeeting.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val latestRheurdtDecision = state.councilDecisions.firstOrNull { it.isRheurdtRelevant } ?: state.councilDecisions.firstOrNull()
                    if (latestRheurdtDecision != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToPolitics() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                            ) {
                                Icon(
                                    Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(6.dp).size(16.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Letzter Beschluss",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32)
                                    )
                                    if (latestRheurdtDecision.isRheurdtRelevant) {
                                        Text(
                                            text = "• 📌 Rheurdt",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                                Text(
                                    text = latestRheurdtDecision.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
}

@Composable
private fun HolidaysCard(holidays: List<HolidayItem>) {
    if (holidays.isEmpty()) return
    Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Ferien & Feiertage NRW",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    holidays.forEach { h ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = h.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            val rangeStr = if (h.startDate == h.endDate) {
                                h.startDate.format(DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN))
                            } else {
                                "${h.startDate.format(DateTimeFormatter.ofPattern("d.M.", Locale.GERMAN))} – ${h.endDate.format(DateTimeFormatter.ofPattern("d.M.", Locale.GERMAN))}"
                            }
                            Text(
                                text = rangeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
}
