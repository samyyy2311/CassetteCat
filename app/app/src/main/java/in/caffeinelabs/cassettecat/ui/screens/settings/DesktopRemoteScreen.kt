package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.device.DesktopRemoteRepository
import `in`.caffeinelabs.cassettecat.data.device.DiscoveredDesktop
import `in`.caffeinelabs.cassettecat.data.device.PairingResult
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import kotlinx.coroutines.launch

@Composable
fun DesktopRemoteScreen(
    desktop: DesktopRemoteRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val state by desktop.state.collectAsStateWithLifecycle()
    val title = stringResource(AppR.string.desktop_remote_title)

    when {
        !state.loaded -> Unit
        state.offlineBlackout -> Column(modifier.fillMaxSize()) {
            RemoteScreenHeader(title, onBack)
            EmptyState(
                iconRes = R.drawable.lucide_ic_monitor,
                title = title,
                message = stringResource(AppR.string.desktop_remote_offline),
                modifier = Modifier.weight(1f)
            )
        }
        state.address != null -> Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RemoteScreenHeader(title, onBack)
            SettingsSection {
                ActionRow(
                    title = state.name ?: stringResource(AppR.string.desktop_remote_your_computer),
                    subtitle = stringResource(
                        if (state.controlling) AppR.string.desktop_remote_controlling else AppR.string.desktop_remote_paired
                    ),
                    iconRes = R.drawable.lucide_ic_monitor,
                    iconTint = if (state.controlling) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    onClick = { desktop.setControlling(!state.controlling) }
                )
            }
            Spacer(Modifier.height(20.dp))
            SettingsSection {
                ActionRow(
                    title = stringResource(AppR.string.desktop_remote_forget),
                    subtitle = stringResource(AppR.string.desktop_remote_forget_description),
                    iconRes = R.drawable.lucide_ic_x,
                    onClick = desktop::forget
                )
            }
            Spacer(Modifier.height(listBottomPadding))
        }
        else -> DesktopPairingForm(title, onBack, desktop, state.name, modifier)
    }
}

@Composable
private fun RemoteScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_chevron_left,
            contentDescription = stringResource(AppR.string.action_back),
            onClick = onBack
        )
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun DesktopPairingForm(
    title: String,
    onBack: () -> Unit,
    desktop: DesktopRemoteRepository,
    // Kept when the computer changed its code, so it is picked again straight away.
    previousName: String?,
    modifier: Modifier
) {
    val found by desktop.found.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<DiscoveredDesktop?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf<PairingResult?>(null) }
    var connecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { desktop.discover() }
    LaunchedEffect(found) {
        if (selected == null) selected = found.firstOrNull { it.name == previousName }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        RemoteScreenHeader(title, onBack)
        SettingsSection(title = stringResource(AppR.string.desktop_remote_found)) {
            found.forEach { computer ->
                ActionRow(
                    title = computer.name,
                    subtitle = computer.host,
                    iconRes = R.drawable.lucide_ic_monitor,
                    iconTint = if (computer == selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    onClick = {
                        selected = computer
                        input = ""
                        result = null
                    }
                )
                SettingsDivider()
            }
            ActionRow(
                title = stringResource(AppR.string.desktop_remote_search_again),
                subtitle = stringResource(AppR.string.desktop_remote_search_hint),
                iconRes = R.drawable.lucide_ic_refresh_cw,
                onClick = desktop::discover
            )
        }
        Spacer(Modifier.height(20.dp))
        SettingsSection {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val target = selected
                Text(
                    when {
                        target != null && target.name == previousName ->
                            stringResource(AppR.string.desktop_remote_code_changed, target.name)
                        target != null -> stringResource(AppR.string.desktop_remote_code_hint, target.name)
                        else -> stringResource(AppR.string.desktop_remote_pair_hint)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        result = null
                    },
                    label = {
                        Text(stringResource(if (target != null) AppR.string.desktop_remote_code else AppR.string.desktop_remote_address))
                    },
                    placeholder = { Text(if (target != null) "ABC234" else "192.168.1.20:47800#ABC234", maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(if (target != null) R.drawable.lucide_ic_key_round else R.drawable.lucide_ic_monitor),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    isError = result != null,
                    supportingText = pairingError(result)?.let { message -> { Text(stringResource(message)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = if (target != null) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                        keyboardType = if (target != null) KeyboardType.Ascii else KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = hapticClick {
                        connecting = true
                        scope.launch {
                            val outcome = if (target != null) desktop.pair(target, input) else desktop.pair(input)
                            result = outcome.takeUnless { it == PairingResult.PAIRED }
                            connecting = false
                        }
                    },
                    enabled = input.isNotBlank() && !connecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(AppR.string.action_connect))
                }
            }
        }
    }
}

private fun pairingError(result: PairingResult?): Int? = when (result) {
    PairingResult.INVALID_ADDRESS -> AppR.string.desktop_remote_address_invalid
    PairingResult.WRONG_CODE -> AppR.string.desktop_remote_wrong_code
    PairingResult.UNREACHABLE -> AppR.string.desktop_remote_unreachable
    PairingResult.PAIRED, null -> null
}

/** Picks which device this phone plays on and controls, like Spotify Connect. */
@Composable
fun DeviceConnectSheet(
    desktop: DesktopRemoteRepository,
    phoneSong: Song?,
    onSelectPhone: () -> Unit,
    onSelectDesktop: () -> Unit,
    onSetUpDesktop: () -> Unit,
    onDismiss: () -> Unit
) {
    val state by desktop.state.collectAsStateWithLifecycle()
    val status by desktop.status.collectAsStateWithLifecycle()
    DisposableEffect(desktop) {
        desktop.startPolling()
        onDispose { desktop.stopPolling() }
    }
    val notPlaying = stringResource(AppR.string.widget_not_playing)
    val selectedTint = MaterialTheme.colorScheme.tertiary
    val idleTint = MaterialTheme.colorScheme.secondary

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Text(
            stringResource(AppR.string.desktop_remote_connect_title),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        SettingsSection {
            ActionRow(
                title = stringResource(AppR.string.desktop_remote_this_phone),
                subtitle = phoneSong?.let { "${it.title} · ${it.artist}" } ?: notPlaying,
                iconRes = R.drawable.lucide_ic_smartphone,
                iconTint = if (state.controlling) idleTint else selectedTint,
                onClick = onSelectPhone
            )
            SettingsDivider()
            if (state.address != null) {
                ActionRow(
                    title = state.name ?: stringResource(AppR.string.desktop_remote_your_computer),
                    subtitle = status?.takeIf { it.trackTitle.isNotEmpty() }?.let { "${it.trackTitle} · ${it.trackArtist}" } ?: notPlaying,
                    iconRes = R.drawable.lucide_ic_monitor,
                    iconTint = if (state.controlling) selectedTint else idleTint,
                    onClick = onSelectDesktop
                )
            } else {
                ActionRow(
                    title = stringResource(AppR.string.desktop_remote_set_up),
                    subtitle = stringResource(AppR.string.desktop_remote_description),
                    iconRes = R.drawable.lucide_ic_monitor,
                    onClick = onSetUpDesktop
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
