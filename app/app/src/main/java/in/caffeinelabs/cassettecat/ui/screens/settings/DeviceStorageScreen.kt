package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.device.DeviceFileEntry
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.onboarding.PairingViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

@Composable
fun DeviceStorageScreen(
    pairingViewModel: PairingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    var currentPath by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<DeviceFileEntry>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pendingDelete by remember { mutableStateOf<DeviceFileEntry?>(null) }
    var latestRequestId by remember { mutableStateOf(0) }

    fun load(path: String) {
        isLoading = true
        val requestId = ++latestRequestId
        pairingViewModel.listDeviceFiles(path) { result ->
            if (requestId == latestRequestId && path == currentPath) {
                entries = result
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentPath) { load(currentPath) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Back",
                onClick = { if (currentPath.isEmpty()) onBack() else currentPath = currentPath.substringBeforeLast('/', "") }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Player Storage", style = MaterialTheme.typography.headlineSmall)
                Text(
                    currentPath.ifEmpty { "/" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            isLoading -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
            }
            entries.isNullOrEmpty() -> EmptyState(
                iconRes = R.drawable.lucide_ic_folder,
                title = "Nothing here",
                message = "The player didn't respond, or this folder is empty.",
                modifier = Modifier.weight(1f)
            )
            else -> LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = listBottomPadding)) {
                items(entries.orEmpty(), key = { it.path }) { entry ->
                    DeviceFileRow(
                        entry = entry,
                        onClick = { if (entry.isDirectory) currentPath = entry.path },
                        onDelete = { pendingDelete = entry }
                    )
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${entry.name}\"?") },
            text = { Text("This permanently removes it from the player's SD card. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pairingViewModel.deleteDeviceFile(entry.path) { load(currentPath) }
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DeviceFileRow(entry: DeviceFileEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().tapScale(onClick).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(if (entry.isDirectory) R.drawable.lucide_ic_folder else R.drawable.lucide_ic_music),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!entry.isDirectory) {
                Text(formatFileBytes(entry.sizeBytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        PressDepthIconButton(
            iconRes = R.drawable.lucide_ic_trash_2,
            contentDescription = "Delete",
            onClick = onDelete
        )
    }
}

private fun formatFileBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024 * 1024 -> "%.0f MB".format(bytes / (1024.0 * 1024))
    else -> "%.0f KB".format(bytes / 1024.0)
}
