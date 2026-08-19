package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun PermissionsScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val permissions = remember { OnboardingPermissions.required() }
    val showNotificationRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Result ignored on purpose: never block or retry-loop on denial, the user
        // can always grant this later from system settings.
        onContinue()
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OnboardingProgressDots(currentStep = 0)
        Spacer(Modifier.height(32.dp))

        Text("Give your library a starting point", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        Text(
            "CassetteCat only asks for what it needs to play your music and keep playback handy.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        PermissionCard(
            iconRes = R.drawable.lucide_ic_music,
            title = "Music and audio",
            description = "Find and play music stored on your phone."
        )
        if (showNotificationRow) {
            Spacer(Modifier.height(20.dp))
            PermissionCard(
                iconRes = R.drawable.lucide_ic_bell,
                title = "Notifications",
                description = "Keep playback controls available outside the app."
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(Modifier.height(20.dp))
            PermissionCard(
                iconRes = R.drawable.lucide_ic_radio,
                title = "Nearby devices",
                description = "Connect and sync with the CassetteCat companion player."
            )
        }

        // Top-anchored, not centered: a short device-menu-style list, not a hero moment.
        Spacer(Modifier.weight(1f))

        Button(
            onClick = hapticClick { launcher.launch(permissions.toTypedArray()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
    }
}

@Composable
private fun PermissionCard(iconRes: Int, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.padding(top = 2.dp).size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionsScreenPreview() {
    CassetteCatTheme {
        PermissionsScreen(onContinue = {})
    }
}
