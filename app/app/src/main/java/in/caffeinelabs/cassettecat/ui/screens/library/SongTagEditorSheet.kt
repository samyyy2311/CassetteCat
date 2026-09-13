package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.SongMetadataOverride
import `in`.caffeinelabs.cassettecat.data.library.SongMetadataOverridesRepository
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.launch

@Composable
fun SongTagEditorSheet(
    song: Song,
    onDismiss: () -> Unit,
    onSaved: (Song) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val repository = remember { SongMetadataOverridesRepository.getInstance(context) }

    val existingOverride = remember(song.id) { repository.overrides.value[song.id] }
    val isOverridden = repository.hasOverride(song.id)

    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf(song.album) }
    var genreText by remember(song.id) { mutableStateOf(song.genres.joinToString(", ")) }
    var yearText by remember(song.id) { mutableStateOf(song.releaseYear?.toString() ?: "") }

    val hasChanges = title != song.title ||
        artist != song.artist ||
        album != song.album ||
        genreText != song.genres.joinToString(", ") ||
        yearText != (song.releaseYear?.toString() ?: "")

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AlbumArt(song = song, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Edit Song Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Custom overrides are saved locally for this track",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                TagInputField(
                    label = "Title",
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Song title"
                )

                Spacer(Modifier.height(14.dp))

                TagInputField(
                    label = "Artist",
                    value = artist,
                    onValueChange = { artist = it },
                    placeholder = "Artist name"
                )

                Spacer(Modifier.height(14.dp))

                TagInputField(
                    label = "Album",
                    value = album,
                    onValueChange = { album = it },
                    placeholder = "Album title"
                )

                Spacer(Modifier.height(14.dp))

                TagInputField(
                    label = "Genre",
                    value = genreText,
                    onValueChange = { genreText = it },
                    placeholder = "e.g. Rock, Pop, Electronic"
                )

                Spacer(Modifier.height(14.dp))

                TagInputField(
                    label = "Release Year",
                    value = yearText,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) yearText = it },
                    placeholder = "e.g. 2024",
                    keyboardType = KeyboardType.Number
                )

                Spacer(Modifier.height(24.dp))

                if (isOverridden) {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            coroutineScope.launch {
                                val origTitle = existingOverride?.originalTitle ?: song.title
                                val origArtist = existingOverride?.originalArtist ?: song.artist
                                val origAlbum = existingOverride?.originalAlbum ?: song.album
                                val origYear = existingOverride?.originalReleaseYear ?: song.releaseYear
                                val origGenres = existingOverride?.originalGenres ?: song.genres

                                repository.removeOverride(song.id)
                                val revertedSong = song.copy(
                                    title = origTitle,
                                    artist = origArtist,
                                    album = origAlbum,
                                    releaseYear = origYear,
                                    genres = origGenres
                                )
                                onSaved(revertedSong)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_rotate_ccw),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Revert to Original Tags")
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasChanges) {
                        OutlinedButton(
                            onClick = {
                                title = song.title
                                artist = song.artist
                                album = song.album
                                genreText = song.genres.joinToString(", ")
                                yearText = song.releaseYear?.toString() ?: ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                    }

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            val parsedYear = yearText.trim().toIntOrNull()
                            val parsedGenres = genreText.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            val newTitle = title.trim().ifBlank { song.title }
                            val newArtist = artist.trim().ifBlank { song.artist }
                            val newAlbum = album.trim().ifBlank { song.album }

                            val override = SongMetadataOverride(
                                songId = song.id,
                                title = newTitle,
                                artist = newArtist,
                                album = newAlbum,
                                releaseYear = parsedYear,
                                releaseYearSet = true,
                                genres = parsedGenres,
                                originalTitle = existingOverride?.originalTitle ?: song.title,
                                originalArtist = existingOverride?.originalArtist ?: song.artist,
                                originalAlbum = existingOverride?.originalAlbum ?: song.album,
                                originalReleaseYear = existingOverride?.originalReleaseYear ?: song.releaseYear,
                                originalGenres = existingOverride?.originalGenres ?: song.genres
                            )
                            val updatedSong = song.copy(
                                title = newTitle,
                                artist = newArtist,
                                album = newAlbum,
                                releaseYear = parsedYear,
                                genres = parsedGenres
                            )
                            coroutineScope.launch {
                                repository.saveOverride(override)
                                onSaved(updatedSong)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(if (hasChanges) 2f else 1f)
                    ) {
                        Text(
                            text = "Save Changes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
