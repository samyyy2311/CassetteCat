package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.BuildConfig
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily

@Composable
fun AboutLegalScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    val appVersion = BuildConfig.VERSION_NAME

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, stringResource(AppR.string.action_back), onBack)
            Column {
                Text(
                    stringResource(AppR.string.about_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    stringResource(AppR.string.about_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(AppR.drawable.ic_app_icon),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(AppR.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            stringResource(AppR.string.about_version, appVersion),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = IbmPlexMonoFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            InfoCard(title = stringResource(AppR.string.about_audio_engine_title)) {
                Text(
                    stringResource(AppR.string.about_audio_engine_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            InfoCard(title = stringResource(AppR.string.about_data_sources_title)) {
                Text(
                    stringResource(AppR.string.about_data_sources_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(AppR.string.about_data_sources_cache),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            InfoCard(title = stringResource(AppR.string.about_privacy_title)) {
                Text(
                    stringResource(AppR.string.about_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(AppR.string.about_privacy_credentials),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            InfoCard(title = stringResource(AppR.string.about_permissions_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PermissionRow(
                        iconRes = R.drawable.lucide_ic_music,
                        iconTint = Color(0xFF38BDF8),
                        title = stringResource(AppR.string.about_permission_music_title),
                        description = stringResource(AppR.string.about_permission_music_description)
                    )
                    PermissionRow(
                        iconRes = R.drawable.lucide_ic_bell,
                        iconTint = Color(0xFFA78BFA),
                        title = stringResource(AppR.string.about_permission_notifications_title),
                        description = stringResource(AppR.string.about_permission_notifications_description)
                    )
                    PermissionRow(
                        iconRes = R.drawable.lucide_ic_radio,
                        iconTint = Color(0xFF10B981),
                        title = stringResource(AppR.string.about_permission_wifi_title),
                        description = stringResource(AppR.string.about_permission_wifi_description)
                    )
                    PermissionRow(
                        iconRes = R.drawable.lucide_ic_bluetooth_connected,
                        iconTint = Color(0xFF10B981),
                        title = stringResource(AppR.string.about_permission_bluetooth_title),
                        description = stringResource(AppR.string.about_permission_bluetooth_description)
                    )
                }
            }

            InfoCard(title = stringResource(AppR.string.about_feedback_title)) {
                Text(
                    stringResource(AppR.string.about_feedback_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(16.dp))

                LegalLinkRow(
                    title = stringResource(AppR.string.about_website),
                    subtitle = "cassettecat.caffeinelabs.in",
                    iconRes = R.drawable.lucide_ic_globe,
                    iconTint = Color(0xFF38BDF8),
                    onClick = { openUrl("https://cassettecat.caffeinelabs.in") }
                )
                LegalLinkRow(
                    title = stringResource(AppR.string.about_privacy_policy),
                    subtitle = "cassettecat.caffeinelabs.in/privacy",
                    iconRes = R.drawable.lucide_ic_shield,
                    iconTint = Color(0xFF10B981),
                    onClick = { openUrl("https://cassettecat.caffeinelabs.in/privacy/") }
                )
                LegalLinkRow(
                    title = stringResource(AppR.string.about_terms),
                    subtitle = "cassettecat.caffeinelabs.in/terms",
                    iconRes = R.drawable.lucide_ic_file_text,
                    iconTint = Color(0xFFA78BFA),
                    onClick = { openUrl("https://cassettecat.caffeinelabs.in/terms/") }
                )
                LegalLinkRow(
                    title = stringResource(AppR.string.about_github),
                    subtitle = "github.com/samyyy2311/CassetteCat",
                    iconRes = AppR.drawable.ic_logo_github,
                    iconTint = Color.Unspecified,
                    onClick = { openUrl("https://github.com/samyyy2311/CassetteCat") }
                )
            }

            Spacer(Modifier.height(listBottomPadding + 20.dp))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    iconRes: Int,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.padding(top = 2.dp).size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun LegalLinkRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.lucide_ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
