package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.PageHeader
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.FolderScanConfigBody
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.LibraryScanViewModel

@Composable
fun ManageScanFoldersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryScanViewModel = viewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        PageHeader(stringResource(AppR.string.settings_scan_folders), onBack)

        FolderScanConfigBody(
            config = config,
            onSetMode = { mode -> viewModel.setMode(mode); viewModel.save {} },
            onAddFolder = { uri -> viewModel.addFolder(uri); viewModel.save {} },
            onRemoveFolder = { path -> viewModel.removeFolder(path); viewModel.save {} },
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
