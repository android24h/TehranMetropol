package com.example.tehranmetropoll.presentation.route

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Woman
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.domain.model.DayType
import com.example.tehranmetropoll.domain.model.LastTrainCheck
import com.example.tehranmetropoll.domain.model.LastTrainReminder
import com.example.tehranmetropoll.domain.model.Route
import com.example.tehranmetropoll.domain.model.WomenCars
import com.example.tehranmetropoll.presentation.theme.MetroTheme
import com.example.tehranmetropoll.presentation.util.displayName
import com.example.tehranmetropoll.presentation.util.formatClock
import com.example.tehranmetropoll.presentation.util.isPersian
import com.example.tehranmetropoll.presentation.util.loc

/**
 * Last-train status for the selected route: a quiet note while there is plenty of time, a
 * warning as the latest departure approaches and a strong message once it has passed.
 */
@Composable
fun LastTrainBanner(
    check: LastTrainCheck,
    route: Route,
    reminder: LastTrainReminder?,
    onRemind: () -> Unit,
    onCancelReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest = check.latestDepartureMinute ?: return
    val minutesLeft = check.minutesLeft ?: return
    val accent = when (check.status) {
        LastTrainCheck.Status.MISSED -> MaterialTheme.colorScheme.error
        LastTrainCheck.Status.AT_RISK -> MetroTheme.colors.warning
        else -> MaterialTheme.colorScheme.primary
    }
    val strong = check.status == LastTrainCheck.Status.MISSED
    val shape = RoundedCornerShape(18.dp)
    val origin = route.origin.displayName()

    val (title, subtitle) = when (check.status) {
        LastTrainCheck.Status.MISSED ->
            stringResource(R.string.last_train_missed_title) to
                stringResource(R.string.last_train_missed_body, origin, formatClock(latest))
        LastTrainCheck.Status.AT_RISK ->
            (if (minutesLeft == 0) stringResource(R.string.last_train_leave_now)
            else stringResource(R.string.last_train_at_risk_title, minutesLeft.loc())) to
                stringResource(R.string.last_train_latest, origin, formatClock(latest))
        else -> stringResource(R.string.last_train_safe_title, formatClock(latest)) to null
    }
    val critical = check.criticalLegIndex?.let(route.legs::getOrNull)

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = if (strong) 0.18f else 0.12f))
            .then(if (strong) Modifier.border(1.5.dp, accent, shape) else Modifier)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                when (check.status) {
                    LastTrainCheck.Status.MISSED -> Icons.Rounded.Block
                    LastTrainCheck.Status.AT_RISK -> Icons.Rounded.Schedule
                    else -> Icons.Rounded.NightsStay
                },
                null,
                tint = accent,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = if (check.status == LastTrainCheck.Status.SAFE) MaterialTheme.typography.titleSmall
                    else MaterialTheme.typography.titleMedium,
                    color = if (strong) accent else MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                val details = buildList {
                    check.dayType?.let { add(stringResource(it.label())) }
                    if (critical != null && route.legs.size > 1) {
                        add(stringResource(R.string.last_train_critical, critical.line.displayName(), critical.direction.displayName()))
                    }
                }
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (check.unverified) {
                    Text(
                        stringResource(R.string.last_train_unverified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (check.status != LastTrainCheck.Status.MISSED) {
            Spacer(Modifier.height(4.dp))
            if (reminder != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(36.dp))
                    Icon(Icons.Rounded.NotificationsActive, null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.reminder_active, formatClock(reminder.latestDepartureMinute - reminder.leadMinutes)),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onCancelReminder) { Text(stringResource(R.string.reminder_cancel)) }
                }
            } else {
                Row {
                    Spacer(Modifier.width(24.dp))
                    TextButton(onClick = onRemind) {
                        Icon(Icons.Rounded.AlarmAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.reminder_set_action))
                    }
                }
            }
        }
    }
}

private fun DayType.label(): Int = when (this) {
    DayType.WEEKDAY -> R.string.day_weekday
    DayType.FRIDAY -> R.string.day_friday
    DayType.HOLIDAY -> R.string.day_holiday
}

/** Lets the user pick how long before the latest departure to be reminded. */
@Composable
fun ReminderDialog(
    leads: List<Int>,
    latestDepartureMinute: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.AlarmAdd, null) },
        title = { Text(stringResource(R.string.reminder_dialog_title)) },
        text = {
            if (leads.isEmpty()) {
                Text(stringResource(R.string.reminder_too_late))
            } else {
                Column {
                    leads.forEach { lead ->
                        Text(
                            stringResource(R.string.reminder_option, lead.loc(), formatClock(latestDepartureMinute - lead)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(lead) }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun FavoriteDialog(defaultLabel: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var label by rememberSaveable { mutableStateOf(defaultLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorite_dialog_title)) },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text(stringResource(R.string.favorite_label)) },
                placeholder = { Text(stringResource(R.string.favorite_label_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(label) }, enabled = label.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * Small diagram of the train for one leg, front of the train toward the direction of travel,
 * with the women-only cars highlighted.
 */
@Composable
fun WomenCarsCard(info: WomenCars, modifier: Modifier = Modifier) {
    val women = MetroTheme.colors.women
    val cars = info.womenOnlyCars.sorted().joinToString(if (isPersian) "، " else ", ").loc()
    val position = when (info.position) {
        WomenCars.Position.FRONT -> stringResource(R.string.women_cars_front)
        WomenCars.Position.REAR -> stringResource(R.string.women_cars_rear)
        WomenCars.Position.FRONT_AND_REAR -> stringResource(R.string.women_cars_front_rear)
        WomenCars.Position.MIDDLE -> stringResource(R.string.women_cars_middle)
        WomenCars.Position.MIXED -> stringResource(R.string.women_cars_numbers, cars)
    }
    val description = stringResource(R.string.women_cars_diagram_cd, info.carsPerTrain.loc(), cars)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(women.copy(alpha = 0.08f))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Woman, null, tint = women, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.women_cars_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(position, style = MaterialTheme.typography.titleSmall)
            }
            if (!info.verified) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        stringResource(R.string.data_unverified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Cars are drawn rear → front along the reading direction, so the train "drives" toward
        // the end edge; RTL layouts mirror this automatically.
        Row(
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.women_cars_rear_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (car in info.carsPerTrain downTo 1) {
                    val highlighted = car in info.womenOnlyCars
                    val carShape = if (car == 1) {
                        RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 10.dp, bottomEnd = 4.dp)
                    } else {
                        RoundedCornerShape(3.dp)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clip(carShape)
                            .background(if (highlighted) women else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (highlighted) women else MaterialTheme.colorScheme.outline, carShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            car.loc(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp).size(14.dp),
            )
            Text(stringResource(R.string.women_cars_front_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
