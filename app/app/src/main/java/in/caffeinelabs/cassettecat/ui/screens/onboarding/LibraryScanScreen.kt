package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ButtonDefaults
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
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterConfig
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterMode
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun LibraryScanScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryScanViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OnboardingProgressDots(currentStep = 1)
        Spacer(Modifier.height(32.dp))

        Text("Choose your music folders", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        Text(
            "You can change this later in Preferences.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        FolderScanConfigBody(
            config = config,
            onSetMode = viewModel::setMode,
            onAddFolder = viewModel::addFolder,
            onRemoveFolder = viewModel::removeFolder
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = hapticClick { viewModel.save(onContinue) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
        TextButton(
            onClick = hapticClick { viewModel.save(onContinue) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) { Text("Skip for now") }
    }
}

@Composable
fun FolderScanConfigBody(
    config: FolderFilterConfig,
    onSetMode: (FolderFilterMode) -> Unit,
    onAddFolder: (Uri) -> Unit,
    onRemoveFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) onAddFolder(uri) }

    Column(modifier = modifier) {
        ScanModeOption(
            title = "All music",
            description = "Find audio anywhere your phone allows.",
            selected = config.mode == FolderFilterMode.NONE,
            onClick = { onSetMode(FolderFilterMode.NONE) }
        )
        Spacer(Modifier.height(12.dp))
        ScanModeOption(
            title = "Only these folders",
            description = "Build a library from selected folders only.",
            selected = config.mode == FolderFilterMode.WHITELIST,
            onClick = { onSetMode(FolderFilterMode.WHITELIST) }
        )
        Spacer(Modifier.height(12.dp))
        ScanModeOption(
            title = "Everything except these folders",
            description = "Ignore selected folders while scanning.",
            selected = config.mode == FolderFilterMode.BLACKLIST,
            onClick = { onSetMode(FolderFilterMode.BLACKLIST) }
        )

        if (config.mode != FolderFilterMode.NONE) {
            Spacer(Modifier.height(20.dp))
            config.folders.forEach { path ->
                FolderRow(path = path, onRemove = { onRemoveFolder(path) })
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = hapticClick { pickFolder.launch(null) }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_folder),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text("Add folder", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ScanModeOption(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = hapticClick(onClick)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                if (selected) R.drawable.lucide_ic_circle_check_big else R.drawable.lucide_ic_circle
            ),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FolderRow(path: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = path.substringAfterLast('/').ifEmpty { path },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_x),
            contentDescription = "Remove folder",
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = hapticClick(onRemove)),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryScanScreenPreview() {
    CassetteCatTheme {
        LibraryScanScreen(onContinue = {})
    }
}
