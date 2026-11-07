package de.wissel.info.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wissel.info.data.UserPreferences
import de.wissel.info.data.WisselDashboardState

@Composable
fun SettingsScreen(
    state: WisselDashboardState,
    onStreetChanged: (String) -> Unit,
    onPvCapacityChanged: (Double) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onSendTestNotification: (Context) -> Unit,
    onTogglePoliticsMeetingNotification: (Boolean) -> Unit = {},
    onTogglePoliticsDecisionNotification: (Boolean) -> Unit = {},
    onSendTestPoliticsMeeting: (Context) -> Unit = {},
    onSendTestPoliticsDecision: (Context) -> Unit = {},
    onToggleSevereAlertsNotification: (Boolean) -> Unit = {},
    onTogglePegelAlertsNotification: (Boolean) -> Unit = {},
    onSendTestSevereAlert: (Context) -> Unit = {},
    onSendTestPegelAlert: (Context) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showStreetDialog by remember { mutableStateOf(false) }
    var streetSearchQuery by remember { mutableStateOf("") }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Einstellungen & Infos",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // 1. Street in Wissel Selection Card
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Meine Straße in Wissel",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.streetName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "47546 Kalkar-Wissel • Abfuhrbezirk",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Button(
                        onClick = {
                            streetSearchQuery = ""
                            showStreetDialog = true
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ändern")
                    }
                }
            }
        }

        // 2. PV Plant Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SolarPower,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Photovoltaik-Anlage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Konfigurierte Generatorleistung: ${state.pvCapacityKwp} kWp",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Dient zur Berechnung der täglichen Ertragsprognose auf der Startseite.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val newVal = (state.pvCapacityKwp - 1.0).coerceAtLeast(1.0)
                            onPvCapacityChanged(newVal)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("- 1 kWp")
                    }
                    Button(
                        onClick = { onPvCapacityChanged(10.0) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("10.0 kWp")
                    }
                    OutlinedButton(
                        onClick = {
                            val newVal = (state.pvCapacityKwp + 1.0).coerceAtMost(30.0)
                            onPvCapacityChanged(newVal)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ 1 kWp")
                    }
                }
            }
        }

        // 3. Notifications Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Benachrichtigungen & Alarmierung",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // 1. Müll-Erinnerung
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Müll-Erinnerung",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Am Vorabend um 19:00 Uhr erinnern",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Switch(
                        checked = state.wasteNotificationEnabled,
                        onCheckedChange = onToggleNotification
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 2. Ratssitzungen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Ratssitzungen ankündigen",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "24 Stunden vor Sitzungsbeginn (18:00 Uhr am Vortag)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Switch(
                        checked = state.politicsMeetingNotificationEnabled,
                        onCheckedChange = onTogglePoliticsMeetingNotification
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 3. Neue Beschlüsse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Neue Beschlüsse mit Wissel-Bezug",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Sofort-Benachrichtigung bei neuen RIS-Drucksachen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Switch(
                        checked = state.politicsDecisionNotificationEnabled,
                        onCheckedChange = onTogglePoliticsDecisionNotification
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 4. Unwetter & Katastrophenschutz
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Unwetter & Katastrophenschutz",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "DWD-Unwetterwarnungen & BBK MoWaS Gefahrendurchsagen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Switch(
                        checked = state.severeAlertsNotificationEnabled,
                        onCheckedChange = onToggleSevereAlertsNotification
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 5. Rheinpegel & Hochwasser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Rheinpegel Rees: Hochwassermarken",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Meldung bei Überschreitung von Marke I (6,50 m) und Marke II (8,00 m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Switch(
                        checked = state.pegelAlertsNotificationEnabled,
                        onCheckedChange = onTogglePegelAlertsNotification
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Test-Benachrichtigungen auf dem Pixel 9 auslösen:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Row 1 Test Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            onSendTestNotification(context)
                            Toast.makeText(context, "Müll-Test gesendet!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("🗑️ Müll", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = {
                            onSendTestPoliticsMeeting(context)
                            Toast.makeText(context, "Sitzungs-Test gesendet!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("🏛️ Sitzung", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = {
                            onSendTestPoliticsDecision(context)
                            Toast.makeText(context, "Beschluss-Test gesendet!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("📜 Beschluss", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Row 2 Test Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            onSendTestSevereAlert(context)
                            Toast.makeText(context, "Unwetter-Test gesendet!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("⚡ Unwetter", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = {
                            onSendTestPegelAlert(context)
                            Toast.makeText(context, "Pegel-Test gesendet!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("🌊 Rheinpegel", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 4. Data Sources Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Aktive Datenquellen & Lizenzen",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                val sources = listOf(
                    "DWD Wetter & Strahlung" to "Deutscher Wetterdienst (DWD) über Bright Sky API (dl-de/by-2-0) • Station Kalkar (4,9 km)",
                    "WSV Rheinpegel Rees" to "Wasserstraßen- und Schifffahrtsverwaltung des Bundes (WSV), bereitgestellt durch PEGELONLINE (dl-de/by-2-0)",
                    "BNetzA Marktstammdatenregister" to "Bundesnetzagentur (MaStR) Open Data (dl-de/by-2-0) • Aggregierte Solarstammdaten Kalkar/Wissel",
                    "MoWaS / NINA Warnsystem" to "Bundesamt für Bevölkerungsschutz und Katastrophenhilfe (BBK) (dl-de/by-2-0) • Kreis Kleve",
                    "ÖPNV Abfahrtsmonitor" to "Verkehrsverbund Rhein-Ruhr (VRR AöR) / EFA Fahrplanauskunft",
                    "Karten & Katasterdaten" to "OpenStreetMap Contributors (ODbL 1.0) & Geobasis NRW (ALKIS dl-de/zero-2-0, BORIS & HWGK dl-de/by-2-0)",
                    "Ratsinformationen (RIS)" to "Stadt Kalkar / SD.NET (§ 5 UrhG amtliche Veröffentlichungen)",
                    "Abfallkalender" to "Amtlicher Abfallkalender der Stadt Kalkar / Entsorgungsbetriebe Schönmackers & Drekopf",
                    "Schulferien & Feiertage" to "OpenHolidays REST-API • Land NRW"
                )

                sources.forEach { (title, desc) ->
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // 5. Legal & Privacy Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Rechtliches & Datenschutz",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Transparenz, Haftungsausschluss und Datenschutzhinweise nach DSGVO & Digitale-Dienste-Gesetz (DDG).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDisclaimerDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Haftung", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Datenschutz", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Impressum", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Text(
            text = "Wissel Info • Version 2.2.0 • Maßgeschneidert für Wissel am Niederrhein",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Street selection dialog with Live Search
    if (showStreetDialog) {
        val query = streetSearchQuery.trim()
        val allList = UserPreferences.ALL_STREETS
        val wisselKnown = UserPreferences.WISSEL_KNOWN_STREETS

        val filteredList = remember(query) {
            if (query.isBlank()) {
                // Show Wissel known streets first, then other streets
                val wisselPart = allList.filter { it in wisselKnown }
                val otherPart = allList.filter { it !in wisselKnown }
                wisselPart + otherPart
            } else {
                allList.filter { it.contains(query, ignoreCase = true) }
            }
        }

        AlertDialog(
            onDismissRequest = { showStreetDialog = false },
            title = {
                Column {
                    Text("Straße auswählen", fontWeight = FontWeight.Bold)
                    Text(
                        text = "348 amtliche Straßen im Verzeichnis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = streetSearchQuery,
                        onValueChange = { streetSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Straße suchen (z. B. Dünen...)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (streetSearchQuery.isNotBlank()) {
                                IconButton(onClick = { streetSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Löschen")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        // Custom manual input option if query has no exact match
                        if (query.isNotBlank() && !allList.any { it.equals(query, ignoreCase = true) }) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onStreetChanged(query)
                                            showStreetDialog = false
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = "Als eigene Straße übernehmen: \"$query\"",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        items(filteredList) { street ->
                            val isSelected = street == state.streetName
                            val isWissel = street in wisselKnown

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onStreetChanged(street)
                                        showStreetDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isWissel) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Wissel",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        text = street,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStreetDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }

    // Legal Dialogs
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Haftungsausschluss", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Inoffizielles Bürgerangebot",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Wissel Info ist ein unabhängiges, nicht-kommerzielles und ehrenamtliches Informationsprojekt für Bürgerinnen und Bürger in Wissel und Kalkar. Die App ist keine offizielle Anwendung der Stadt Kalkar oder einer sonstigen Behörde.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "2. Keine Gewähr für Daten & Prognosen",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sämtliche Messwerte (Rheinpegel, Wetter, Strahlung), Hochwasserrisikokarten, Unwetterwarnungen, Solarertragsprognosen, Ratsbeschlüsse und Abfahrtszeiten stammen aus frei zugänglichen Schnittstellen (Open Data). Trotz sorgfältigster Einbindung wird keine Haftung für Richtigkeit, Vollständigkeit oder ständige Verfügbarkeit übernommen.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "3. Notfall- & Sicherheitslage",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Bei akuten Hochwasserlagen, Unwettern oder behördlichen Katastrophenwarnungen sind ausschließlich die offiziellen Weisungen und Durchsagen der Katastrophenschutzbehörden, Rettungsleitstellen und der Stadt Kalkar verbindlich.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("Verstanden")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Datenschutzerklärung (DSGVO)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. 100 % Lokale Datenspeicherung",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ihre ausgewählte Wohnstraße, Ihre PV-Anlagengröße sowie Benachrichtigungseinstellungen werden ausschließlich lokal in den privaten App-Einstellungen (SharedPreferences) Ihres Smartphones gespeichert. Es gibt keinen zentralen App-Server und keine Nutzerkonten.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "2. Abruf externer Schnittstellen",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Zum Laden der aktuellen Daten (Wetter, Pegel, ÖPNV, OpenStreetMap) baut die App verschlüsselte HTTPS-Verbindungen zu den jeweiligen öffentlichen Schnittstellen (DWD, WSV, VRR, OpenStreetMap, Land NRW) auf. Dabei wird technisch bedingt Ihre IP-Adresse an den jeweiligen Server übertragen. Die App übermittelt dabei keinerlei persönliche Daten.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "3. Kein Tracking & keine Werbung",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Die App enthält weder Analyse-Frameworks (z. B. Google Analytics oder Firebase Tracking) noch Werbenetzwerke oder Tracking-Pixel. Ihre Nutzung bleibt vollkommen anonym.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Impressum & Information", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Wissel Info App",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Version 2.2.0 • Stand Oktober 2026",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Herausgeber & Entwicklung:\nPrivates Nachbarschaftsprojekt für Wissel am Niederrhein zur Förderung des Dorflebens und digitaler Bürgertransparenz.\n\nKontakt & Feedback:\nE-Mail: kontakt@wissel-info.de\n\nRechtliche Grundlagen:\nNutzung offener Behördendaten gem. § 12a EGovG (Open Data Gesetz) und GeoZG.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }
}
