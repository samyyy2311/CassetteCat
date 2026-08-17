package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = onBack
            )
            Text("Equalizer", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (levels.bandLevelsMb.any { it != 0 }) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_rotate_ccw,
                    contentDescription = "Reset",
                    onClick = { viewModel.reset() }
                )
            }
        }

        if (!isAvailable) {
            Text(
                "Play something to enable the equalizer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            val range = viewModel.levelRangeMb
            val presetNames = viewModel.presetNames
            if (presetNames.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(presetNames) { index, name ->
                        PresetChip(name = name, onClick = { viewModel.applyPreset(index) })
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                repeat(viewModel.numberOfBands) { band ->
                    EqualizerBandSlider(
                        label = formatFreqLabel(viewModel.centerFreqHz(band)),
                        valueMb = levels.bandLevelsMb.getOrElse(band) { 0 },
                        rangeMb = range,
                        onValueChange = { levelMb -> viewModel.setBandLevel(band, levelMb) }
                    )
                    if (band != viewModel.numberOfBands - 1) Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun formatFreqLabel(hz: Int): String =
    if (hz >= 1000) String.format(Locale.US, "%.1f kHz", hz / 1000f) else "$hz Hz"

@Composable
private fun PresetChip(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .tapScale(onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EqualizerBandSlider(label: String, valueMb: Int, rangeMb: IntRange, onValueChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                String.format(Locale.US, "%+.1f dB", valueMb / 100f),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = valueMb.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = rangeMb.first.toFloat()..rangeMb.last.toFloat()
        )
    }
}
