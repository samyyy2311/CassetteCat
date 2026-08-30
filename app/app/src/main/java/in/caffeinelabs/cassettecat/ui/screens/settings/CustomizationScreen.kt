package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.download.DEFAULT_MAX_CACHE_BYTES
import `in`.caffeinelabs.cassettecat.data.download.DOWNLOAD_CACHE_LIMIT_OPTIONS_MB
import `in`.caffeinelabs.cassettecat.data.settings.AlbumArtCornerStyle
import `in`.caffeinelabs.cassettecat.data.settings.AppFontFamily
import `in`.caffeinelabs.cassettecat.data.settings.DefaultLibraryTab
import `in`.caffeinelabs.cassettecat.data.settings.DefaultSortMetric
import `in`.caffeinelabs.cassettecat.data.settings.DefaultStartScreen
import `in`.caffeinelabs.cassettecat.data.settings.HomeSection
import `in`.caffeinelabs.cassettecat.data.settings.LyricsActiveStyle
import `in`.caffeinelabs.cassettecat.data.settings.LyricsAlignment
import `in`.caffeinelabs.cassettecat.data.settings.LyricsFontFamily
import `in`.caffeinelabs.cassettecat.data.settings.LyricsFontSize
import `in`.caffeinelabs.cassettecat.data.settings.MiniPlayerAction
import `in`.caffeinelabs.cassettecat.data.settings.NowPlayingBackdropStyle
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    val cornerLabel = AlbumArtCornerStyle.entries.first { it.radiusDp == prefs.albumArtCornerRadiusDp }.label.substringBefore(" (")
    val crossfadeLabel = if (prefs.crossfadeSeconds == 0) "Crossfade off" else "${prefs.crossfadeSeconds}s crossfade"
    val homeFeedCount = remember(
        prefs.showHomeRecentlyPlayed,
        prefs.showHomeHeavyRotation,
        prefs.showHomeRecentlyAdded,
        prefs.showHomeForgottenFavorites
    ) {
        listOf(
            prefs.showHomeRecentlyPlayed,
            prefs.showHomeHeavyRotation,
            prefs.showHomeRecentlyAdded,
            prefs.showHomeForgottenFavorites
        ).count { it }
    }

    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
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
                subtitle = "$cornerLabel corners",
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
                subtitle = "Manage offline downloads and local library filtering",
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
    const val LIBRARY_TABS = "main/settings/customization/library_tabs"
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
private fun Modifier.categoryModifier(listBottomPadding: Dp) = this
    .fillMaxSize()
    .verticalScroll(rememberScrollState())
    .padding(top = 8.dp, bottom = listBottomPadding + 32.dp)

@Composable
fun CustomizationThemeScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
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
            var showCustomColorSheet by remember { mutableStateOf(false) }
            val accentColumns = 4
            val accentRows = ThemeAccent.entries.toList().chunked(accentColumns)
            accentRows.forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { accent ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (accent == ThemeAccent.CUSTOM) {
                                val isCustomSelected = prefs.themeAccent == ThemeAccent.CUSTOM
                                AccentSwatchItem(
                                    color = Color(prefs.customAccentColor),
                                    label = "Custom",
                                    isSelected = isCustomSelected,
                                    iconRes = R.drawable.lucide_ic_pipette,
                                    iconSize = 18.dp,
                                    onClick = { showCustomColorSheet = true }
                                )
                            } else {
                                val isSelected = prefs.themeAccent == accent
                                AccentSwatchItem(
                                    color = Color(accent.colorValue),
                                    label = accent.label.substringBefore(" "),
                                    isSelected = isSelected,
                                    iconRes = if (isSelected) R.drawable.lucide_ic_check else null,
                                    iconTint = if (accent == ThemeAccent.MONOCHROME_SILVER) Color.Black else Color.White,
                                    onClick = { viewModel.setThemeAccent(accent) }
                                )
                            }
                        }
                    }
                    repeat(accentColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
                if (rowIndex != accentRows.lastIndex) Spacer(Modifier.height(16.dp))
            }
            if (showCustomColorSheet) {
                CustomAccentColorSheet(
                    currentColor = prefs.customAccentColor,
                    onApply = { color ->
                        viewModel.setCustomAccentColor(color)
                        showCustomColorSheet = false
                    },
                    onDismiss = { showCustomColorSheet = false }
                )
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
        SettingsDivider()
        ToggleRow(
            title = "Artwork Accent",
            subtitle = "Use the current album artwork colour while music is playing",
            checked = prefs.artworkAccentEnabled,
            onCheckedChange = viewModel::setArtworkAccentEnabled,
            iconRes = R.drawable.lucide_ic_palette,
        )
        SettingsDivider()
        ToggleRow(
            title = "Mini-Player Progress",
            subtitle = "Show a thin progress line on the mini-player",
            checked = prefs.showMiniPlayerProgress,
            onCheckedChange = viewModel::setShowMiniPlayerProgress,
            iconRes = R.drawable.lucide_ic_activity,
        )
        SettingsDivider()
        SheetPickerRow(
            title = "Mini-Player Action",
            subtitle = "Choose the button shown beside play and pause",
            iconRes = R.drawable.lucide_ic_mouse_pointer_click,
            options = MiniPlayerAction.entries,
            selected = prefs.miniPlayerAction,
            label = { it.label },
            onSelect = viewModel::setMiniPlayerAction
        )
        SettingsDivider()
        SheetPickerRow(
            title = "App Typography",
            subtitle = "Choose the font family used throughout the app",
            iconRes = R.drawable.lucide_ic_type,
            options = AppFontFamily.entries,
            selected = prefs.appFontFamily,
            label = { it.shortName },
            sheetLabel = { it.label },
            onSelect = viewModel::setAppFontFamily
        )
        }
    }
}

@Composable
internal fun AccentSwatchItem(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconRes: Int? = null,
    iconSize: Dp = 20.dp,
    iconTint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.tapScale(onClick)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomAccentColorSheet(
    currentColor: Long,
    onApply: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var hexText by remember { mutableStateOf(String.format("%06X", currentColor and 0xFFFFFF)) }
    val parsedColor = remember(hexText) {
        val clean = hexText.trim().removePrefix("#")
        if (clean.length == 6 && clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            0xFF000000 or clean.toLong(16)
        } else {
            null
        }
    }
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Custom Accent Colour", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(parsedColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = hexText,
                onValueChange = { hexText = it },
                label = { Text("Hex colour") },
                placeholder = { Text("C23B30") },
                leadingIcon = { Text("#", style = MaterialTheme.typography.bodyLarge) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { parsedColor?.let(onApply) },
                enabled = parsedColor != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
fun CustomizationStartupLibraryScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToLibraryTabs: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    val visibleTabs = prefs.libraryTabOrder.filterNot { it in prefs.hiddenLibraryTabs }
    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
        CategoryHeader("Startup & Library", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        SheetPickerRow(
            title = "Start Screen",
            subtitle = "Choose where CassetteCat opens",
            iconRes = R.drawable.lucide_ic_house,
            options = DefaultStartScreen.entries,
            selected = prefs.defaultStartScreen,
            label = { it.label },
            onSelect = viewModel::setDefaultStartScreen
        )
        SettingsDivider()
        SheetPickerRow(
            title = "Default Library Tab",
            subtitle = "Which section to show when opening the Library",
            iconRes = R.drawable.lucide_ic_library,
            options = visibleTabs,
            selected = prefs.defaultLibraryTab.takeIf { it in visibleTabs } ?: visibleTabs.first(),
            label = { it.label },
            onSelect = viewModel::setDefaultLibraryTab
        )
        SettingsDivider()
        NavigationRow(
            title = "Library Tabs",
            subtitle = "${visibleTabs.size} of ${prefs.libraryTabOrder.size} shown · Reorder and visibility",
            iconRes = R.drawable.lucide_ic_list,
            onClick = onNavigateToLibraryTabs
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
        CategoryHeader("Now Playing & Gestures", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        SheetPickerRow(
            title = "Album Art Corner Style",
            subtitle = "Modern rounded corners or classic vinyl square edges",
            iconRes = R.drawable.lucide_ic_disc,
            options = AlbumArtCornerStyle.entries,
            selected = AlbumArtCornerStyle.entries.first { it.radiusDp == prefs.albumArtCornerRadiusDp },
            label = { it.shortName },
            sheetLabel = { it.label },
            sheetSubtitle = {
                when (it) {
                    AlbumArtCornerStyle.CURVED -> "Modern 16dp rounded squircle"
                    AlbumArtCornerStyle.SOFT -> "Subtle 8dp softened corners"
                    AlbumArtCornerStyle.SQUARE -> "Classic 0dp vinyl square edges"
                }
            },
            onSelect = { viewModel.setAlbumArtCornerRadiusDp(it.radiusDp) }
        )
        SettingsDivider()
        SheetPickerRow(
            title = "Backdrop Style",
            subtitle = "Atmospheric visual theme for Now Playing",
            iconRes = R.drawable.lucide_ic_image,
            options = NowPlayingBackdropStyle.entries,
            selected = prefs.nowPlayingBackdropStyle,
            label = { it.label },
            sheetSubtitle = { it.description },
            optionLeading = { item, isSheet -> BackdropStylePreviewDot(item, size = if (isSheet) 24.dp else 12.dp) },
            onSelect = viewModel::setNowPlayingBackdropStyle
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
        ToggleRow(
            title = "Mini-Player Swipe to Skip",
            subtitle = "Swipe horizontally on the mini-player to quickly switch tracks",
            checked = prefs.miniPlayerSwipeToSkip,
            onCheckedChange = viewModel::setMiniPlayerSwipeToSkip,
            iconRes = R.drawable.lucide_ic_move_horizontal,
        )
        SettingsDivider()
        ToggleRow(
            title = "Swipe Up on Art for Lyrics",
            subtitle = "Quickly reveal synchronised lyrics with an upward swipe",
            checked = prefs.swipeUpLyricsEnabled,
            onCheckedChange = viewModel::setSwipeUpLyricsEnabled,
            iconRes = R.drawable.lucide_ic_chevrons_up,
        )
        SettingsDivider()
        ToggleRow(
            title = "Flip to Pause",
            subtitle = "Place your phone face-down to pause music, pick up to resume",
            checked = prefs.flipToPauseEnabled,
            onCheckedChange = viewModel::setFlipToPauseEnabled,
            iconRes = R.drawable.lucide_ic_rotate_ccw,
        )
        SettingsDivider()
        ToggleRow(
            title = "Wave to Skip",
            subtitle = "Wave your finger over the top proximity sensor to skip to the next track",
            checked = prefs.proximityWaveSkipEnabled,
            onCheckedChange = viewModel::setProximityWaveSkipEnabled,
            iconRes = R.drawable.lucide_ic_hand,
        )
        SettingsDivider()
        ToggleRow(
            title = "Shake to Skip",
            subtitle = "Quickly shake your phone to skip to the next track",
            checked = prefs.shakeToSkipEnabled,
            onCheckedChange = viewModel::setShakeToSkipEnabled,
            iconRes = R.drawable.lucide_ic_smartphone,
        )
        if (prefs.shakeToSkipEnabled) {
            SettingsDivider()
            SettingsSliderRow(
                title = "Shake Sensitivity",
                subtitle = "Adjust how firmly you need to shake the device",
                value = prefs.shakeSensitivity.toFloat(),
                valueRange = 1f..5f,
                steps = 3,
                label = when (prefs.shakeSensitivity) {
                    1 -> "Gentle"
                    2 -> "Light"
                    3 -> "Medium"
                    4 -> "Firm"
                    else -> "Strong"
                },
                iconRes = R.drawable.lucide_ic_sliders_horizontal,
                onValueChange = { viewModel.setShakeSensitivity(it.roundToInt()) }
            )
        }
        }
    }
}

@Composable
fun CustomizationAudioEngineScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
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
            title = "Volume Limit (Ear Protection)",
            subtitle = "Cap the maximum playback level regardless of system volume",
            checked = prefs.volumeLimitEnabled,
            onCheckedChange = viewModel::setVolumeLimitEnabled,
            iconRes = R.drawable.lucide_ic_ear,
        )
        if (prefs.volumeLimitEnabled) {
            SettingsDivider()
            SheetPickerRow(
                title = "Maximum Volume",
                subtitle = "Loudest level the app will ever play at",
                iconRes = R.drawable.lucide_ic_volume_1,
                options = listOf(50, 60, 70, 80, 90, 100),
                selected = prefs.maxVolumePercent,
                label = { "$it%" },
                onSelect = viewModel::setMaxVolumePercent
            )
        }
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
            title = "Autoplay",
            subtitle = "Keep playing similar songs when the queue ends",
            checked = prefs.autoplayEnabled,
            onCheckedChange = viewModel::setAutoplayEnabled,
            iconRes = R.drawable.lucide_ic_infinity,
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
            title = "Auto Drive Mode",
            subtitle = "Automatically open Drive Mode when connected to car Bluetooth audio",
            checked = prefs.autoDriveModeBluetooth,
            onCheckedChange = viewModel::setAutoDriveModeBluetooth,
            iconRes = R.drawable.lucide_ic_car,
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
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
            title = "Lyrics Font Family",
            subtitle = "Typography personality for karaoke lyrics",
            iconRes = R.drawable.lucide_ic_type,
            options = LyricsFontFamily.entries,
            selected = prefs.lyricsFontFamily,
            label = { it.label },
            onSelect = viewModel::setLyricsFontFamily
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    val maxCacheBytes by viewModel.maxCacheBytes.collectAsStateWithLifecycle(initialValue = DEFAULT_MAX_CACHE_BYTES)
    val autoDownloadFavorites by viewModel.autoDownloadFavorites.collectAsStateWithLifecycle(initialValue = false)
    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
        CategoryHeader("Storage & Cache", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
        SheetPickerRow(
            title = "Streaming Cache Quota",
            subtitle = "Max device storage for caching streamed audio",
            iconRes = R.drawable.lucide_ic_download,
            options = DOWNLOAD_CACHE_LIMIT_OPTIONS_MB,
            selected = (maxCacheBytes / 1024 / 1024).toInt(),
            label = { if (it < 1024) "$it MB" else "${it / 1024} GB" },
            onSelect = viewModel::setMaxCacheSizeMb
        )
        SettingsDivider()
        ToggleRow(
            title = "Auto-Cache Favourite Tracks",
            subtitle = "Automatically download and cache tracks when marked as favourite",
            checked = autoDownloadFavorites,
            onCheckedChange = viewModel::setAutoDownloadFavorites,
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
        CategoryHeader("Home Feed", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            prefs.homeSectionOrder.forEachIndexed { index, section ->
                val checked = when (section) {
                    HomeSection.HEAVY_ROTATION -> prefs.showHomeHeavyRotation
                    HomeSection.RECENTLY_PLAYED -> prefs.showHomeRecentlyPlayed
                    HomeSection.RECENTLY_ADDED -> prefs.showHomeRecentlyAdded
                    HomeSection.FORGOTTEN_FAVORITES -> prefs.showHomeForgottenFavorites
                }
                OrderableToggleRow(
                    title = section.label,
                    checked = checked,
                    canMoveUp = index > 0,
                    canMoveDown = index < prefs.homeSectionOrder.lastIndex,
                    onCheckedChange = { enabled ->
                        when (section) {
                            HomeSection.HEAVY_ROTATION -> viewModel.setShowHomeHeavyRotation(enabled)
                            HomeSection.RECENTLY_PLAYED -> viewModel.setShowHomeRecentlyPlayed(enabled)
                            HomeSection.RECENTLY_ADDED -> viewModel.setShowHomeRecentlyAdded(enabled)
                            HomeSection.FORGOTTEN_FAVORITES -> viewModel.setShowHomeForgottenFavorites(enabled)
                        }
                    },
                    onMoveUp = { viewModel.moveHomeSection(section, -1) },
                    onMoveDown = { viewModel.moveHomeSection(section, 1) }
                )
            }
        }
    }
}

@Composable
fun CustomizationLibraryTabsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    val visibleTabs = prefs.libraryTabOrder.filterNot { it in prefs.hiddenLibraryTabs }
    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
        CategoryHeader("Library Tabs", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            prefs.libraryTabOrder.forEachIndexed { index, tab ->
                OrderableToggleRow(
                    title = tab.label,
                    checked = tab !in prefs.hiddenLibraryTabs,
                    canDisable = visibleTabs.size > 1 || tab in prefs.hiddenLibraryTabs,
                    canMoveUp = index > 0,
                    canMoveDown = index < prefs.libraryTabOrder.lastIndex,
                    onCheckedChange = { viewModel.setLibraryTabVisible(tab, it) },
                    onMoveUp = { viewModel.moveLibraryTab(tab, -1) },
                    onMoveDown = { viewModel.moveLibraryTab(tab, 1) }
                )
                if (index != prefs.libraryTabOrder.lastIndex) SettingsDivider(startPadding = 24.dp)
            }
        }
    }
}

@Composable
private fun OrderableToggleRow(
    title: String,
    checked: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canDisable: Boolean = true
) {
    var showMoveMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Box {
            PressDepthIconButton(R.drawable.lucide_ic_ellipsis_vertical, "Reorder $title", { showMoveMenu = true })
            DropdownMenu(expanded = showMoveMenu, onDismissRequest = { showMoveMenu = false }) {
                if (canMoveUp) {
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        onClick = { showMoveMenu = false; onMoveUp() }
                    )
                }
                if (canMoveDown) {
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        onClick = { showMoveMenu = false; onMoveDown() }
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = canDisable,
            colors = appSwitchColors()
        )
    }
}

@Composable
fun BackdropStylePreviewDot(
    style: NowPlayingBackdropStyle,
    size: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
            .then(
                when (style) {
                    NowPlayingBackdropStyle.ATMOSPHERE_BLUR -> Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF4A2818),
                                Color(0xFF1F120C),
                                Color(0xFF0A0908)
                            )
                        )
                    )
                    NowPlayingBackdropStyle.LIQUID_GRADIENT -> Modifier.background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFE53935),
                                Color(0xFF8E24AA),
                                Color(0xFF1E88E5),
                                Color(0xFF43A047),
                                Color(0xFFE53935)
                            )
                        )
                    )
                    NowPlayingBackdropStyle.AMBIENT_GLOW -> Modifier.background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFE53935).copy(alpha = 0.85f),
                                Color(0xFF2B1111),
                                Color(0xFF000000)
                            )
                        )
                    )
                    NowPlayingBackdropStyle.OLED_BLACK -> Modifier
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF333333), CircleShape)
                }
            )
    )
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
    sheetLabel: ((T) -> String)? = null,
    sheetSubtitle: ((T) -> String?)? = null,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
    optionLeading: (@Composable (item: T, isSheet: Boolean) -> Unit)? = null,
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
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (optionLeading != null) {
                optionLeading(selected, false)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label(selected),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.lucide_ic_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
    if (open) {
        FullOpenBottomSheet(onDismiss = { open = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column {
                        options.forEachIndexed { index, option ->
                            val isSelected = option == selected
                            val sub = sheetSubtitle?.invoke(option)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isSelected) Modifier.background(
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                                        ) else Modifier
                                    )
                                    .clickable(onClick = hapticClick {
                                        onSelect(option)
                                        open = false
                                    })
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (optionLeading != null) {
                                    optionLeading(option, true)
                                    Spacer(Modifier.width(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        (sheetLabel ?: label)(option),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (sub != null) {
                                        Text(
                                            sub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.surfaceContainerHighest
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(R.drawable.lucide_ic_check),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                            if (index < options.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(
                                        start = if (optionLeading != null) 54.dp else 18.dp,
                                        end = 18.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    label: String,
    iconRes: Int,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp)
        )
    }
}

