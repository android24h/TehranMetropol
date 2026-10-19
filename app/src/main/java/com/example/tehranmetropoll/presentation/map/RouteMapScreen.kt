package com.example.tehranmetropoll.presentation.map

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.domain.model.MetroNetwork
import com.example.tehranmetropoll.domain.model.Route
import com.example.tehranmetropoll.presentation.components.CircleIconButton
import com.example.tehranmetropoll.presentation.components.LineBadge
import com.example.tehranmetropoll.presentation.components.SystemBarsAppearance
import com.example.tehranmetropoll.presentation.route.RouteUiState
import com.example.tehranmetropoll.presentation.route.RouteViewModel
import com.example.tehranmetropoll.presentation.theme.MetroTheme
import com.example.tehranmetropoll.presentation.util.ceilMinutes
import com.example.tehranmetropoll.presentation.util.displayName
import com.example.tehranmetropoll.presentation.util.formatClock
import com.example.tehranmetropoll.presentation.util.isPersian
import com.example.tehranmetropoll.presentation.util.loc
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun RouteMapScreen(viewModel: RouteViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    val dark = MetroTheme.colors.isDark
    var mapView by remember { mutableStateOf<MapView?>(null) }
    SystemBarsAppearance(lightIcons = dark)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        OsmMap(darkTiles = dark, online = online, modifier = Modifier.fillMaxSize(), onReady = { mapView = it })

        val success = state as? RouteUiState.Success
        if (success != null) {
            RouteOverlays(mapView, success.route, success.network, dark)
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        // Top bar
        Column(
            Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .fillMaxWidth(),
        ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MapButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), MaterialTheme.colorScheme.onSurface, onBack)
            if (success != null) {
                Spacer(Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            success.route.origin.displayName(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward, null,
                            tint = MetroTheme.colors.destination,
                            modifier = Modifier.padding(horizontal = 6.dp).size(16.dp),
                        )
                        Text(
                            success.route.destination.displayName(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
        OfflineMapBanner(visible = !online, modifier = Modifier.padding(top = 10.dp))
        }

        if (success != null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                MapButton(Icons.Rounded.CenterFocusStrong, null, MaterialTheme.colorScheme.primary, { mapView?.zoomToPoints(routePoints(success.route), paddingDp = 90, animate = true) })
                Spacer(Modifier.size(12.dp))
                RouteSummaryCard(success.route)
            }
        }
        Text(
            stringResource(R.string.map_attribution),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 8.dp, bottom = 2.dp),
        )
    }
}

fun routePoints(route: Route): List<GeoPoint> = route.allStops.map { it.station.location.toGeoPoint() }

@Composable
private fun RouteOverlays(mapView: MapView?, route: Route, network: MetroNetwork, dark: Boolean) {
    val context = LocalContext.current
    val metro = MetroTheme.colors
    val colors = remember(metro) { MarkerColors(metro.origin.toArgb(), metro.destination.toArgb(), metro.transfer.toArgb()) }
    val persian = isPersian
    LaunchedEffect(mapView, route, dark, persian) {
        val map = mapView ?: return@LaunchedEffect
        map.overlays.clear()
        map.addNetworkLines(network, alpha = if (dark) 0x55 else 0x45, widthDp = 3f)

        val casing = if (dark) 0xFF0A0E1A.toInt() else 0xFFFFFFFF.toInt()
        route.legs.forEach { leg ->
            map.addRouteLine(leg.stops.map { it.station.location.toGeoPoint() }, casing, 13f)
        }
        route.legs.forEach { leg ->
            map.addRouteLine(leg.stops.map { it.station.location.toGeoPoint() }, leg.line.colorArgb.toInt(), 7.5f)
        }

        // Path and per-segment colours for the animated train.
        val path = routePoints(route)
        val segmentColors = route.legs.flatMap { leg -> List(leg.stops.size - 1) { leg.line.colorArgb.toInt() } }
        val train = TrainOverlay(context).apply { setPath(path) }

        val origin = colors.origin
        val destination = colors.destination
        val transfer = colors.transfer
        val white = 0xFFFFFFFF.toInt()
        val transferIds = route.legs.drop(1).map { it.board.id }.toSet()
        val dots = StationDotsOverlay(context, dark).apply {
            labelMinZoom = 13.5
            this.dots = route.legs.flatMapIndexed { legIndex, leg ->
                leg.stops.mapIndexedNotNull { i, stop ->
                    val s = stop.station
                    when {
                        legIndex == 0 && i == 0 -> StationDotsOverlay.Dot(s, s.name(persian), white, origin, 10f, alwaysLabel = true)
                        legIndex == route.legs.lastIndex && i == leg.stops.lastIndex ->
                            StationDotsOverlay.Dot(s, s.name(persian), white, destination, 10f, alwaysLabel = true)
                        s.id in transferIds && i == 0 -> null // drawn once, as the end of the previous leg
                        s.id in transferIds -> StationDotsOverlay.Dot(s, s.name(persian), white, transfer, 9f, alwaysLabel = true)
                        else -> StationDotsOverlay.Dot(s, s.name(persian), white, leg.line.colorArgb.toInt(), 5.5f)
                    }
                }
            }
        }
        map.overlays.add(train)
        map.overlays.add(dots)
        map.invalidate()
        map.zoomToPoints(path, paddingDp = 90)

        // Loop the train along the route: a full pass every ~7 seconds, paused briefly at the end.
        val periodMs = 7_000L
        while (isActive) {
            val t = (SystemClock.uptimeMillis() % (periodMs + 900)).toFloat() / periodMs
            train.fraction = t.coerceAtMost(1f)
            train.position()?.second?.let { segment -> segmentColors.getOrNull(segment)?.let { train.color = it } }
            map.postInvalidate()
            delay(33)
        }
    }
}

@Composable
private fun RouteSummaryCard(route: Route) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    route.totalMinutes.ceilMinutes().loc(),
                    fontSize = 40.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.minutes_short),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(bottom = 2.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.arrive_at, formatClock(route.arrivalMinuteOfDay)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                route.legs.forEachIndexed { index, leg ->
                    if (index > 0) {
                        Icon(
                            Icons.AutoMirrored.Rounded.DirectionsWalk, null,
                            tint = MetroTheme.colors.transfer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Row(
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LineBadge(leg.line.number, size = 26.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(leg.board.displayName(), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            Text(
                                "→ " + leg.direction.displayName(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MarkerColors(val origin: Int, val destination: Int, val transfer: Int)

@Composable
fun MapButton(icon: ImageVector, contentDescription: String?, tint: Color, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
        CircleIconButton(icon, contentDescription, onClick, container = Color.Transparent, tint = tint, size = 48.dp)
    }
}
