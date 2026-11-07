package de.wissel.info.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.wissel.info.data.BusDeparture
import de.wissel.info.data.MapLayerType
import de.wissel.info.data.MapOverlayManager
import de.wissel.info.data.PoiCategory
import de.wissel.info.data.WisselBusStop
import de.wissel.info.data.WisselDashboardState
import de.wissel.info.data.WisselPoi
import de.wissel.info.data.WisselPois
import de.wissel.info.data.WisselRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    state: WisselDashboardState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<PoiCategory?>(null) }
    var showRegional by remember { mutableStateOf(false) } // false = nur Wissel, true = inkl. Rees/Emmerich
    var selectedPoi by remember { mutableStateOf<WisselPoi?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // WMS & Radar Layer State
    var selectedLayer by remember { mutableStateOf(MapLayerType.NONE) }
    var layerAlpha by remember { mutableFloatStateOf(0.70f) }
    
    
    var currentTilesOverlay by remember { mutableStateOf<TilesOverlay?>(null) }
    val markerList = remember { mutableStateListOf<Marker>() }



    val allPois = remember(state.pegelRees?.currentCm) {
        WisselPois.getPois(state.pegelRees?.currentCm)
    }

    val filteredPois = remember(selectedCategory, showRegional, allPois) {
        allPois.filter { poi ->
            val categoryMatches = selectedCategory == null || poi.category == selectedCategory
            val regionalMatches = showRegional || !poi.isRegional
            categoryMatches && regionalMatches
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = File(context.cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(context.cacheDir, "osm_tiles")
    }

    // Handle Active Tile Layer Transitions
    LaunchedEffect(selectedLayer, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect

        // Remove old overlay
        currentTilesOverlay?.let { prev ->
            mv.overlays.remove(prev)
            prev.onDetach(mv)
        }

        if (selectedLayer != MapLayerType.NONE) {
            // Auto-zoom for detail layers if camera is too far out
            if (selectedLayer == MapLayerType.ALKIS && mv.zoomLevelDouble < 16.2) {
                mv.controller.setZoom(16.8)
            } else if (selectedLayer == MapLayerType.BORIS && mv.zoomLevelDouble < 15.0) {
                mv.controller.setZoom(15.5)
            }

            val newOverlay = MapOverlayManager.createTilesOverlay(
                context = context,
                mapView = mv,
                layerType = selectedLayer,
                alpha = layerAlpha
            )
            currentTilesOverlay = newOverlay
            if (newOverlay != null) {
                // Insert underneath markers (index 0)
                mv.overlays.add(0, newOverlay)
                mv.invalidate()
            }
        } else {
            currentTilesOverlay = null
            mv.invalidate()
        }
    }

    // Handle Opacity Slider without resetting tiles
    LaunchedEffect(layerAlpha) {
        currentTilesOverlay?.let { overlay ->
            val matrix = ColorMatrix().apply {
                setScale(1f, 1f, 1f, layerAlpha.coerceIn(0.1f, 1.0f))
            }
            overlay.setColorFilter(ColorMatrixColorFilter(matrix))
            mapViewRef?.invalidate()
        }
    }

    // Handle Marker Updates
    LaunchedEffect(filteredPois, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect

        // Remove existing markers
        markerList.forEach { mv.overlays.remove(it) }
        markerList.clear()

        filteredPois.forEach { poi ->
            val marker = Marker(mv).apply {
                position = GeoPoint(poi.lat, poi.lon)
                title = poi.title
                snippet = poi.subtitle
                icon = createCustomMarker(context, poi)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    selectedPoi = poi
                    true
                }
            }
            markerList.add(marker)
            mv.overlays.add(marker)
        }
        mv.invalidate()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(14.8)
                    controller.setCenter(GeoPoint(51.7715, 6.2880)) // Exakt Dorfmitte Wissel
                    mapViewRef = this
                }
            }
        )

        // Top Controls: Category & Layer Chips
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Thematic Overlays (WMS / Geodaten & Radar)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ebene:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    MapLayerType.values().forEach { layer ->
                        val isSelected = selectedLayer == layer
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedLayer = if (isSelected && layer != MapLayerType.NONE) {
                                    MapLayerType.NONE
                                } else {
                                    layer
                                }
                            },
                            label = {
                                Text(
                                    text = "${layer.icon} ${layer.shortName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Row 2: POI Categories & Regional Switch
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Alle POIs", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedCategory == PoiCategory.KULTUR,
                        onClick = { selectedCategory = if (selectedCategory == PoiCategory.KULTUR) null else PoiCategory.KULTUR },
                        label = { Text("🏛️ Ort", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedCategory == PoiCategory.NATUR,
                        onClick = { selectedCategory = if (selectedCategory == PoiCategory.NATUR) null else PoiCategory.NATUR },
                        label = { Text("🌳 Natur", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedCategory == PoiCategory.HALTESTELLE,
                        onClick = { selectedCategory = if (selectedCategory == PoiCategory.HALTESTELLE) null else PoiCategory.HALTESTELLE },
                        label = { Text("🚌 Bus", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = showRegional,
                        onClick = {
                            showRegional = !showRegional
                            if (showRegional) {
                                mapViewRef?.controller?.setZoom(12.5)
                                mapViewRef?.controller?.setCenter(GeoPoint(51.765, 6.350))
                            } else {
                                mapViewRef?.controller?.setZoom(14.8)
                                mapViewRef?.controller?.setCenter(GeoPoint(51.7715, 6.2880))
                            }
                        },
                        label = {
                            Text(
                                if (showRegional) "📍 Nur Wissel" else "↔ Regional",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }

        // Active Layer Info & Transparency Slider (Floating at bottom if a layer is active)
        if (selectedLayer != MapLayerType.NONE && selectedPoi == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = selectedLayer.icon,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Column {
                                Text(
                                    text = selectedLayer.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Amtlicher Dienst • OpenData NRW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { selectedLayer = MapLayerType.NONE },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ebene schließen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (selectedLayer.legendInfo.isNotEmpty()) {
                        Text(
                            text = selectedLayer.legendInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }

                    // Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Deckkraft:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = layerAlpha,
                            onValueChange = { layerAlpha = it },
                            valueRange = 0.2f..1.0f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(layerAlpha * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    
                }
            }
        }

                // Attribution Badge (OpenStreetMap ODbL & Geobasis NRW compliance)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = if (selectedLayer != MapLayerType.NONE && selectedPoi == null) 140.dp else 16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = "© OpenStreetMap • Geobasis NRW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Map Center Floating Action Button (to quickly return to Wissel village center)
        FloatingActionButton(
            onClick = {
                mapViewRef?.controller?.setZoom(14.8)
                mapViewRef?.controller?.animateTo(GeoPoint(51.7715, 6.2880))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedLayer != MapLayerType.NONE && selectedPoi == null) 140.dp else 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Zentrieren auf Wissel"
            )
        }

        // POI Bottom Sheet
        selectedPoi?.let { poi ->
            ModalBottomSheet(
                onDismissRequest = { selectedPoi = null },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(poi.category.colorHex).copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = poi.category.icon, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = poi.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = poi.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = poi.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )

                    val matchedStop = when (poi.id) {
                        "bus_schule" -> WisselBusStop.GRUNDSCHULE
                        "bus_molkerei" -> WisselBusStop.MOLKEREI
                        "bus_ortseingang" -> WisselBusStop.ORTSEINGANG
                        else -> null
                    }

                    var stopDepartures by remember(poi.id) { mutableStateOf<List<BusDeparture>?>(null) }
                    var isStopLoading by remember(poi.id) { mutableStateOf(false) }

                    LaunchedEffect(matchedStop) {
                        if (matchedStop != null) {
                            isStopLoading = true
                            val deps = withContext(Dispatchers.IO) {
                                runCatching { WisselRepository().fetchBusDepartures(matchedStop) }.getOrDefault(emptyList())
                            }
                            stopDepartures = deps
                            isStopLoading = false
                        }
                    }

                    if (matchedStop != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nächste VRR-Abfahrten",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (isStopLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(4.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (!stopDepartures.isNullOrEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                stopDepartures!!.take(3).forEach { dep ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF1565C0)
                                            ) {
                                                Text(
                                                    text = dep.line.ifEmpty { "BUS" },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                            Text(
                                                text = dep.direction,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                maxLines = 1
                                            )
                                        }
                                        val countdown = when {
                                            dep.countdownMinutes <= 0 -> "Jetzt"
                                            dep.countdownMinutes < 60 -> "${dep.countdownMinutes} min"
                                            else -> dep.scheduledTime
                                        }
                                        Text(
                                            text = countdown,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Keine anstehenden Abfahrten.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val uri = Uri.parse("geo:${poi.lat},${poi.lon}?q=${poi.lat},${poi.lon}(${Uri.encode(poi.title)})")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Directions, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("In Google Maps öffnen")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// Generate a crisp, colored pin drawable with category icon
private fun createCustomMarker(context: Context, poi: WisselPoi): Drawable {
    val sizePx = 96
    val bitmap = Bitmap.createBitmap(sizePx, sizePx + 16, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = poi.category.colorHex.toInt()
        style = Paint.Style.FILL
    }

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    val radius = sizePx / 2f
    val centerX = radius
    val centerY = radius

    // Pin body (circle)
    canvas.drawCircle(centerX, centerY, radius - 4f, paint)
    canvas.drawCircle(centerX, centerY, radius - 4f, strokePaint)

    // Pin pointer triangle at bottom
    val path = Path().apply {
        moveTo(centerX - 16f, centerY + 18f)
        lineTo(centerX + 16f, centerY + 18f)
        lineTo(centerX, sizePx + 12f)
        close()
    }
    canvas.drawPath(path, paint)

    // Emoji icon inside circle
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }
    val yOffset = ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(poi.category.icon, centerX, centerY - yOffset, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
