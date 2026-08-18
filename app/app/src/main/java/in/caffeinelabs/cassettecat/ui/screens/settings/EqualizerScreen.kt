package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.playback.AutoEqProfile
import `in`.caffeinelabs.cassettecat.data.playback.AutoEqProfiles
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import java.util.Locale

@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp,
    viewModel: EqualizerViewModel = viewModel()
) {
    val levels by viewModel.levels.collectAsState()
    val isAvailable by viewModel.isAvailable.collectAsState()
    val isBassBoostSupported by viewModel.isBassBoostSupported.collectAsState()
    val isVirtualizerSupported by viewModel.isVirtualizerSupported.collectAsState()

    val isEnabled = levels.enabled
    val hasCustomModifications = levels.bandLevelsMb.any { it != 0 } || levels.bassBoostStrength > 0 || levels.virtualizerStrength > 0
    val contentAlpha by animateFloatAsState(targetValue = if (isEnabled) 1.0f else 0.45f, label = "eqContentAlpha")

    var showAutoEqPicker by remember { mutableStateOf(false) }
    var selectedAutoEqName by remember { mutableStateOf<String?>(null) }

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
                    onClick = { viewModel.reset() }
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = { viewModel.setMasterEnabled(it) },
                enabled = isAvailable
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

                if (presetNames.isNotEmpty()) {
                    Text(
                        "PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(presetNames) { index, name ->
                            val isSelected = levels.selectedPresetIndex == index
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
                }

                Text(
                    "FREQUENCY BANDS",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    repeat(viewModel.numberOfBands) { band ->
                        EqualizerBandSlider(
                            label = formatFreqLabel(viewModel.centerFreqHz(band)),
                            valueMb = levels.bandLevelsMb.getOrElse(band) { 0 },
                            rangeMb = range,
                            enabled = isEnabled,
                            onValueChange = { levelMb -> viewModel.setBandLevel(band, levelMb) }
                        )
                        if (band != viewModel.numberOfBands - 1) {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                if (isBassBoostSupported || isVirtualizerSupported) {
                    Spacer(Modifier.height(24.dp))
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
                            EffectControlSlider(
                                label = "Bass Boost",
                                strength = levels.bassBoostStrength,
                                enabled = isEnabled,
                                onValueChange = { viewModel.setBassBoostStrength(it) }
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        if (isVirtualizerSupported) {
                            EffectControlSlider(
                                label = "Surround Virtualizer",
                                strength = levels.virtualizerStrength,
                                enabled = isEnabled,
                                onValueChange = { viewModel.setVirtualizerStrength(it) }
                            )
                        }
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
    val bg = if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
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
            style = MaterialTheme.typography.bodyMedium,
            color = fg
        )
    }
}

@Composable
private fun EqualizerBandSlider(
    label: String,
    valueMb: Int,
    rangeMb: IntRange,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                String.format(Locale.US, "%+.1f dB", valueMb / 100f),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = if (valueMb != 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = valueMb.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = rangeMb.first.toFloat()..rangeMb.last.toFloat(),
            enabled = enabled
        )
    }
}

@Composable
private fun EffectControlSlider(
    label: String,
    strength: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    val percentage = (strength / 10).coerceIn(0, 100)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "$percentage%",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = if (percentage > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = strength.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..1000f,
            enabled = enabled
        )
    }
}
