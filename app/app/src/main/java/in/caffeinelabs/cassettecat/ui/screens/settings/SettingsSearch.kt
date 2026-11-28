package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.streaming.StreamingProtocol
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.navigation.MainRoute
import `in`.caffeinelabs.cassettecat.R as AppR

// Sheets that open over Settings, rather than pages it navigates to.
internal const val LISTENING_ROOM_SHEET = "sheet/listening_room"
internal const val WHATS_NEW_SHEET = "sheet/whats_new"

/** A page or setting that search can find: its name, the pages above it, and the page that holds it. */
private class SettingsEntry(val title: Int, val path: List<Int>, val route: String)

private val customisation = listOf(AppR.string.settings_customisation)
private val navigation = customisation + AppR.string.customization_startup_library
private val storage = listOf(AppR.string.customization_storage)

// Every settings page, and the settings people look for by name, each with the page it's on.
private val settingsEntries = listOf(
    SettingsEntry(AppR.string.customization_theme, customisation, CustomizationRoute.THEME),
    SettingsEntry(AppR.string.customization_accent_palette, customisation + AppR.string.customization_theme, CustomizationRoute.THEME),
    SettingsEntry(AppR.string.customization_amoled_title, customisation + AppR.string.customization_theme, CustomizationRoute.THEME),
    SettingsEntry(AppR.string.customization_artwork_accent, customisation + AppR.string.customization_theme, CustomizationRoute.THEME),
    SettingsEntry(AppR.string.customization_app_typography, customisation + AppR.string.customization_theme, CustomizationRoute.THEME),
    SettingsEntry(AppR.string.customization_startup_library, customisation, CustomizationRoute.STARTUP_LIBRARY),
    SettingsEntry(AppR.string.customization_start_screen, navigation, CustomizationRoute.STARTUP_LIBRARY),
    SettingsEntry(AppR.string.customization_default_library_tab, navigation, CustomizationRoute.STARTUP_LIBRARY),
    SettingsEntry(AppR.string.customization_nav_tabs, navigation, CustomizationRoute.NAV_TABS),
    SettingsEntry(AppR.string.customization_library_tabs, navigation, CustomizationRoute.LIBRARY_TABS),
    SettingsEntry(AppR.string.customization_home_feed, customisation, CustomizationRoute.HOME_FEED),
    SettingsEntry(AppR.string.customization_now_playing, customisation, CustomizationRoute.NOW_PLAYING),
    SettingsEntry(AppR.string.customization_player_controls, customisation + AppR.string.customization_now_playing, CustomizationRoute.NOW_PLAYING),
    SettingsEntry(AppR.string.customization_seek_bar, customisation + AppR.string.customization_now_playing, CustomizationRoute.NOW_PLAYING),
    SettingsEntry(AppR.string.customization_mini_action, customisation + AppR.string.customization_now_playing, CustomizationRoute.NOW_PLAYING),
    SettingsEntry(AppR.string.customization_gestures, customisation, CustomizationRoute.GESTURES),
    SettingsEntry(AppR.string.customization_mini_swipe, customisation + AppR.string.customization_gestures, CustomizationRoute.GESTURES),
    SettingsEntry(AppR.string.customization_flip_pause, customisation + AppR.string.customization_gestures, CustomizationRoute.GESTURES),
    SettingsEntry(AppR.string.customization_shake_skip, customisation + AppR.string.customization_gestures, CustomizationRoute.GESTURES),
    SettingsEntry(AppR.string.customization_audio_engine, customisation, CustomizationRoute.AUDIO_ENGINE),
    SettingsEntry(AppR.string.customization_crossfade_duration, customisation + AppR.string.customization_audio_engine, CustomizationRoute.AUDIO_ENGINE),
    SettingsEntry(AppR.string.customization_stream_quality_wifi, customisation + AppR.string.customization_audio_engine, CustomizationRoute.AUDIO_ENGINE),
    SettingsEntry(AppR.string.customization_stream_quality_mobile, customisation + AppR.string.customization_audio_engine, CustomizationRoute.AUDIO_ENGINE),
    SettingsEntry(AppR.string.customization_skip_silence, customisation + AppR.string.customization_audio_engine, CustomizationRoute.AUDIO_ENGINE),
    SettingsEntry(AppR.string.customization_replaygain, customisation + AppR.string.customization_audio_engine, CustomizationRoute.AUDIO_ENGINE),
    SettingsEntry(AppR.string.customization_playback, customisation, CustomizationRoute.PLAYBACK),
    SettingsEntry(AppR.string.customization_lyrics, customisation, CustomizationRoute.LYRICS),
    SettingsEntry(AppR.string.customization_lyrics_font_size, customisation + AppR.string.customization_lyrics, CustomizationRoute.LYRICS),
    SettingsEntry(AppR.string.customization_lyrics_for_search, customisation + AppR.string.customization_lyrics, CustomizationRoute.LYRICS),
    SettingsEntry(AppR.string.settings_sleep_timer, emptyList(), MainRoute.SLEEP_TIMER),
    SettingsEntry(AppR.string.settings_equalizer, emptyList(), MainRoute.EQUALIZER),
    SettingsEntry(AppR.string.settings_listening_record, emptyList(), MainRoute.STATS),
    SettingsEntry(AppR.string.settings_listening_room, emptyList(), LISTENING_ROOM_SHEET),
    SettingsEntry(AppR.string.whats_new_title, emptyList(), WHATS_NEW_SHEET),
    SettingsEntry(AppR.string.settings_scan_folders, emptyList(), MainRoute.MANAGE_SCAN_FOLDERS),
    SettingsEntry(AppR.string.downloads_title, emptyList(), MainRoute.DOWNLOADS),
    SettingsEntry(AppR.string.customization_storage, emptyList(), CustomizationRoute.STORAGE),
    SettingsEntry(AppR.string.fix_tags_title, storage, MainRoute.FIX_TAGS),
    SettingsEntry(AppR.string.duplicates_title, storage, MainRoute.DUPLICATES),
    SettingsEntry(AppR.string.settings_subsonic_name, emptyList(), MainRoute.connectServer(StreamingProtocol.SUBSONIC)),
    SettingsEntry(AppR.string.settings_jellyfin_name, emptyList(), MainRoute.connectServer(StreamingProtocol.JELLYFIN)),
    SettingsEntry(AppR.string.scrobbling_title, emptyList(), MainRoute.SCROBBLING),
    SettingsEntry(AppR.string.scrobbling_sync_likes, listOf(AppR.string.scrobbling_title), MainRoute.SCROBBLING),
    SettingsEntry(AppR.string.settings_external_services, emptyList(), MainRoute.EXTERNAL_SERVICES),
    SettingsEntry(AppR.string.desktop_remote_title, emptyList(), MainRoute.DESKTOP_REMOTE),
    SettingsEntry(AppR.string.settings_privacy_security, emptyList(), MainRoute.PRIVACY),
    SettingsEntry(AppR.string.settings_backup_restore, emptyList(), MainRoute.BACKUP_RESTORE),
    SettingsEntry(AppR.string.settings_about_legal, emptyList(), MainRoute.ABOUT_LEGAL),
    SettingsEntry(AppR.string.settings_credits, emptyList(), MainRoute.CREDITS)
)

@Composable
internal fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(AppR.string.settings_search_placeholder), style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_x,
                    contentDescription = stringResource(AppR.string.action_clear),
                    onClick = { onQueryChange("") }
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
}

/** The pages and settings whose name, or a page above them, has every word of [query], each opening its page. */
@Composable
internal fun SettingsSearchResults(query: String, onOpen: (String) -> Unit) {
    val resources = LocalResources.current
    val words = query.lowercase().split(' ').filter { it.isNotBlank() }
    val matches = settingsEntries.mapNotNull { entry ->
        val title = resources.getString(entry.title)
        val path = entry.path.map { resources.getString(it) }
        val text = (listOf(title) + path).joinToString(" ").lowercase()
        if (words.all { it in text }) Triple(entry, title, path) else null
    }.sortedBy { (_, title, _) -> if (title.lowercase().startsWith(words.firstOrNull().orEmpty())) 0 else 1 }

    if (matches.isEmpty()) {
        Text(
            stringResource(AppR.string.settings_search_empty, query),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        return
    }
    Column {
        SettingsSection {
            matches.forEachIndexed { index, (entry, title, path) ->
                if (index > 0) SettingsDivider()
                NavigationRow(
                    title = title,
                    subtitle = path.joinToString(" › ").ifEmpty { stringResource(AppR.string.settings_title) },
                    iconRes = R.drawable.lucide_ic_settings,
                    onClick = { onOpen(entry.route) }
                )
            }
        }
    }
}
