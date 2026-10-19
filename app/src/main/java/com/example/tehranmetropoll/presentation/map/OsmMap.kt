package com.example.tehranmetropoll.presentation.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.domain.model.LatLng
import com.example.tehranmetropoll.domain.model.MetroNetwork
import com.example.tehranmetropoll.domain.model.Station
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import kotlin.math.hypot

object MetroTiles {
    private const val ATTRIBUTION = "© OpenStreetMap contributors © CARTO"
    private fun carto(name: String, style: String) = XYTileSource(
        name, 3, 20, 256, "@2x.png",
        arrayOf("a", "b", "c", "d").map { "https://$it.basemaps.cartocdn.com/$style/" }.toTypedArray(),
        ATTRIBUTION,
    )

    val Light = carto("CartoPositron", "light_all")
    val Dark = carto("CartoDarkMatter", "dark_all")
}

val TEHRAN_CENTER = GeoPoint(35.7000, 51.4000)

fun LatLng.toGeoPoint() = GeoPoint(lat, lng)

/**
 * Compose wrapper around an osmdroid [MapView] that follows the screen lifecycle.
 *
 * Street tiles are the only part of the app that needs the internet. Offline, the map stops
 * requesting tiles, shows any it cached earlier and paints the rest in the theme background, so
 * the metro lines and stations drawn on top (bundled data) form a plain schematic map.
 */
@Composable
fun OsmMap(
    darkTiles: Boolean,
    online: Boolean,
    modifier: Modifier = Modifier,
    onReady: (MapView) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            isVerticalMapRepetitionEnabled = false
            isHorizontalMapRepetitionEnabled = false
            minZoomLevel = 9.0
            maxZoomLevel = 19.0
            setScrollableAreaLimitDouble(BoundingBox(36.2, 51.9, 35.2, 50.6))
            controller.setZoom(11.5)
            controller.setCenter(TEHRAN_CENTER)
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val emptyTileColor = MaterialTheme.colorScheme.background.toArgb()
    LaunchedEffect(darkTiles, emptyTileColor) {
        mapView.setTileSource(if (darkTiles) MetroTiles.Dark else MetroTiles.Light)
        mapView.overlayManager.tilesOverlay.apply {
            loadingBackgroundColor = emptyTileColor
            loadingLineColor = emptyTileColor
        }
        mapView.invalidate()
    }

    LaunchedEffect(online) {
        mapView.setUseDataConnection(online)
        mapView.invalidate()
    }

    LaunchedEffect(mapView) { onReady(mapView) }

    AndroidView(factory = { mapView }, modifier = modifier)
}

fun MapView.dp(value: Float): Float = value * resources.displayMetrics.density

/** Thin translucent strokes for the whole network, giving context beneath the highlighted route. */
fun MapView.addNetworkLines(network: MetroNetwork, alpha: Int, widthDp: Float) {
    network.lines.forEach { line ->
        val polyline = Polyline(this).apply {
            setPoints(line.stationIds.map { network.station(it).location.toGeoPoint() })
            outlinePaint.apply {
                color = (line.colorArgb.toInt() and 0x00FFFFFF) or (alpha shl 24)
                strokeWidth = dp(widthDp)
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            infoWindow = null
            setOnClickListener { _, _, _ -> false }
        }
        overlays.add(polyline)
    }
}

fun MapView.addRouteLine(points: List<GeoPoint>, color: Int, widthDp: Float) {
    overlays.add(
        Polyline(this).apply {
            setPoints(points)
            outlinePaint.apply {
                this.color = color
                strokeWidth = dp(widthDp)
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            infoWindow = null
            setOnClickListener { _, _, _ -> false }
        },
    )
}

fun MapView.zoomToPoints(points: List<GeoPoint>, paddingDp: Int = 64, animate: Boolean = false) {
    if (points.isEmpty()) return
    val run = {
        if (points.size == 1) {
            controller.animateTo(points.first(), 15.0, 600L)
        } else {
            zoomToBoundingBox(BoundingBox.fromGeoPoints(points), animate, dp(paddingDp.toFloat()).toInt())
        }
    }
    if (width > 0 && height > 0) run() else addOnFirstLayoutListener { _, _, _, _, _ -> run() }
}

private fun typeface(context: Context, bold: Boolean): Typeface =
    ResourcesCompat.getFont(context, if (bold) R.font.vazirmatn_bold else R.font.vazirmatn_medium)
        ?: Typeface.DEFAULT

/**
 * Draws stations as crisp dots (optionally labelled) and reports taps on the nearest one.
 * Using a single overlay keeps hundreds of stations smooth while panning.
 */
class StationDotsOverlay(
    context: Context,
    private val darkMap: Boolean,
    private val onTap: ((Station) -> Unit)? = null,
) : Overlay() {

    data class Dot(
        val station: Station,
        val label: String?,
        val fillColor: Int,
        val ringColor: Int,
        val radiusDp: Float,
        val alwaysLabel: Boolean = false,
    ) {
        internal val point = GeoPoint(station.location.lat, station.location.lng)
    }

    var dots: List<Dot> = emptyList()
    var labelMinZoom: Double = 14.0
    var highlightedId: String? = null

    private val density = context.resources.displayMetrics.density
    private val screenPoint = Point()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (darkMap) 0x66FFFFFF else 0x55312E81
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typeface(context, bold = true)
        textSize = 12.5f * density
        color = if (darkMap) 0xFFF1F3FA.toInt() else 0xFF1B1E2E.toInt()
    }
    private val textStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = textPaint.typeface
        textSize = textPaint.textSize
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
        color = if (darkMap) 0xE60A0E1A.toInt() else 0xF2FFFFFF.toInt()
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection
        val zoom = mapView.zoomLevelDouble
        val zoomScale = (0.65 + (zoom - 11) * 0.14).coerceIn(0.6, 1.5).toFloat()
        for (dot in dots) {
            projection.toPixels(dot.point, screenPoint)
            val x = screenPoint.x.toFloat()
            val y = screenPoint.y.toFloat()
            val r = dot.radiusDp * density * zoomScale
            if (dot.station.id == highlightedId) {
                canvas.drawCircle(x, y, r * 2.6f, halo)
            }
            ring.color = dot.ringColor
            fill.color = dot.fillColor
            canvas.drawCircle(x, y, r, ring)
            canvas.drawCircle(x, y, r * 0.58f, fill)
            val label = dot.label
            if (label != null && (dot.alwaysLabel || zoom >= labelMinZoom || dot.station.id == highlightedId)) {
                val tx = x + r + 6 * density
                val ty = y + textPaint.textSize * 0.35f
                canvas.drawText(label, tx, ty, textStroke)
                canvas.drawText(label, tx, ty, textPaint)
            }
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val callback = onTap ?: return false
        val projection = mapView.projection
        var best: Dot? = null
        var bestDistance = Float.MAX_VALUE
        for (dot in dots) {
            projection.toPixels(dot.point, screenPoint)
            val d = hypot(screenPoint.x - e.x, screenPoint.y - e.y)
            if (d < bestDistance) {
                bestDistance = d
                best = dot
            }
        }
        if (best != null && bestDistance <= TOUCH_SLOP_DP * density) {
            callback(best.station)
            return true
        }
        return false
    }

    private companion object {
        const val TOUCH_SLOP_DP = 30f
    }
}

/** A glowing "train" that travels along the route path; [fraction] is 0..1 of path length. */
class TrainOverlay(context: Context) : Overlay() {
    private val density = context.resources.displayMetrics.density
    private val screenPoint = Point()
    private var path: List<GeoPoint> = emptyList()
    private var cumulative: DoubleArray = DoubleArray(0)
    var fraction: Float = 0f
    var color: Int = 0xFF7C3AED.toInt()

    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val centre = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setPath(points: List<GeoPoint>) {
        path = points
        cumulative = DoubleArray(points.size)
        for (i in 1 until points.size) {
            cumulative[i] = cumulative[i - 1] + points[i - 1].distanceToAsDouble(points[i])
        }
    }

    /** Returns the colour segment index the train is in, for tinting. */
    fun position(): Pair<GeoPoint, Int>? {
        if (path.size < 2) return null
        val target = cumulative.last() * fraction
        var i = cumulative.indexOfFirst { it >= target }.coerceAtLeast(1)
        if (i >= path.size) i = path.lastIndex
        val a = path[i - 1]
        val b = path[i]
        val span = (cumulative[i] - cumulative[i - 1]).takeIf { it > 0 } ?: 1.0
        val t = ((target - cumulative[i - 1]) / span).coerceIn(0.0, 1.0)
        return GeoPoint(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t) to i - 1
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val (point, _) = position() ?: return
        mapView.projection.toPixels(point, screenPoint)
        val x = screenPoint.x.toFloat()
        val y = screenPoint.y.toFloat()
        glow.color = (color and 0x00FFFFFF) or (0x55 shl 24)
        canvas.drawCircle(x, y, 16 * density, glow)
        glow.color = (color and 0x00FFFFFF) or (0x99 shl 24)
        canvas.drawCircle(x, y, 10 * density, glow)
        canvas.drawCircle(x, y, 7 * density, core)
        centre.color = color
        canvas.drawCircle(x, y, 4.5f * density, centre)
    }
}
