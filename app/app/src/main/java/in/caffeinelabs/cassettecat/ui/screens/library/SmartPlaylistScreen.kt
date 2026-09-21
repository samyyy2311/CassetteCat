package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.download.SongDownloadRepository
import `in`.caffeinelabs.cassettecat.data.library.FavoritesRepository
import `in`.caffeinelabs.cassettecat.data.library.MusicSource
import `in`.caffeinelabs.cassettecat.data.stats.ListeningStatsRepository
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.TransportButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily

import androidx.compose.ui.graphics.Color

enum class SmartPlaylistType(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val iconRes: Int,
    val color: Color = Color(0xFFE57A3A),
    val gradient: List<Color> = listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68))
) {
    TOP_50(
        id = "top_50",
        titleRes = AppR.string.smart_playlist_top_50_title,
        descriptionRes = AppR.string.smart_playlist_top_50_desc,
        iconRes = R.drawable.lucide_ic_flame,
        color = Color(0xFFFF5E3A),
        gradient = listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68))
    ),
    RECENTLY_ADDED(
        id = "recently_added",
        titleRes = AppR.string.smart_playlist_recently_added_title,
        descriptionRes = AppR.string.smart_playlist_recently_added_desc,
        iconRes = R.drawable.lucide_ic_clock,
        color = Color(0xFF6A11CB),
        gradient = listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
    ),
    FORGOTTEN_GEMS(
        id = "forgotten_gems",
        titleRes = AppR.string.smart_playlist_forgotten_gems_title,
        descriptionRes = AppR.string.smart_playlist_forgotten_gems_desc,
        iconRes = R.drawable.lucide_ic_compass,
        color = Color(0xFF0BA360),
        gradient = listOf(Color(0xFF0BA360), Color(0xFF3CBA92))
    ),
    HEAVY_ROTATION(
        id = "heavy_rotation",
        titleRes = AppR.string.smart_playlist_heavy_rotation_title,
        descriptionRes = AppR.string.smart_playlist_heavy_rotation_desc,
        iconRes = R.drawable.lucide_ic_repeat,
        color = Color(0xFF8E2DE2),
        gradient = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
    ),
    EXTENDED_CUTS(
        id = "extended_cuts",
        titleRes = AppR.string.smart_playlist_extended_cuts_title,
        descriptionRes = AppR.string.smart_playlist_extended_cuts_desc,
        iconRes = R.drawable.lucide_ic_timer,
        color = Color(0xFF0072FF),
        gradient = listOf(Color(0xFF0072FF), Color(0xFF00C6FF))
    ),
    VINTAGE_HITS(
        id = "vintage_hits",
        titleRes = AppR.string.smart_playlist_vintage_hits_title,
        descriptionRes = AppR.string.smart_playlist_vintage_hits_desc,
        iconRes = R.drawable.lucide_ic_disc_3,
        color = Color(0xFFF37335),
        gradient = listOf(Color(0xFFF37335), Color(0xFFFDC830))
    );

    companion object {
        fun fromId(id: String): SmartPlaylistType =
            entries.firstOrNull { it.id == id } ?: TOP_50
    }
}

internal fun isExtendedCut(durationMs: Long): Boolean = durationMs > 5 * 60_000L

@Composable
fun SmartPlaylistScreen(
    playlistType: SmartPlaylistType,
    libraryViewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val statsRepository = remember { ListeningStatsRepository(context) }
    val monthlyStats by statsRepository.monthlyStats.collectAsStateWithLifecycle(initialValue = emptyMap())
    val favoritesRepository = remember { FavoritesRepository(context) }
    val favoriteIds by favoritesRepository.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val allSongs = (uiState as? LibraryUiState.Loaded)?.songs.orEmpty()

    val songs = remember(allSongs, monthlyStats, playlistType, favoriteIds) {
        when (playlistType) {
            SmartPlaylistType.TOP_50 -> {
                val totalPlayCounts = HashMap<String, Int>()
                monthlyStats.values.forEach { month ->
                    month.songPlayCounts.forEach { (songId, count) ->
                        totalPlayCounts[songId] = (totalPlayCounts[songId] ?: 0) + count
                    }
                }
                allSongs.filter { (totalPlayCounts[it.id] ?: 0) > 0 }
                    .sortedByDescending { totalPlayCounts[it.id] ?: 0 }
                    .take(50)
            }
            SmartPlaylistType.RECENTLY_ADDED -> {
                allSongs.sortedByDescending { it.dateAddedMs }.take(50)
            }
            SmartPlaylistType.FORGOTTEN_GEMS -> {
                val totalPlayCounts = HashMap<String, Int>()
                monthlyStats.values.forEach { month ->
                    month.songPlayCounts.forEach { (songId, count) ->
                        totalPlayCounts[songId] = (totalPlayCounts[songId] ?: 0) + count
                    }
                }
                allSongs.filter {
                    (totalPlayCounts[it.id] ?: 0) == 0 || (it.id in favoriteIds && (totalPlayCounts[it.id] ?: 0) < 3)
                }.take(50)
            }
            SmartPlaylistType.HEAVY_ROTATION -> {
                val totalPlayCounts = HashMap<String, Int>()
                monthlyStats.values.forEach { month ->
                    month.songPlayCounts.forEach { (songId, count) ->
                        totalPlayCounts[songId] = (totalPlayCounts[songId] ?: 0) + count
                    }
                }
                allSongs.filter { it.id in favoriteIds || (totalPlayCounts[it.id] ?: 0) >= 3 }
                    .sortedByDescending { (totalPlayCounts[it.id] ?: 0) + (if (it.id in favoriteIds) 10 else 0) }
                    .take(50)
            }
            SmartPlaylistType.EXTENDED_CUTS -> {
                allSongs.filter { isExtendedCut(it.durationMs) }
                    .sortedByDescending { it.durationMs }
                    .take(50)
            }
            SmartPlaylistType.VINTAGE_HITS -> {
                allSongs.filter { song ->
                    val year = song.releaseYear
                    year != null && year in 1950 until 2005
                }.sortedBy { it.releaseYear }.take(50)
            }
        }
    }

    val downloadRepository = remember { SongDownloadRepository.getInstance(context) }
    val downloadableSongs = remember(songs) { songs.filter { it.source != MusicSource.Local } }

    val totalDurationMs = remember(songs) { songs.sumOf { it.durationMs } }
    val durationText = if (totalDurationMs > 0) {
        val totalSeconds = totalDurationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        if (hours > 0) {
            stringResource(AppR.string.smart_playlist_duration_hours_minutes, hours, minutes)
        } else {
            stringResource(AppR.string.smart_playlist_duration_minutes, minutes)
        }
    } else ""

    val songsCountText = pluralStringResource(AppR.plurals.library_songs, songs.size, songs.size)
    val subtitleDetails = listOfNotNull(
        songsCountText,
        durationText.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    fun playAll(shuffle: Boolean) {
        if (songs.isEmpty()) return
        val wasIdle = playbackViewModel.playbackState.value.currentSong == null
        if (shuffle) {
            playbackViewModel.shuffleAll(songs)
        } else {
            playbackViewModel.playQueue(songs, 0, shuffle = false)
        }
        if (wasIdle) onNavigateToNowPlaying()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, stringResource(AppR.string.action_back), onBack)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(playlistType.titleRes), style = MaterialTheme.typography.headlineSmall)
                Text(
                    subtitleDetails,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IbmPlexMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (downloadableSongs.isNotEmpty()) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_download,
                    contentDescription = stringResource(AppR.string.desc_download_smart_playlist),
                    onClick = { downloadableSongs.forEach(downloadRepository::download) }
                )
                Spacer(Modifier.width(4.dp))
            }
            TransportButton(
                iconRes = R.drawable.lucide_ic_play,
                size = 42.dp,
                tint = MaterialTheme.colorScheme.tertiary,
                accented = true,
                onClick = { playAll(shuffle = false) }
            )
            Spacer(Modifier.width(8.dp))
            TransportButton(
                iconRes = R.drawable.lucide_ic_shuffle,
                size = 42.dp,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = { playAll(shuffle = true) }
            )
        }

        if (songs.isEmpty()) {
            EmptyState(
                catRes = AppR.drawable.cat_gray_dancing,
                title = stringResource(AppR.string.smart_playlist_empty_title),
                message = stringResource(playlistType.descriptionRes),
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 12.dp, bottom = listBottomPadding)
            ) {
                items(songs, key = { it.id }) { song ->
                    LibrarySongRow(
                        song = song,
                        onClick = {
                            val wasIdle = playbackViewModel.playbackState.value.currentSong == null
                            val index = songs.indexOfFirst { it.id == song.id }
                            playbackViewModel.playQueue(songs, index)
                            if (wasIdle) onNavigateToNowPlaying()
                        }
                    )
                }
            }
        }
    }
}
