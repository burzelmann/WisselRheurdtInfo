package de.rheurdt.info.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rheurdt.info.data.CharacteristicValue
import de.rheurdt.info.data.PegelData
import de.rheurdt.info.data.PegelPoint
import de.rheurdt.info.data.PegelStationData
import de.rheurdt.info.data.RheurdtRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PegelTimeRange(val label: String, val hours: Int) {
    HOURS_24("24 Std.", 24),
    DAYS_7("7 Tage", 7 * 24),
    DAYS_30("30 Tage", 30 * 24)
}

@Composable
fun PegelChartCard(
    pegel: PegelData?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var selectedStationCode by remember { mutableStateOf("DUISBURG") }
    var selectedRange by remember { mutableStateOf(PegelTimeRange.DAYS_30) }

    val fallbackPegel = remember { RheurdtRepository().getFallbackPegel() }

    val activeStationName = if (selectedStationCode == "DUISBURG") {
        pegel?.station ?: fallbackPegel.station
    } else {
        pegel?.weselData?.stationName ?: fallbackPegel.weselData?.stationName ?: "Wesel (km 814,0)"
    }

    val activeCurrentCm = if (selectedStationCode == "DUISBURG") {
        pegel?.currentCm ?: fallbackPegel.currentCm
    } else {
        pegel?.weselData?.currentCm ?: fallbackPegel.weselData?.currentCm ?: 82.0
    }

    val activeTrend = if (selectedStationCode == "DUISBURG") {
        pegel?.trend ?: fallbackPegel.trend
    } else {
        pegel?.weselData?.trend ?: fallbackPegel.weselData?.trend ?: "gleichbleibend"
    }

    val activeDiff24h = if (selectedStationCode == "DUISBURG") {
        pegel?.diffToYesterdayCm ?: fallbackPegel.diffToYesterdayCm
    } else {
        pegel?.weselData?.diffToYesterdayCm ?: fallbackPegel.weselData?.diffToYesterdayCm ?: 0.0
    }

    val activeTimestamp = if (selectedStationCode == "DUISBURG") {
        pegel?.timestamp ?: fallbackPegel.timestamp
    } else {
        pegel?.weselData?.timestamp ?: fallbackPegel.weselData?.timestamp ?: "Aktuell"
    }

    val activeCharacteristics = if (selectedStationCode == "DUISBURG") {
        if (!pegel?.characteristics.isNullOrEmpty()) pegel!!.characteristics
        else fallbackPegel.characteristics
    } else {
        if (!pegel?.weselData?.characteristics.isNullOrEmpty()) pegel!!.weselData!!.characteristics
        else fallbackPegel.weselData?.characteristics ?: emptyList()
    }

    val activeHistoryAll = remember(pegel, selectedStationCode) {
        if (selectedStationCode == "DUISBURG") {
            if (!pegel?.history30d.isNullOrEmpty()) pegel!!.history30d
            else fallbackPegel.history30d
        } else {
            if (!pegel?.weselData?.history30d.isNullOrEmpty()) pegel!!.weselData!!.history30d
            else fallbackPegel.weselData?.history30d ?: fallbackPegel.history30d
        }
    }

    // Filter time series according to selected range
    val filteredHistory = remember(activeHistoryAll, selectedRange, selectedStationCode) {
        if (activeHistoryAll.isEmpty()) fallbackPegel.history30d
        else {
            val maxEpoch = activeHistoryAll.maxOfOrNull { it.epochSeconds } ?: (System.currentTimeMillis() / 1000L)
            val minEpoch = maxEpoch - (selectedRange.hours * 3600L)
            val inRange = activeHistoryAll.filter { it.epochSeconds >= minEpoch }

            val sampled = when (selectedRange) {
                PegelTimeRange.HOURS_24 -> inRange
                PegelTimeRange.DAYS_7 -> inRange.filterIndexed { idx, _ -> idx % 4 == 0 || idx == inRange.lastIndex }
                PegelTimeRange.DAYS_30 -> inRange.filterIndexed { idx, _ -> idx % 8 == 0 || idx == inRange.lastIndex }
            }
            if (sampled.size >= 2) sampled else activeHistoryAll.takeLast(24)
        }
    }

    // Statistics in selected range
    val minVal = filteredHistory.minOfOrNull { it.value } ?: activeCurrentCm
    val maxVal = filteredHistory.maxOfOrNull { it.value } ?: activeCurrentCm
    val avgVal = if (filteredHistory.isNotEmpty()) {
        (filteredHistory.sumOf { it.value } / filteredHistory.size * 10).toInt() / 10.0
    } else activeCurrentCm

    val mwChar = activeCharacteristics.firstOrNull { it.shortname == "MW" }?.value ?: if (selectedStationCode == "DUISBURG") 394.0 else 280.0
    val mnwChar = activeCharacteristics.firstOrNull { it.shortname == "MNW" }?.value ?: if (selectedStationCode == "DUISBURG") 201.0 else 100.0
    val glwChar = activeCharacteristics.firstOrNull { it.shortname == "GlW" }?.value ?: if (selectedStationCode == "DUISBURG") 227.0 else 174.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF0288D1).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌊", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = "Rheinpegel & Zeitreihen",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "PegelOnline WSV • $activeTimestamp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Expand / Collapse Chevron button
                FilledTonalIconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Einklappen" else "Ausklappen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Station Selector Chips (Duisburg-Ruhrort vs. Wesel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStationCode == "DUISBURG",
                    onClick = { selectedStationCode = "DUISBURG" },
                    label = { Text("Duisburg-Ruhrort (km 780,8)", fontSize = 12.sp) },
                    leadingIcon = if (selectedStationCode == "DUISBURG") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedStationCode == "WESEL",
                    onClick = { selectedStationCode = "WESEL" },
                    label = { Text("Wesel (km 814,0)", fontSize = 12.sp) },
                    leadingIcon = if (selectedStationCode == "WESEL") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Metric Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${activeCurrentCm.toInt()} cm",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val trendColor = when (activeTrend) {
                            "steigend" -> Color(0xFF4CAF50)
                            "fallend" -> Color(0xFFFF9800)
                            else -> Color(0xFF0288D1)
                        }
                        val trendIcon = when (activeTrend) {
                            "steigend" -> "↗"
                            "fallend" -> "↘"
                            else -> "→"
                        }
                        Text(
                            text = "$trendIcon $activeTrend",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = trendColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = "24h-Änderung: ${if (activeDiff24h > 0) "+$activeDiff24h" else "$activeDiff24h"} cm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Badge distance to MW
                val diffToMw = (activeCurrentCm - mwChar).toInt()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (diffToMw < 0) Color(0xFFFFF3E0) else Color(0xFFE1F5FE),
                    border = BorderStroke(1.dp, if (diffToMw < 0) Color(0xFFFFB74D) else Color(0xFF81D4FA))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (diffToMw < 0) "$diffToMw cm" else "+$diffToMw cm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (diffToMw < 0) Color(0xFFE65100) else Color(0xFF0277BD)
                        )
                        Text(
                            text = if (diffToMw < 0) "unter MW" else "über MW",
                            fontSize = 10.sp,
                            color = if (diffToMw < 0) Color(0xFFE65100) else Color(0xFF0277BD)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeframe Selector Chips (24h / 7d / 30d)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PegelTimeRange.values().forEach { range ->
                    val isSelected = selectedRange == range
                    Surface(
                        onClick = { selectedRange = range },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = range.label,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Chart Canvas with Touch Scrubbing
            InteractivePegelCanvas(
                points = filteredHistory,
                currentCm = activeCurrentCm,
                mw = mwChar,
                mnw = mnwChar,
                glw = glwChar,
                range = selectedRange,
                isExpanded = isExpanded,
                modifier = Modifier.fillMaxWidth()
            )

            // Collapsed Quick-Summary or Expanded Detailed Stats
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zeitraum min: ${minVal.toInt()} cm • max: ${maxVal.toInt()} cm • MW: ${mwChar.toInt()} cm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "30-Tage Analyse ▸",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Expanded Details Section
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        // 3 Stats Summary Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                title = "Tiefststand",
                                value = "${minVal.toInt()} cm",
                                icon = "📉",
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "Mittelwert",
                                value = "${avgVal.toInt()} cm",
                                icon = "📊",
                                color = Color(0xFF0288D1),
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "Höchststand",
                                value = "${maxVal.toInt()} cm",
                                icon = "📈",
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Official Characteristic Water Levels Info
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Amtliche Hauptwerte $activeStationName",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val histTiefst = if (selectedStationCode == "DUISBURG") "153 cm (23.10.2018)" else "100 cm"
                                Text(
                                    text = "• Mittelwasser (MW): ${mwChar.toInt()} cm (langjähriger Durchschnitt)\n" +
                                            "• Mittleres Niedrigwasser (MNW): ${mnwChar.toInt()} cm\n" +
                                            "• Gleichwertiger Wasserstand (GlW): ${glwChar.toInt()} cm\n" +
                                            "• Historischer Tiefstwert: $histTiefst",
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Link to official ELWAS NRW portal
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.elwasweb.nrw.de/")
                                    )
                                    context.startActivity(intent)
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("💧", fontSize = 16.sp)
                                    Text(
                                        text = "Grundwasserpegel NRW (ELWAS) ↗",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Das Land NRW stellt hydrologische Grundwasserdaten primär als periodische OpenData-Kataloge bereit. Interaktive Messstellenkarten für Rheurdt und den Niederrhein sind über das amtliche Fachportal ELWAS-WEB abrufbar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 17.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun InteractivePegelCanvas(
    points: List<PegelPoint>,
    currentCm: Double,
    mw: Double,
    mnw: Double,
    glw: Double,
    range: PegelTimeRange,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Box(
            modifier = modifier
                .height(110.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Lade Zeitreihen-Messdaten...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF0288D1))
        }
        return
    }

    var touchX by remember { mutableStateOf<Float?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val minV = points.minOf { it.value }
    val maxV = points.maxOf { it.value }

    // Pad range so line has breathing space
    val yMin = (minV - 2.0).coerceAtLeast(-50.0)
    val yMax = (maxV + 4.0).coerceAtLeast(yMin + 5.0)
    val valRange = yMax - yMin

    val chartHeight = if (isExpanded) 175.dp else 115.dp

    Column(modifier = modifier) {
        // Inspection Tooltip Bubble when dragging
        if (touchX != null && selectedIndex != null && selectedIndex in points.indices) {
            val pt = points[selectedIndex!!]
            val dateStr = runCatching {
                val zdt = Instant.ofEpochSecond(pt.epochSeconds).atZone(ZoneId.of("Europe/Berlin"))
                zdt.format(DateTimeFormatter.ofPattern("EEE, d. MMM • HH:mm 'Uhr'", Locale.GERMAN))
            }.getOrDefault(pt.timestamp)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0288D1),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = "${pt.value.toInt()} cm (${(pt.value - mw).toInt()} cm zu MW)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Dedicated Chart Container Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures(
                            onPress = { offset ->
                                touchX = offset.x
                                val step = size.width / (points.size - 1).coerceAtLeast(1)
                                selectedIndex = (offset.x / step).toInt().coerceIn(0, points.lastIndex)
                                tryAwaitRelease()
                                touchX = null
                                selectedIndex = null
                            }
                        )
                    }
                    .pointerInput(points) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                touchX = offset.x
                                val step = size.width / (points.size - 1).coerceAtLeast(1)
                                selectedIndex = (offset.x / step).toInt().coerceIn(0, points.lastIndex)
                            },
                            onDrag = { change, _ ->
                                touchX = change.position.x
                                val step = size.width / (points.size - 1).coerceAtLeast(1)
                                selectedIndex = (change.position.x / step).toInt().coerceIn(0, points.lastIndex)
                            },
                            onDragEnd = {
                                touchX = null
                                selectedIndex = null
                            },
                            onDragCancel = {
                                touchX = null
                                selectedIndex = null
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val pointCount = points.size
                val stepX = w / (pointCount - 1).coerceAtLeast(1)

                // Coordinate mapping function
                fun getY(v: Double): Float {
                    val normalized = 1f - ((v - yMin) / valRange).toFloat()
                    return (normalized * (h - 24f) + 12f).coerceIn(4f, h - 4f)
                }

                val gridDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // 3 Subtle horizontal grid lines (at 25%, 50%, 75% height)
                for (factor in listOf(0.25f, 0.5f, 0.75f)) {
                    val gy = h * factor
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, gy),
                        end = Offset(w, gy),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = gridDash
                    )
                }

                // MNW Line if visible
                if (mnw in yMin..yMax) {
                    val mnwY = getY(mnw)
                    drawLine(
                        color = Color(0xFFFF9800).copy(alpha = 0.6f),
                        start = Offset(0f, mnwY),
                        end = Offset(w, mnwY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = gridDash
                    )
                }

                // Build curve path
                val path = Path()
                val fillPath = Path()

                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = getY(pt.value)
                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(w, h)
                fillPath.close()

                // Draw area gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00B0FF).copy(alpha = 0.35f),
                            Color(0xFF00B0FF).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )

                // Draw curve line (vibrant blue, high contrast)
                drawPath(
                    path = path,
                    color = Color(0xFF29B6F6),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw end dot for latest measurement
                val lastY = getY(points.last().value)
                drawCircle(
                    color = Color(0xFF00B0FF).copy(alpha = 0.35f),
                    radius = 8.dp.toPx(),
                    center = Offset(w, lastY)
                )
                drawCircle(
                    color = Color(0xFF29B6F6),
                    radius = 4.5.dp.toPx(),
                    center = Offset(w, lastY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(w, lastY)
                )

                // Handle touch scrubbing indicator
                touchX?.let { tx ->
                    val clampedX = tx.coerceIn(0f, w)
                    val idx = ((clampedX / stepX).toInt()).coerceIn(0, pointCount - 1)
                    val pt = points[idx]
                    val exactX = idx * stepX
                    val exactY = getY(pt.value)

                    // Vertical cursor hairline
                    drawLine(
                        color = Color(0xFF29B6F6).copy(alpha = 0.8f),
                        start = Offset(exactX, 0f),
                        end = Offset(exactX, h),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Outer halo & highlighted dot
                    drawCircle(
                        color = Color(0xFF00B0FF).copy(alpha = 0.4f),
                        radius = 10.dp.toPx(),
                        center = Offset(exactX, exactY)
                    )
                    drawCircle(
                        color = Color(0xFF0288D1),
                        radius = 5.dp.toPx(),
                        center = Offset(exactX, exactY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(exactX, exactY)
                    )
                }
            }
        }

        // Bottom Date/Time Axis Labels
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val firstTs = points.first().epochSeconds
            val lastTs = points.last().epochSeconds
            val formatter = when (range) {
                PegelTimeRange.HOURS_24 -> DateTimeFormatter.ofPattern("HH:mm")
                PegelTimeRange.DAYS_7 -> DateTimeFormatter.ofPattern("EEE, d.M.", Locale.GERMAN)
                PegelTimeRange.DAYS_30 -> DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)
            }
            val startStr = Instant.ofEpochSecond(firstTs).atZone(ZoneId.of("Europe/Berlin")).format(formatter)
            val midStr = Instant.ofEpochSecond((firstTs + lastTs) / 2).atZone(ZoneId.of("Europe/Berlin")).format(formatter)
            val endStr = Instant.ofEpochSecond(lastTs).atZone(ZoneId.of("Europe/Berlin")).format(formatter)

            Text(startStr, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(midStr, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(endStr, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
