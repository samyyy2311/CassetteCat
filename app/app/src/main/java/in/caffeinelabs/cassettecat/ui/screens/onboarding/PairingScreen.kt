package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.device.DeviceConnectionType
import `in`.caffeinelabs.cassettecat.data.device.DevicePairingState
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun PairingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PairingViewModel = viewModel()
) {
    val state by viewModel.pairingState.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OnboardingProgressDots(currentStep = 3)
        Spacer(Modifier.height(32.dp))

        Column(modifier = Modifier.weight(1f)) {
            when (val current = state) {
                is DevicePairingState.SelectingMode -> {
                    Text("How is your device connected?", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Choose the connection it is using right now.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(28.dp))
                    PairingOptionCard(
                        iconRes = R.drawable.lucide_ic_radio_tower,
                        title = "Its own Wi-Fi network",
                        onClick = { viewModel.selectMode(DeviceConnectionType.SOFT_AP) }
                    )
                    Spacer(Modifier.height(12.dp))
                    PairingOptionCard(
                        iconRes = R.drawable.lucide_ic_house_wifi,
                        title = "My home Wi-Fi network",
                        onClick = { viewModel.selectMode(DeviceConnectionType.STATION) }
                    )
                }

                is DevicePairingState.Searching -> {
                    PairingStatus {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Looking for your device...", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = hapticClick { viewModel.cancelSearch() }) { Text("Cancel") }
                    }
                }

                is DevicePairingState.DeviceFound -> {
                    PairingStatus {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_cassette_tape),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Found ${current.device.name}", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = hapticClick { viewModel.connect(current.device) }) { Text("Connect") }
                    }
                }

                is DevicePairingState.Connecting -> {
                    PairingStatus {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Connecting to ${current.device.name}...", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                is DevicePairingState.Connected -> {
                    PairingStatus {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_circle_check_big),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Connected to ${current.device.name}", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = hapticClick(onFinish)) { Text("Done") }
                    }
                }

                is DevicePairingState.Failed -> {
                    PairingStatus {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_triangle_alert),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Couldn't connect", style = MaterialTheme.typography.headlineSmall)
                        Text(current.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = hapticClick { viewModel.selectMode(current.mode) }) { Text("Try again") }
                    }
                }
            }
        }

        if (state !is DevicePairingState.Connected) {
            TextButton(onClick = hapticClick(onFinish), modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now")
            }
        }
    }
}

@Composable
private fun PairingOptionCard(iconRes: Int, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = hapticClick(onClick)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PairingStatus(content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun PairingScreenPreview() {
    CassetteCatTheme {
        PairingScreen(onFinish = {})
    }
}
