package `in`.caffeinelabs.cassettecat.ui.screens.radio

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.radio.RadioSortOrder
import `in`.caffeinelabs.cassettecat.data.radio.RadioStation
import `in`.caffeinelabs.cassettecat.data.radio.toSong
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettings
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.ui.components.AlbumArt
import `in`.caffeinelabs.cassettecat.ui.components.EmptyState
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.playback.PlaybackViewModel
import `in`.caffeinelabs.cassettecat.ui.screens.library.SortDirection
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
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
    val serviceSettingsRepository = remember { ServiceSettingsRepository(context) }
    val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val serviceSettings by serviceSettingsRepository.settings.collectAsStateWithLifecycle(initialValue = ServiceSettings())
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteStations.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentStations by viewModel.recentStations.collectAsStateWithLifecycle(initialValue = emptyList())
    val topStations by viewModel.topStations.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val fetchFailed by viewModel.fetchFailed.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
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
    var showRefineSheet by remember { mutableStateOf(false) }
    var filterCategory by remember { mutableStateOf<RadioFilterCategory?>(null) }
    val anyFilterActive = selectedCountry != null || selectedState != null || selectedLanguage != null || selectedTag != null
    val isCustomized = anyFilterActive || sortOrder != RadioSortOrder.POPULARITY || sortDirection != SortDirection.DESCENDING

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

    val showingSearch = query.isNotBlank()
    val stations = if (showingSearch) searchResults else topStations

    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Radio", style = MaterialTheme.typography.headlineSmall)
                val subtitleText = when {
                    serviceSettings.offlineBlackoutMode -> "Offline Blackout Mode"
                    isOffline -> "Offline"
                    anyFilterActive -> "Filtered stations"
                    showingSearch -> "${stations.size} station(s) found"
                    else -> "Explore worldwide stations & broadcasts"
                }
                Text(
                    subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOffline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_sliders_horizontal,
                    contentDescription = "Refine & sort stations",
                    tint = if (isCustomized) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        filterCategory = null
                        showRefineSheet = true
                    }
                )
                PressDepthIconButton(
                    iconRes = R.drawable.lucide_ic_plus,
                    contentDescription = "Add custom station",
                    onClick = { showAddCustom = true }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text(
                    "Search stations…",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    PressDepthIconButton(
                        iconRes = R.drawable.lucide_ic_x,
                        contentDescription = "Clear",
                        onClick = { query = "" }
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        LaunchedEffect(query) {
            if (query.isNotBlank()) delay(SEARCH_DEBOUNCE_MS)
            viewModel.search(query)
        }

        if (anyFilterActive) {
            Spacer(Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedCountry != null) {
                    item(key = "country") {
                        ActiveFilterChip(
                            label = "Country: $selectedCountry",
                            onRemove = { viewModel.setCountry(null) },
                            onClick = {
                                filterCategory = RadioFilterCategory.COUNTRY
                                showRefineSheet = true
                            }
                        )
                    }
                }
                if (selectedState != null) {
                    item(key = "state") {
                        ActiveFilterChip(
                            label = "State: $selectedState",
                            onRemove = { viewModel.setState(null) },
                            onClick = {
                                filterCategory = RadioFilterCategory.STATE
                                showRefineSheet = true
                            }
                        )
                    }
                }
                if (selectedLanguage != null) {
                    item(key = "language") {
                        ActiveFilterChip(
                            label = "Language: $selectedLanguage",
                            onRemove = { viewModel.setLanguage(null) },
                            onClick = {
                                filterCategory = RadioFilterCategory.LANGUAGE
                                showRefineSheet = true
                            }
                        )
                    }
                }
                if (selectedTag != null) {
                    item(key = "tag") {
                        ActiveFilterChip(
                            label = "Tag: $selectedTag",
                            onRemove = { viewModel.setTag(null) },
                            onClick = {
                                filterCategory = RadioFilterCategory.TAG
                                showRefineSheet = true
                            }
                        )
                    }
                }
                item(key = "clear_all") {
                    Text(
                        "Clear all",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.resetFiltersAndSort() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isOffline) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_radio,
                title = if (serviceSettings.offlineBlackoutMode) "Offline Blackout Mode" else "You're offline",
                message = if (serviceSettings.offlineBlackoutMode) "All network streaming and external radio lookups are disabled." else "Connect to the internet to browse and play radio stations.",
                modifier = Modifier.weight(1f)
            )
        } else if (stations.isEmpty() && favorites.isEmpty() && recentStations.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.lucide_ic_radio,
                title = when {
                    fetchFailed -> "Couldn't reach Radio Browser"
                    showingSearch -> if (isSearching) "Searching..." else "No stations found"
                    else -> "Loading stations..."
                },
                message = when {
                    fetchFailed -> "Check your connection and tap to retry."
                    showingSearch -> "Try a different search term."
                    else -> "Fetching top stations from Radio Browser."
                },
                modifier = Modifier
                    .weight(1f)
                    .then(if (fetchFailed) Modifier.clickable { viewModel.retry() } else Modifier)
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

    if (showRefineSheet) {
        when (filterCategory) {
            null -> RadioRefineAndSortSheet(
                country = selectedCountry,
                state = selectedState,
                language = selectedLanguage,
                tag = selectedTag,
                sortOrder = sortOrder,
                sortDirection = sortDirection,
                onSelectCategory = { filterCategory = it },
                onSelectSort = { viewModel.setSortOrder(it) },
                onResetAll = { viewModel.resetFiltersAndSort() },
                onDismiss = { showRefineSheet = false }
            )
            RadioFilterCategory.COUNTRY -> NameFilterSheet(
                title = "Filter by Country",
                options = countries,
                selected = selectedCountry,
                onSelect = { viewModel.setCountry(it) },
                onDismiss = { filterCategory = null }
            )
            RadioFilterCategory.STATE -> NameFilterSheet(
                title = "Filter by State / Province",
                options = states,
                selected = selectedState,
                onSelect = { viewModel.setState(it) },
                onDismiss = { filterCategory = null }
            )
            RadioFilterCategory.LANGUAGE -> NameFilterSheet(
                title = "Filter by Language",
                options = languages,
                selected = selectedLanguage,
                onSelect = { viewModel.setLanguage(it) },
                onDismiss = { filterCategory = null }
            )
            RadioFilterCategory.TAG -> NameFilterSheet(
                title = "Filter by Genre / Tag",
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
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(
            station.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = if (song.artist.isNotBlank()) song.artist else station.country.ifEmpty { "Radio" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (station.bitrate > 0) {
                Spacer(Modifier.width(6.dp))
                val isHd = station.bitrate >= 192
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isHd) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "${station.bitrate}k",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHd) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private enum class RadioFilterCategory { COUNTRY, STATE, LANGUAGE, TAG }

@Composable
private fun ActiveFilterChip(
    label: String,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
            .tapScale(onClick)
            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_x),
                contentDescription = "Remove filter",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}



@Composable
private fun RadioRefineAndSortSheet(
    country: String?,
    state: String?,
    language: String?,
    tag: String?,
    sortOrder: RadioSortOrder,
    sortDirection: SortDirection,
    onSelectCategory: (RadioFilterCategory) -> Unit,
    onSelectSort: (RadioSortOrder) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val isCustomized = country != null || state != null || language != null || tag != null || sortOrder != RadioSortOrder.POPULARITY || sortDirection != SortDirection.DESCENDING

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Refine & Sort",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (isCustomized) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .tapScale(onResetAll)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_rotate_ccw),
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            "Reset",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "FILTER BY",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioFilterCategoryCard(
                    iconRes = R.drawable.lucide_ic_globe,
                    label = "Country",
                    selectedValue = country,
                    onClick = { onSelectCategory(RadioFilterCategory.COUNTRY) }
                )
                RadioFilterCategoryCard(
                    iconRes = R.drawable.lucide_ic_map_pin,
                    label = "State / Province",
                    selectedValue = state,
                    onClick = { onSelectCategory(RadioFilterCategory.STATE) }
                )
                RadioFilterCategoryCard(
                    iconRes = R.drawable.lucide_ic_languages,
                    label = "Language",
                    selectedValue = language,
                    onClick = { onSelectCategory(RadioFilterCategory.LANGUAGE) }
                )
                RadioFilterCategoryCard(
                    iconRes = R.drawable.lucide_ic_tag,
                    label = "Genre / Tag",
                    selectedValue = tag,
                    onClick = { onSelectCategory(RadioFilterCategory.TAG) }
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "SORT STATIONS BY",
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
                RadioSortOrder.entries.forEach { option ->
                    val isSelected = option == sortOrder
                    val bg = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
                    val border = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    val tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(bg)
                            .border(1.dp, border, RoundedCornerShape(100.dp))
                            .tapScale { onSelectSort(option) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSelected) {
                            Icon(
                                painter = painterResource(
                                    if (sortDirection == SortDirection.ASCENDING) R.drawable.lucide_ic_arrow_up else R.drawable.lucide_ic_arrow_down
                                ),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioFilterCategoryCard(
    iconRes: Int,
    label: String,
    selectedValue: String?,
    onClick: () -> Unit
) {
    val isActive = selectedValue != null
    val bg = if (isActive) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
    val border = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .tapScale(onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = selectedValue ?: "All",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = if (isActive) IbmPlexMonoFontFamily else null
                ),
                color = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                painter = painterResource(R.drawable.lucide_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

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

    FullOpenBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (selected != null) {
                    Text(
                        "Clear",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelect(null)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text("Search…") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        PressDepthIconButton(
                            iconRes = R.drawable.lucide_ic_x,
                            contentDescription = "Clear search",
                            onClick = { filter = "" }
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(top = 6.dp)
            ) {
                item {
                    NameFilterRow(
                        label = "All",
                        selected = selected == null,
                        onClick = {
                            onSelect(null)
                            onDismiss()
                        }
                    )
                }
                items(filtered, key = { it }) { option ->
                    NameFilterRow(
                        label = option,
                        selected = option == selected,
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
private fun NameFilterRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
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
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun AddCustomStationDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    val trimmedUrl = url.trim()
    val isUrlValid = trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true)

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
                    isError = url.isNotBlank() && !isUrlValid,
                    supportingText = {
                        if (url.isNotBlank() && !isUrlValid) Text("Must start with http:// or https://")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && isUrlValid,
                onClick = hapticClick { onAdd(name.trim(), trimmedUrl) }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = hapticClick(onDismiss)) { Text("Cancel") }
        }
    )
}

