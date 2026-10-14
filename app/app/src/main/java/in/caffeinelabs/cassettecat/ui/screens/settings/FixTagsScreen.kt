package `in`.caffeinelabs.cassettecat.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import kotlinx.coroutines.launch

@Composable
fun FixTagsScreen(libraryViewModel: LibraryViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, listBottomPadding: Dp = 0.dp) {
    val context = LocalContext.current
    val repository = remember { TagFixRepository.getInstance(context) }
    val overrides = remember { SongMetadataOverridesRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val state by repository.state.collectAsStateWithLifecycle()
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val songs = (uiState as? LibraryUiState.Loaded)?.songs.orEmpty()
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
                            state.running -> stringResource(AppR.string.fix_tags_checking, state.checked, state.queue.size)
                            state.paused -> stringResource(AppR.string.fix_tags_paused, state.checked, state.queue.size)
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
                        when {
                            state.running -> OutlinedButton(onClick = repository::stop) {
                                Text(stringResource(AppR.string.fix_tags_stop))
                            }
                            state.paused -> OutlinedButton(onClick = repository::resume) {
                                Text(stringResource(AppR.string.fix_tags_continue))
                            }
                            state.checkedBefore.isEmpty() -> OutlinedButton(onClick = { repository.checkLibrary(songs) }) {
                                Text(stringResource(AppR.string.fix_tags_start))
                            }
                            // Starting over drops unapplied fixes, so it's only offered once there are none.
                            state.fixes.isEmpty() -> OutlinedButton(onClick = { repository.checkAllAgain(songs) }) {
                                Text(stringResource(AppR.string.fix_tags_check_again))
                            }
                        }
                    }
                }
            }

            if (state.fixes.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { repository.setAllSkipped(accepted > 0) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(if (accepted > 0) AppR.string.fix_tags_turn_all_off else AppR.string.fix_tags_turn_all_on))
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.apply(songs, overrides)
                                    libraryViewModel.refresh()
                                }
                            },
                            enabled = !state.running && accepted > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(pluralStringResource(AppR.plurals.fix_tags_apply, accepted, accepted))
                        }
                    }
                }

                item {
                    SettingsSection {
                        Column {
                            state.fixes.forEachIndexed { index, fix ->
                                if (index > 0) SettingsDivider()
                                ToggleRow(
                                    title = "${fix.song.title} · ${fix.song.artist}",
                                    subtitle = fix.changes.joinToString("\n") { changeText(it) },
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
