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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import java.util.Locale

@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel = viewModel()
) {
    val levels by viewModel.levels.collectAsState()
    val isAvailable by viewModel.isAvailable.collectAsState()
    val isBassBoostSupported by viewModel.isBassBoostSupported.collectAsState()
    val isVirtualizerSupported by viewModel.isVirtualizerSupported.collectAsState()

    val isEnabled = levels.enabled
    val hasCustomModifications = levels.bandLevelsMb.any { it != 0 } || levels.bassBoostStrength > 0 || levels.virtualizerStrength > 0
    val contentAlpha by animateFloatAsState(targetValue = if (isEnabled) 1.0f else 0.45f, label = "eqContentAlpha")

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
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .alpha(contentAlpha)
                    .padding(bottom = 32.dp)
            ) {
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
                                onClick = { viewModel.applyPreset(index) }
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
