package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterConfig
import `in`.caffeinelabs.cassettecat.data.library.FolderFilterMode
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

@Composable
fun LibraryScanScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryScanViewModel = viewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OnboardingHeaderRow(currentStep = 1, totalSteps = 5, onSkip = { viewModel.skip(onSkip) })
        Spacer(Modifier.height(10.dp))

        Text(stringResource(AppR.string.onboarding_library_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(AppR.string.onboarding_library_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        FolderScanConfigBody(
            config = config,
            onSetMode = viewModel::setMode,
            onAddFolder = viewModel::addFolder,
            onRemoveFolder = viewModel::removeFolder
        )

        Spacer(Modifier.weight(1f))

        val needsFolder = config.mode == FolderFilterMode.WHITELIST && config.folders.isEmpty()
        Button(
            onClick = hapticClick { viewModel.save(onContinue) },
            enabled = !needsFolder,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(AppR.string.action_continue)) }
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
            title = stringResource(AppR.string.onboarding_library_all_music_title),
            description = stringResource(AppR.string.onboarding_library_all_music_description),
            selected = config.mode == FolderFilterMode.NONE,
            onClick = { onSetMode(FolderFilterMode.NONE) }
        )
        ScanModeOption(
            title = stringResource(AppR.string.onboarding_library_selected_folders_title),
            description = stringResource(AppR.string.onboarding_library_selected_folders_description),
            selected = config.mode == FolderFilterMode.WHITELIST,
            onClick = { onSetMode(FolderFilterMode.WHITELIST) }
        )
        ScanModeOption(
            title = stringResource(AppR.string.onboarding_library_excluded_folders_title),
            description = stringResource(AppR.string.onboarding_library_excluded_folders_description),
            selected = config.mode == FolderFilterMode.BLACKLIST,
            onClick = { onSetMode(FolderFilterMode.BLACKLIST) }
        )

        if (config.mode != FolderFilterMode.NONE) {
            Spacer(Modifier.height(8.dp))
            if (config.folders.isEmpty() && config.mode == FolderFilterMode.WHITELIST) {
                Text(
                    stringResource(AppR.string.onboarding_library_folder_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }
            config.folders.forEach { path ->
                FolderRow(path = path, onRemove = { onRemoveFolder(path) })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tapScale { pickFolder.launch(null) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_folder),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(16.dp))
                Text(stringResource(AppR.string.action_add_folder), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ScanModeOption(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(
                if (selected) R.drawable.lucide_ic_circle_check_big else R.drawable.lucide_ic_circle
            ),
            contentDescription = null,
            modifier = Modifier.padding(top = 2.dp).size(24.dp),
            tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.lucide_ic_folder),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = path.substringAfterLast('/').ifEmpty { path },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.lucide_ic_x),
            contentDescription = stringResource(AppR.string.action_remove_folder),
            modifier = Modifier
                .tapScale(onRemove)
                .padding(4.dp)
                .size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryScanScreenPreview() {
    CassetteCatTheme {
        LibraryScanScreen(onContinue = {}, onSkip = {})
    }
}
