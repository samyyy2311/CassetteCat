package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton

@Composable
fun ExternalServicesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = onBack
            )
            Text("External Services", style = MaterialTheme.typography.headlineSmall)
        }

        ToggleRow(
            title = "Offline Blackout Mode",
            subtitle = if (uiState.services.offlineBlackoutMode) "All network services and streaming disabled" else "Disable all network calls, streaming, and metadata lookups",
            checked = uiState.services.offlineBlackoutMode,
            onCheckedChange = { viewModel.setOfflineBlackoutMode(it) },
            iconRes = R.drawable.lucide_ic_radio,
            iconTint = if (uiState.services.offlineBlackoutMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
        )

        SettingsDivider()

        Column {
            externalServices.forEach { service ->
                val (iconRes, iconTint) = when (service) {
                    ExternalService.DEEZER -> AppR.drawable.ic_logo_deezer to Color.Unspecified
                    ExternalService.AUDIODB -> AppR.drawable.ic_logo_theaudiodb to Color.Unspecified
                    ExternalService.LRCLIB -> AppR.drawable.ic_logo_lrclib to Color.Unspecified
                    ExternalService.COVER_ART_ARCHIVE -> R.drawable.lucide_ic_disc_3 to MaterialTheme.colorScheme.secondary
                    ExternalService.WIKIPEDIA -> AppR.drawable.ic_logo_wikipedia to Color.Unspecified
                    ExternalService.GITHUB_UPDATES -> AppR.drawable.ic_logo_github to Color.Unspecified
                    ExternalService.RADIO_BROWSER -> R.drawable.lucide_ic_radio to MaterialTheme.colorScheme.secondary
                }
                val rawEnabled = when (service) {
                    ExternalService.DEEZER -> uiState.services.deezerEnabled
                    ExternalService.AUDIODB -> uiState.services.audioDbEnabled
                    ExternalService.LRCLIB -> uiState.services.lrcLibEnabled
                    ExternalService.COVER_ART_ARCHIVE -> uiState.services.coverArtArchiveEnabled
                    ExternalService.WIKIPEDIA -> uiState.services.wikipediaEnabled
                    ExternalService.GITHUB_UPDATES -> uiState.services.githubUpdatesEnabled
                    ExternalService.RADIO_BROWSER -> uiState.services.radioBrowserEnabled
                }
                ServiceToggleRow(
                    service = service,
                    enabled = rawEnabled,
                    isBlackedOut = uiState.services.offlineBlackoutMode,
                    iconRes = iconRes,
                    iconTint = iconTint,
                    onToggle = { enabled -> viewModel.setServiceEnabled(service, enabled) }
                )
            }
        }
    }
}
