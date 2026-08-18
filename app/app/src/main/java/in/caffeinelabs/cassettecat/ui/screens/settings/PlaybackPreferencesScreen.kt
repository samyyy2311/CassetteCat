package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

private val sleepDurations = listOf(5L, 10L, 15L, 30L, 45L, 60L)

@Composable
fun PlaybackPreferencesScreen(
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sleepTimerEndMs by playbackViewModel.sleepTimerEndMs.collectAsState()
    val fadeOut by playbackViewModel.sleepTimerFadeOut.collectAsState()
    val finishTrack by playbackViewModel.sleepTimerFinishTrack.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, "Back", onBack)
            Text("Sleep timer", style = MaterialTheme.typography.headlineSmall)
        }

        Text(
            text = "Stop playback automatically after a chosen time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        SettingsSection(title = "Set a timer") {
            SleepTimerRow(
                label = "End of current song",
                onClick = hapticClick { playbackViewModel.startSleepTimer(-1L) }
            )
            SettingsDivider(startPadding = 24.dp)
            sleepDurations.forEachIndexed { index, minutes ->
                SleepTimerRow(
                    label = "$minutes minutes",
                    onClick = hapticClick { playbackViewModel.startSleepTimer(minutes * 60_000) }
                )
                if (index != sleepDurations.lastIndex) SettingsDivider(startPadding = 24.dp)
            }
        }

        SettingsSection(title = "Behavior") {
            ToggleRow(
                title = "Gentle volume fade out",
                subtitle = "Smoothly lower volume over the last 30 seconds",
                checked = fadeOut,
                onCheckedChange = { playbackViewModel.setSleepTimerFadeOut(it) },
                iconRes = R.drawable.lucide_ic_volume_2
            )
            SettingsDivider(startPadding = 24.dp)
            ToggleRow(
                title = "Wait for song to finish",
                subtitle = "Avoid cutting off in the middle of a track",
                checked = finishTrack,
                onCheckedChange = { playbackViewModel.setSleepTimerFinishTrack(it) },
                iconRes = R.drawable.lucide_ic_disc_3
            )
        }

        if (sleepTimerEndMs != null) {
            SettingsSection(title = "Active timer") {
                SleepTimerRow(
                    label = "Sleep timer is on",
                    value = "Turn off",
                    isDestructive = true,
                    onClick = hapticClick { playbackViewModel.cancelSleepTimer() }
                )
            }
        }
    }
}

@Composable
private fun SleepTimerRow(
    label: String,
    value: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(horizontal = 24.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
