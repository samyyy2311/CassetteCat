package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.BuildConfig
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.ApprovedEasterEggCats
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily

@Composable
fun CreditsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
    val haptic = LocalHapticFeedback.current
    var easterEggTaps by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }

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
            Text(stringResource(AppR.string.credits_title), style = MaterialTheme.typography.headlineSmall)
        }

        val appVersion = BuildConfig.VERSION_NAME

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    easterEggTaps++
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (easterEggTaps >= 5) {
                        easterEggTaps = 0
                        showEasterEgg = true
                    }
                }
        ) {
            Text(
                buildAnnotatedString {
                    append(stringResource(AppR.string.credits_brand_prefix))
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                        append(stringResource(AppR.string.credits_brand_accent))
                    }
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                stringResource(AppR.string.credits_version, appVersion),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                stringResource(AppR.string.credits_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.credits_services_section)) {
            NavigationRow(
                title = stringResource(AppR.string.credits_lrclib),
                subtitle = stringResource(AppR.string.credits_lrclib_description),
                iconRes = AppR.drawable.ic_logo_lrclib,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://lrclib.net") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_deezer),
                subtitle = stringResource(AppR.string.credits_deezer_description),
                iconRes = AppR.drawable.ic_logo_deezer,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://deezer.com") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_itunes),
                subtitle = stringResource(AppR.string.credits_itunes_description),
                iconRes = AppR.drawable.ic_logo_apple,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://itunes.apple.com") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_audiodb),
                subtitle = stringResource(AppR.string.credits_audiodb_description),
                iconRes = AppR.drawable.ic_logo_theaudiodb,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://theaudiodb.com") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_radio_browser),
                subtitle = stringResource(AppR.string.credits_radio_browser_description),
                iconRes = R.drawable.lucide_ic_radio,
                onClick = { openUrl("https://radio-browser.info") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_wikipedia),
                subtitle = stringResource(AppR.string.credits_wikipedia_description),
                iconRes = AppR.drawable.ic_logo_wikipedia,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://wikipedia.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_musicbrainz),
                subtitle = stringResource(AppR.string.credits_musicbrainz_description),
                iconRes = AppR.drawable.ic_logo_musicbrainz,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://musicbrainz.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_cover_art_archive),
                subtitle = stringResource(AppR.string.credits_cover_art_archive_description),
                iconRes = AppR.drawable.ic_logo_archive,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://coverartarchive.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_listenbrainz),
                subtitle = stringResource(AppR.string.credits_listenbrainz_description),
                iconRes = AppR.drawable.ic_logo_listenbrainz,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://listenbrainz.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_librefm),
                subtitle = stringResource(AppR.string.credits_librefm_description),
                iconRes = AppR.drawable.ic_logo_librefm,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://libre.fm") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_subsonic),
                subtitle = stringResource(AppR.string.credits_subsonic_description),
                iconRes = AppR.drawable.ic_logo_subsonic,
                iconTint = Color.Unspecified,
                onClick = { openUrl("http://www.subsonic.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_jellyfin),
                subtitle = stringResource(AppR.string.credits_jellyfin_description),
                iconRes = AppR.drawable.ic_logo_jellyfin,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://jellyfin.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_github),
                subtitle = stringResource(AppR.string.credits_github_description),
                iconRes = AppR.drawable.ic_logo_github,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com") }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.credits_audio_section)) {
            NavigationRow(
                title = stringResource(AppR.string.credits_autoeq),
                subtitle = stringResource(AppR.string.credits_autoeq_description),
                iconRes = AppR.drawable.ic_logo_autoeq,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/jaakkopasanen/AutoEq") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_media3),
                subtitle = stringResource(AppR.string.credits_media3_description),
                iconRes = AppR.drawable.ic_logo_media3,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://developer.android.com/media/media3") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_audiofx),
                subtitle = stringResource(AppR.string.credits_audiofx_description),
                iconRes = AppR.drawable.ic_logo_android,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://developer.android.com/reference/android/media/audiofx/AudioEffect") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_kotlinx),
                subtitle = stringResource(AppR.string.credits_kotlinx_description),
                iconRes = AppR.drawable.ic_logo_kotlin,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/Kotlin/kotlinx.serialization") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_datastore),
                subtitle = stringResource(AppR.string.credits_datastore_description),
                iconRes = AppR.drawable.ic_logo_android,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://developer.android.com/topic/libraries/architecture/datastore") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_okhttp),
                subtitle = stringResource(AppR.string.credits_okhttp_description),
                iconRes = AppR.drawable.ic_logo_okhttp,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/square/okhttp") }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.credits_design_section)) {
            NavigationRow(
                title = stringResource(AppR.string.credits_lucide),
                subtitle = stringResource(AppR.string.credits_lucide_description),
                iconRes = AppR.drawable.ic_logo_lucide,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://lucide.dev") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_simple_icons),
                subtitle = stringResource(AppR.string.credits_simple_icons_description),
                iconRes = AppR.drawable.ic_logo_simpleicons,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://simpleicons.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_ibm_plex),
                subtitle = stringResource(AppR.string.credits_ibm_plex_description),
                iconRes = AppR.drawable.ic_logo_ibm,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/IBM/plex") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_space_grotesk),
                subtitle = stringResource(AppR.string.credits_space_grotesk_description),
                iconRes = R.drawable.lucide_ic_type,
                onClick = { openUrl("https://github.com/floriankarsten/space-grotesk") }
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.credits_compose),
                subtitle = stringResource(AppR.string.credits_compose_description),
                iconRes = AppR.drawable.ic_logo_compose,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://developer.android.com/jetpack/compose") }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(AppR.string.credits_license_section)) {
            NavigationRow(
                title = stringResource(AppR.string.credits_gpl),
                subtitle = stringResource(AppR.string.credits_gpl_description),
                iconRes = AppR.drawable.ic_logo_gpl,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/samyyy2311/CassetteCat") }
            )
        }

        Spacer(Modifier.height(listBottomPadding + 24.dp))
    }

    if (showEasterEgg) {
        FullOpenBottomSheet(onDismiss = { showEasterEgg = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(AppR.string.credits_easter_egg_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = stringResource(AppR.string.credits_easter_egg_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ApprovedEasterEggCats.forEach { catRes ->
                        Image(
                            painter = painterResource(catRes),
                            contentDescription = stringResource(AppR.string.credits_easter_egg_cat_description),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(72.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                        )
                    }
                }

                Text(
                    text = stringResource(AppR.string.credits_easter_egg_footer),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEasterEgg = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Text(stringResource(AppR.string.credits_easter_egg_close))
                }
            }
        }
    }
}
