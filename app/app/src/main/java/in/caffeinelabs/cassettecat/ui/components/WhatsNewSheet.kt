package `in`.caffeinelabs.cassettecat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.caffeinelabs.cassettecat.BuildConfig
import `in`.caffeinelabs.cassettecat.R
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun WhatsNewSheet(notes: List<String>, onDismiss: () -> Unit) {
    ReleaseSheet(
        title = stringResource(R.string.whats_new_title),
        version = BuildConfig.VERSION_NAME,
        notes = notes,
        onDismiss = onDismiss
    )
}

/** Offers [version], listing what's new in it, with a button to download it. */
@Composable
fun UpdateSheet(version: String, notes: List<String>, onDownload: () -> Unit, onDismiss: () -> Unit) {
    ReleaseSheet(
        title = stringResource(R.string.update_prompt_title),
        version = version,
        notes = notes,
        onDismiss = onDismiss
    ) {
        Text(
            stringResource(R.string.update_prompt_message, version),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = hapticClick(onDownload), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_download))
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_later))
        }
    }
}

// The app icon and a version over a list of changes, shared by What's New and the update offer.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseSheet(
    title: String,
    version: String,
    notes: List<String>,
    onDismiss: () -> Unit,
    actions: (@Composable () -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.whats_new_version, version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // A long release scrolls within half the screen, so the sheet stays compact and its buttons in reach.
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.45f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    notes.forEach { note ->
                        Row {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (actions != null) {
                Spacer(Modifier.height(20.dp))
                actions()
            }
        }
    }
}
