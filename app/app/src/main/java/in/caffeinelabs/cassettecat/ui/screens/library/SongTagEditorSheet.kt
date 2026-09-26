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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.library.SongMetadataOverride
import `in`.caffeinelabs.cassettecat.data.library.SongMetadataOverridesRepository
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.launch

/** Changed fields from the editor; null (or yearEdited = false) leaves that field as it was. */
internal data class TagEdits(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genres: List<String>? = null,
    val yearEdited: Boolean = false,
    val year: Int? = null
)

internal fun applyTagEdits(song: Song, existing: SongMetadataOverride?, edits: TagEdits): Pair<SongMetadataOverride, Song> {
    val updated = song.copy(
        title = edits.title?.trim()?.ifBlank { null } ?: song.title,
        artist = edits.artist?.trim()?.ifBlank { null } ?: song.artist,
        album = edits.album?.trim()?.ifBlank { null } ?: song.album,
        genres = edits.genres ?: song.genres,
        releaseYear = if (edits.yearEdited) edits.year else song.releaseYear
    )
    val override = SongMetadataOverride(
        songId = song.id,
        title = updated.title,
        artist = updated.artist,
        album = updated.album,
        releaseYear = updated.releaseYear,
        releaseYearSet = true,
        genres = updated.genres,
        originalTitle = existing?.originalTitle ?: song.title,
        originalArtist = existing?.originalArtist ?: song.artist,
        originalAlbum = existing?.originalAlbum ?: song.album,
        originalReleaseYear = existing?.originalReleaseYear ?: song.releaseYear,
        originalGenres = existing?.originalGenres ?: song.genres
    )
    return override to updated
}

@Composable
fun SongTagEditorSheet(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onSaved: (List<Song>) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val repository = remember { SongMetadataOverridesRepository.getInstance(context) }
    val batch = songs.size > 1
    val songIds = songs.map { it.id }

    // A field is prefilled only when every song agrees; otherwise it starts empty and shows "Mixed".
    fun shared(value: (Song) -> String): String? = songs.map(value).distinct().singleOrNull()
    val initialTitle = remember(songIds) { shared { it.title } ?: "" }
    val initialArtist = remember(songIds) { shared { it.artist } }
    val initialAlbum = remember(songIds) { shared { it.album } }
    val initialGenres = remember(songIds) { shared { it.genres.joinToString(", ") } }
    val initialYear = remember(songIds) { shared { it.releaseYear?.toString() ?: "" } }

    var title by remember(songIds) { mutableStateOf(initialTitle) }
    var artist by remember(songIds) { mutableStateOf(initialArtist ?: "") }
    var album by remember(songIds) { mutableStateOf(initialAlbum ?: "") }
    var genreText by remember(songIds) { mutableStateOf(initialGenres ?: "") }
    var yearText by remember(songIds) { mutableStateOf(initialYear ?: "") }

    val hasChanges = title != initialTitle ||
        artist != (initialArtist ?: "") ||
        album != (initialAlbum ?: "") ||
        genreText != (initialGenres ?: "") ||
        yearText != (initialYear ?: "")
    val overriddenSongs = songs.filter { repository.hasOverride(it.id) }
    val mixed = stringResource(AppR.string.tag_editor_mixed)

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
                    AlbumArt(song = songs.first(), modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (batch) pluralStringResource(AppR.plurals.library_songs, songs.size, songs.size) else songs.first().title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = initialArtist ?: mixed,
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
                    text = stringResource(AppR.string.tag_editor_heading),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(if (batch) AppR.string.tag_editor_description_batch else AppR.string.tag_editor_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                if (!batch) {
                    TagInputField(
                        label = stringResource(AppR.string.tag_editor_title),
                        value = title,
                        onValueChange = { title = it },
                        placeholder = stringResource(AppR.string.tag_editor_title_placeholder)
                    )

                    Spacer(Modifier.height(14.dp))
                }

                TagInputField(
                    label = stringResource(AppR.string.tag_editor_artist),
                    value = artist,
                    onValueChange = { artist = it },
                    placeholder = if (initialArtist == null) mixed else stringResource(AppR.string.tag_editor_artist_placeholder)
                )

                Spacer(Modifier.height(14.dp))

                TagInputField(
                    label = stringResource(AppR.string.tag_editor_album),
                    value = album,
                    onValueChange = { album = it },
                    placeholder = if (initialAlbum == null) mixed else stringResource(AppR.string.tag_editor_album_placeholder)
                )

                Spacer(Modifier.height(14.dp))

                TagInputField(
                    label = stringResource(AppR.string.tag_editor_genre),
                    value = genreText,
                    onValueChange = { genreText = it },
                    placeholder = if (initialGenres == null) mixed else stringResource(AppR.string.tag_editor_genre_placeholder)
                )

                Spacer(Modifier.height(14.dp))

                TagInputField(
                    label = stringResource(AppR.string.tag_editor_year),
                    value = yearText,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) yearText = it },
                    placeholder = if (initialYear == null) mixed else stringResource(AppR.string.tag_editor_year_placeholder),
                    keyboardType = KeyboardType.Number
                )

                Spacer(Modifier.height(24.dp))

                if (overriddenSongs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            coroutineScope.launch {
                                val reverted = overriddenSongs.map { song ->
                                    val existing = repository.overrides.value[song.id]
                                    song.copy(
                                        title = existing?.originalTitle ?: song.title,
                                        artist = existing?.originalArtist ?: song.artist,
                                        album = existing?.originalAlbum ?: song.album,
                                        releaseYear = existing?.originalReleaseYear ?: song.releaseYear,
                                        genres = existing?.originalGenres ?: song.genres
                                    )
                                }
                                repository.removeOverrides(overriddenSongs.map { it.id })
                                onSaved(reverted)
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
                        Text(stringResource(AppR.string.tag_editor_revert))
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
                                title = initialTitle
                                artist = initialArtist ?: ""
                                album = initialAlbum ?: ""
                                genreText = initialGenres ?: ""
                                yearText = initialYear ?: ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(AppR.string.action_reset))
                        }
                    }

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            val edits = TagEdits(
                                title = title.takeIf { it != initialTitle },
                                artist = artist.takeIf { it != (initialArtist ?: "") },
                                album = album.takeIf { it != (initialAlbum ?: "") },
                                genres = genreText.takeIf { it != (initialGenres ?: "") }
                                    ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() },
                                yearEdited = yearText != (initialYear ?: ""),
                                year = yearText.trim().toIntOrNull()
                            )
                            val results = songs.map { applyTagEdits(it, repository.overrides.value[it.id], edits) }
                            coroutineScope.launch {
                                repository.saveOverrides(results.map { it.first })
                                onSaved(results.map { it.second })
                                onDismiss()
                            }
                        },
                        enabled = hasChanges || !batch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(if (hasChanges) 2f else 1f)
                    ) {
                        Text(
                            text = stringResource(AppR.string.tag_editor_save_changes),
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
