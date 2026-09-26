package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun DesktopRemoteScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp,
    viewModel: DesktopRemoteViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
        state.address != null -> DeviceNowPlayingScreen(
            remote = viewModel,
            onBack = onBack,
            modifier = modifier,
            listBottomPadding = listBottomPadding,
            title = title,
            waitingMessage = stringResource(AppR.string.desktop_remote_unreachable),
            headerAction = {
                TextButton(onClick = hapticClick(viewModel::forget)) {
                    Text(stringResource(AppR.string.desktop_remote_forget))
                }
            }
        )
        else -> DesktopPairingForm(title, onBack, viewModel::pair, modifier)
    }
}

@Composable
private fun DesktopPairingForm(title: String, onBack: () -> Unit, onPair: (String) -> Boolean, modifier: Modifier) {
    var address by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        RemoteScreenHeader(title, onBack)
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                stringResource(AppR.string.desktop_remote_pair_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                    invalid = false
                },
                label = { Text(stringResource(AppR.string.desktop_remote_address)) },
                placeholder = { Text("192.168.1.20:47800#ABC234", maxLines = 1) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_monitor),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                isError = invalid,
                supportingText = if (invalid) {
                    { Text(stringResource(AppR.string.desktop_remote_address_invalid)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
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
                onClick = hapticClick { invalid = !onPair(address) },
                enabled = address.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(AppR.string.desktop_remote_connect))
            }
        }
    }
}
