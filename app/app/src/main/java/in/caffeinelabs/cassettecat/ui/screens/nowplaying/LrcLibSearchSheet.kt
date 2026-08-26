package `in`.caffeinelabs.cassettecat.ui.screens.nowplaying

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.data.playback.LrcLibClient
import `in`.caffeinelabs.cassettecat.data.playback.LrcLibSearchResultItem
import `in`.caffeinelabs.cassettecat.data.playback.parseLrc
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticToggle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SONG_TITLE_CLEANUP_REGEX = Regex(
    """\s*[\(\[\{](?:feat\.?|ft\.?|remaster(?:ed)?|bonus|explicit|version|deluxe|edit|live|mono|stereo|single|duet|soundtrack|ost|audio|video|official).*?[\)\]\}]""",
    RegexOption.IGNORE_CASE
)

private fun cleanSearchTitle(title: String): String {
    val cleaned = title.replace(SONG_TITLE_CLEANUP_REGEX, "").trim()
    return cleaned.ifBlank { title.trim() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LrcLibSearchSheet(
    song: Song,
    playbackViewModel: PlaybackViewModel,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val lrcLibClient = remember { LrcLibClient(context.cacheDir) }

    var query by remember(song.id) { mutableStateOf(song.title.trim()) }
    var results by remember { mutableStateOf<List<LrcLibSearchResultItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    // In-app custom lyrics editor state
    var showCustomLyricsEditor by remember { mutableStateOf(false) }
    var customLyricsText by remember { mutableStateOf("") }
    var contributeToLrcLib by remember { mutableStateOf(true) }
    var isPublishing by remember { mutableStateOf(false) }

    fun executeSearch() {
        if (query.isBlank() || isSearching) return
        keyboardController?.hide()
        isSearching = true
        hasSearched = true
        coroutineScope.launch {
            val list = lrcLibClient.search(query)
            if (list.isEmpty()) {
                val cleanedTitle = cleanSearchTitle(query)
                val fallbackList = if (cleanedTitle.isNotBlank() && cleanedTitle != query) {
                    lrcLibClient.search(cleanedTitle)
                } else emptyList()
                results = fallbackList.ifEmpty { list }
            } else {
                results = list
            }
            isSearching = false
        }
    }

    LaunchedEffect(song.id) {
        executeSearch()
    }

    FullOpenBottomSheet(onDismiss = onDismiss) {
        if (showCustomLyricsEditor) {
            // In-App Custom Lyrics & LRCLIB Contribution View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Custom Lyrics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${song.title} • ${song.artist}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { showCustomLyricsEditor = false }) {
                        Text("Search", color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = customLyricsText,
                    onValueChange = { customLyricsText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 280.dp),
                    placeholder = {
                        Text(
                            "Paste synchronized LRC or plain lyrics here...\n\nExample synced format:\n[00:12.30]First line of the song\n[00:16.80]Second line of the song...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = IbmPlexMonoFontFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )

                Spacer(Modifier.height(14.dp))

                // LRCLIB Contribution Toggle Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable { contributeToLrcLib = !contributeToLrcLib }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_globe),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Contribute to LRCLIB",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Share with the open-source global lyrics database",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Switch(
                            checked = contributeToLrcLib,
                            onCheckedChange = hapticToggle { contributeToLrcLib = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                                checkedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                checkedBorderColor = MaterialTheme.colorScheme.tertiary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = { showCustomLyricsEditor = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (customLyricsText.isBlank() || isPublishing) return@Button
                            keyboardController?.hide()
                            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)

                            val parsedSynced = parseLrc(customLyricsText)
                            val hasSynced = parsedSynced.isNotEmpty()
                            val syncedList = if (hasSynced) parsedSynced else null
                            val plainString = customLyricsText.trim()

                            playbackViewModel.applyManualLyrics(
                                song = song,
                                synced = syncedList,
                                plain = plainString,
                                provider = if (hasSynced) "Custom LRC" else "Custom Plain"
                            )

                            if (contributeToLrcLib) {
                                isPublishing = true
                                coroutineScope.launch {
                                    val syncedPayload = if (hasSynced) customLyricsText.trim() else null
                                    val plainPayload = if (!hasSynced) customLyricsText.trim() else null
                                    val published = lrcLibClient.publishLyrics(
                                        trackName = cleanSearchTitle(song.title),
                                        artistName = song.artist,
                                        albumName = song.album,
                                        durationSeconds = (song.durationMs / 1000L).toInt().coerceAtLeast(1),
                                        plainLyrics = plainPayload,
                                        syncedLyrics = syncedPayload
                                    )
                                    isPublishing = false
                                    withContext(Dispatchers.Main) {
                                        if (published) {
                                            Toast.makeText(context, "Lyrics applied & contributed to LRCLIB! 🌐", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Lyrics applied locally", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    onDismiss()
                                }
                            } else {
                                Toast.makeText(context, "Lyrics applied locally", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(2f),
                        enabled = customLyricsText.isNotBlank() && !isPublishing,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onTertiary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Publishing...")
                        } else {
                            Text("Save & Apply Lyrics")
                        }
                    }
                }
            }
        } else {
            // Search View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Search Online Lyrics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Powered by LRCLIB database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    TextButton(onClick = { showCustomLyricsEditor = true }) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_pencil),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add Custom", color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Search Bar Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Track title...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { executeSearch() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .clickable { executeSearch() }
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_search),
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Results List
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                    }
                } else if (results.isEmpty() && hasSearched) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lucide_ic_search),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No lyrics found for this query",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { showCustomLyricsEditor = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.lucide_ic_pencil),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Add & Contribute Lyrics", fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(results, key = { index, item -> item.id ?: index.toLong() }) { _, item ->
                            val hasSynced = !item.syncedLyrics.isNullOrBlank()
                            val hasPlain = !item.plainLyrics.isNullOrBlank()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                        val parsedSynced = item.syncedLyrics?.let(::parseLrc)
                                        playbackViewModel.applyManualLyrics(
                                            song = song,
                                            synced = parsedSynced,
                                            plain = item.plainLyrics,
                                            provider = "LRCLIB"
                                        )
                                        onDismiss()
                                    }
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.trackName.orEmpty().ifEmpty { song.title },
                                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.artistName.orEmpty()} • ${item.albumName.orEmpty()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        // Badge
                                        if (hasSynced) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.lucide_ic_sparkles),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "SYNCED",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                            }
                                        } else if (hasPlain) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "PLAIN",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Preview snippet
                                    val snippet = (item.syncedLyrics ?: item.plainLyrics).orEmpty()
                                        .lineSequence()
                                        .filter { it.isNotBlank() && !it.startsWith("[") }
                                        .take(2)
                                        .joinToString("\n")

                                    if (snippet.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = snippet,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
