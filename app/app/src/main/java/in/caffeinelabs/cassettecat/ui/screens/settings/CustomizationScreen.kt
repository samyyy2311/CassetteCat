package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.settings.AlbumArtCornerStyle
import `in`.caffeinelabs.cassettecat.data.settings.DefaultLibraryTab
import `in`.caffeinelabs.cassettecat.data.settings.DefaultSortMetric
import `in`.caffeinelabs.cassettecat.data.settings.DefaultStartScreen
import `in`.caffeinelabs.cassettecat.data.settings.LyricsActiveStyle
import `in`.caffeinelabs.cassettecat.data.settings.LyricsAlignment
import `in`.caffeinelabs.cassettecat.data.settings.LyricsFontSize
import `in`.caffeinelabs.cassettecat.data.settings.ThemeAccent
import `in`.caffeinelabs.cassettecat.data.settings.TrackRowDensity
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

@Composable
fun CustomizationScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    val cornerLabel = AlbumArtCornerStyle.entries.first { it.radiusDp == prefs.albumArtCornerRadiusDp }.label.substringBefore(" (")
    val crossfadeLabel = if (prefs.crossfadeSeconds == 0) "Crossfade off" else "${prefs.crossfadeSeconds}s crossfade"
    val homeFeedCount = listOf(
        prefs.showHomeRecentlyPlayed,
        prefs.showHomeHeavyRotation,
        prefs.showHomeRecentlyAdded,
        prefs.showHomeForgottenFavorites
    ).count { it }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = listBottomPadding + 32.dp)
    ) {
        CategoryHeader("Customisation", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            NavigationRow(
                title = "Theme",
                subtitle = "${prefs.themeAccent.label} · AMOLED ${if (prefs.amoledDarkTheme) "on" else "off"}",
                iconRes = R.drawable.lucide_ic_palette,
                onClick = { onNavigate(CustomizationRoute.THEME) },
            )
            SettingsDivider()
            NavigationRow(
                title = "Startup & Library",
                subtitle = "${prefs.defaultStartScreen.label} · ${prefs.defaultLibraryTab.label} tab",
                iconRes = R.drawable.lucide_ic_library,
                onClick = { onNavigate(CustomizationRoute.STARTUP_LIBRARY) },
            )
            SettingsDivider()
            NavigationRow(
                title = "Now Playing & Gestures",
                subtitle = "$cornerLabel corners · ${prefs.seekStepSeconds}s seek step",
                iconRes = R.drawable.lucide_ic_hand,
                onClick = { onNavigate(CustomizationRoute.NOW_PLAYING) },
            )
            SettingsDivider()
            NavigationRow(
                title = "Audio Engine",
                subtitle = "$crossfadeLabel · ReplayGain ${if (prefs.replayGainEnabled) "on" else "off"}",
                iconRes = R.drawable.lucide_ic_waves,
                onClick = { onNavigate(CustomizationRoute.AUDIO_ENGINE) },
            )
            SettingsDivider()
            NavigationRow(
                title = "Lyrics",
                subtitle = "${prefs.lyricsFontSize.label} size · ${prefs.lyricsAlignment.label.substringBefore(" (")}",
                iconRes = R.drawable.lucide_ic_file_text,
                onClick = { onNavigate(CustomizationRoute.LYRICS) },
            )
            SettingsDivider()
            NavigationRow(
                title = "Storage & Cache",
                subtitle = "${prefs.maxCacheSizeMb / 1024} GB cache · Auto-cache favourites ${if (prefs.autoCacheFavorites) "on" else "off"}",
                iconRes = R.drawable.lucide_ic_download,
                onClick = { onNavigate(CustomizationRoute.STORAGE) },
            )
            SettingsDivider()
            NavigationRow(
                title = "Home Feed",
                subtitle = "$homeFeedCount of 4 sections shown",
                iconRes = R.drawable.lucide_ic_history,
                onClick = { onNavigate(CustomizationRoute.HOME_FEED) },
            )
        }
    }
}

object CustomizationRoute {
    const val THEME = "main/settings/customization/theme"
    const val STARTUP_LIBRARY = "main/settings/customization/startup_library"
    const val NOW_PLAYING = "main/settings/customization/now_playing"
    const val AUDIO_ENGINE = "main/settings/customization/audio_engine"
    const val LYRICS = "main/settings/customization/lyrics"
    const val STORAGE = "main/settings/customization/storage"
    const val HOME_FEED = "main/settings/customization/home_feed"
}

@Composable
private fun CategoryHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onBack)
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun categoryModifier(modifier: Modifier, listBottomPadding: Dp) = modifier
    .fillMaxSize()
    .verticalScroll(rememberScrollState())
    .padding(top = 8.dp, bottom = listBottomPadding + 32.dp)

@Composable
fun CustomizationThemeScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    Column(modifier = categoryModifier(modifier, listBottomPadding)) {
        CategoryHeader("Theme", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Accent Palette", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Controls primary highlights, active sliders, and indicators",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ThemeAccent.entries.forEach { accent ->
                    val isSelected = prefs.themeAccent == accent
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.tapScale { viewModel.setThemeAccent(accent) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(accent.colorValue))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.lucide_ic_check),
                                    contentDescription = null,
                                    tint = if (accent == ThemeAccent.MONOCHROME_SILVER) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            accent.label.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        SettingsDivider()
        ToggleRow(
            title = "Pure Black (AMOLED Mode)",
            subtitle = "True #000000 background for OLED battery savings and sleek contrast",
            checked = prefs.amoledDarkTheme,
            onCheckedChange = viewModel::setAmoledDarkTheme,
            iconRes = R.drawable.lucide_ic_moon,
        )
        }
    }
}

@Composable
fun CustomizationStartupLibraryScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    Column(modifier = categoryModifier(modifier, listBottomPadding)) {
        CategoryHeader("Startup & Library", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        DefaultStartScreen.entries.forEachIndexed { index, option ->
            val isSelected = prefs.defaultStartScreen == option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setDefaultStartScreen(option) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { viewModel.setDefaultStartScreen(option) },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.tertiary)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when (option) {
                            DefaultStartScreen.HOME -> "Open to your personalized Home dashboard"
                            DefaultStartScreen.LIBRARY -> "Open directly to your music Library"
                            DefaultStartScreen.LAST_OPENED -> "Resume on whichever tab you used last"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (index != DefaultStartScreen.entries.lastIndex) SettingsDivider(startPadding = 56.dp)
        }
        SettingsDivider()
        SheetPickerRow(
            title = "Default Library Tab",
            subtitle = "Which section to show when opening the Library",
            iconRes = R.drawable.lucide_ic_library,
            options = DefaultLibraryTab.entries,
            selected = prefs.defaultLibraryTab,
            label = { it.label },
            onSelect = viewModel::setDefaultLibraryTab
        )
        SettingsDivider()
        SheetPickerRow(
            title = "Grid Columns",
            subtitle = "Card sizing for albums & artists",
            iconRes = R.drawable.lucide_ic_grid_2x2,
            options = listOf(2, 3, 4),
            selected = prefs.gridColumnCount,
            label = { "$it Columns" },
            onSelect = viewModel::setGridColumnCount
        )
        SettingsDivider()
        SheetPickerRow(
            title = "Song Row Density",
            subtitle = "Detailed adds album & quality badges; Compact fits more tracks",
            iconRes = R.drawable.lucide_ic_list,
            options = TrackRowDensity.entries,
            selected = prefs.trackRowDensity,
            label = { it.label },
            onSelect = viewModel::setTrackRowDensity
        )
        SettingsDivider()
        SheetPickerRow(
            title = "Default Sort Order",
            subtitle = "How the song list is sorted when you first open it",
            iconRes = R.drawable.lucide_ic_arrow_up_down,
            options = DefaultSortMetric.entries,
            selected = prefs.defaultSortMetric,
            label = { it.label },
            onSelect = viewModel::setDefaultSortMetric
        )
        SettingsDivider()
        ToggleRow(
            title = "Show Audio Format Badges",
            subtitle = "Display FLAC, MP3, AAC, HI-RES pills on track rows",
            checked = prefs.showAudioQualityBadge,
            onCheckedChange = viewModel::setShowAudioQualityBadge,
            iconRes = R.drawable.lucide_ic_music,
        )
        }
    }
}

@Composable
fun CustomizationNowPlayingScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    Column(modifier = categoryModifier(modifier, listBottomPadding)) {
        CategoryHeader("Now Playing & Gestures", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        SheetPickerRow(
            title = "Album Art Corner Style",
            subtitle = "Modern rounded corners or classic vinyl square edges",
            iconRes = R.drawable.lucide_ic_disc,
            options = AlbumArtCornerStyle.entries,
            selected = AlbumArtCornerStyle.entries.first { it.radiusDp == prefs.albumArtCornerRadiusDp },
            label = { it.label },
            onSelect = { viewModel.setAlbumArtCornerRadiusDp(it.radiusDp) }
        )
        SettingsDivider()
        ToggleRow(
            title = "Blurred Background",
            subtitle = "Ambient blurred album art behind the Now Playing screen",
            checked = prefs.showNowPlayingBlur,
            onCheckedChange = viewModel::setShowNowPlayingBlur,
            iconRes = R.drawable.lucide_ic_layers,
        )
        SettingsDivider()
        ToggleRow(
            title = "Show Remaining Time",
            subtitle = "Display countdown time (e.g. -02:45) instead of total song length",
            checked = prefs.showRemainingTime,
            onCheckedChange = viewModel::setShowRemainingTime,
            iconRes = R.drawable.lucide_ic_timer,
        )
        SettingsDivider()
        SheetPickerRow(
            title = "Fast Seek Jump Step",
            subtitle = "Duration to jump when scrubbing",
            iconRes = R.drawable.lucide_ic_zap,
            options = listOf(5, 10, 15, 30),
            selected = prefs.seekStepSeconds,
            label = { "${it}s" },
            onSelect = viewModel::setSeekStepSeconds
        )
        SettingsDivider()
        ToggleRow(
            title = "Double-Tap Sides to Seek",
            subtitle = "Double tap left side to rewind, right side to fast-forward",
            checked = prefs.doubleTapSeekEnabled,
            onCheckedChange = viewModel::setDoubleTapSeekEnabled,
            iconRes = R.drawable.lucide_ic_hand,
        )
        SettingsDivider()
        ToggleRow(
            title = "Swipe Up on Art for Lyrics",
            subtitle = "Quickly reveal synchronised lyrics with an upward swipe",
            checked = prefs.swipeUpLyricsEnabled,
            onCheckedChange = viewModel::setSwipeUpLyricsEnabled,
            iconRes = R.drawable.lucide_ic_chevrons_up,
        )
        }
    }
}

@Composable
fun CustomizationAudioEngineScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    Column(modifier = categoryModifier(modifier, listBottomPadding)) {
        CategoryHeader("Audio Engine", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        SheetPickerRow(
            title = "Crossfade Duration",
            subtitle = "Fade out and in around track transitions",
            iconRes = R.drawable.lucide_ic_square_stack,
            options = listOf(0, 2, 4, 6, 8, 12),
            selected = prefs.crossfadeSeconds,
            label = { if (it == 0) "Off" else "${it}s" },
            onSelect = viewModel::setCrossfadeSeconds
        )
        SettingsDivider()
        ToggleRow(
            title = "Gapless Playback",
            subtitle = "Eliminate silent gaps between live concert and continuous album tracks",
            checked = prefs.gaplessPlayback,
            onCheckedChange = viewModel::setGaplessPlayback,
            iconRes = R.drawable.lucide_ic_disc,
        )
        SettingsDivider()
        ToggleRow(
            title = "Volume Normalisation (ReplayGain)",
            subtitle = "Balance loudness across tracks to avoid sudden volume spikes",
            checked = prefs.replayGainEnabled,
            onCheckedChange = viewModel::setReplayGainEnabled,
            iconRes = R.drawable.lucide_ic_waves,
        )
        SettingsDivider()
        SheetPickerRow(
            title = "ReplayGain Pre-Amp Gain",
            subtitle = "Boost or attenuate level for tracks without loudness tags",
            iconRes = R.drawable.lucide_ic_gauge,
            options = listOf(-6, -3, 0, 3, 6),
            selected = prefs.replayGainPreAmpDb,
            label = { if (it > 0) "+${it}dB" else "${it}dB" },
            onSelect = viewModel::setReplayGainPreAmpDb
        )
        SettingsDivider()
        ToggleRow(
            title = "Mono Audio Downmix",
            subtitle = "Combine stereo channels into mono for single-earbud listening",
            checked = prefs.monoAudio,
            onCheckedChange = viewModel::setMonoAudio,
            iconRes = R.drawable.lucide_ic_speaker,
        )
        SettingsDivider()
        ToggleRow(
            title = "Resume Queue on Launch",
            subtitle = "Auto-restore active track and queue on startup",
            checked = prefs.resumeQueueOnLaunch,
            onCheckedChange = viewModel::setResumeQueueOnLaunch,
            iconRes = R.drawable.lucide_ic_play,
        )
        SettingsDivider()
        ToggleRow(
            title = "Pause on Disconnect",
            subtitle = "Pause music when headphones or Bluetooth disconnect",
            checked = prefs.pauseOnHeadphoneDisconnect,
            onCheckedChange = viewModel::setPauseOnHeadphoneDisconnect,
            iconRes = R.drawable.lucide_ic_headphones,
        )
        SettingsDivider()
        ToggleRow(
            title = "Tactile Haptic Feedback",
            subtitle = "Vibration ticks when scrubbing, scrolling, and tapping",
            checked = prefs.hapticFeedbackEnabled,
            onCheckedChange = viewModel::setHapticFeedbackEnabled,
            iconRes = R.drawable.lucide_ic_vibrate,
        )
        }
    }
}

@Composable
fun CustomizationLyricsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    Column(modifier = categoryModifier(modifier, listBottomPadding)) {
        CategoryHeader("Lyrics", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        SheetPickerRow(
            title = "Lyrics Font Size",
            subtitle = "Synced lyrics typography scale",
            iconRes = R.drawable.lucide_ic_type,
            options = LyricsFontSize.entries,
            selected = prefs.lyricsFontSize,
            label = { it.label },
            onSelect = viewModel::setLyricsFontSize
        )
        SettingsDivider()
        SheetPickerRow(
            title = "Lyrics Text Alignment",
            subtitle = "Karaoke centre alignment or editorial left align",
            iconRes = R.drawable.lucide_ic_text_align_center,
            options = LyricsAlignment.entries,
            selected = prefs.lyricsAlignment,
            label = { it.label },
            onSelect = viewModel::setLyricsAlignment
        )
        SettingsDivider()
        SheetPickerRow(
            title = "Active Line Focus Style",
            subtitle = "Highlight the current sung lyric in accent or clean white",
            iconRes = R.drawable.lucide_ic_highlighter,
            options = LyricsActiveStyle.entries,
            selected = prefs.lyricsActiveStyle,
            label = { it.label },
            onSelect = viewModel::setLyricsActiveStyle
        )
        SettingsDivider()
        ToggleRow(
            title = "Keep Screen Awake for Lyrics",
            subtitle = "Prevent display sleep while viewing live lyrics",
            checked = prefs.keepScreenOnLyrics,
            onCheckedChange = viewModel::setKeepScreenOnLyrics,
            iconRes = R.drawable.lucide_ic_sun,
        )
        SettingsDivider()
        ToggleRow(
            title = "Prioritize Local .lrc Sidecars",
            subtitle = "Use local .lrc files from media folders before online lookup",
            checked = prefs.localLrcPriority,
            onCheckedChange = viewModel::setLocalLrcPriority,
            iconRes = R.drawable.lucide_ic_file_text,
        )
        }
    }
}

@Composable
fun CustomizationStorageScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    Column(modifier = categoryModifier(modifier, listBottomPadding)) {
        CategoryHeader("Storage & Cache", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        SheetPickerRow(
            title = "Streaming Cache Quota",
            subtitle = "Max device storage for caching streamed audio",
            iconRes = R.drawable.lucide_ic_download,
            options = listOf(1024, 2048, 5120, 10240),
            selected = prefs.maxCacheSizeMb,
            label = { "${it / 1024} GB" },
            onSelect = viewModel::setMaxCacheSizeMb
        )
        SettingsDivider()
        ToggleRow(
            title = "Auto-Cache Favourite Tracks",
            subtitle = "Automatically download and cache tracks when marked as favourite",
            checked = prefs.autoCacheFavorites,
            onCheckedChange = viewModel::setAutoCacheFavorites,
            iconRes = R.drawable.lucide_ic_heart,
        )
        SettingsDivider()
        ToggleRow(
            title = "Ignore Short Audio Clips",
            subtitle = "Hide audio files shorter than 30 seconds (ringtones/voice notes)",
            checked = prefs.ignoreShortAudioClips,
            onCheckedChange = viewModel::setIgnoreShortAudioClips,
            iconRes = R.drawable.lucide_ic_clock,
        )
        }
    }
}

@Composable
fun CustomizationHomeFeedScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    Column(modifier = categoryModifier(modifier, listBottomPadding)) {
        CategoryHeader("Home Feed", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        ToggleRow(
            title = "Recently Played",
            subtitle = "Surface tracks you recently listened to",
            checked = prefs.showHomeRecentlyPlayed,
            onCheckedChange = viewModel::setShowHomeRecentlyPlayed,
            iconRes = R.drawable.lucide_ic_history,
        )
        SettingsDivider()
        ToggleRow(
            title = "Heavy Rotation",
            subtitle = "Your top played tracks this month",
            checked = prefs.showHomeHeavyRotation,
            onCheckedChange = viewModel::setShowHomeHeavyRotation,
            iconRes = R.drawable.lucide_ic_flame,
        )
        SettingsDivider()
        ToggleRow(
            title = "Recently Added",
            subtitle = "New tracks imported to your library",
            checked = prefs.showHomeRecentlyAdded,
            onCheckedChange = viewModel::setShowHomeRecentlyAdded,
            iconRes = R.drawable.lucide_ic_clock_plus,
        )
        SettingsDivider()
        ToggleRow(
            title = "Forgotten Favourites",
            subtitle = "Loved tracks you haven't played in a while",
            checked = prefs.showHomeForgottenFavorites,
            onCheckedChange = viewModel::setShowHomeForgottenFavorites,
            iconRes = R.drawable.lucide_ic_heart,
        )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SheetPickerRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
    onSelect: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale { open = true }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label(selected), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.lucide_ic_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
    if (open) {
        FullOpenBottomSheet(onDismiss = { open = false }) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 12.dp)
            )
            options.forEach { option ->
                val isSelected = option == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option); open = false }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
