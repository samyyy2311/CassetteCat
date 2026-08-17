package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.PlaylistCoverStorage
import `in`.caffeinelabs.cassettecat.data.library.PlaylistCoverType
import `in`.caffeinelabs.cassettecat.ui.components.PLAYLIST_EMOJI_OPTIONS
import `in`.caffeinelabs.cassettecat.ui.components.PLAYLIST_ICON_OPTIONS
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlinx.coroutines.launch

private enum class CoverPickerTab(val label: String) { PHOTO("Photo"), ICON("Icon"), EMOJI("Emoji") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCoverPickerSheet(
    playlist: Playlist,
    onSetCover: (PlaylistCoverType, String?) -> Unit,
    onClearCover: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coverStorage = remember { PlaylistCoverStorage(context) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(CoverPickerTab.PHOTO) }
    var isSaving by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            isSaving = true
            scope.launch {
                val path = coverStorage.save(playlist.id, uri)
                isSaving = false
                if (path != null) {
                    onSetCover(PlaylistCoverType.IMAGE, path)
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Change Cover",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            CoverPickerTabs(selected = tab, onSelect = { tab = it })
            Spacer(Modifier.height(16.dp))

            when (tab) {
                CoverPickerTab.PHOTO -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tapScale {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_image_plus),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            if (isSaving) "Saving…" else "Choose from Photos",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                CoverPickerTab.ICON -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxWidth().height(280.dp).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PLAYLIST_ICON_OPTIONS, key = { it.first }) { (name, iconRes) ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .tapScale { onSetCover(PlaylistCoverType.ICON, name); onDismiss() }
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                CoverPickerTab.EMOJI -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxWidth().height(280.dp).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PLAYLIST_EMOJI_OPTIONS) { emoji ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .tapScale { onSetCover(PlaylistCoverType.EMOJI, emoji); onDismiss() }
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }

            if (playlist.coverType != PlaylistCoverType.NONE) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tapScale { onClearCover(); onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_trash_2),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("Remove Cover", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// plain text tabs, not TabRow: matches LibraryViewModeTabs' no-ripple/no-filled-indicator style
@Composable
private fun CoverPickerTabs(selected: CoverPickerTab, onSelect: (CoverPickerTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CoverPickerTab.entries.forEach { entry ->
            val isSelected = entry == selected
            val tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier.tapScale { onSelect(entry) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(entry.label, style = MaterialTheme.typography.titleSmall, color = tint)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (isSelected) 20.dp else 0.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(tint)
                )
            }
        }
    }
}
