package ir.halvaei.metropol.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.halvaei.metropol.R
import ir.halvaei.metropol.domain.model.AppLanguage
import ir.halvaei.metropol.domain.model.MetroLine
import ir.halvaei.metropol.domain.model.RoutePreference
import ir.halvaei.metropol.domain.model.Station
import ir.halvaei.metropol.domain.model.ThemeMode
import ir.halvaei.metropol.presentation.components.CircleIconButton
import ir.halvaei.metropol.presentation.components.GradientButton
import ir.halvaei.metropol.presentation.components.LineBadge
import ir.halvaei.metropol.presentation.components.LineBadges
import ir.halvaei.metropol.presentation.components.MetroCard
import ir.halvaei.metropol.presentation.components.MetroLinesDecoration
import ir.halvaei.metropol.presentation.components.SectionTitle
import ir.halvaei.metropol.presentation.components.SystemBarsAppearance
import ir.halvaei.metropol.presentation.theme.MetroTheme
import ir.halvaei.metropol.presentation.tutorial.CoachMarkOverlay
import ir.halvaei.metropol.presentation.tutorial.CoachMarkState
import ir.halvaei.metropol.presentation.tutorial.CoachTarget
import ir.halvaei.metropol.presentation.tutorial.coachTarget
import ir.halvaei.metropol.presentation.tutorial.rememberCoachMarkState
import ir.halvaei.metropol.presentation.util.color
import ir.halvaei.metropol.presentation.util.displayName
import ir.halvaei.metropol.presentation.util.loc
import ir.halvaei.metropol.presentation.util.secondaryName
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    themeMode: ThemeMode,
    language: AppLanguage,
    onThemeSelected: (ThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenRoute: (originId: String, destinationId: String, preference: RoutePreference) -> Unit,
    onOpenPicker: (originId: String?, destinationId: String?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.OpenRoute -> {
                    focusManager.clearFocus()
                    onOpenRoute(event.originId, event.destinationId, event.preference)
                }
                is HomeEvent.Message -> snackbar.showSnackbar(context.getString(event.text))
            }
        }
    }

    val coach = rememberCoachMarkState()
    LaunchedEffect(state.tutorialVisible) {
        if (state.tutorialVisible) focusManager.clearFocus()
    }

    SystemBarsAppearance(lightIcons = !drawerState.isOpen || MetroTheme.colors.isDark)

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    Box(Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    themeMode = themeMode,
                    language = language,
                    onThemeSelected = onThemeSelected,
                    onLanguageSelected = onLanguageSelected,
                    onOpenTutorial = {
                        scope.launch { drawerState.close() }
                        viewModel.showTutorial()
                    },
                    onClose = { scope.launch { drawerState.close() } },
                )
            },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                ) {
                    HomeHeader(
                        lineCount = state.lines.size,
                        stationCount = state.stationCount,
                        onMenu = { scope.launch { drawerState.open() } },
                        onHelp = viewModel::showTutorial,
                        coach = coach,
                    )
                    SearchCard(
                        state = state,
                        coach = coach,
                        onQueryChange = viewModel::onQueryChange,
                        onFocus = viewModel::onFieldFocused,
                        onSuggestion = viewModel::onSuggestionSelected,
                        onClear = viewModel::onClear,
                        onSwap = viewModel::onSwap,
                        onPickOnMap = {
                            focusManager.clearFocus()
                            onOpenPicker(state.origin?.id, state.destination?.id)
                        },
                        onDone = viewModel::onFindRoute,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .offset(y = (-56).dp),
                    )
                    Column(Modifier.offset(y = (-36).dp)) {
                        GradientButton(
                            text = stringResource(R.string.fastest_route),
                            icon = Icons.Rounded.Bolt,
                            enabled = state.canSearch,
                            onClick = viewModel::onFindRoute,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .coachTarget(coach, CoachTarget.FASTEST_ROUTE),
                        )
                        AnimatedVisibility(visible = state.favorites.isNotEmpty()) {
                            FavoriteRoutes(
                                favorites = state.favorites,
                                onOpen = viewModel::onFavorite,
                                onRemove = viewModel::onRemoveFavorite,
                            )
                        }
                        AnimatedVisibility(visible = state.recentTrips.isNotEmpty()) {
                            RecentTrips(
                                trips = state.recentTrips,
                                onTrip = viewModel::onRecentTrip,
                                onClear = viewModel::onClearRecent,
                            )
                        }
                        if (state.lines.isNotEmpty()) {
                            LinesOverview(
                                lines = state.lines,
                                expanded = state.linesExpanded,
                                onToggle = viewModel::onToggleLines,
                                headerModifier = Modifier.coachTarget(coach, CoachTarget.LINES),
                            )
                        }
                    }
                }
                SnackbarHost(
                    snackbar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .imePadding(),
                )
            }
        }

        if (state.tutorialVisible) {
            CoachMarkOverlay(state = coach, onFinish = viewModel::dismissTutorial)
        }
    }
}

@Composable
private fun HomeHeader(
    lineCount: Int,
    stationCount: Int,
    onMenu: () -> Unit,
    onHelp: () -> Unit,
    coach: CoachMarkState,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(MetroTheme.colors.heroGradient),
    ) {
        MetroLinesDecoration(Modifier.matchParentSize())
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 84.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    Icons.Rounded.Menu, stringResource(R.string.menu), onMenu,
                    modifier = Modifier.coachTarget(coach, CoachTarget.MENU),
                )
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Train, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.weight(1f))
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.help), onHelp,
                    modifier = Modifier.coachTarget(coach, CoachTarget.HELP),
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.home_greeting_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(6.dp))
            if (stationCount > 0) {
                Text(
                    stringResource(R.string.home_greeting_subtitle, lineCount.loc(), stationCount.loc()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchCard(
    state: HomeUiState,
    coach: CoachMarkState,
    onQueryChange: (StationField, String) -> Unit,
    onFocus: (StationField?) -> Unit,
    onSuggestion: (Station) -> Unit,
    onClear: (StationField) -> Unit,
    onSwap: () -> Unit,
    onPickOnMap: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val destinationFocus = remember { FocusRequester() }
    var swapRotation by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(swapRotation, label = "swap")

    MetroCard(modifier = modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TripTimeline(Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StationInput(
                        label = stringResource(R.string.origin_label),
                        placeholder = stringResource(R.string.origin_placeholder),
                        station = state.origin,
                        query = state.originQuery,
                        onQueryChange = { onQueryChange(StationField.ORIGIN, it) },
                        onFocus = { if (it) onFocus(StationField.ORIGIN) },
                        onClear = { onClear(StationField.ORIGIN) },
                        imeAction = ImeAction.Next,
                        onIme = { destinationFocus.requestFocus() },
                        modifier = Modifier.coachTarget(coach, CoachTarget.ORIGIN),
                    )
                    SuggestionList(
                        visible = state.activeField == StationField.ORIGIN,
                        query = state.originQuery,
                        suggestions = state.suggestions,
                        onSelect = {
                            onSuggestion(it)
                            if (state.destination == null) destinationFocus.requestFocus() else focusManager.clearFocus()
                        },
                    )
                    StationInput(
                        label = stringResource(R.string.destination_label),
                        placeholder = stringResource(R.string.destination_placeholder),
                        station = state.destination,
                        query = state.destinationQuery,
                        onQueryChange = { onQueryChange(StationField.DESTINATION, it) },
                        onFocus = { if (it) onFocus(StationField.DESTINATION) },
                        onClear = { onClear(StationField.DESTINATION) },
                        imeAction = ImeAction.Search,
                        onIme = {
                            focusManager.clearFocus()
                            onDone()
                        },
                        modifier = Modifier
                            .focusRequester(destinationFocus)
                            .coachTarget(coach, CoachTarget.DESTINATION),
                    )
                    SuggestionList(
                        visible = state.activeField == StationField.DESTINATION,
                        query = state.destinationQuery,
                        suggestions = state.suggestions,
                        onSelect = {
                            onSuggestion(it)
                            focusManager.clearFocus()
                        },
                    )
                }
                Column(
                    Modifier.padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircleIconButton(
                        icon = Icons.Rounded.Map,
                        contentDescription = stringResource(R.string.pick_on_map),
                        onClick = onPickOnMap,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.coachTarget(coach, CoachTarget.MAP),
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.SwapVert,
                        contentDescription = stringResource(R.string.swap),
                        onClick = {
                            swapRotation += 180f
                            onSwap()
                        },
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .coachTarget(coach, CoachTarget.SWAP)
                            .rotate(rotation),
                    )
                }
            }
        }
    }
}

/** Origin ring, dotted connector and destination pin drawn beside the inputs. */
@Composable
private fun TripTimeline(modifier: Modifier = Modifier) {
    val originColor = MetroTheme.colors.origin
    val destinationColor = MetroTheme.colors.destination
    val dash = MaterialTheme.colorScheme.outline
    Canvas(modifier.size(width = 14.dp, height = 108.dp)) {
        val cx = size.width / 2
        val top = Offset(cx, 22.dp.toPx())
        val bottom = Offset(cx, size.height - 22.dp.toPx())
        drawLine(
            dash, top.copy(y = top.y + 9.dp.toPx()), bottom.copy(y = bottom.y - 9.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
        )
        drawCircle(originColor, radius = 7.dp.toPx(), center = top)
        drawCircle(Color.White, radius = 3.dp.toPx(), center = top)
        drawCircle(destinationColor, radius = 7.dp.toPx(), center = bottom)
        drawCircle(Color.White, radius = 3.dp.toPx(), center = bottom)
    }
}

@Composable
private fun StationInput(
    label: String,
    placeholder: String,
    station: Station?,
    query: String,
    onQueryChange: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    onClear: () -> Unit,
    imeAction: ImeAction,
    onIme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableFloatStateOf(0f) }
    val borderAlpha by animateFloatAsState(focused, label = "border")
    val text = station?.displayName() ?: query
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = text,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onIme() }, onSearch = { onIme() }),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                focused = if (it.isFocused) 1f else 0f
                onFocus(it.isFocused)
            },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha), shape)
                    .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                }
                if (station != null) {
                    LineBadges(station.lineNumbers, size = 18.dp, modifier = Modifier.padding(horizontal = 4.dp))
                }
                if (text.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SuggestionList(
    visible: Boolean,
    query: String,
    suggestions: List<Station>,
    onSelect: (Station) -> Unit,
) {
    AnimatedVisibility(
        visible = visible && query.isNotBlank(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        ) {
            if (suggestions.isEmpty()) {
                Text(
                    stringResource(R.string.no_results, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            suggestions.forEachIndexed { index, station ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(station) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            station.displayName(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            station.secondaryName(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LineBadges(station.lineNumbers, size = 22.dp)
                }
            }
        }
    }
}

@Composable
private fun FavoriteRoutes(
    favorites: List<FavoriteItem>,
    onOpen: (FavoriteItem) -> Unit,
    onRemove: (FavoriteItem) -> Unit,
) {
    Column(Modifier.padding(top = 24.dp)) {
        SectionTitle(stringResource(R.string.favorites_title), Modifier.padding(horizontal = 16.dp))
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            favorites.forEach { item ->
                MetroCard(
                    modifier = Modifier
                        .widthIn(min = 200.dp, max = 280.dp)
                        .clickable { onOpen(item) },
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MetroTheme.colors.transfer.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Star, null, tint = MetroTheme.colors.transfer, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.favorite.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.origin.displayName(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward, null,
                                    tint = MetroTheme.colors.destination,
                                    modifier = Modifier.padding(horizontal = 4.dp).size(12.dp),
                                )
                                Text(
                                    item.destination.displayName(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }
                        }
                        IconButton(onClick = { onRemove(item) }) {
                            Icon(
                                Icons.Rounded.Close,
                                stringResource(R.string.favorite_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTrips(
    trips: List<Pair<Station, Station>>,
    onTrip: (Station, Station) -> Unit,
    onClear: () -> Unit,
) {
    Column(Modifier.padding(top = 24.dp)) {
        SectionTitle(stringResource(R.string.recent_trips), Modifier.padding(horizontal = 16.dp)) {
            TextButton(onClick = onClear) { Text(stringResource(R.string.clear_all)) }
        }
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            trips.forEach { (origin, destination) ->
                MetroCard(
                    modifier = Modifier
                        .widthIn(min = 200.dp, max = 260.dp)
                        .clickable { onTrip(origin, destination) },
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(origin.displayName(), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward, null,
                                    tint = MetroTheme.colors.destination,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    destination.displayName(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Accordion card: a compact header with the line colours, expanding to the full list. */
@Composable
private fun LinesOverview(
    lines: List<LineSummary>,
    expanded: Boolean,
    onToggle: () -> Unit,
    headerModifier: Modifier = Modifier,
) {
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "linesArrow")
    val stateLabel = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    MetroCard(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
            .fillMaxWidth(),
    ) {
        Column {
            Row(
                headerModifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics { stateDescription = stateLabel }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Train, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.metro_lines), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    AnimatedVisibility(
                        visible = !expanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        LineColorStrip(lines)
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Text(
                            stringResource(R.string.metro_lines_summary, lines.size.loc()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(arrowRotation),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Column(Modifier.padding(bottom = 6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    lines.forEachIndexed { index, line ->
                        if (index > 0) HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        LineRow(line)
                    }
                }
            }
        }
    }
}

/** Collapsed preview: one small numbered dot per line in its official colour. */
@Composable
private fun LineColorStrip(lines: List<LineSummary>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { summary ->
            LineBadge(summary.line.number, size = 20.dp)
        }
    }
}

@Composable
private fun LineRow(summary: LineSummary) {
    val line = summary.line
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineBadge(line.number, size = 34.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.line_number, line.number.loc()), style = MaterialTheme.typography.titleSmall)
            Text(
                "${summary.first.displayName()}  ⇄  ${summary.last.displayName()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .width(56.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(line.color()),
        )
    }
}
