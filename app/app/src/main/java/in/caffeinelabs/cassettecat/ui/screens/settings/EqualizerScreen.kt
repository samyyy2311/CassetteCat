package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.playback.AutoEqProfile
import `in`.caffeinelabs.cassettecat.data.playback.AutoEqProfiles
import `in`.caffeinelabs.cassettecat.data.playback.CustomEqualizerPreset
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticToggle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp,
    viewModel: EqualizerViewModel = viewModel()
) {
    val levels by viewModel.levels.collectAsStateWithLifecycle()
    val isAvailable by viewModel.isAvailable.collectAsStateWithLifecycle()
    val isBassBoostSupported by viewModel.isBassBoostSupported.collectAsStateWithLifecycle()
    val isVirtualizerSupported by viewModel.isVirtualizerSupported.collectAsStateWithLifecycle()

    val isEnabled = levels.enabled
    val hasCustomModifications = levels.bandLevelsMb.any { it != 0 } || levels.bassBoostStrength > 0 ||
        levels.virtualizerStrength > 0 || levels.preampGainMb != 0 || levels.loudnessNormalization
    val contentAlpha by animateFloatAsState(targetValue = if (isEnabled) 1.0f else 0.45f, label = "eqContentAlpha")

    var showAutoEqPicker by remember { mutableStateOf(false) }
    var selectedAutoEqName by remember { mutableStateOf<String?>(null) }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 20.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = onBack
            )
            Text(
                "Equalizer",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(visible = hasCustomModifications && isAvailable) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_rotate_ccw,
                    contentDescription = "Reset EQ",
                    onClick = {
                        viewModel.reset()
                        selectedAutoEqName = null
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = hapticToggle { viewModel.setMasterEnabled(it) },
                enabled = isAvailable,
                colors = appSwitchColors()
            )
        }

        if (!isAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Audio DSP Idle",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Play a song to attach the equalizer and audio effects.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            val range = viewModel.levelRangeMb
            val presetNames = viewModel.presetNames
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .alpha(contentAlpha)
                    .padding(bottom = listBottomPadding + 32.dp)
            ) {
                Text(
                    "HEADPHONE CALIBRATION (AUTOEQ)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(enabled = isEnabled) { showAutoEqPicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_headphones),
                        contentDescription = null,
                        tint = if (selectedAutoEqName != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedAutoEqName ?: "Select Headphone Model",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = if (selectedAutoEqName != null) IbmPlexMonoFontFamily else MaterialTheme.typography.bodyMedium.fontFamily),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedAutoEqName != null) "AutoEq target curve applied" else "${AutoEqProfiles.profiles.size}+ calibrated models",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "PRESETS",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable(enabled = isEnabled) { showSavePresetDialog = true }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_plus),
                                contentDescription = "Save Preset",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Save Preset",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    items(levels.customPresets, key = { it.id }) { preset ->
                        val isSelected = levels.selectedCustomPresetId == preset.id
                        CustomPresetChip(
                            preset = preset,
                            selected = isSelected,
                            enabled = isEnabled,
                            onClick = {
                                selectedAutoEqName = null
                                viewModel.applyCustomPreset(preset)
                            },
                            onDelete = { viewModel.deleteCustomPreset(preset.id) }
                        )
                    }

                    itemsIndexed(presetNames) { index, name ->
                        val isSelected = levels.selectedPresetIndex == index && levels.selectedCustomPresetId == null
                        PresetChip(
                            name = name,
                            selected = isSelected,
                            enabled = isEnabled,
                            onClick = {
                                selectedAutoEqName = null
                                viewModel.applyPreset(index)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                Text(
                    "FREQUENCY BANDS",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    repeat(viewModel.numberOfBands) { band ->
                        BipolarEqualizerBandSlider(
                            label = formatFreqLabel(viewModel.centerFreqHz(band)),
                            valueMb = levels.bandLevelsMb.getOrElse(band) { 0 },
                            rangeMb = range,
                            enabled = isEnabled,
                            onValueLiveChange = { levelMb -> viewModel.setBandLevelLive(band, levelMb) },
                            onValueCommit = { levelMb -> viewModel.setBandLevel(band, levelMb) }
                        )
                        if (band != viewModel.numberOfBands - 1) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                if (isBassBoostSupported || isVirtualizerSupported) {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "AUDIO ENHANCEMENTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        if (isBassBoostSupported) {
                            UnipolarAudioSlider(
                                label = "Bass Boost",
                                value = levels.bassBoostStrength,
                                maxVal = 1000,
                                enabled = isEnabled,
                                displayFormatter = { "${(it / 10).coerceIn(0, 100)}%" },
                                onValueLiveChange = { viewModel.setBassBoostStrengthLive(it) },
                                onValueCommit = { viewModel.setBassBoostStrength(it) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        if (isVirtualizerSupported) {
                            UnipolarAudioSlider(
                                label = "Surround Virtualizer",
                                value = levels.virtualizerStrength,
                                maxVal = 1000,
                                enabled = isEnabled,
                                displayFormatter = { "${(it / 10).coerceIn(0, 100)}%" },
                                onValueLiveChange = { viewModel.setVirtualizerStrengthLive(it) },
                                onValueCommit = { viewModel.setVirtualizerStrength(it) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "LOUDNESS & DYNAMICS",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        UnipolarAudioSlider(
                            label = "Preamp Gain",
                            value = levels.preampGainMb,
                            maxVal = 1200,
                            enabled = isEnabled,
                            displayFormatter = { String.format(Locale.US, "%+.1f dB", it / 100f) },
                            onValueLiveChange = { viewModel.setPreampGainMbLive(it) },
                            onValueCommit = { viewModel.setPreampGainMb(it) }
                        )
                        Spacer(Modifier.height(16.dp))
                        ToggleRow(
                            title = "Loudness Normalization",
                            subtitle = "Balance output volume across diverse audio sources",
                            checked = levels.loudnessNormalization,
                            onCheckedChange = { viewModel.setLoudnessNormalization(it) },
                            iconRes = R.drawable.lucide_ic_volume_2,
                            enabled = isEnabled
                        )
                    }
                }
            }
        }
    }

    if (showAutoEqPicker) {
        AutoEqPickerSheet(
            currentSelected = selectedAutoEqName,
            onDismiss = { showAutoEqPicker = false },
            onSelect = { profile ->
                selectedAutoEqName = "${profile.brand} ${profile.name}"
                viewModel.applyAutoEq(profile)
                showAutoEqPicker = false
            }
        )
    }

    if (showSavePresetDialog) {
        var presetNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Equalizer Preset") },
            text = {
                Column {
                    Text(
                        "Save the current band levels, bass boost, and preamp gain as a custom preset.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    TextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        placeholder = { Text("e.g. Bass Boost Max, Vocal Clarity") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetNameInput.isNotBlank()) {
                            viewModel.saveCurrentAsPreset(presetNameInput)
                            showSavePresetDialog = false
                        }
                    },
                    enabled = presetNameInput.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoEqPickerSheet(
    currentSelected: String?,
    onDismiss: () -> Unit,
    onSelect: (AutoEqProfile) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val filteredProfiles = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            AutoEqProfiles.profiles
        } else {
            val q = searchQuery.trim().lowercase(Locale.US)
            AutoEqProfiles.profiles.filter {
                it.brand.lowercase(Locale.US).contains(q) || it.name.lowercase(Locale.US).contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 32.dp)
        ) {
            Text(
                "Headphone Calibration",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                "Calibrated frequency response curves from AutoEq",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search brand or model (e.g. Sony, HD 600)...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredProfiles, key = { "${it.brand}_${it.name}" }) { profile ->
                    val fullName = "${profile.brand} ${profile.name}"
                    val isSelected = currentSelected == fullName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(profile) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                profile.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                profile.brand,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_check),
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFreqLabel(hz: Int): String =
    if (hz >= 1000) String.format(Locale.US, "%.1f kHz", hz / 1000f) else "$hz Hz"

@Composable
private fun PresetChip(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val fg = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .border(if (selected) 1.dp else 0.5.dp, borderColor, RoundedCornerShape(100.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = fg
        )
    }
}

@Composable
private fun CustomPresetChip(
    preset: CustomEqualizerPreset,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val fg = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .border(if (selected) 1.dp else 0.5.dp, borderColor, RoundedCornerShape(100.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            preset.name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = fg
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_x),
                contentDescription = "Delete preset",
                tint = fg.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BipolarEqualizerBandSlider(
    label: String,
    valueMb: Int,
    rangeMb: IntRange,
    enabled: Boolean,
    onValueLiveChange: (Int) -> Unit,
    onValueCommit: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var dragValueMb by remember { mutableStateOf<Float?>(null) }
    var previousSign by remember { mutableStateOf(if (valueMb >= 0) 1 else -1) }
    val displayedValueMb = dragValueMb ?: valueMb.toFloat()
    val min = rangeMb.first.toFloat()
    val max = rangeMb.last.toFloat()
    val validRange = if (max > min) min..max else -1500f..1500f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format(Locale.US, "%+.1f dB", displayedValueMb / 100f),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = if (displayedValueMb.roundToInt() != 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = enabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        dragValueMb = null
                        onValueLiveChange(0)
                        onValueCommit(0)
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Slider(
            value = displayedValueMb.coerceIn(validRange),
            onValueChange = { raw ->
                val currentSign = if (raw >= 0) 1 else -1
                if (currentSign != previousSign) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    previousSign = currentSign
                }
                dragValueMb = raw
                onValueLiveChange(raw.roundToInt())
            },
            onValueChangeFinished = {
                val finalVal = (dragValueMb ?: valueMb.toFloat()).roundToInt()
                onValueCommit(finalVal)
                dragValueMb = null
            },
            valueRange = validRange,
            enabled = enabled,
            track = { sliderState -> BipolarEqTrack(sliderState) },
            thumb = { EqFaderThumb() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnipolarAudioSlider(
    label: String,
    value: Int,
    maxVal: Int,
    enabled: Boolean,
    displayFormatter: (Int) -> String,
    onValueLiveChange: (Int) -> Unit,
    onValueCommit: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val displayedValue = dragValue ?: value.toFloat()
    val validRange = 0f..maxVal.toFloat().coerceAtLeast(1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = displayFormatter(displayedValue.roundToInt()),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = if (displayedValue.roundToInt() > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = enabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        dragValue = null
                        onValueLiveChange(0)
                        onValueCommit(0)
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Slider(
            value = displayedValue.coerceIn(validRange),
            onValueChange = { raw ->
                dragValue = raw
                onValueLiveChange(raw.roundToInt())
            },
            onValueChangeFinished = {
                val finalVal = (dragValue ?: value.toFloat()).roundToInt()
                onValueCommit(finalVal)
                dragValue = null
            },
            valueRange = validRange,
            enabled = enabled,
            track = { sliderState -> UnipolarTrack(sliderState) },
            thumb = { EqFaderThumb() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BipolarEqTrack(sliderState: SliderState) {
    val grooveColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val activeColor = MaterialTheme.colorScheme.tertiary
    val centerMarkColor = MaterialTheme.colorScheme.outlineVariant

    val range = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(1f)
    val fraction = ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
    val centerFraction = 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(grooveColor)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val centerX = size.width * centerFraction
            val thumbX = size.width * fraction

            // Center zero notch
            drawLine(
                color = centerMarkColor,
                start = Offset(centerX, -4.dp.toPx()),
                end = Offset(centerX, size.height + 4.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Active bar radiating from center to thumb
            if (thumbX > centerX + 1f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(centerX, 0f),
                    size = Size(thumbX - centerX, size.height),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            } else if (thumbX < centerX - 1f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(thumbX, 0f),
                    size = Size(centerX - thumbX, size.height),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnipolarTrack(sliderState: SliderState) {
    val grooveColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val activeColor = MaterialTheme.colorScheme.tertiary

    val range = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(1f)
    val fraction = ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(grooveColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(activeColor, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun EqFaderThumb() {
    Box(
        modifier = Modifier
            .size(width = 12.dp, height = 24.dp)
            .shadow(3.dp, RoundedCornerShape(3.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )
                ),
                RoundedCornerShape(3.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.onTertiary, RoundedCornerShape(1.dp))
        )
    }
}
