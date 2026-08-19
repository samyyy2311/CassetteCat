package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.FolderScanConfigBody
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.LibraryScanViewModel

@Composable
fun ManageScanFoldersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryScanViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = onBack
            )
            Text("Scan Folders", style = MaterialTheme.typography.headlineSmall)
        }

        FolderScanConfigBody(
            config = config,
            onSetMode = { mode -> viewModel.setMode(mode); viewModel.save {} },
            onAddFolder = { uri -> viewModel.addFolder(uri); viewModel.save {} },
            onRemoveFolder = { path -> viewModel.removeFolder(path); viewModel.save {} },
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
