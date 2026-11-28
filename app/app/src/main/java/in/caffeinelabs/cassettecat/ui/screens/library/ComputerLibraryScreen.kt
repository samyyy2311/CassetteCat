package `in`.caffeinelabs.cassettecat.ui.screens.library

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.device.ComputerLibraryResult
import `in`.caffeinelabs.cassettecat.data.device.DesktopLibraryTrack
import `in`.caffeinelabs.cassettecat.data.device.DesktopRemoteRepository
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.screens.settings.ActionRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import `in`.caffeinelabs.cassettecat.R as AppR

private const val SEARCH_DELAY_MS = 300L

/** Searches the paired computer's library and plays songs on the computer or on this phone. */
@Composable
fun ComputerLibraryScreen(
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val desktop = remember { DesktopRemoteRepository.getInstance(context) }
    val desktopState by desktop.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<DesktopLibraryTrack>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failure by remember { mutableStateOf<ComputerLibraryResult?>(null) }
    var optionsFor by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    // Restarts on every keystroke, so only the search typed last is sent.
    LaunchedEffect(query) {
        loading = true
        delay(SEARCH_DELAY_MS)
        val result = desktop.computerLibrary(query, offset = 0)
        val page = (result as? ComputerLibraryResult.Loaded)?.page
        failure = result.takeIf { page == null }
        tracks = page?.tracks.orEmpty()
        total = page?.total ?: 0
        loading = false
        listState.scrollToItem(0)
    }

    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= tracks.size - 10
        }
    }
    LaunchedEffect(query) {
        var exhausted = false
        snapshotFlow { nearEnd && !loading && !exhausted && tracks.size < total }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                loading = true
                val more = (desktop.computerLibrary(query, offset = tracks.size) as? ComputerLibraryResult.Loaded)?.page?.tracks
                if (more.isNullOrEmpty()) exhausted = true else tracks = tracks + more
                loading = false
            }
    }

    val playOnComputer = { index: Int ->
        desktop.playOnComputer(tracks, index) { sent ->
            Toast.makeText(
                context,
                if (sent) AppR.string.toast_playing_on_computer else AppR.string.toast_computer_unreachable,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val playHere = { index: Int -> playbackViewModel.playQueue(desktop.computerSongs(tracks), index) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, stringResource(AppR.string.action_back), onBack)
            Text(
                desktopState.name?.let { stringResource(AppR.string.computer_library_title, it) }
                    ?: stringResource(AppR.string.computer_library_title_fallback),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Text(
            stringResource(
                if (desktopState.controlling) AppR.string.computer_library_plays_on_computer
                else AppR.string.computer_library_plays_here
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(AppR.string.computer_library_search)) },
            leadingIcon = { Icon(painter = painterResource(R.drawable.lucide_ic_search), contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_x,
                        contentDescription = stringResource(AppR.string.action_clear),
                        onClick = { query = "" }
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
        )

        val message = when {
            failure == ComputerLibraryResult.NeedsUpdate -> AppR.string.computer_library_needs_update
            failure != null -> AppR.string.toast_computer_unreachable
            loading && tracks.isEmpty() -> AppR.string.library_computer_playlists_loading
            tracks.isEmpty() && query.isBlank() -> AppR.string.computer_library_empty
            tracks.isEmpty() -> AppR.string.computer_library_no_match
            else -> null
        }
        if (message != null) {
            Text(
                stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = listBottomPadding + 32.dp)) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                val song = remember(track, desktopState.address) { desktop.computerSongs(listOf(track)).firstOrNull() }
                if (song != null) {
                    SongRow(
                        song = song,
                        onMoreClick = { optionsFor = index },
                        onClick = { if (desktopState.controlling) playOnComputer(index) else playHere(index) }
                    )
                }
            }
        }
    }

    optionsFor?.let { index ->
        FullOpenBottomSheet(onDismiss = { optionsFor = null }) {
            ActionRow(
                title = stringResource(AppR.string.computer_library_play_here),
                subtitle = stringResource(AppR.string.computer_library_play_here_description),
                iconRes = R.drawable.lucide_ic_smartphone,
                onClick = {
                    optionsFor = null
                    playHere(index)
                }
            )
            ActionRow(
                title = stringResource(AppR.string.computer_library_play_on_computer),
                subtitle = stringResource(AppR.string.computer_library_play_on_computer_description),
                iconRes = R.drawable.lucide_ic_monitor,
                onClick = {
                    optionsFor = null
                    playOnComputer(index)
                }
            )
        }
    }
}
