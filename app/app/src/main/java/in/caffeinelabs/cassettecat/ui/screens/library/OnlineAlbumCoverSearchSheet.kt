package `in`.caffeinelabs.cassettecat.ui.screens.library

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.data.library.AlbumCoverRepository
import `in`.caffeinelabs.cassettecat.data.library.AlbumCoverStorage
import `in`.caffeinelabs.cassettecat.data.library.OnlineAlbumCoverClient
import `in`.caffeinelabs.cassettecat.data.library.OnlineCoverResult
import `in`.caffeinelabs.cassettecat.data.streaming.decodeSampledBitmap
import `in`.caffeinelabs.cassettecat.data.streaming.sharedHttpClient
import `in`.caffeinelabs.cassettecat.ui.components.PressDepthIconButton
import `in`.caffeinelabs.cassettecat.ui.components.invalidateAlbumArtCache
import `in`.caffeinelabs.cassettecat.ui.screens.nowplaying.FullOpenBottomSheet
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OnlineAlbumCoverSearchSheet(
    initialAlbum: String,
    initialArtist: String,
    albumId: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val client = remember { OnlineAlbumCoverClient() }
    val storage = remember { AlbumCoverStorage(context) }
    val repository = remember { AlbumCoverRepository.getInstance(context) }
    val albumCovers by repository.albumCovers.collectAsStateWithLifecycle()
    val hasCustomCover = remember(initialAlbum, initialArtist, albumId, albumCovers) {
        repository.getCoverPath(initialAlbum, initialArtist, albumId) != null
    }

    val cleanInitialAlbum = initialAlbum.trim().takeUnless {
        it.equals("<unknown>", ignoreCase = true) || it.equals("unknown", ignoreCase = true)
    }.orEmpty()
    val cleanInitialArtist = initialArtist.trim().takeUnless {
        it.equals("<unknown>", ignoreCase = true) || it.equals("unknown", ignoreCase = true)
    }.orEmpty()

    var albumQuery by remember(initialAlbum) { mutableStateOf(cleanInitialAlbum) }
    var artistQuery by remember(initialArtist) { mutableStateOf(cleanInitialArtist) }
    var results by remember { mutableStateOf<List<OnlineCoverResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var applyingCoverId by remember { mutableStateOf<String?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val targetAlbum = initialAlbum.ifBlank { albumQuery }
                val targetArtist = initialArtist.ifBlank { artistQuery }
                val key = AlbumCoverRepository.albumKey(targetAlbum, targetArtist)
                val path = storage.save(key, uri)
                if (path != null) {
                    val previousPath = repository.getCoverPath(targetAlbum, targetArtist, albumId)
                    repository.setCover(targetAlbum, targetArtist, albumId, path)
                    if (previousPath != null && previousPath != path) {
                        storage.delete(previousPath)
                    }
                    invalidateAlbumArtCache(context)
                    Toast.makeText(context, AppR.string.cover_applied_toast, Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    Toast.makeText(context, AppR.string.cover_failed_load_toast, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun executeSearch() {
        if (isSearching) return
        keyboardController?.hide()
        isSearching = true
        hasSearched = true
        coroutineScope.launch {
            results = client.searchCovers(albumQuery, artistQuery)
            isSearching = false
        }
    }

    LaunchedEffect(initialAlbum, initialArtist) {
        executeSearch()
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(AppR.string.cover_choose_album_cover),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${albumQuery.ifBlank { stringResource(AppR.string.cover_unknown_album) }} • ${artistQuery.ifBlank { stringResource(AppR.string.cover_unknown_artist) }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PressDepthIconButton(R.drawable.lucide_ic_x, stringResource(AppR.string.close), onDismiss)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = albumQuery,
                    onValueChange = { albumQuery = it },
                    placeholder = { Text(stringResource(AppR.string.search_album_placeholder), maxLines = 1) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { executeSearch() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                OutlinedTextField(
                    value = artistQuery,
                    onValueChange = { artistQuery = it },
                    placeholder = { Text(stringResource(AppR.string.search_artist_placeholder), maxLines = 1) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.85f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { executeSearch() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                        .clickable(onClick = hapticClick { executeSearch() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_search),
                        contentDescription = stringResource(AppR.string.lrclib_search),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isSearching) {
                    Text(
                        stringResource(AppR.string.cover_searching_catalogs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (hasSearched) {
                    Text(
                        stringResource(AppR.string.cover_artworks_found, results.size),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = IbmPlexMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        stringResource(AppR.string.cover_select_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable(onClick = hapticClick {
                            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lucide_ic_image),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        stringResource(AppR.string.cover_choose_from_device),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (hasCustomCover) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = hapticClick {
                                coroutineScope.launch {
                                    val previousPath = repository.getCoverPath(initialAlbum, initialArtist, albumId)
                                    repository.clearCover(initialAlbum, initialArtist, albumId)
                                    if (previousPath != null) storage.delete(previousPath)
                                    invalidateAlbumArtCache(context)
                                    Toast.makeText(context, AppR.string.cover_removed_toast, Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            })
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_rotate_ccw),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            stringResource(AppR.string.cover_revert_to_original),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (isSearching && results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else if (hasSearched && results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_image),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(AppR.string.cover_no_covers_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(AppR.string.cover_no_covers_suggestion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(results, key = { it.id }) { item ->
                        val isApplying = applyingCoverId == item.id
                        OnlineCoverCard(
                            item = item,
                            isApplying = isApplying,
                            onClick = {
                                if (applyingCoverId != null) return@OnlineCoverCard
                                applyingCoverId = item.id
                                coroutineScope.launch {
                                    val bitmap = client.downloadCover(item.downloadUrl, fallbackUrl = item.previewUrl)
                                    if (bitmap != null) {
                                        val targetAlbum = initialAlbum.ifBlank { albumQuery }
                                        val targetArtist = initialArtist.ifBlank { artistQuery }
                                        val key = AlbumCoverRepository.albumKey(targetAlbum, targetArtist)
                                        val path = storage.save(key, bitmap)
                                        if (path != null) {
                                            val previousPath = repository.getCoverPath(targetAlbum, targetArtist, albumId)
                                            repository.setCover(targetAlbum, targetArtist, albumId, path)
                                            if (previousPath != null && previousPath != path) {
                                                storage.delete(previousPath)
                                            }
                                            invalidateAlbumArtCache(context)
                                            Toast.makeText(context, AppR.string.cover_updated_toast, Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                            return@launch
                                        }
                                    }
                                    Toast.makeText(context, AppR.string.cover_failed_download_toast, Toast.LENGTH_SHORT).show()
                                    applyingCoverId = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineCoverCard(
    item: OnlineCoverResult,
    isApplying: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    if (isApplying) 2.dp else 0.5.dp,
                    if (isApplying) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            RemoteCoverPreview(
                url = item.previewUrl,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.source.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = IbmPlexMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = item.resolutionLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = IbmPlexMonoFontFamily,
                        fontSize = 9.sp
                    ),
                    color = Color.White
                )
            }

            if (isApplying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.album,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (item.year != null) "${item.artist} (${item.year})" else item.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RemoteCoverPreview(url: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "CassetteCat/1.0")
                    .build()
                sharedHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        decodeSampledBitmap(response.body.bytes(), maxDimension = 300)
                    } else null
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.lucide_ic_image),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
