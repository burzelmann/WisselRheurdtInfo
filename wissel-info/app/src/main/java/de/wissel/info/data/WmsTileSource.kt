package de.wissel.info.data

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.util.Locale

enum class MapLayerType(
    val title: String,
    val icon: String,
    val shortName: String,
    val description: String,
    val legendInfo: String,
    val defaultAlpha: Float = 0.70f
) {
    NONE(
        title = "Standardkarte",
        icon = "🗺️",
        shortName = "Basis",
        description = "Reine Straßen-, Gebäude- und Geländekarte von OpenStreetMap.",
        legendInfo = ""
    ),
    HOCHWASSER(
        title = "Hochwassergefahren (HWGK)",
        icon = "🌊",
        shortName = "Hochwasser",
        description = "Amtliche Hochwassergefahrenkarte des Landes NRW für den Rheinabschnitt Kalkar/Wissel.",
        legendInfo = "Blau/Cyan: Berechnete Überflutungsflächen & Wassertiefen bei 100-jährlichem Rheinhochwasser (HQ100)."
    ),
    BORIS(
        title = "Bodenrichtwerte (BORIS)",
        icon = "💰",
        shortName = "Bodenwerte",
        description = "Amtliche Bodenrichtwerte der Gutachterausschüsse für Bauland und Grundstücke.",
        legendInfo = "Rote Umrandung: Amtliche Bodenrichtwertzonen des Kreises Kleve. Beim Heranzoomen erscheinen die Quadratmeterpreise (€/m²) & Nutzungen (W = Wohnen, MI = Dorfgebiet, G = Gewerbe)."
    ),
    ALKIS(
        title = "Flurstücke & Kataster (ALKIS)",
        icon = "📐",
        shortName = "Flurstücke",
        description = "Amtliches Liegenschaftskataster der Vermessungsverwaltung NRW.",
        legendInfo = "Schwarze Linien: Amtliche Flurstücks- & Grundstücksgrenzen des Katasteramts. Aus Datenschutz- & Maßstabsgründen ab Zoom 16 aktiv (automatisch herangezoomt)."
    ),
    REGENRADAR(
        title = "DWD-Niederschlagsradar",
        icon = "🌧️",
        shortName = "Regenradar",
        description = "Amtlicher Live-Niederschlagsfilm des Deutschen Wetterdienstes (DWD).",
        legendInfo = "Niederschlagszonen: Zeigt aktuelle Regen- und Schauergebiete über dem Niederrhein."
    )
}

object MapOverlayManager {

    fun createTilesOverlay(
        context: Context,
        mapView: MapView?,
        layerType: MapLayerType,
        alpha: Float = 0.70f
    ): TilesOverlay? {
        if (layerType == MapLayerType.NONE) return null

        val tileSource = when (layerType) {
            MapLayerType.HOCHWASSER -> createHwgkTileSource()
            MapLayerType.BORIS -> createBorisTileSource()
            MapLayerType.ALKIS -> createAlkisTileSource()
            MapLayerType.REGENRADAR -> createDwdRadarTileSource()
            MapLayerType.NONE -> return null
        }

        val provider = MapTileProviderBasic(context, tileSource)
        mapView?.tileRequestCompleteHandler?.let { handler ->
            provider.tileRequestCompleteHandlers.add(handler)
        }

        return TilesOverlay(provider, context).apply {
            loadingBackgroundColor = android.graphics.Color.TRANSPARENT
            loadingLineColor = android.graphics.Color.TRANSPARENT

            if (alpha < 0.99f) {
                val matrix = ColorMatrix().apply {
                    setScale(1f, 1f, 1f, alpha.coerceIn(0.1f, 1.0f))
                }
                setColorFilter(ColorMatrixColorFilter(matrix))
            }
        }
    }

    private fun createHwgkTileSource(): OnlineTileSourceBase {
        return object : OnlineTileSourceBase(
            "HWGK_NRW",
            10,
            18,
            256,
            ".png",
            arrayOf("https://www.wms.nrw.de/umwelt/HW_Gefahrenkarte")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                val bbox = tileToBBox3857(x, y, zoom)
                return "https://www.wms.nrw.de/umwelt/HW_Gefahrenkarte" +
                        "?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap" +
                        "&LAYERS=Tiefen_Ueberflutungsgebiet_mw,Sonstige_Gewaesserflaechen" +
                        "&STYLES=" +
                        "&CRS=EPSG:3857" +
                        "&BBOX=$bbox" +
                        "&WIDTH=256&HEIGHT=256" +
                        "&FORMAT=image/png" +
                        "&TRANSPARENT=TRUE"
            }
        }
    }

    private fun createBorisTileSource(): OnlineTileSourceBase {
        return object : OnlineTileSourceBase(
            "BORIS_NRW",
            11,
            18,
            256,
            ".png",
            arrayOf("https://www.wms.nrw.de/boris/wms_de_bodenrichtwerte")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                val bbox = tileToBBox3857(x, y, zoom)
                return "https://www.wms.nrw.de/boris/wms_de_bodenrichtwerte" +
                        "?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap" +
                        "&LAYERS=brw_wohnbauflaeche,brw_gemischte_bauweise,brw_sonderbauflaeche,brw_gewerbliche_bauweise,brw_landwirtschaftliche_flaeche,brw_sonstige_flaechen" +
                        "&STYLES=" +
                        "&CRS=EPSG:3857" +
                        "&BBOX=$bbox" +
                        "&WIDTH=256&HEIGHT=256" +
                        "&FORMAT=image/png" +
                        "&TRANSPARENT=TRUE"
            }
        }
    }

    private fun createAlkisTileSource(): OnlineTileSourceBase {
        return object : OnlineTileSourceBase(
            "ALKIS_NRW_V4",
            14,
            20,
            256,
            ".png",
            arrayOf("https://www.wms.nrw.de/geobasis/wms_nw_alkis")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                val bbox = tileToBBox3857(x, y, zoom)
                return "https://www.wms.nrw.de/geobasis/wms_nw_alkis" +
                        "?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap" +
                        "&LAYERS=adv_alkis_flurstuecke" +
                        "&STYLES=" +
                        "&CRS=EPSG:3857" +
                        "&BBOX=$bbox" +
                        "&WIDTH=512&HEIGHT=512" +
                        "&FORMAT=image/png" +
                        "&TRANSPARENT=TRUE"
            }
        }
    }
    private fun createDwdRadarTileSource(): OnlineTileSourceBase {
        return object : OnlineTileSourceBase(
            "DWD_RADAR",
            4,
            18,
            256,
            ".png",
            arrayOf("https://maps.dwd.de/geoserver/dwd/wms")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                val bbox = tileToBBox3857(x, y, zoom)
                return "https://maps.dwd.de/geoserver/dwd/wms" +
                        "?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap" +
                        "&LAYERS=dwd:Niederschlagsradar" +
                        "&STYLES=" +
                        "&CRS=EPSG:3857" +
                        "&BBOX=$bbox" +
                        "&WIDTH=256&HEIGHT=256" +
                        "&FORMAT=image/png" +
                        "&TRANSPARENT=TRUE"
            }
        }
    }

    fun tileToBBox3857(x: Int, y: Int, zoom: Int): String {
        val originShift = 20037508.342789244
        val initialResolution = 2 * originShift / 256.0
        val resolution = initialResolution / (1 shl zoom)

        val minX = x * 256.0 * resolution - originShift
        val maxX = (x + 1) * 256.0 * resolution - originShift

        val maxY = originShift - y * 256.0 * resolution
        val minY = originShift - (y + 1) * 256.0 * resolution

        return String.format(Locale.US, "%.2f,%.2f,%.2f,%.2f", minX, minY, maxX, maxY)
    }
}
