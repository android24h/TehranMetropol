package com.example.tehranmetropoll.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.tehranmetropoll.MainActivity
import com.example.tehranmetropoll.MetroApplication
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.core.AppLinks
import com.example.tehranmetropoll.core.isPersianLocale
import com.example.tehranmetropoll.core.localizeDigits
import com.example.tehranmetropoll.core.withAppLocale
import com.example.tehranmetropoll.domain.repository.WidgetUpdater
import com.example.tehranmetropoll.domain.usecase.RouteResult
import com.example.tehranmetropoll.presentation.theme.DarkColors
import com.example.tehranmetropoll.presentation.theme.LightColors
import com.example.tehranmetropoll.presentation.util.lineColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.ceil

private val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

private data class WidgetRoute(
    val label: String,
    val from: String,
    val to: String,
    val minutes: Int?,
    val lineNumbers: List<Int>,
    val uri: Uri,
)

/**
 * Home-screen widget listing favourite routes with their estimated travel time. Everything is
 * computed on the device from bundled data, so it works fully offline.
 */
class FavoritesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as MetroApplication).container
        val localized = context.withAppLocale()
        val persian = localized.isPersianLocale
        val network = container.getMetroNetwork()
        val routes = container.favorites.favorites.first().mapNotNull { favorite ->
            val origin = network.stationOrNull(favorite.originId) ?: return@mapNotNull null
            val destination = network.stationOrNull(favorite.destinationId) ?: return@mapNotNull null
            val route = (container.findRoute(origin.id, destination.id, favorite.preference) as? RouteResult.Found)?.route
            WidgetRoute(
                label = favorite.label.ifBlank { "${origin.name(persian)} – ${destination.name(persian)}" },
                from = origin.name(persian),
                to = destination.name(persian),
                minutes = route?.let { ceil(it.totalMinutes - 0.05).toInt().coerceAtLeast(1) },
                lineNumbers = route?.legs?.map { it.line.number }?.distinct().orEmpty(),
                uri = AppLinks.routeUri(origin.id, destination.id, favorite.preference),
            )
        }
        val strings = WidgetStrings(
            title = localized.getString(R.string.widget_title),
            empty = localized.getString(R.string.widget_empty),
            minutes = localized.getString(R.string.minutes_short),
            arrow = if (persian) "←" else "→",
            persian = persian,
        )
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                WidgetContent(context, routes, strings)
            }
        }
    }
}

private data class WidgetStrings(
    val title: String,
    val empty: String,
    val minutes: String,
    val arrow: String,
    val persian: Boolean,
)

@Composable
private fun WidgetContent(context: Context, routes: List<WidgetRoute>, strings: WidgetStrings) {
    val openApp = Intent(context, MainActivity::class.java)
    Column(
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.background)
            .padding(12.dp),
    ) {
        Row(
            GlanceModifier.fillMaxWidth().padding(bottom = 8.dp).clickable(actionStartActivity(openApp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                GlanceModifier.size(26.dp).cornerRadius(13.dp).background(GlanceTheme.colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    ImageProvider(R.drawable.ic_stat_metro),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    modifier = GlanceModifier.size(16.dp),
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            Text(
                strings.title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GlanceTheme.colors.onBackground),
            )
        }
        if (routes.isEmpty()) {
            Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity(openApp)), contentAlignment = Alignment.Center) {
                Text(
                    strings.empty,
                    style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        } else {
            LazyColumn {
                items(routes) { route ->
                    Column(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        RouteRow(context, route, strings)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteRow(context: Context, route: WidgetRoute, strings: WidgetStrings) {
    Row(
        GlanceModifier
            .fillMaxWidth()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivity(AppLinks.intent(context, route.uri))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(GlanceModifier.defaultWeight()) {
            Text(
                route.label,
                maxLines = 1,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
            )
            Spacer(GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                route.lineNumbers.forEach { number ->
                    Box(
                        GlanceModifier.size(8.dp).cornerRadius(4.dp).background(ColorProvider(lineColor(number))),
                    ) {}
                    Spacer(GlanceModifier.width(3.dp))
                }
                Spacer(GlanceModifier.width(3.dp))
                Text(
                    "${route.from} ${strings.arrow} ${route.to}",
                    maxLines = 1,
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        if (route.minutes != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    route.minutes.toString().localizeDigits(strings.persian),
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GlanceTheme.colors.primary),
                )
                Text(strings.minutes, style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant))
            }
        }
    }
}

class FavoritesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavoritesWidget()
}

class GlanceWidgetUpdater(context: Context) : WidgetUpdater {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun refresh() {
        scope.launch { runCatching { FavoritesWidget().updateAll(appContext) } }
    }
}
