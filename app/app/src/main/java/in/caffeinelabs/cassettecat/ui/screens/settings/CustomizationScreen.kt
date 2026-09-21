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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.download.DEFAULT_MAX_CACHE_BYTES
import `in`.caffeinelabs.cassettecat.data.download.DOWNLOAD_CACHE_LIMIT_OPTIONS_MB
import `in`.caffeinelabs.cassettecat.data.library.AlbumCoverRepository
import `in`.caffeinelabs.cassettecat.data.library.AlbumCoverStorage
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
import `in`.caffeinelabs.cassettecat.ui.components.invalidateAlbumArtCache
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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
    val cornerStyle = AlbumArtCornerStyle.entries.first { it.radiusDp == prefs.albumArtCornerRadiusDp }
    val cornerLabel = albumCornerShortLabel(cornerStyle)
    val crossfadeLabel = if (prefs.crossfadeSeconds == 0) {
        stringResource(AppR.string.customization_crossfade_off)
    } else {
        stringResource(AppR.string.customization_crossfade_summary, prefs.crossfadeSeconds)
    }
    val statusOn = stringResource(AppR.string.customization_on)
    val statusOff = stringResource(AppR.string.customization_off)
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
        CategoryHeader(stringResource(AppR.string.customization_title), onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            NavigationRow(
                title = stringResource(AppR.string.customization_theme),
                subtitle = stringResource(
                    AppR.string.customization_theme_summary,
                    themeAccentLabel(prefs.themeAccent),
                    if (prefs.amoledDarkTheme) statusOn else statusOff
                ),
                iconRes = R.drawable.lucide_ic_palette,
                onClick = { onNavigate(CustomizationRoute.THEME) },
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.customization_startup_library),
                subtitle = stringResource(
                    AppR.string.customization_startup_summary,
                    startScreenLabel(prefs.defaultStartScreen),
                    libraryTabLabel(prefs.defaultLibraryTab)
                ),
                iconRes = R.drawable.lucide_ic_library,
                onClick = { onNavigate(CustomizationRoute.STARTUP_LIBRARY) },
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.customization_now_playing),
                subtitle = stringResource(AppR.string.customization_corner_summary, cornerLabel),
                iconRes = R.drawable.lucide_ic_hand,
                onClick = { onNavigate(CustomizationRoute.NOW_PLAYING) },
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.customization_audio_engine),
                subtitle = stringResource(
                    AppR.string.customization_audio_summary,
                    crossfadeLabel,
                    if (prefs.replayGainEnabled) statusOn else statusOff
                ),
                iconRes = R.drawable.lucide_ic_waves,
                onClick = { onNavigate(CustomizationRoute.AUDIO_ENGINE) },
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.customization_lyrics),
                subtitle = stringResource(
                    AppR.string.customization_lyrics_summary,
                    lyricsFontSizeLabel(prefs.lyricsFontSize),
                    lyricsAlignmentLabel(prefs.lyricsAlignment)
                ),
                iconRes = R.drawable.lucide_ic_file_text,
                onClick = { onNavigate(CustomizationRoute.LYRICS) },
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.customization_storage),
                subtitle = stringResource(AppR.string.customization_storage_summary),
                iconRes = R.drawable.lucide_ic_download,
                onClick = { onNavigate(CustomizationRoute.STORAGE) },
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.customization_home_feed),
                subtitle = stringResource(AppR.string.customization_home_summary, homeFeedCount),
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
        PressDepthIconButton(R.drawable.lucide_ic_chevron_left, stringResource(AppR.string.action_back), onBack)
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
        CategoryHeader(stringResource(AppR.string.customization_theme), onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(stringResource(AppR.string.customization_accent_palette), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(AppR.string.customization_accent_palette_description),
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
                                        label = stringResource(AppR.string.customization_custom_accent),
                                        isSelected = isCustomSelected,
                                        iconRes = R.drawable.lucide_ic_pipette,
                                        iconSize = 18.dp,
                                        onClick = { showCustomColorSheet = true }
                                    )
                                } else {
                                    val isSelected = prefs.themeAccent == accent
                                    AccentSwatchItem(
                                        color = Color(accent.colorValue),
                                        label = themeAccentShortLabel(accent),
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
                title = stringResource(AppR.string.customization_amoled_title),
                subtitle = stringResource(AppR.string.customization_amoled_description),
                checked = prefs.amoledDarkTheme,
                onCheckedChange = viewModel::setAmoledDarkTheme,
                iconRes = R.drawable.lucide_ic_moon,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_artwork_accent),
                subtitle = stringResource(AppR.string.customization_artwork_accent_description),
                checked = prefs.artworkAccentEnabled,
                onCheckedChange = viewModel::setArtworkAccentEnabled,
                iconRes = R.drawable.lucide_ic_palette,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_mini_progress),
                subtitle = stringResource(AppR.string.customization_mini_progress_description),
                checked = prefs.showMiniPlayerProgress,
                onCheckedChange = viewModel::setShowMiniPlayerProgress,
                iconRes = R.drawable.lucide_ic_activity,
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_mini_action),
                subtitle = stringResource(AppR.string.customization_mini_action_description),
                iconRes = R.drawable.lucide_ic_mouse_pointer_click,
                options = MiniPlayerAction.entries,
                selected = prefs.miniPlayerAction,
                label = { miniPlayerActionLabel(it) },
                onSelect = viewModel::setMiniPlayerAction
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_app_typography),
                subtitle = stringResource(AppR.string.customization_app_typography_description),
                iconRes = R.drawable.lucide_ic_type,
                options = AppFontFamily.entries,
                selected = prefs.appFontFamily,
                label = { appFontShortLabel(it) },
                sheetLabel = { appFontFullLabel(it) },
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
            Text(
                stringResource(AppR.string.customization_custom_colour_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 12.dp)
            )
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
                label = { Text(stringResource(AppR.string.customization_hex_colour)) },
                placeholder = { Text(stringResource(AppR.string.customization_hex_placeholder)) },
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
                Text(stringResource(AppR.string.customization_apply))
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
        CategoryHeader(stringResource(AppR.string.customization_startup_library), onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            SheetPickerRow(
                title = stringResource(AppR.string.customization_start_screen),
                subtitle = stringResource(AppR.string.customization_start_screen_description),
                iconRes = R.drawable.lucide_ic_house,
                options = DefaultStartScreen.entries,
                selected = prefs.defaultStartScreen,
                label = { startScreenLabel(it) },
                onSelect = viewModel::setDefaultStartScreen
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_default_library_tab),
                subtitle = stringResource(AppR.string.customization_default_library_tab_description),
                iconRes = R.drawable.lucide_ic_library,
                options = visibleTabs,
                selected = prefs.defaultLibraryTab.takeIf { it in visibleTabs } ?: visibleTabs.first(),
                label = { libraryTabLabel(it) },
                onSelect = viewModel::setDefaultLibraryTab
            )
            SettingsDivider()
            NavigationRow(
                title = stringResource(AppR.string.customization_library_tabs),
                subtitle = stringResource(
                    AppR.string.customization_library_tabs_summary,
                    visibleTabs.size,
                    prefs.libraryTabOrder.size
                ),
                iconRes = R.drawable.lucide_ic_list,
                onClick = onNavigateToLibraryTabs
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_grid_columns),
                subtitle = stringResource(AppR.string.customization_grid_columns_description),
                iconRes = R.drawable.lucide_ic_grid_2x2,
                options = listOf(2, 3, 4),
                selected = prefs.gridColumnCount,
                label = { stringResource(AppR.string.customization_columns, it) },
                onSelect = viewModel::setGridColumnCount
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_song_density),
                subtitle = stringResource(AppR.string.customization_song_density_description),
                iconRes = R.drawable.lucide_ic_list,
                options = TrackRowDensity.entries,
                selected = prefs.trackRowDensity,
                label = { trackRowDensityLabel(it) },
                onSelect = viewModel::setTrackRowDensity
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_default_sort),
                subtitle = stringResource(AppR.string.customization_default_sort_description),
                iconRes = R.drawable.lucide_ic_arrow_up_down,
                options = DefaultSortMetric.entries,
                selected = prefs.defaultSortMetric,
                label = { defaultSortLabel(it) },
                onSelect = viewModel::setDefaultSortMetric
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_format_badges),
                subtitle = stringResource(AppR.string.customization_format_badges_description),
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
        CategoryHeader(stringResource(AppR.string.customization_now_playing), onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            SheetPickerRow(
                title = stringResource(AppR.string.customization_album_corner),
                subtitle = stringResource(AppR.string.customization_album_corner_description),
                iconRes = R.drawable.lucide_ic_disc,
                options = AlbumArtCornerStyle.entries,
                selected = AlbumArtCornerStyle.entries.first { it.radiusDp == prefs.albumArtCornerRadiusDp },
                label = { albumCornerShortLabel(it) },
                sheetLabel = { albumCornerFullLabel(it) },
                sheetSubtitle = { albumCornerDescription(it) },
                onSelect = { viewModel.setAlbumArtCornerRadiusDp(it.radiusDp) }
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_backdrop),
                subtitle = stringResource(AppR.string.customization_backdrop_description),
                iconRes = R.drawable.lucide_ic_image,
                options = NowPlayingBackdropStyle.entries,
                selected = prefs.nowPlayingBackdropStyle,
                label = { backdropLabel(it) },
                sheetSubtitle = { backdropDescription(it) },
                optionLeading = { item, isSheet -> BackdropStylePreviewDot(item, size = if (isSheet) 24.dp else 12.dp) },
                onSelect = viewModel::setNowPlayingBackdropStyle
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_blurred_background),
                subtitle = stringResource(AppR.string.customization_blurred_background_description),
                checked = prefs.showNowPlayingBlur,
                onCheckedChange = viewModel::setShowNowPlayingBlur,
                iconRes = R.drawable.lucide_ic_layers,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_remaining_time),
                subtitle = stringResource(AppR.string.customization_remaining_time_description),
                checked = prefs.showRemainingTime,
                onCheckedChange = viewModel::setShowRemainingTime,
                iconRes = R.drawable.lucide_ic_timer,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_mini_swipe),
                subtitle = stringResource(AppR.string.customization_mini_swipe_description),
                checked = prefs.miniPlayerSwipeToSkip,
                onCheckedChange = viewModel::setMiniPlayerSwipeToSkip,
                iconRes = R.drawable.lucide_ic_move_horizontal,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_swipe_lyrics),
                subtitle = stringResource(AppR.string.customization_swipe_lyrics_description),
                checked = prefs.swipeUpLyricsEnabled,
                onCheckedChange = viewModel::setSwipeUpLyricsEnabled,
                iconRes = R.drawable.lucide_ic_chevrons_up,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_flip_pause),
                subtitle = stringResource(AppR.string.customization_flip_pause_description),
                checked = prefs.flipToPauseEnabled,
                onCheckedChange = viewModel::setFlipToPauseEnabled,
                iconRes = R.drawable.lucide_ic_rotate_ccw,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_wave_skip),
                subtitle = stringResource(AppR.string.customization_wave_skip_description),
                checked = prefs.proximityWaveSkipEnabled,
                onCheckedChange = viewModel::setProximityWaveSkipEnabled,
                iconRes = R.drawable.lucide_ic_hand,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_shake_skip),
                subtitle = stringResource(AppR.string.customization_shake_skip_description),
                checked = prefs.shakeToSkipEnabled,
                onCheckedChange = viewModel::setShakeToSkipEnabled,
                iconRes = R.drawable.lucide_ic_smartphone,
            )
            if (prefs.shakeToSkipEnabled) {
                SettingsDivider()
                SettingsSliderRow(
                    title = stringResource(AppR.string.customization_shake_sensitivity),
                    subtitle = stringResource(AppR.string.customization_shake_sensitivity_description),
                    value = prefs.shakeSensitivity.toFloat(),
                    valueRange = 1f..5f,
                    steps = 3,
                    label = shakeSensitivityLabel(prefs.shakeSensitivity),
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
        CategoryHeader(stringResource(AppR.string.customization_audio_engine), onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            SheetPickerRow(
                title = stringResource(AppR.string.customization_crossfade_duration),
                subtitle = stringResource(AppR.string.customization_crossfade_duration_description),
                iconRes = R.drawable.lucide_ic_square_stack,
                options = listOf(0, 2, 4, 6, 8, 12),
                selected = prefs.crossfadeSeconds,
                label = {
                    if (it == 0) stringResource(AppR.string.customization_off)
                    else stringResource(AppR.string.customization_seconds_short, it)
                },
                onSelect = viewModel::setCrossfadeSeconds
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_gapless),
                subtitle = stringResource(AppR.string.customization_gapless_description),
                checked = prefs.gaplessPlayback,
                onCheckedChange = viewModel::setGaplessPlayback,
                iconRes = R.drawable.lucide_ic_disc,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_replaygain),
                subtitle = stringResource(AppR.string.customization_replaygain_description),
                checked = prefs.replayGainEnabled,
                onCheckedChange = viewModel::setReplayGainEnabled,
                iconRes = R.drawable.lucide_ic_waves,
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_replaygain_preamp),
                subtitle = stringResource(AppR.string.customization_replaygain_preamp_description),
                iconRes = R.drawable.lucide_ic_gauge,
                options = listOf(-6, -3, 0, 3, 6),
                selected = prefs.replayGainPreAmpDb,
                label = { if (it > 0) "+${it}dB" else "${it}dB" },
                onSelect = viewModel::setReplayGainPreAmpDb
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_mono_audio),
                subtitle = stringResource(AppR.string.customization_mono_audio_description),
                checked = prefs.monoAudio,
                onCheckedChange = viewModel::setMonoAudio,
                iconRes = R.drawable.lucide_ic_speaker,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_volume_limit),
                subtitle = stringResource(AppR.string.customization_volume_limit_description),
                checked = prefs.volumeLimitEnabled,
                onCheckedChange = viewModel::setVolumeLimitEnabled,
                iconRes = R.drawable.lucide_ic_ear,
            )
            if (prefs.volumeLimitEnabled) {
                SettingsDivider()
                SheetPickerRow(
                    title = stringResource(AppR.string.customization_max_volume),
                    subtitle = stringResource(AppR.string.customization_max_volume_description),
                    iconRes = R.drawable.lucide_ic_volume_1,
                    options = listOf(50, 60, 70, 80, 90, 100),
                    selected = prefs.maxVolumePercent,
                    label = { stringResource(AppR.string.customization_percent, it) },
                    onSelect = viewModel::setMaxVolumePercent
                )
            }
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_resume_queue),
                subtitle = stringResource(AppR.string.customization_resume_queue_description),
                checked = prefs.resumeQueueOnLaunch,
                onCheckedChange = viewModel::setResumeQueueOnLaunch,
                iconRes = R.drawable.lucide_ic_play,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_autoplay),
                subtitle = stringResource(AppR.string.customization_autoplay_description),
                checked = prefs.autoplayEnabled,
                onCheckedChange = viewModel::setAutoplayEnabled,
                iconRes = R.drawable.lucide_ic_infinity,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_pause_disconnect),
                subtitle = stringResource(AppR.string.customization_pause_disconnect_description),
                checked = prefs.pauseOnHeadphoneDisconnect,
                onCheckedChange = viewModel::setPauseOnHeadphoneDisconnect,
                iconRes = R.drawable.lucide_ic_headphones,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_drive_mode),
                subtitle = stringResource(AppR.string.customization_drive_mode_description),
                checked = prefs.autoDriveModeBluetooth,
                onCheckedChange = viewModel::setAutoDriveModeBluetooth,
                iconRes = R.drawable.lucide_ic_car,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_haptics),
                subtitle = stringResource(AppR.string.customization_haptics_description),
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
        CategoryHeader(stringResource(AppR.string.customization_lyrics), onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            SheetPickerRow(
                title = stringResource(AppR.string.customization_lyrics_font_size),
                subtitle = stringResource(AppR.string.customization_lyrics_font_size_description),
                iconRes = R.drawable.lucide_ic_type,
                options = LyricsFontSize.entries,
                selected = prefs.lyricsFontSize,
                label = { lyricsFontSizeLabel(it) },
                onSelect = viewModel::setLyricsFontSize
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_lyrics_font_family),
                subtitle = stringResource(AppR.string.customization_lyrics_font_family_description),
                iconRes = R.drawable.lucide_ic_type,
                options = LyricsFontFamily.entries,
                selected = prefs.lyricsFontFamily,
                label = { lyricsFontFamilyLabel(it) },
                onSelect = viewModel::setLyricsFontFamily
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_lyrics_alignment),
                subtitle = stringResource(AppR.string.customization_lyrics_alignment_description),
                iconRes = R.drawable.lucide_ic_text_align_center,
                options = LyricsAlignment.entries,
                selected = prefs.lyricsAlignment,
                label = { lyricsAlignmentLabel(it) },
                onSelect = viewModel::setLyricsAlignment
            )
            SettingsDivider()
            SheetPickerRow(
                title = stringResource(AppR.string.customization_lyrics_active_style),
                subtitle = stringResource(AppR.string.customization_lyrics_active_style_description),
                iconRes = R.drawable.lucide_ic_highlighter,
                options = LyricsActiveStyle.entries,
                selected = prefs.lyricsActiveStyle,
                label = { lyricsActiveStyleLabel(it) },
                onSelect = viewModel::setLyricsActiveStyle
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_keep_awake),
                subtitle = stringResource(AppR.string.customization_keep_awake_description),
                checked = prefs.keepScreenOnLyrics,
                onCheckedChange = viewModel::setKeepScreenOnLyrics,
                iconRes = R.drawable.lucide_ic_sun,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_local_lrc),
                subtitle = stringResource(AppR.string.customization_local_lrc_description),
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val albumCoverRepo = remember { AlbumCoverRepository.getInstance(context) }
    val albumCovers by albumCoverRepo.albumCovers.collectAsStateWithLifecycle()
    val storage = remember { AlbumCoverStorage(context) }
    var storageSizeBytes by remember(albumCovers) { mutableStateOf(storage.getStorageSizeBytes()) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
        CategoryHeader(stringResource(AppR.string.customization_storage), onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            SheetPickerRow(
                title = stringResource(AppR.string.customization_streaming_cache),
                subtitle = stringResource(AppR.string.customization_streaming_cache_description),
                iconRes = R.drawable.lucide_ic_download,
                options = DOWNLOAD_CACHE_LIMIT_OPTIONS_MB,
                selected = (maxCacheBytes / 1024 / 1024).toInt(),
                label = {
                    if (it < 1024) stringResource(AppR.string.customization_mb, it)
                    else stringResource(AppR.string.customization_gb, it / 1024)
                },
                onSelect = viewModel::setMaxCacheSizeMb
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_auto_cache),
                subtitle = stringResource(AppR.string.customization_auto_cache_description),
                checked = autoDownloadFavorites,
                onCheckedChange = viewModel::setAutoDownloadFavorites,
                iconRes = R.drawable.lucide_ic_heart,
            )
            SettingsDivider()
            ToggleRow(
                title = stringResource(AppR.string.customization_ignore_short),
                subtitle = stringResource(AppR.string.customization_ignore_short_description),
                checked = prefs.ignoreShortAudioClips,
                onCheckedChange = viewModel::setIgnoreShortAudioClips,
                iconRes = R.drawable.lucide_ic_clock,
            )
            SettingsDivider()
            val coverCount = albumCovers.values.toSet().size
            val formattedSize = if (storageSizeBytes < 1024 * 1024) {
                "${(storageSizeBytes / 1024.0).roundToInt()} KB"
            } else {
                "%.1f MB".format(storageSizeBytes / (1024.0 * 1024.0))
            }
            ActionRow(
                title = stringResource(AppR.string.customization_custom_covers),
                subtitle = if (coverCount == 0) {
                    stringResource(AppR.string.customization_no_custom_covers)
                } else {
                    pluralStringResource(
                        AppR.plurals.customization_custom_cover_summary,
                        coverCount,
                        coverCount,
                        formattedSize
                    )
                },
                iconRes = R.drawable.lucide_ic_image,
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    if (coverCount > 0) showResetDialog = true
                }
            )
        }
    }

    if (showResetDialog) {
        val coverCount = albumCovers.values.toSet().size
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(AppR.string.customization_reset_covers_title)) },
            text = {
                Text(
                    pluralStringResource(
                        AppR.plurals.customization_reset_covers_message,
                        coverCount,
                        coverCount
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    coroutineScope.launch {
                        albumCoverRepo.clearAllCovers()
                        withContext(Dispatchers.IO) { storage.clearAll() }
                        storageSizeBytes = 0L
                        invalidateAlbumArtCache(context)
                    }
                }) {
                    Text(stringResource(AppR.string.customization_reset_all), color = MaterialTheme.colorScheme.tertiary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(AppR.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun CustomizationHomeFeedScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    Column(modifier = modifier.categoryModifier(listBottomPadding)) {
        CategoryHeader(stringResource(AppR.string.customization_home_feed), onBack)
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
                    title = homeSectionLabel(section),
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
        CategoryHeader(stringResource(AppR.string.customization_library_tabs), onBack)
        Spacer(Modifier.height(16.dp))
        SettingsSection {
            prefs.libraryTabOrder.forEachIndexed { index, tab ->
                OrderableToggleRow(
                    title = libraryTabLabel(tab),
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
            PressDepthIconButton(
                R.drawable.lucide_ic_ellipsis_vertical,
                stringResource(AppR.string.customization_reorder, title),
                { showMoveMenu = true }
            )
            DropdownMenu(expanded = showMoveMenu, onDismissRequest = { showMoveMenu = false }) {
                if (canMoveUp) {
                    DropdownMenuItem(
                        text = { Text(stringResource(AppR.string.customization_move_up)) },
                        onClick = { showMoveMenu = false; onMoveUp() }
                    )
                }
                if (canMoveDown) {
                    DropdownMenuItem(
                        text = { Text(stringResource(AppR.string.customization_move_down)) },
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
    label: @Composable (T) -> String,
    sheetLabel: (@Composable (T) -> String)? = null,
    sheetSubtitle: (@Composable (T) -> String?)? = null,
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

@Composable
private fun themeAccentLabel(accent: ThemeAccent): String = stringResource(
    when (accent) {
        ThemeAccent.RECORD_RED -> AppR.string.customization_theme_record_red
        ThemeAccent.CASSETTE_AMBER -> AppR.string.customization_theme_cassette_amber
        ThemeAccent.ELECTRIC_CYAN -> AppR.string.customization_theme_electric_cyan
        ThemeAccent.NEON_EMERALD -> AppR.string.customization_theme_neon_emerald
        ThemeAccent.TAPE_MAGENTA -> AppR.string.customization_theme_tape_magenta
        ThemeAccent.MONOCHROME_SILVER -> AppR.string.customization_theme_monochrome
        ThemeAccent.CUSTOM -> AppR.string.customization_custom_accent
    }
)

@Composable
private fun themeAccentShortLabel(accent: ThemeAccent): String = stringResource(
    when (accent) {
        ThemeAccent.RECORD_RED -> AppR.string.customization_theme_record_red_short
        ThemeAccent.CASSETTE_AMBER -> AppR.string.customization_theme_cassette_amber_short
        ThemeAccent.ELECTRIC_CYAN -> AppR.string.customization_theme_electric_cyan_short
        ThemeAccent.NEON_EMERALD -> AppR.string.customization_theme_neon_emerald_short
        ThemeAccent.TAPE_MAGENTA -> AppR.string.customization_theme_tape_magenta_short
        ThemeAccent.MONOCHROME_SILVER -> AppR.string.customization_theme_monochrome_short
        ThemeAccent.CUSTOM -> AppR.string.customization_custom_accent
    }
)

@Composable
private fun libraryTabLabel(tab: DefaultLibraryTab): String = stringResource(
    when (tab) {
        DefaultLibraryTab.SONGS -> AppR.string.customization_library_songs
        DefaultLibraryTab.ARTISTS -> AppR.string.customization_library_artists
        DefaultLibraryTab.ALBUMS -> AppR.string.customization_library_albums
        DefaultLibraryTab.GENRES -> AppR.string.customization_library_genres
        DefaultLibraryTab.PLAYLISTS -> AppR.string.customization_library_playlists
        DefaultLibraryTab.FOLDERS -> AppR.string.customization_library_folders
    }
)

@Composable
private fun homeSectionLabel(section: HomeSection): String = stringResource(
    when (section) {
        HomeSection.HEAVY_ROTATION -> AppR.string.customization_home_heavy_rotation
        HomeSection.RECENTLY_PLAYED -> AppR.string.customization_home_recently_played
        HomeSection.RECENTLY_ADDED -> AppR.string.customization_home_recently_added
        HomeSection.FORGOTTEN_FAVORITES -> AppR.string.customization_home_forgotten_favorites
    }
)

@Composable
private fun miniPlayerActionLabel(action: MiniPlayerAction): String = stringResource(
    when (action) {
        MiniPlayerAction.NEXT -> AppR.string.customization_mini_next
        MiniPlayerAction.PREVIOUS -> AppR.string.customization_mini_previous
        MiniPlayerAction.FAVORITE -> AppR.string.customization_mini_favorite
        MiniPlayerAction.QUEUE -> AppR.string.customization_mini_queue
        MiniPlayerAction.REPEAT -> AppR.string.customization_mini_repeat
    }
)

@Composable
private fun albumCornerShortLabel(style: AlbumArtCornerStyle): String = stringResource(
    when (style) {
        AlbumArtCornerStyle.CURVED -> AppR.string.customization_corner_curved
        AlbumArtCornerStyle.SOFT -> AppR.string.customization_corner_soft
        AlbumArtCornerStyle.SQUARE -> AppR.string.customization_corner_square
    }
)

@Composable
private fun albumCornerFullLabel(style: AlbumArtCornerStyle): String = stringResource(
    when (style) {
        AlbumArtCornerStyle.CURVED -> AppR.string.customization_corner_curved_full
        AlbumArtCornerStyle.SOFT -> AppR.string.customization_corner_soft_full
        AlbumArtCornerStyle.SQUARE -> AppR.string.customization_corner_square_full
    }
)

@Composable
private fun albumCornerDescription(style: AlbumArtCornerStyle): String = stringResource(
    when (style) {
        AlbumArtCornerStyle.CURVED -> AppR.string.customization_corner_curved_description
        AlbumArtCornerStyle.SOFT -> AppR.string.customization_corner_soft_description
        AlbumArtCornerStyle.SQUARE -> AppR.string.customization_corner_square_description
    }
)

@Composable
private fun startScreenLabel(screen: DefaultStartScreen): String = stringResource(
    when (screen) {
        DefaultStartScreen.HOME -> AppR.string.customization_start_home
        DefaultStartScreen.LIBRARY -> AppR.string.customization_start_library
        DefaultStartScreen.LAST_OPENED -> AppR.string.customization_start_last
    }
)

@Composable
private fun lyricsFontSizeLabel(size: LyricsFontSize): String = stringResource(
    when (size) {
        LyricsFontSize.SMALL -> AppR.string.customization_size_small
        LyricsFontSize.MEDIUM -> AppR.string.customization_size_standard
        LyricsFontSize.LARGE -> AppR.string.customization_size_large
    }
)

@Composable
private fun trackRowDensityLabel(density: TrackRowDensity): String = stringResource(
    when (density) {
        TrackRowDensity.DETAILED -> AppR.string.customization_density_detailed
        TrackRowDensity.COMPACT -> AppR.string.customization_density_compact
    }
)

@Composable
private fun defaultSortLabel(metric: DefaultSortMetric): String = stringResource(
    when (metric) {
        DefaultSortMetric.TITLE -> AppR.string.customization_sort_title
        DefaultSortMetric.ARTIST -> AppR.string.customization_sort_artist
        DefaultSortMetric.ALBUM -> AppR.string.customization_sort_album
    }
)

@Composable
private fun lyricsAlignmentLabel(alignment: LyricsAlignment): String = stringResource(
    when (alignment) {
        LyricsAlignment.CENTER -> AppR.string.customization_alignment_center
        LyricsAlignment.LEFT -> AppR.string.customization_alignment_left
    }
)

@Composable
private fun lyricsActiveStyleLabel(style: LyricsActiveStyle): String = stringResource(
    when (style) {
        LyricsActiveStyle.ACCENT_GLOW -> AppR.string.customization_active_accent
        LyricsActiveStyle.CLEAN_WHITE -> AppR.string.customization_active_white
    }
)

@Composable
private fun appFontShortLabel(font: AppFontFamily): String = stringResource(
    when (font) {
        AppFontFamily.SPACE_GROTESK -> AppR.string.customization_font_space_grotesk
        AppFontFamily.IBM_PLEX_SANS -> AppR.string.customization_font_ibm_sans
        AppFontFamily.IBM_PLEX_MONO -> AppR.string.customization_font_ibm_mono
        AppFontFamily.SILKSCREEN -> AppR.string.customization_font_silkscreen
        AppFontFamily.VT323 -> AppR.string.customization_font_vt323
        AppFontFamily.MONOCRAFT -> AppR.string.customization_font_monocraft
        AppFontFamily.SYSTEM_DEFAULT -> AppR.string.customization_font_system_sans
        AppFontFamily.SYSTEM_SERIF -> AppR.string.customization_font_system_serif
        AppFontFamily.SYSTEM_MONO -> AppR.string.customization_font_system_mono
    }
)

@Composable
private fun appFontFullLabel(font: AppFontFamily): String = stringResource(
    when (font) {
        AppFontFamily.SPACE_GROTESK -> AppR.string.customization_font_space_grotesk_full
        AppFontFamily.IBM_PLEX_SANS -> AppR.string.customization_font_ibm_sans_full
        AppFontFamily.IBM_PLEX_MONO -> AppR.string.customization_font_ibm_mono_full
        AppFontFamily.SILKSCREEN -> AppR.string.customization_font_silkscreen_full
        AppFontFamily.VT323 -> AppR.string.customization_font_vt323_full
        AppFontFamily.MONOCRAFT -> AppR.string.customization_font_monocraft_full
        AppFontFamily.SYSTEM_DEFAULT -> AppR.string.customization_font_system_sans_full
        AppFontFamily.SYSTEM_SERIF -> AppR.string.customization_font_system_serif_full
        AppFontFamily.SYSTEM_MONO -> AppR.string.customization_font_system_mono_full
    }
)

@Composable
private fun lyricsFontFamilyLabel(font: LyricsFontFamily): String = stringResource(
    when (font) {
        LyricsFontFamily.SPACE_GROTESK -> AppR.string.customization_font_space_grotesk
        LyricsFontFamily.IBM_PLEX_SANS -> AppR.string.customization_font_ibm_sans
        LyricsFontFamily.IBM_PLEX_MONO -> AppR.string.customization_font_ibm_mono
        LyricsFontFamily.SILKSCREEN -> AppR.string.customization_lyrics_font_silkscreen
        LyricsFontFamily.VT323 -> AppR.string.customization_lyrics_font_vt323
        LyricsFontFamily.MONOCRAFT -> AppR.string.customization_lyrics_font_monocraft
        LyricsFontFamily.SYSTEM_DEFAULT -> AppR.string.customization_lyrics_font_system_sans
        LyricsFontFamily.SYSTEM_SERIF -> AppR.string.customization_lyrics_font_system_serif
        LyricsFontFamily.SYSTEM_MONO -> AppR.string.customization_lyrics_font_system_mono
    }
)

@Composable
private fun backdropLabel(style: NowPlayingBackdropStyle): String = stringResource(
    when (style) {
        NowPlayingBackdropStyle.ATMOSPHERE_BLUR -> AppR.string.customization_backdrop_atmosphere
        NowPlayingBackdropStyle.LIQUID_GRADIENT -> AppR.string.customization_backdrop_liquid
        NowPlayingBackdropStyle.AMBIENT_GLOW -> AppR.string.customization_backdrop_glow
        NowPlayingBackdropStyle.OLED_BLACK -> AppR.string.customization_backdrop_oled
    }
)

@Composable
private fun backdropDescription(style: NowPlayingBackdropStyle): String = stringResource(
    when (style) {
        NowPlayingBackdropStyle.ATMOSPHERE_BLUR -> AppR.string.customization_backdrop_atmosphere_description
        NowPlayingBackdropStyle.LIQUID_GRADIENT -> AppR.string.customization_backdrop_liquid_description
        NowPlayingBackdropStyle.AMBIENT_GLOW -> AppR.string.customization_backdrop_glow_description
        NowPlayingBackdropStyle.OLED_BLACK -> AppR.string.customization_backdrop_oled_description
    }
)

@Composable
private fun shakeSensitivityLabel(value: Int): String = stringResource(
    when (value) {
        1 -> AppR.string.customization_shake_gentle
        2 -> AppR.string.customization_shake_light
        3 -> AppR.string.customization_shake_medium
        4 -> AppR.string.customization_shake_firm
        else -> AppR.string.customization_shake_strong
    }
)
