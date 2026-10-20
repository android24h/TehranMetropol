package ir.halvaei.metropol.presentation.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.halvaei.metropol.BuildConfig
import ir.halvaei.metropol.R
import ir.halvaei.metropol.domain.model.AppLanguage
import ir.halvaei.metropol.domain.model.ThemeMode
import ir.halvaei.metropol.presentation.components.CircleIconButton
import ir.halvaei.metropol.presentation.components.MetroLinesDecoration
import ir.halvaei.metropol.presentation.theme.MetroTheme
import ir.halvaei.metropol.presentation.util.loc

@Composable
fun AppDrawer(
    themeMode: ThemeMode,
    language: AppLanguage,
    onThemeSelected: (ThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenTutorial: () -> Unit,
    onClose: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            DrawerHeader(onClose)

            DrawerSection(Icons.Rounded.Translate, stringResource(R.string.drawer_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceTile(
                        title = stringResource(R.string.language_english),
                        caption = "EN",
                        selected = language == AppLanguage.ENGLISH,
                        onClick = { onLanguageSelected(AppLanguage.ENGLISH) },
                        modifier = Modifier.weight(1f),
                    )
                    ChoiceTile(
                        title = stringResource(R.string.language_persian),
                        caption = "FA",
                        selected = language == AppLanguage.PERSIAN,
                        onClick = { onLanguageSelected(AppLanguage.PERSIAN) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            DrawerSection(Icons.Rounded.Palette, stringResource(R.string.drawer_appearance)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeTile(Icons.Rounded.LightMode, stringResource(R.string.theme_light), themeMode == ThemeMode.LIGHT, Modifier.weight(1f)) {
                        onThemeSelected(ThemeMode.LIGHT)
                    }
                    ThemeTile(Icons.Rounded.DarkMode, stringResource(R.string.theme_dark), themeMode == ThemeMode.DARK, Modifier.weight(1f)) {
                        onThemeSelected(ThemeMode.DARK)
                    }
                    ThemeTile(Icons.Rounded.BrightnessAuto, stringResource(R.string.theme_system), themeMode == ThemeMode.SYSTEM, Modifier.weight(1f)) {
                        onThemeSelected(ThemeMode.SYSTEM)
                    }
                }
            }

            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onOpenTutorial)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.School, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.drawer_help), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }

            DrawerSection(Icons.Rounded.Code, stringResource(R.string.drawer_developer)) {
                DeveloperCard()
            }

            Text(
                stringResource(R.string.version_label, BuildConfig.VERSION_NAME.loc()) + "  ·  " + stringResource(R.string.map_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun DrawerHeader(onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomEnd = 32.dp))
            .background(MetroTheme.colors.heroGradient),
    ) {
        MetroLinesDecoration(Modifier.matchParentSize(), alpha = 0.3f)
        // Mirrors automatically: points toward the edge the drawer slides back into.
        CircleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.close_menu),
            onClick = onClose,
            size = 40.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 12.dp),
        )
        Column(Modifier.statusBarsPadding().padding(24.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun DrawerSection(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun ChoiceTile(title: String, caption: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ThemeTile(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Profile card: gradient banner, monogram avatar, name/role and tappable contact rows. */
@Composable
private fun DeveloperCard() {
    val context = LocalContext.current
    val phone = stringResource(R.string.developer_phone)
    val email = stringResource(R.string.developer_email)
    val subject = stringResource(R.string.app_name)
    val linkedIn = stringResource(R.string.developer_linkedin_url)
    val gitHub = stringResource(R.string.developer_github_url)
    fun launch(intent: Intent) = try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(104.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(MetroTheme.colors.actionGradient),
            ) {
                MetroLinesDecoration(Modifier.matchParentSize(), alpha = 0.32f)
                Text(
                    stringResource(R.string.developed_by),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            // Avatar overlapping the banner, ringed in the card colour.
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MetroTheme.colors.heroGradient),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.developer_initials),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp)) {
            Text(stringResource(R.string.developer_name), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Code, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.developer_role),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        HorizontalDivider(Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(R.string.developer_cta),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
        )
        Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
            ContactRow(
                icon = rememberVectorPainter(Icons.Rounded.Call),
                accent = Color(0xFF10B981),
                label = stringResource(R.string.mobile),
                value = stringResource(R.string.developer_phone_display).loc(),
            ) { launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
            ContactRow(
                icon = rememberVectorPainter(Icons.Rounded.Email),
                accent = Color(0xFFF43F5E),
                label = stringResource(R.string.email),
                value = email,
            ) {
                launch(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                        .putExtra(Intent.EXTRA_SUBJECT, subject),
                )
            }
            ContactRow(
                icon = painterResource(R.drawable.ic_linkedin),
                accent = Color(0xFF0A66C2),
                label = stringResource(R.string.linkedin),
                value = stringResource(R.string.developer_linkedin_handle),
            ) { launch(Intent(Intent.ACTION_VIEW, Uri.parse(linkedIn))) }
            ContactRow(
                icon = painterResource(R.drawable.ic_github),
                accent = MaterialTheme.colorScheme.onSurface,
                label = stringResource(R.string.github),
                value = stringResource(R.string.developer_github_handle),
            ) { launch(Intent(Intent.ACTION_VIEW, Uri.parse(gitHub))) }
        }
    }
}

@Composable
private fun ContactRow(icon: Painter, accent: Color, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForwardIos,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp),
        )
    }
}
