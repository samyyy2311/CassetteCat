package `in`.caffeinelabs.cassettecat.ui.screens.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R
import androidx.compose.ui.graphics.Color
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
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onBack)
            Text("Credits & Attribution", style = MaterialTheme.typography.headlineSmall)
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
                    append("Cassette")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) { append("Cat") }
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "v$appVersion",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                "An open-source, local-first hi-fi audio player built with precision audio DSP and companion hardware sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "SERVICES & DATA") {
            NavigationRow(
                title = "LRCLIB",
                subtitle = "Real-time synchronized and plain lyrics (lrclib.net)",
                iconRes = AppR.drawable.ic_logo_lrclib,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://lrclib.net") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Deezer",
                subtitle = "Artist photos and high-resolution album artwork (deezer.com)",
                iconRes = AppR.drawable.ic_logo_deezer,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://deezer.com") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Apple iTunes Search API",
                subtitle = "Official high-resolution album artwork catalog (itunes.apple.com)",
                iconRes = AppR.drawable.ic_logo_apple,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://itunes.apple.com") }
            )
            SettingsDivider()
            NavigationRow(
                title = "TheAudioDB",
                subtitle = "Fallback artist images and biographies (theaudiodb.com)",
                iconRes = AppR.drawable.ic_logo_theaudiodb,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://theaudiodb.com") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Radio Browser",
                subtitle = "Community-run internet radio station directory (radio-browser.info)",
                iconRes = R.drawable.lucide_ic_radio,
                onClick = { openUrl("https://radio-browser.info") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Wikipedia & Wikimedia",
                subtitle = "Artist biographies and album background (CC BY-SA 4.0)",
                iconRes = AppR.drawable.ic_logo_wikipedia,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://wikipedia.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = "MusicBrainz",
                subtitle = "Open music encyclopedia and metadata references",
                iconRes = AppR.drawable.ic_logo_musicbrainz,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://musicbrainz.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Cover Art Archive",
                subtitle = "Archival CD and vinyl cover scans (Internet Archive & MusicBrainz)",
                iconRes = AppR.drawable.ic_logo_archive,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://coverartarchive.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = "ListenBrainz",
                subtitle = "Open scrobbling platform and CC0 listening data",
                iconRes = AppR.drawable.ic_logo_listenbrainz,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://listenbrainz.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Libre.fm",
                subtitle = "Free software music scrobbling network (GNU FM)",
                iconRes = AppR.drawable.ic_logo_librefm,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://libre.fm") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Subsonic API",
                subtitle = "Open streaming protocol for Navidrome, Gonic, and Subsonic servers",
                iconRes = AppR.drawable.ic_logo_subsonic,
                iconTint = Color.Unspecified,
                onClick = { openUrl("http://www.subsonic.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Jellyfin API",
                subtitle = "Free software media streaming system and server protocol",
                iconRes = AppR.drawable.ic_logo_jellyfin,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://jellyfin.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = "GitHub",
                subtitle = "Release update checks (github.com)",
                iconRes = AppR.drawable.ic_logo_github,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com") }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "CORE AUDIO & ARCHITECTURE") {
            NavigationRow(
                title = "AutoEq",
                subtitle = "Calibrated headphone equalizer response curves by Jaakko Pasanen",
                iconRes = AppR.drawable.ic_logo_autoeq,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/jaakkopasanen/AutoEq") }
            )
            SettingsDivider()
            NavigationRow(
                title = "AndroidX Media3 (ExoPlayer)",
                subtitle = "Audio streaming, session management, and offline cache",
                iconRes = AppR.drawable.ic_logo_media3,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://developer.android.com/media/media3") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Android AudioFX",
                subtitle = "Parametric equalizer, bass boost, and virtualizer DSP",
                iconRes = AppR.drawable.ic_logo_android,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://developer.android.com/reference/android/media/audiofx/AudioEffect") }
            )
            SettingsDivider()
            NavigationRow(
                title = "KotlinX Coroutines & Serialization",
                subtitle = "Asynchronous flows, reactive state, and atomic JSON persistence",
                iconRes = AppR.drawable.ic_logo_kotlin,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/Kotlin/kotlinx.serialization") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Jetpack DataStore",
                subtitle = "Local-first atomic persistence and data stores",
                iconRes = AppR.drawable.ic_logo_android,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://developer.android.com/topic/libraries/architecture/datastore") }
            )
            SettingsDivider()
            NavigationRow(
                title = "OkHttp",
                subtitle = "High-performance HTTP client for lyrics, artwork, and streaming",
                iconRes = AppR.drawable.ic_logo_okhttp,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/square/okhttp") }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "DESIGN & TYPOGRAPHY") {
            NavigationRow(
                title = "Lucide Icons",
                subtitle = "App logo & open-source iconography (lucide.dev, ISC License)",
                iconRes = AppR.drawable.ic_logo_lucide,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://lucide.dev") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Simple Icons",
                subtitle = "Authentic brand SVG icons (simpleicons.org, CC0 1.0)",
                iconRes = AppR.drawable.ic_logo_simpleicons,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://simpleicons.org") }
            )
            SettingsDivider()
            NavigationRow(
                title = "IBM Plex (Sans & Mono)",
                subtitle = "Designed by Mike Abbink and Bold Monday for IBM (OFL 1.1)",
                iconRes = AppR.drawable.ic_logo_ibm,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://github.com/IBM/plex") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Space Grotesk",
                subtitle = "Proportional monospace display typeface by Florian Karsten (OFL 1.1)",
                iconRes = R.drawable.lucide_ic_type,
                onClick = { openUrl("https://github.com/floriankarsten/space-grotesk") }
            )
            SettingsDivider()
            NavigationRow(
                title = "Jetpack Compose & Material 3",
                subtitle = "Declarative Android UI toolkit and dynamic theming",
                iconRes = AppR.drawable.ic_logo_compose,
                iconTint = Color.Unspecified,
                onClick = { openUrl("https://developer.android.com/jetpack/compose") }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "OPEN SOURCE LICENSE") {
            NavigationRow(
                title = "GNU General Public License v3.0",
                subtitle = "View source code and contribute on GitHub",
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
                    text = "(=^･ω･^=) Cassette Cat Lounge",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Secret Easter Egg Unlocked! Tap any cat to make it purr.",
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
                            contentDescription = "CassetteCat",
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
                    text = "Crafted with 💖 for audiophiles & tape deck enthusiasts.",
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
                    Text("Purr & Close")
                }
            }
        }
    }
}
