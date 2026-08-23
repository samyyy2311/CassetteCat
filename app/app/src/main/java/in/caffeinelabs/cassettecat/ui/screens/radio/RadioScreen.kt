package `in`.caffeinelabs.cassettecat.ui.screens.radio

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.radio.RadioSortOrder
import `in`.caffeinelabs.cassettecat.data.radio.RadioStation
import `in`.caffeinelabs.cassettecat.data.radio.toSong
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.SortOptionsSheet
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import `in`.caffeinelabs.cassettecat.ui.util.tapScaleSelectable
import kotlinx.coroutines.delay

private const val SEARCH_DEBOUNCE_MS = 400L

@Composable
fun RadioScreen(
    playbackViewModel: PlaybackViewModel,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    listBottomPadding: Dp = 0.dp,
    viewModel: RadioViewModel = viewModel()
) {
    val context = LocalContext.current
    val appPreferencesRepository = remember { AppPreferencesRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteStations.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentStations by viewModel.recentStations.collectAsStateWithLifecycle(initialValue = emptyList())
    val topStations by viewModel.topStations.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val sortDirection by viewModel.sortDirection.collectAsStateWithLifecycle()
    val countries by viewModel.countries.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val states by viewModel.states.collectAsStateWithLifecycle()
    val selectedState by viewModel.selectedState.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showAddCustom by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterCategory by remember { mutableStateOf<RadioFilterCategory?>(null) }
    val anyFilterActive = selectedCountry != null || selectedState != null || selectedLanguage != null || selectedTag != null

    fun play(station: RadioStation) {
        val wasIdle = playbackState.currentSong == null
        playbackViewModel.playQueue(listOf(station.toSong()), 0)
        viewModel.recordPlay(station)
        if (wasIdle) onNavigateToNowPlaying()
    }

    fun share(station: RadioStation) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${station.name}\n${station.streamUrl}")
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Radio", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_sliders_horizontal,
                contentDescription = "Filter stations",
                tint = if (anyFilterActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    filterCategory = null
                    showFilterSheet = true
                }
            )
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_arrow_up_down,
                contentDescription = "Sort by",
                onClick = { showSortSheet = true }
            )
            PressDepthIconButton(
                iconRes = R.drawable.lucide_ic_plus,
                contentDescription = "Add custom station",
                onClick = { showAddCustom = true }
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search stations") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LaunchedEffect(query) {
            if (query.isNotBlank()) delay(SEARCH_DEBOUNCE_MS)
            viewModel.search(query)
        }
        Spacer(Modifier.height(12.dp))

        val showingSearch = query.isNotBlank()
        val stations = if (showingSearch) searchResults else topStations
        if (isOffline) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_radio,
                title = "You're offline",
                message = "Connect to the internet to browse and play radio stations.",
                modifier = Modifier.weight(1f)
            )
        } else if (stations.isEmpty() && favorites.isEmpty() && recentStations.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_radio,
                title = if (showingSearch) "No stations found" else "Loading stations...",
                message = if (showingSearch) "Try a different search term." else "Fetching top stations from Radio Browser.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(preferences.gridColumnCount),
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = listBottomPadding + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (!showingSearch && favorites.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel("FAVORITES")
                    }
                    items(favorites, key = { "fav-${it.uuid}" }) { station ->
                        RadioStationCard(station = station, onClick = { play(station) }, onLongClick = { share(station) })
                    }
                }
                if (!showingSearch && recentStations.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel("RECENTLY PLAYED")
                    }
                    items(recentStations, key = { "recent-${it.uuid}" }) { station ->
                        RadioStationCard(station = station, onClick = { play(station) }, onLongClick = { share(station) })
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionLabel(if (showingSearch) "SEARCH RESULTS" else "TOP STATIONS")
                }
                items(stations, key = { if (showingSearch) "search-${it.uuid}" else "top-${it.uuid}" }) { station ->
                    RadioStationCard(station = station, onClick = { play(station) }, onLongClick = { share(station) })
                }
            }
        }
    }

    if (showAddCustom) {
        AddCustomStationDialog(
            onAdd = { name, url ->
                viewModel.addCustomStation(name, url)
                showAddCustom = false
            },
            onDismiss = { showAddCustom = false }
        )
    }

    if (showSortSheet) {
        SortOptionsSheet(
            options = RadioSortOrder.entries,
            labelOf = { it.label },
            selected = sortOrder,
            direction = sortDirection,
            onSelect = { viewModel.setSortOrder(it) },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showFilterSheet) {
        when (filterCategory) {
            null -> RadioFilterRootSheet(
                country = selectedCountry,
                state = selectedState,
                language = selectedLanguage,
                tag = selectedTag,
                onSelectCategory = { filterCategory = it },
                onDismiss = { showFilterSheet = false }
            )
            RadioFilterCategory.COUNTRY -> NameFilterSheet(
                title = "Filter by country",
                options = countries,
                selected = selectedCountry,
                onSelect = { viewModel.setCountry(it) },
                onDismiss = { filterCategory = null }
            )
            RadioFilterCategory.STATE -> NameFilterSheet(
                title = "Filter by state",
                options = states,
                selected = selectedState,
                onSelect = { viewModel.setState(it) },
                onDismiss = { filterCategory = null }
            )
            RadioFilterCategory.LANGUAGE -> NameFilterSheet(
                title = "Filter by language",
                options = languages,
                selected = selectedLanguage,
                onSelect = { viewModel.setLanguage(it) },
                onDismiss = { filterCategory = null }
            )
            RadioFilterCategory.TAG -> NameFilterSheet(
                title = "Filter by tag",
                options = tags,
                selected = selectedTag,
                onSelect = { viewModel.setTag(it) },
                onDismiss = { filterCategory = null }
            )
        }
    }
}

@Composable
private fun RadioStationCard(station: RadioStation, onClick: () -> Unit, onLongClick: () -> Unit) {
    val song = remember(station.uuid) { station.toSong() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tapScaleSelectable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(station.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (station.bitrate > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        "${station.bitrate}k",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private enum class RadioFilterCategory { COUNTRY, STATE, LANGUAGE, TAG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioFilterRootSheet(
    country: String?,
    state: String?,
    language: String?,
    tag: String?,
    onSelectCategory: (RadioFilterCategory) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Filter stations",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            FilterCategoryRow(label = "Country", value = country ?: "All", onClick = { onSelectCategory(RadioFilterCategory.COUNTRY) })
            FilterCategoryRow(label = "State", value = state ?: "All", onClick = { onSelectCategory(RadioFilterCategory.STATE) })
            FilterCategoryRow(label = "Language", value = language ?: "All", onClick = { onSelectCategory(RadioFilterCategory.LANGUAGE) })
            FilterCategoryRow(label = "Tag", value = tag ?: "All", onClick = { onSelectCategory(RadioFilterCategory.TAG) })
        }
    }
}

@Composable
private fun FilterCategoryRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().tapScale(onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.lucide_ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameFilterSheet(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by rememberSaveable { mutableStateOf("") }
    val filtered = remember(options, filter) {
        if (filter.isBlank()) options else options.filter { it.contains(filter, ignoreCase = true) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    NameFilterRow(label = "All", selected = selected == null, onClick = { onSelect(null); onDismiss() })
                }
                items(filtered, key = { it }) { option ->
                    NameFilterRow(label = option, selected = option == selected, onClick = { onSelect(option); onDismiss() })
                }
            }
        }
    }
}

@Composable
private fun NameFilterRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().tapScale(onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) tint else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_check),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun AddCustomStationDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom station") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Station name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL") },
                    placeholder = { Text("https://stream.example.com/live") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && url.isNotBlank(),
                onClick = hapticClick { onAdd(name.trim(), url.trim()) }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = hapticClick(onDismiss)) { Text("Cancel") }
        }
    )
}
