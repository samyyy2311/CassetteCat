package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun DeviceIntroScreen(
    onSetUpDevice: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OnboardingHeaderRow(currentStep = 3, totalSteps = 5, onSkip = onSkip)
        Spacer(Modifier.height(10.dp))

        Text("Add your listening device", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Optional. Connect a CassetteCat device whenever you are ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            OnboardingHeroIcon(iconRes = R.drawable.lucide_ic_wifi)
        }
        Text(
            "Connect over Wi-Fi to keep playback and device updates in sync.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Button(onClick = hapticClick(onSetUpDevice), modifier = Modifier.fillMaxWidth()) {
            Text("Set up device")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceIntroScreenPreview() {
    CassetteCatTheme {
        DeviceIntroScreen(onSetUpDevice = {}, onSkip = {})
    }
}
