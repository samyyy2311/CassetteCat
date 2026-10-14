package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.SongMetadataOverridesRepository
import `in`.caffeinelabs.cassettecat.data.library.TagChange
import `in`.caffeinelabs.cassettecat.data.library.TagField
import `in`.caffeinelabs.cassettecat.data.library.TagFixProblem
import `in`.caffeinelabs.cassettecat.data.library.TagFixRepository
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryUiState
import `in`.caffeinelabs.cassettecat.ui.screens.library.LibraryViewModel
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import kotlinx.coroutines.launch

@Composable
fun FixTagsScreen(libraryViewModel: LibraryViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val context = LocalContext.current
    val repository = remember { TagFixRepository.getInstance(context) }
    val overrides = remember { SongMetadataOverridesRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val state by repository.state.collectAsStateWithLifecycle()
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    // Null until the library loads, so a check can't start on no songs and Apply can't drop fixes it can't find.
    val songs = (uiState as? LibraryUiState.Loaded)?.songs
    val accepted = state.accepted.size
    val labels = mapOf(
        TagField.TITLE to stringResource(AppR.string.tag_editor_title),
        TagField.ARTIST to stringResource(AppR.string.tag_editor_artist),
        TagField.ALBUM to stringResource(AppR.string.tag_editor_album),
        TagField.YEAR to stringResource(AppR.string.tag_editor_year)
    )
    val empty = stringResource(AppR.string.fix_tags_empty_value)
    val changeFormat = stringResource(AppR.string.fix_tags_change)
    fun changeText(change: TagChange) = changeFormat.format(labels.getValue(change.field), change.from.ifBlank { empty }, change.to)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressDepthIconButton(R.drawable.lucide_ic_chevron_left, stringResource(AppR.string.action_back), onBack)
            Text(stringResource(AppR.string.fix_tags_title), style = MaterialTheme.typography.headlineSmall)
        }

        LazyColumn(contentPadding = PaddingValues(bottom = listBottomPadding + 32.dp)) {
            item {
                SettingsSection {
                    ActionRow(
                        title = when {
                            state.running -> pluralStringResource(AppR.plurals.fix_tags_checking, state.queue.size, state.checked, state.queue.size)
                            state.paused -> pluralStringResource(AppR.plurals.fix_tags_paused, state.queue.size, state.checked, state.queue.size)
                            state.queue.isEmpty() && state.fixes.isEmpty() -> stringResource(AppR.string.fix_tags_not_started)
                            else -> stringResource(AppR.string.fix_tags_finished)
                        },
                        subtitle = when {
                            state.problem == TagFixProblem.OFF -> stringResource(AppR.string.fix_tags_off)
                            state.problem == TagFixProblem.BUSY -> stringResource(AppR.string.fix_tags_busy)
                            state.running -> stringResource(AppR.string.fix_tags_running_hint)
                            state.fixes.isNotEmpty() -> stringResource(AppR.string.fix_tags_review_hint)
                            state.queue.isEmpty() && state.checkedBefore.isEmpty() -> stringResource(AppR.string.fix_tags_start_hint)
                            state.queue.isEmpty() -> stringResource(AppR.string.fix_tags_all_checked_hint)
                            else -> stringResource(AppR.string.fix_tags_nothing_hint)
                        },
                        iconRes = R.drawable.lucide_ic_search,
                        onClick = {}
                    ) {
                        val action: Pair<Int, () -> Unit>? = when {
                            state.running -> AppR.string.fix_tags_stop to repository::stop
                            state.paused -> AppR.string.fix_tags_continue to repository::resume
                            state.checkedBefore.isEmpty() -> songs?.let { loaded -> AppR.string.fix_tags_start to { repository.checkLibrary(loaded) } }
                            // Starting over drops unapplied fixes, so it's only offered once there are none.
                            state.fixes.isEmpty() -> songs?.let { loaded -> AppR.string.fix_tags_check_again to { repository.checkAllAgain(loaded) } }
                            else -> null
                        }
                        action?.let { (label, onClick) ->
                            TextButton(onClick = hapticClick(onClick)) {
                                Text(stringResource(label), color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    if (state.running || state.paused) {
                        LinearProgressIndicator(
                            progress = { state.checked.toFloat() / state.queue.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }

            if (state.fixes.isNotEmpty()) {
                item {
                    Button(
                        onClick = hapticClick {
                            songs?.let { loaded ->
                                coroutineScope.launch {
                                    repository.apply(loaded, overrides)
                                    libraryViewModel.refresh()
                                }
                            }
                        },
                        enabled = accepted > 0 && songs != null,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(pluralStringResource(AppR.plurals.fix_tags_apply, accepted, accepted))
                    }
                    TextButton(
                        onClick = hapticClick { repository.setAllSkipped(accepted > 0) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            stringResource(if (accepted > 0) AppR.string.fix_tags_turn_all_off else AppR.string.fix_tags_turn_all_on),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                item {
                    SettingsSection {
                        Column {
                            state.fixes.forEachIndexed { index, fix ->
                                if (index > 0) SettingsDivider()
                                ToggleRow(
                                    title = fix.song.title,
                                    subtitle = (listOf(fix.song.artist) + fix.changes.map { changeText(it) }).joinToString("\n"),
                                    checked = fix.song.id !in state.skipped,
                                    onCheckedChange = { apply -> repository.setSkipped(fix.song.id, !apply) },
                                    iconRes = R.drawable.lucide_ic_music
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
