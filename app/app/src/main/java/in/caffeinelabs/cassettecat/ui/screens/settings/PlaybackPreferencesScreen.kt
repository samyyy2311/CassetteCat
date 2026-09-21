package `in`.caffeinelabs.cassettecat.ui.screens.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

private val sleepDurations = listOf(5L, 10L, 15L, 30L, 45L, 60L)
private val fadeDurations = listOf(10, 20, 30, 45, 60)

@Composable
fun PlaybackPreferencesScreen(
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val sleepTimerEndMs by playbackViewModel.sleepTimerEndMs.collectAsStateWithLifecycle()
    val fadeOut by playbackViewModel.sleepTimerFadeOut.collectAsStateWithLifecycle()
    val finishTrack by playbackViewModel.sleepTimerFinishTrack.collectAsStateWithLifecycle()
    val fadeSeconds by playbackViewModel.sleepTimerFadeSeconds.collectAsStateWithLifecycle()
    val fadeLabels = (fadeDurations + fadeSeconds).distinct().associateWith {
        stringResource(AppR.string.sleep_timer_seconds_short, it)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = listBottomPadding + 40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, stringResource(AppR.string.action_back), onBack)
            Text(stringResource(AppR.string.sleep_timer_title), style = MaterialTheme.typography.headlineSmall)
        }

        Text(
            text = stringResource(AppR.string.sleep_timer_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        SettingsSection(title = stringResource(AppR.string.sleep_timer_set_section)) {
            SleepTimerRow(
                label = stringResource(AppR.string.sleep_timer_end_of_song),
                onClick = hapticClick { playbackViewModel.startSleepTimer(-1L) }
            )
            SettingsDivider(startPadding = 24.dp)
            sleepDurations.forEachIndexed { index, minutes ->
                val minuteCount = minutes.toInt()
                SleepTimerRow(
                    label = pluralStringResource(AppR.plurals.sleep_timer_minutes, minuteCount, minuteCount),
                    onClick = hapticClick { playbackViewModel.startSleepTimer(minutes * 60_000) }
                )
                if (index != sleepDurations.lastIndex) SettingsDivider(startPadding = 24.dp)
            }
        }

        SettingsSection(title = stringResource(AppR.string.sleep_timer_behavior_section)) {
            ToggleRow(
                title = stringResource(AppR.string.sleep_timer_fade_title),
                subtitle = stringResource(AppR.string.sleep_timer_fade_description, fadeSeconds),
                checked = fadeOut,
                onCheckedChange = { playbackViewModel.setSleepTimerFadeOut(it) },
                iconRes = R.drawable.lucide_ic_volume_2
            )
            SettingsDivider(startPadding = 24.dp)
            SheetPickerRow(
                title = stringResource(AppR.string.sleep_timer_fade_duration),
                subtitle = stringResource(AppR.string.sleep_timer_fade_duration_description),
                iconRes = R.drawable.lucide_ic_timer,
                options = fadeDurations,
                selected = fadeSeconds,
                label = { fadeLabels.getValue(it) },
                onSelect = playbackViewModel::setSleepTimerFadeSeconds
            )
            SettingsDivider(startPadding = 24.dp)
            ToggleRow(
                title = stringResource(AppR.string.sleep_timer_finish_song),
                subtitle = stringResource(AppR.string.sleep_timer_finish_song_description),
                checked = finishTrack,
                onCheckedChange = { playbackViewModel.setSleepTimerFinishTrack(it) },
                iconRes = R.drawable.lucide_ic_disc_3
            )
        }

        if (sleepTimerEndMs != null) {
            SettingsSection(title = stringResource(AppR.string.sleep_timer_active_section)) {
                SleepTimerRow(
                    label = stringResource(AppR.string.sleep_timer_active),
                    value = stringResource(AppR.string.sleep_timer_turn_off),
                    isDestructive = true,
                    onClick = hapticClick { playbackViewModel.cancelSleepTimer() }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
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
