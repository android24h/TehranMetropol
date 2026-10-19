package com.example.tehranmetropoll.presentation.picker

import android.Manifest
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.core.localizeDigits
import com.example.tehranmetropoll.domain.model.LatLng
import com.example.tehranmetropoll.domain.model.Station
import com.example.tehranmetropoll.presentation.components.GradientButton
import com.example.tehranmetropoll.presentation.components.LineBadges
import com.example.tehranmetropoll.presentation.components.SystemBarsAppearance
import com.example.tehranmetropoll.presentation.map.MapButton
import com.example.tehranmetropoll.presentation.map.OfflineMapBanner
import com.example.tehranmetropoll.presentation.map.OsmMap
import com.example.tehranmetropoll.presentation.map.StationDotsOverlay
import com.example.tehranmetropoll.presentation.map.addNetworkLines
import com.example.tehranmetropoll.presentation.map.toGeoPoint
import com.example.tehranmetropoll.presentation.theme.MetroTheme
import com.example.tehranmetropoll.presentation.util.displayName
import com.example.tehranmetropoll.presentation.util.isPersian
import com.example.tehranmetropoll.presentation.util.lineColor
import com.example.tehranmetropoll.presentation.util.secondaryName
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.util.Locale

@Composable
fun PickerScreen(
    viewModel: PickerViewModel,
    onBack: () -> Unit,
    onConfirm: (originId: String?, destinationId: String?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    val dark = MetroTheme.colors.isDark
    val context = LocalContext.current
    val persian = isPersian
    val snackbar = remember { SnackbarHostState() }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    SystemBarsAppearance(lightIcons = dark)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) viewModel.onLocate() else viewModel.onLocationDenied()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PickerEvent.Focus -> mapView?.controller?.animateTo(event.station.location.toGeoPoint(), 15.5, 800L)
                is PickerEvent.Message -> {
                    val text = if (event.station != null) {
                        val distance = if (event.distanceKm < 1) {
                            context.getString(R.string.distance_m, (event.distanceKm * 1000).toInt().toString().localizeDigits(persian))
                        } else {
                            context.getString(R.string.distance_km, String.format(Locale.US, "%.1f", event.distanceKm).localizeDigits(persian))
                        }
                        context.getString(event.text, event.station.name(persian), distance)
                    } else {
                        context.getString(event.text)
                    }
                    snackbar.showSnackbar(text)
                }
            }
        }
    }

    // Overlays: network lines, every station, and the user's position.
    val metro = MetroTheme.colors
    val dotsOverlay = remember(dark) {
        StationDotsOverlay(context, dark, onTap = viewModel::onStationTapped).apply { labelMinZoom = 14.0 }
    }
    val userOverlay = remember { UserLocationOverlay(context) }
    LaunchedEffect(mapView, state.network, dark) {
        val map = mapView ?: return@LaunchedEffect
        val network = state.network ?: return@LaunchedEffect
        map.overlays.clear()
        map.addNetworkLines(network, alpha = 0xD0, widthDp = 4f)
        map.overlays.add(userOverlay)
        map.overlays.add(dotsOverlay)
        map.invalidate()
    }
    LaunchedEffect(state, persian, metro) {
        val network = state.network ?: return@LaunchedEffect
        val white = 0xFFFFFFFF.toInt()
        dotsOverlay.highlightedId = state.selected?.id
        dotsOverlay.dots = network.stations.map { s ->
            when (s.id) {
                state.origin?.id -> StationDotsOverlay.Dot(s, s.name(persian), white, metro.origin.toArgb(), 9f, alwaysLabel = true)
                state.destination?.id -> StationDotsOverlay.Dot(s, s.name(persian), white, metro.destination.toArgb(), 9f, alwaysLabel = true)
                else -> StationDotsOverlay.Dot(
                    s, s.name(persian),
                    fillColor = white,
                    ringColor = if (s.isInterchange) 0xFF1B1E2E.toInt() else lineColor(s.lineNumbers.first()).toArgb(),
                    radiusDp = if (s.isInterchange) 6.5f else 5f,
                )
            }
        }
        userOverlay.location = state.userLocation
        mapView?.invalidate()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        OsmMap(darkTiles = dark, online = online, modifier = Modifier.fillMaxSize(), onReady = { mapView = it })

        Column(
            Modifier
                .statusBarsPadding()
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MapButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), MaterialTheme.colorScheme.onSurface, onBack)
                Spacer(Modifier.width(10.dp))
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp, modifier = Modifier.weight(1f)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TouchApp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.picker_title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.picker_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            OfflineMapBanner(visible = !online, modifier = Modifier.padding(top = 10.dp))
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    if (state.locating) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    } else {
                        IconButton(onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            )
                        }) {
                            Icon(Icons.Rounded.MyLocation, stringResource(R.string.near_me), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AnimatedVisibility(
                visible = state.selected != null,
                enter = slideInVertically { it } ,
                exit = slideOutVertically { it },
            ) {
                state.selected?.let { station ->
                    SelectedStationCard(
                        station = station,
                        onOrigin = viewModel::onSetOrigin,
                        onDestination = viewModel::onSetDestination,
                        onDismiss = viewModel::onDismissSelection,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SelectionSummary(
                origin = state.origin,
                destination = state.destination,
                onConfirm = { onConfirm(state.origin?.id, state.destination?.id) },
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp))
        Text(
            stringResource(R.string.map_attribution),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 8.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun SelectedStationCard(
    station: Station,
    onOrigin: () -> Unit,
    onDestination: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.displayName(), style = MaterialTheme.typography.titleLarge)
                    Text(station.secondaryName(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LineBadges(station.lineNumbers, size = 26.dp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.clear), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceButton(Icons.Rounded.TripOrigin, stringResource(R.string.set_origin), MetroTheme.colors.origin, onOrigin, Modifier.weight(1f))
                ChoiceButton(Icons.Rounded.Flag, stringResource(R.string.set_destination), MetroTheme.colors.destination, onDestination, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChoiceButton(icon: ImageVector, text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.14f),
        modifier = modifier.height(50.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
private fun SelectionSummary(origin: Station?, destination: Station?, onConfirm: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SlotView(stringResource(R.string.from_label), origin, MetroTheme.colors.origin, Modifier.weight(1f))
                SlotView(stringResource(R.string.to_label), destination, MetroTheme.colors.destination, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            GradientButton(
                text = stringResource(R.string.confirm),
                icon = Icons.Rounded.Check,
                enabled = origin != null || destination != null,
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SlotView(label: String, station: Station?, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.5.dp, if (station != null) color else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                station?.displayName() ?: stringResource(R.string.not_selected),
                style = MaterialTheme.typography.titleSmall,
                color = if (station != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Blue "you are here" dot with an accuracy-style halo. */
private class UserLocationOverlay(context: android.content.Context) : Overlay() {
    var location: LatLng? = null
    private val density = context.resources.displayMetrics.density
    private val point = Point()
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x332563EB }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2563EB.toInt() }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        val here = location ?: return
        if (shadow) return
        mapView.projection.toPixels(here.toGeoPoint(), point)
        val x = point.x.toFloat()
        val y = point.y.toFloat()
        canvas.drawCircle(x, y, 26 * density, halo)
        canvas.drawCircle(x, y, 9 * density, ring)
        canvas.drawCircle(x, y, 6.5f * density, dot)
    }
}
