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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun PermissionsScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val permissions = remember { OnboardingPermissions.required() }
    val showNotificationRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onContinue()
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OnboardingHeaderRow(currentStep = 0, totalSteps = 5)
        Spacer(Modifier.height(10.dp))

        Text(stringResource(AppR.string.onboarding_permissions_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(AppR.string.onboarding_permissions_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        PermissionCard(
            iconRes = R.drawable.lucide_ic_music,
            title = stringResource(AppR.string.onboarding_permission_music_title),
            description = stringResource(AppR.string.onboarding_permission_music_description)
        )
        if (showNotificationRow) {
            Spacer(Modifier.height(28.dp))
            PermissionCard(
                iconRes = R.drawable.lucide_ic_bell,
                title = stringResource(AppR.string.onboarding_permission_notifications_title),
                description = stringResource(AppR.string.onboarding_permission_notifications_description)
            )
            Spacer(Modifier.height(28.dp))
            PermissionCard(
                iconRes = R.drawable.lucide_ic_wifi,
                title = stringResource(AppR.string.onboarding_permission_nearby_title),
                description = stringResource(AppR.string.onboarding_permission_nearby_description)
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = hapticClick { launcher.launch(permissions.toTypedArray()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(AppR.string.action_continue)) }
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
