package `in`.caffeinelabs.cassettecat.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.Playlist
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.util.tapScale

@Composable
internal fun <T> LibraryRefineSheet(
    filter: SongFilter,
    onFilterSelect: (SongFilter) -> Unit,
    sortOptions: List<T>,
    sortLabelOf: (T) -> String,
    selectedSort: T,
    sortDirection: SortDirection,
    onSortSelect: (T) -> Unit,
    sourceFilter: LibrarySourceFilter = LibrarySourceFilter.ALL,
    availableSources: List<LibrarySourceFilter> = emptyList(),
    onSourceFilterSelect: (LibrarySourceFilter) -> Unit = {},
    onDismiss: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(AppR.string.library_refine_and_sort),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                val isCustomized = filter != SongFilter.ALL ||
                    sourceFilter != LibrarySourceFilter.ALL ||
                    selectedSort != sortOptions.firstOrNull() ||
                    sortDirection != SortDirection.ASCENDING
                if (isCustomized) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .tapScale {
                                onFilterSelect(SongFilter.ALL)
                                onSourceFilterSelect(LibrarySourceFilter.ALL)
                                sortOptions.firstOrNull()?.let { onSortSelect(it) }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_rotate_ccw),
                            contentDescription = stringResource(AppR.string.library_reset),
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            stringResource(AppR.string.library_reset),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            if (availableSources.size > 1) {
                Text(
                    stringResource(AppR.string.library_section_source),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )

                SourceFilterRow(
                    sources = availableSources,
                    selected = sourceFilter,
                    onSelect = onSourceFilterSelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                )

                Spacer(Modifier.height(8.dp))
            }

            Text(
                stringResource(AppR.string.library_section_filter_by),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SongFilter.entries.forEach { option ->
                    val isSelected = option == filter
                    val bg = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
                    val border = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    val tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    val textTint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .border(1.dp, border, RoundedCornerShape(14.dp))
                            .tapScale { onFilterSelect(option) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(option.iconRes()),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            option.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = textTint
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Text(
                stringResource(AppR.string.library_section_sort_by),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sortOptions.forEach { option ->
                    val label = sortLabelOf(option)
                    val isSelected = option == selectedSort
                    SortOptionCard(
                        label = label,
                        selected = isSelected,
                        direction = sortDirection,
                        onClick = { onSortSelect(option) }
                    )
                }
            }
        }
    }
}

@Composable
fun <T> SortOptionsSheet(
    options: List<T>,
    labelOf: (T) -> String,
    selected: T,
    direction: SortDirection,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            Text(
                stringResource(AppR.string.library_sort_by),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { option ->
                    val label = labelOf(option)
                    SortOptionCard(
                        label = label,
                        selected = option == selected,
                        direction = direction,
                        onClick = {
                            onSelect(option)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun SortOptionCard(
    label: String,
    selected: Boolean,
    direction: SortDirection,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow
    val border = if (selected) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val textTint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .tapScale(onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(sortIconFor(label)),
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textTint,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            val directionText = directionLabelFor(label, direction)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    directionText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Icon(
                    painter = painterResource(
                        if (direction == SortDirection.ASCENDING) R.drawable.lucide_ic_chevron_up
                        else R.drawable.lucide_ic_chevron_down
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun directionLabelFor(label: String, direction: SortDirection): String = when {
    label.contains("Song Count", ignoreCase = true) -> if (direction == SortDirection.ASCENDING) "1 → 9" else "9 → 1"
    label.contains("Recent", ignoreCase = true) || label.contains("Date", ignoreCase = true) -> if (direction == SortDirection.ASCENDING) "Oldest" else "Newest"
    label.contains("Duration", ignoreCase = true) -> if (direction == SortDirection.ASCENDING) "Shortest" else "Longest"
    else -> if (direction == SortDirection.ASCENDING) "A → Z" else "Z → A"
}

private fun sortIconFor(label: String): Int = when {
    label.contains("Song Count", ignoreCase = true) -> R.drawable.lucide_ic_hash
    label.contains("Recent", ignoreCase = true) || label.contains("Duration", ignoreCase = true) || label.contains("Date", ignoreCase = true) -> R.drawable.lucide_ic_clock
    label.contains("Album", ignoreCase = true) -> R.drawable.lucide_ic_disc_3
    label.contains("Artist", ignoreCase = true) -> R.drawable.lucide_ic_mic_vocal
    label.contains("Folder", ignoreCase = true) -> R.drawable.lucide_ic_folder
    else -> R.drawable.lucide_ic_arrow_up_down
}

private fun SongFilter.iconRes(): Int = when (this) {
    SongFilter.ALL -> R.drawable.lucide_ic_music
    SongFilter.FAVORITES -> R.drawable.lucide_ic_heart
    SongFilter.DOWNLOADED -> R.drawable.lucide_ic_download
    SongFilter.RECENTLY_ADDED -> R.drawable.lucide_ic_clock
}

@Composable
internal fun PlaylistPickerSheet(playlists: List<Playlist>, onSelect: (Playlist) -> Unit, onDismiss: () -> Unit) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            Text(
                stringResource(AppR.string.library_add_to_playlist),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            if (playlists.isEmpty()) {
                Text(
                    stringResource(AppR.string.library_no_playlists),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            } else {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                playlists.forEach { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tapScale { onSelect(playlist) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_list_music),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                pluralStringResource(AppR.plurals.library_songs, playlist.songIds.size, playlist.songIds.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SongOptionsSheet(
    song: Song,
    isFavorite: Boolean = false,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onSearchCoverOnline: (() -> Unit)? = null,
    onRemoveCustomCover: (() -> Unit)? = null,
    onEditTags: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                ) {
                    AlbumArt(
                        song = song,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val likeBg = if (isFavorite) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
                val likeBorder = if (isFavorite) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                val likeTint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(likeBg)
                        .border(1.dp, likeBorder, RoundedCornerShape(12.dp))
                        .tapScale(onToggleFavorite)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_heart),
                        contentDescription = null,
                        tint = likeTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isFavorite) stringResource(AppR.string.library_liked) else stringResource(AppR.string.library_favorite),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .tapScale { onPlayNext(); onDismiss() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(AppR.string.library_play_next),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .tapScale { onShare(); onDismiss() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_share_2),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(AppR.string.library_share),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SongOptionCardRow(
                    iconRes = R.drawable.lucide_ic_list_plus,
                    title = stringResource(AppR.string.library_add_to_queue),
                    subtitle = stringResource(AppR.string.library_add_to_queue_description),
                    onClick = { onAddToQueue(); onDismiss() }
                )
                SongOptionCardRow(
                    iconRes = R.drawable.lucide_ic_list_music,
                    title = stringResource(AppR.string.library_add_to_playlist),
                    subtitle = stringResource(AppR.string.library_add_to_playlist_description),
                    onClick = { onAddToPlaylist(); onDismiss() }
                )
                if (onEditTags != null) {
                    SongOptionCardRow(
                        iconRes = R.drawable.lucide_ic_pencil,
                        title = stringResource(AppR.string.library_edit_details),
                        subtitle = stringResource(AppR.string.library_edit_details_description),
                        onClick = { onDismiss(); onEditTags() }
                    )
                }
                if (onSearchCoverOnline != null) {
                    SongOptionCardRow(
                        iconRes = R.drawable.lucide_ic_image,
                        title = stringResource(AppR.string.library_search_cover),
                        subtitle = stringResource(AppR.string.library_search_cover_description),
                        onClick = { onDismiss(); onSearchCoverOnline() }
                    )
                }
                if (onRemoveCustomCover != null) {
                    SongOptionCardRow(
                        iconRes = R.drawable.lucide_ic_rotate_ccw,
                        title = stringResource(AppR.string.library_reset_cover),
                        subtitle = stringResource(AppR.string.library_reset_cover_description),
                        onClick = { onDismiss(); onRemoveCustomCover() }
                    )
                }
                if (onDelete != null) {
                    SongOptionCardRow(
                        iconRes = R.drawable.lucide_ic_trash_2,
                        title = stringResource(AppR.string.library_delete_from_device),
                        subtitle = stringResource(AppR.string.library_delete_from_device_description),
                        destructive = true,
                        onClick = { onDelete(); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongOptionCardRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .tapScale(onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textColor
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!destructive) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
