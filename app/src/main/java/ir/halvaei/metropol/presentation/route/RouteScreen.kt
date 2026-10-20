package ir.halvaei.metropol.presentation.route

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.LowPriority
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.halvaei.metropol.R
import ir.halvaei.metropol.core.isPersianLocale
import ir.halvaei.metropol.core.localizeDigits
import ir.halvaei.metropol.domain.model.LastTrainCheck
import ir.halvaei.metropol.domain.model.Route
import ir.halvaei.metropol.domain.model.RouteLeg
import ir.halvaei.metropol.domain.model.RoutePreference
import ir.halvaei.metropol.domain.model.ServiceStatus
import ir.halvaei.metropol.domain.model.Station
import ir.halvaei.metropol.domain.model.WomenCars
import ir.halvaei.metropol.presentation.components.CircleIconButton
import ir.halvaei.metropol.presentation.components.GradientButton
import ir.halvaei.metropol.presentation.components.LineBadge
import ir.halvaei.metropol.presentation.components.LineBadges
import ir.halvaei.metropol.presentation.components.LinePill
import ir.halvaei.metropol.presentation.components.MetroCard
import ir.halvaei.metropol.presentation.components.MetroLinesDecoration
import ir.halvaei.metropol.presentation.components.SectionTitle
import ir.halvaei.metropol.presentation.components.StatChip
import ir.halvaei.metropol.presentation.components.SystemBarsAppearance
import ir.halvaei.metropol.presentation.theme.MetroTheme
import ir.halvaei.metropol.presentation.util.ceilMinutes
import ir.halvaei.metropol.presentation.util.color
import ir.halvaei.metropol.presentation.util.displayName
import ir.halvaei.metropol.presentation.util.formatClock
import ir.halvaei.metropol.presentation.util.isPersian
import ir.halvaei.metropol.presentation.util.loc
import java.util.Locale

@Composable
fun RouteScreen(
    viewModel: RouteViewModel,
    onBack: () -> Unit,
    onShowMap: (RoutePreference) -> Unit,
    onTripStarted: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preference by viewModel.preference.collectAsStateWithLifecycle()
    val lastTrain by viewModel.lastTrain.collectAsStateWithLifecycle()
    val favorite by viewModel.favorite.collectAsStateWithLifecycle()
    val reminder by viewModel.reminder.collectAsStateWithLifecycle()
    val comparison by viewModel.comparison.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showFavoriteDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderDialog by rememberSaveable { mutableStateOf(false) }
    SystemBarsAppearance(lightIcons = true)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RouteEvent.Message -> snackbar.showSnackbar(
                    if (event.minuteOfDay != null) {
                        context.getString(event.text, clockText(event.minuteOfDay, context.isPersianLocale))
                    } else {
                        context.getString(event.text)
                    },
                )
                RouteEvent.TripStarted -> onTripStarted()
            }
        }
    }

    // Alerts still vibrate and ring without this permission; it only adds the visible notification.
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingAction?.invoke()
        pendingAction = null
    }
    fun withNotificationPermission(action: () -> Unit) {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pendingAction = action
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val s = state) {
            RouteUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            RouteUiState.NotFound -> NotFound(onBack)
            is RouteUiState.Success -> {
                RouteContent(
                    route = s.route,
                    co2 = s.co2SavedKg,
                    womenCars = s.womenCars,
                    comparison = comparison,
                    preference = preference,
                    isFavorite = favorite != null,
                    lastTrainBanner = lastTrain?.takeIf { it.status != LastTrainCheck.Status.UNKNOWN }?.let { check ->
                        @Composable {
                            LastTrainBanner(
                                check = check,
                                route = s.route,
                                reminder = reminder,
                                onRemind = { withNotificationPermission { showReminderDialog = true } },
                                onCancelReminder = viewModel::onCancelReminder,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    },
                    onPreference = viewModel::onPreferenceChange,
                    onBack = onBack,
                    onToggleFavorite = {
                        if (favorite != null) viewModel.onRemoveFavorite() else showFavoriteDialog = true
                    },
                    onShowMap = { onShowMap(preference) },
                    onWakeMeUp = { withNotificationPermission(viewModel::onStartTrip) },
                )
                if (showFavoriteDialog) {
                    val arrow = if (isPersian) " ← " else " → "
                    FavoriteDialog(
                        defaultLabel = s.route.origin.displayName() + arrow + s.route.destination.displayName(),
                        onSave = {
                            viewModel.onSaveFavorite(it)
                            showFavoriteDialog = false
                        },
                        onDismiss = { showFavoriteDialog = false },
                    )
                }
                val latest = lastTrain?.latestDepartureMinute
                if (showReminderDialog && latest != null) {
                    ReminderDialog(
                        leads = viewModel.reminderLeads(),
                        latestDepartureMinute = latest,
                        onPick = {
                            viewModel.onSetReminder(it)
                            showReminderDialog = false
                        },
                        onDismiss = { showReminderDialog = false },
                    )
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        )
    }
}

private fun clockText(minuteOfDay: Int, persian: Boolean): String {
    val m = ((minuteOfDay % 1440) + 1440) % 1440
    return String.format(Locale.US, "%02d:%02d", m / 60, m % 60).localizeDigits(persian)
}

@Composable
private fun NotFound(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.route_not_found), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun RouteContent(
    route: Route,
    co2: Double,
    womenCars: List<WomenCars?>,
    comparison: TravelComparison?,
    preference: RoutePreference,
    isFavorite: Boolean,
    lastTrainBanner: (@Composable () -> Unit)?,
    onPreference: (RoutePreference) -> Unit,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowMap: () -> Unit,
    onWakeMeUp: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { RouteHero(route, isFavorite, onBack, onToggleFavorite) }
            item {
                PreferenceToggle(
                    preference, onPreference,
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
            }
            if (lastTrainBanner != null) item(key = "lastTrain") { lastTrainBanner() }
            if (route.serviceStatus != ServiceStatus.NORMAL) {
                item { ServiceBanner(route.serviceStatus) }
            }
            item { JourneyStrip(route, Modifier.padding(horizontal = 16.dp)) }
            if (comparison != null) {
                item(key = "comparison") { TravelComparisonCard(comparison, Modifier.padding(horizontal = 16.dp)) }
            }
            item { SectionTitle(stringResource(R.string.steps_title), Modifier.padding(horizontal = 16.dp)) }
            item { StepsCard(route, womenCars) }
            item {
                DetailsToggle(expanded) { expanded = !expanded }
            }
            item {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    FullTimeline(route)
                }
            }
            item { EcoCard(co2, route.distanceKm) }
            item {
                Text(
                    stringResource(R.string.estimates_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        BottomActions(onShowMap, onWakeMeUp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteHero(route: Route, isFavorite: Boolean, onBack: () -> Unit, onToggleFavorite: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(MetroTheme.colors.heroGradient),
    ) {
        MetroLinesDecoration(Modifier.matchParentSize(), alpha = 0.22f)
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.route_title),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                CircleIconButton(
                    icon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = stringResource(if (isFavorite) R.string.favorite_remove else R.string.favorite_add),
                    onClick = onToggleFavorite,
                    tint = if (isFavorite) MetroTheme.colors.transfer else Color.White,
                )
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TripOrigin, null, tint = MetroTheme.colors.origin, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(route.origin.displayName(), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
                }
                Box(
                    Modifier
                        .padding(start = 7.dp)
                        .width(2.dp)
                        .height(14.dp)
                        .background(Color.White.copy(alpha = 0.35f)),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Flag, null, tint = MetroTheme.colors.destination, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(route.destination.displayName(), color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        route.totalMinutes.ceilMinutes().loc(),
                        color = Color.White,
                        fontSize = 64.sp,
                        lineHeight = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.minutes_short),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(Icons.Rounded.Schedule, stringResource(R.string.arrive_at, formatClock(route.arrivalMinuteOfDay)))
                    StatChip(Icons.Rounded.Train, pluralStringResource(R.plurals.stops_count, route.stopCount, route.stopCount.loc()))
                    StatChip(
                        Icons.Rounded.SyncAlt,
                        if (route.transfers == 0) stringResource(R.string.transfers_none)
                        else pluralStringResource(R.plurals.transfers_count, route.transfers, route.transfers.loc()),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferenceToggle(selected: RoutePreference, onSelect: (RoutePreference) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        Triple(RoutePreference.FASTEST, R.string.pref_fastest, Icons.Rounded.Bolt),
        Triple(RoutePreference.FEWEST_TRANSFERS, R.string.pref_fewest, Icons.Rounded.LowPriority),
    )
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, (pref, label, icon) ->
            SegmentedButton(
                selected = pref == selected,
                onClick = { onSelect(pref) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun ServiceBanner(status: ServiceStatus) {
    val (icon, text) = when (status) {
        ServiceStatus.PEAK -> Icons.Rounded.People to R.string.status_peak
        ServiceStatus.LATE_NIGHT -> Icons.Rounded.NightsStay to R.string.status_late
        else -> Icons.Rounded.Info to R.string.status_closed
    }
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MetroTheme.colors.warning.copy(alpha = 0.14f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MetroTheme.colors.warning)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Proportional bar of the trip: waiting, riding (in line colours) and walking. */
@Composable
private fun JourneyStrip(route: Route, modifier: Modifier = Modifier) {
    val waitColor = MaterialTheme.colorScheme.outline
    MetroCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(50)),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                route.legs.forEach { leg ->
                    val idle = (leg.walkMinutes + leg.waitMinutes).toFloat()
                    Box(Modifier.weight(idle.coerceAtLeast(0.5f)).fillMaxHeight().background(waitColor.copy(alpha = 0.5f)))
                    Box(Modifier.weight(leg.rideMinutes.toFloat().coerceAtLeast(0.5f)).fillMaxHeight().background(leg.line.color()))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                route.legs.forEachIndexed { index, leg ->
                    if (index > 0) {
                        Icon(
                            Icons.AutoMirrored.Rounded.DirectionsWalk, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp).padding(horizontal = 1.dp),
                        )
                    }
                    LineBadge(leg.line.number, size = 24.dp)
                    Text(
                        " ${leg.rideMinutes.ceilMinutes().loc()}′ ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.total_wait, route.totalWaitMinutes.ceilMinutes().loc()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepsCard(route: Route, womenCars: List<WomenCars?>) {
    MetroCard(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            StepRow(
                icon = Icons.Rounded.TripOrigin,
                tint = MetroTheme.colors.origin,
                title = stringResource(R.string.start_at, route.origin.displayName()),
                connectorBelow = route.legs.first().line.color(),
            )
            route.legs.forEachIndexed { index, leg ->
                if (index > 0) {
                    StepRow(
                        icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                        tint = MetroTheme.colors.transfer,
                        title = stringResource(R.string.change_at, leg.board.displayName()),
                        subtitle = stringResource(
                            R.string.walk_to_line,
                            leg.walkMinutes.ceilMinutes().loc(),
                            stringResource(R.string.line_number, leg.line.number.loc()),
                        ),
                        connectorAbove = route.legs[index - 1].line.color(),
                        connectorBelow = leg.line.color(),
                        dashed = true,
                    )
                }
                LegStep(leg, womenCars.getOrNull(index), isLast = index == route.legs.lastIndex)
            }
            StepRow(
                icon = Icons.Rounded.Flag,
                tint = MetroTheme.colors.destination,
                title = stringResource(R.string.arrive_destination, route.destination.displayName()),
                subtitle = stringResource(R.string.arrive_eta, formatClock(route.arrivalMinuteOfDay)),
                connectorAbove = route.legs.last().line.color(),
            )
        }
    }
}

@Composable
private fun LegStep(leg: RouteLeg, womenCars: WomenCars?, isLast: Boolean) {
    val lineColor = leg.line.color()
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
    ) {
        Box(Modifier.width(32.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(6.dp).fillMaxHeight().clip(RoundedCornerShape(50)).background(lineColor))
            LineBadge(leg.line.number, size = 30.dp, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.padding(vertical = 12.dp).weight(1f)) {
            Text(
                stringResource(R.string.take_line_toward, leg.line.displayName(), leg.direction.displayName()),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.board_hint, leg.direction.displayName()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            InfoLine(
                Icons.Rounded.Timer,
                stringResource(
                    R.string.wait_about,
                    leg.waitMinutes.ceilMinutes().loc(),
                    formatHeadway(leg.effectiveHeadwayMinutes).loc(),
                ),
            )
            InfoLine(
                Icons.Rounded.Train,
                stringResource(
                    R.string.ride_stops,
                    pluralStringResource(R.plurals.stops_count, leg.stopCount, leg.stopCount.loc()),
                    leg.rideMinutes.ceilMinutes().loc(),
                ),
            )
            InfoLine(
                if (isLast) Icons.Rounded.Flag else Icons.Rounded.SyncAlt,
                stringResource(R.string.get_off_at, leg.alight.displayName()),
                emphasize = true,
            )
            if (womenCars != null) {
                Spacer(Modifier.height(8.dp))
                WomenCarsCard(womenCars)
            }
        }
    }
}

private fun formatHeadway(minutes: Double): String =
    if (minutes % 1.0 < 0.05) minutes.toInt().toString() else String.format(Locale.US, "%.1f", minutes)

@Composable
private fun InfoLine(icon: ImageVector, text: String, emphasize: Boolean = false) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String? = null,
    connectorAbove: Color? = null,
    connectorBelow: Color? = null,
    dashed: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(32.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
                connectorAbove?.let {
                    drawLine(it, Offset(cx, 0f), Offset(cx, size.height / 2), strokeWidth = 6.dp.toPx(), pathEffect = effect)
                }
                connectorBelow?.let {
                    drawLine(it, Offset(cx, size.height / 2), Offset(cx, size.height), strokeWidth = 6.dp.toPx(), pathEffect = effect)
                }
            }
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, tint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.padding(vertical = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailsToggle(expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onToggle)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Train, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(if (expanded) R.string.less_details else R.string.more_details),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.rotate(rotation))
    }
}

/** Every stop with its estimated clock time, grouped by leg with wait/walk annotations. */
@Composable
private fun FullTimeline(route: Route) {
    MetroCard(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(
                stringResource(R.string.all_stations),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            route.legs.forEachIndexed { legIndex, leg ->
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LinePill(leg.line, stringResource(R.string.leg_header, leg.line.displayName(), leg.direction.displayName()))
                }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (legIndex > 0) {
                        TimelineChip(Icons.AutoMirrored.Rounded.DirectionsWalk, stringResource(R.string.walk_leg, leg.walkMinutes.ceilMinutes().loc()))
                    }
                    TimelineChip(Icons.Rounded.Timer, stringResource(R.string.wait_leg, leg.waitMinutes.ceilMinutes().loc()))
                }
                leg.stops.forEachIndexed { stopIndex, stop ->
                    TimelineStop(
                        station = stop.station,
                        clock = formatClock(route.departureMinuteOfDay + stop.offsetMinutes.toInt()),
                        color = leg.line.color(),
                        first = stopIndex == 0,
                        last = stopIndex == leg.stops.lastIndex,
                        currentLine = leg.line.number,
                        isTransfer = (stopIndex == leg.stops.lastIndex && legIndex < route.legs.lastIndex) ||
                            (stopIndex == 0 && legIndex > 0),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineChip(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TimelineStop(
    station: Station,
    clock: String,
    color: Color,
    first: Boolean,
    last: Boolean,
    currentLine: Int,
    isTransfer: Boolean,
) {
    val surface = MaterialTheme.colorScheme.surface
    val major = first || last
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            clock,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(46.dp),
        )
        Canvas(Modifier.width(24.dp).fillMaxHeight()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val w = 5.dp.toPx()
            if (!first) drawLine(color, Offset(cx, 0f), Offset(cx, cy), strokeWidth = w)
            if (!last) drawLine(color, Offset(cx, cy), Offset(cx, size.height), strokeWidth = w)
            val r = if (major) 7.dp.toPx() else 5.dp.toPx()
            drawCircle(color, radius = r, center = Offset(cx, cy))
            drawCircle(surface, radius = r - 2.5.dp.toPx(), center = Offset(cx, cy))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            station.displayName(),
            style = if (major) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 9.dp),
        )
        val others = station.lineNumbers.filter { it != currentLine }
        if (isTransfer) {
            Surface(shape = RoundedCornerShape(50), color = MetroTheme.colors.transfer.copy(alpha = 0.18f)) {
                Text(
                    stringResource(R.string.transfer_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MetroTheme.colors.transfer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        if (others.isNotEmpty()) LineBadges(others, size = 18.dp)
    }
}

@Composable
private fun TravelComparisonCard(comparison: TravelComparison, modifier: Modifier = Modifier) {
    MetroCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.comparison_title), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ComparisonTile(
                    icon = Icons.Rounded.Train,
                    label = stringResource(R.string.comparison_metro),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    ComparisonMinutes(comparison.metroMinutes)
                }
                ComparisonTile(
                    icon = Icons.Rounded.DirectionsCar,
                    label = stringResource(R.string.comparison_car),
                    tint = MetroTheme.colors.warning,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    when (val car = comparison.car) {
                        CarTimeState.Loading -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        is CarTimeState.Ready -> ComparisonMinutes(car.minutes)
                        CarTimeState.Offline -> ComparisonError(stringResource(R.string.comparison_car_offline))
                        CarTimeState.Failed -> ComparisonError(stringResource(R.string.comparison_car_error))
                    }
                }
            }
            (comparison.car as? CarTimeState.Ready)?.let { car ->
                val diff = car.minutes - comparison.metroMinutes
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        diff > 0 -> stringResource(R.string.comparison_metro_faster, diff.loc())
                        diff < 0 -> stringResource(R.string.comparison_car_faster, (-diff).loc())
                        else -> stringResource(R.string.comparison_same)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (diff >= 0) MetroTheme.colors.success else MetroTheme.colors.warning,
                )
                Text(
                    stringResource(R.string.comparison_car_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ComparisonTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
        }
        Spacer(Modifier.height(8.dp))
        value()
    }
}

@Composable
private fun ComparisonMinutes(minutes: Int) {
    Text(
        stringResource(R.string.comparison_minutes, minutes.loc()),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ComparisonError(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun EcoCard(co2Kg: Double, distanceKm: Double) {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MetroTheme.colors.success.copy(alpha = 0.12f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MetroTheme.colors.success),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Eco, null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(R.string.eco_title), style = MaterialTheme.typography.titleSmall, color = MetroTheme.colors.success)
            Text(
                stringResource(
                    R.string.eco_body,
                    String.format(Locale.US, "%.1f", co2Kg).loc(),
                    String.format(Locale.US, "%.1f", distanceKm).loc(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun BottomActions(onShowMap: () -> Unit, onWakeMeUp: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GradientButton(
                text = stringResource(R.string.show_on_map),
                icon = Icons.Rounded.Map,
                onClick = onShowMap,
                modifier = Modifier.weight(1.2f),
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onWakeMeUp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Alarm, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.wake_me_up),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
