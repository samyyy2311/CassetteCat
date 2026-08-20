package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
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
        OnboardingProgressDots(currentStep = 2)
        Spacer(Modifier.height(32.dp))

        Text("Add your listening device", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Optional. Your phone library works on its own, and you can connect a CassetteCat device whenever you are ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(36.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                                )
                            )
                        )
                )
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_wifi),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Connect over Wi-Fi to keep playback and device updates in sync.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(Modifier.weight(1f))

        Button(onClick = hapticClick(onSetUpDevice), modifier = Modifier.fillMaxWidth()) {
            Text("Set up device")
        }
        TextButton(
            onClick = hapticClick(onSkip),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) { Text("Skip for now") }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceIntroScreenPreview() {
    CassetteCatTheme {
        DeviceIntroScreen(onSetUpDevice = {}, onSkip = {})
    }
}
