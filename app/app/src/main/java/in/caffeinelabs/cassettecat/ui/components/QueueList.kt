package `in`.caffeinelabs.cassettecat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.abs
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.data.library.Song
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import kotlinx.coroutines.delay

// True ease-in-out.
private val SmoothEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
// Material You "Emphasized Accelerate" — gentle start, fast exit.
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

// QueueRow's actual rendered height (48dp art + 10dp padding); fixed since every
// row is deterministic (same art size, single-line-truncated text).
private val QUEUE_ROW_HEIGHT = 68.dp

// Inline, not a modal sheet (cf. Apple Music's queue view); embedded directly in
// NowPlayingContent's weight(1f) container, sized to whatever space is left there.
@Composable
fun QueueList(
    upNext: List<Song>,
    history: List<Song>,
    onSongClick: (Song) -> Unit,
    onReorderUpNext: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemoveUpNext: (Song) -> Unit,
    onClearHistory: () -> Unit,
    onScrollDelta: (Float) -> Unit = {},
    bottomPaddingDp: Dp = 240.dp,
    controlsVisible: Boolean = true,
    onInteraction: () -> Unit = {},
    modifier: Modifier = Modifier,
    // Hoisted so the caller can observe isScrollInProgress too (auto-hides the
    // player's scrubber/transport chrome while this list is scrolling).
    state: LazyListState = rememberLazyListState()
) {
    var order by remember(upNext) { mutableStateOf(upNext) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isReordering by remember { mutableStateOf(false) }
    val rowHeightPx = with(LocalDensity.current) { QUEUE_ROW_HEIGHT.toPx() }
    // Pre-compute outside `remember` — LocalDensity can't be read inside a non-composable lambda.
    val scrollThresholdPx = with(LocalDensity.current) { 1.5.dp.toPx() }
    val haptics = LocalHapticFeedback.current
    val currentOnScrollDelta by rememberUpdatedState(onScrollDelta)
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Velocity gate: ignore micro-jitter below 1.5 dp/frame so the auto-hide
                // logic in the caller doesn't oscillate at near-zero scroll speeds.
                if (source == NestedScrollSource.UserInput && kotlin.math.abs(available.y) > scrollThresholdPx) {
                    currentOnScrollDelta(available.y)
                }
                return Offset.Zero
            }
        }
    }

    val fadeStart by animateFloatAsState(
        targetValue = if (controlsVisible) 0.48f else 0.85f,
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = 400f
        ),
        label = "queueFadeStart"
    )
    val fadeEnd by animateFloatAsState(
        targetValue = if (controlsVisible) 0.65f else 0.98f,
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = 400f
        ),
        label = "queueFadeEnd"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(scrollConnection)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onInteraction
            )
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.04f to Color.Black.copy(alpha = 0.35f),
                        0.09f to Color.Black,
                        fadeStart to Color.Black,
                        ((fadeStart + fadeEnd) / 2f) to Color.Black.copy(alpha = 0.35f),
                        fadeEnd to Color.Transparent,
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        state = state,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 8.dp,
            bottom = bottomPaddingDp
        ),
        userScrollEnabled = !isReordering
    ) {
        // History above Up Next; reversed so the most recently played
        // song sits next to Up Next, reading as one continuous timeline top to bottom.
        if (history.isNotEmpty()) {
            item(key = "history-header") {
                SectionHeader("History") {
                    Text(
                        "Clear",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                        modifier = Modifier
                            .clickable(onClick = hapticClick(onClearHistory))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            itemsIndexed(history.asReversed(), key = { index, song -> "history:${song.id}_$index" }) { _, song ->
                QueueRow(
                    song = song,
                    onClick = hapticClick { onSongClick(song) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 20.dp)
                )
            }
        }
        if (order.isNotEmpty()) {
            item(key = "up-next-header") { SectionHeader("Playing Next") }
            itemsIndexed(order, key = { index, song -> "next:${song.id}_$index" }) { index, song ->
                val isDragging = index == draggingIndex
                // Per-item dismissed flag: set to true on swipe confirm to trigger the
                // AnimatedVisibility collapse animation before the data model removes the item.
                var dismissed by remember { mutableStateOf(false) }
                val currentSong by rememberUpdatedState(song)
                LaunchedEffect(dismissed) {
                    if (dismissed) {
                        // Wait for the shrinkVertically animation to finish before notifying
                        // the caller — prevents the row from snapping out mid-collapse.
                        delay(280)
                        onRemoveUpNext(currentSong)
                    }
                }
                val dismissState = rememberSwipeToDismissBoxState(
                    positionalThreshold = { distance -> distance * 0.40f },
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart && !dismissed) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            dismissed = true
                        }
                        true
                    }
                )
                AnimatedVisibility(
                    visible = !dismissed,
                    // Collapses the item's height while fading it out, so the rows below
                    // slide up into position rather than snapping.
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(280, easing = EmphasizedAccelerate)
                    ) + fadeOut(tween(180, easing = SmoothEasing))
                ) {
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundContent = {
                        val offsetPx: Float = runCatching { dismissState.requireOffset() }.getOrNull() ?: 0f
                        val isSwiping = offsetPx < -1f || dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                        if (isSwiping) {
                            val revealWidthDp = with(LocalDensity.current) { abs(offsetPx).toDp() }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(revealWidthDp)
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val iconAlpha = (abs(offsetPx) / 100f).coerceIn(0f, 1f)
                                    val iconScale = (abs(offsetPx) / 120f).coerceIn(0.6f, 1.15f)
                                    Icon(
                                        painter = painterResource(R.drawable.lucide_ic_trash_2),
                                        contentDescription = "Remove from queue",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer {
                                                alpha = iconAlpha
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            }
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragging) dragOffsetPx else 0f },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QueueRow(
                            song = song,
                            onClick = hapticClick { onSongClick(song) },
                            modifier = Modifier.weight(1f)
                        )
                        // Drag handle: dimmed at rest so it doesn’t clutter the list;
                        // animates to full opacity when a reorder is in progress.
                        val handleAlpha by animateFloatAsState(
                            targetValue = if (isReordering) 1f else 0.35f,
                            animationSpec = tween(220, easing = SmoothEasing),
                            label = "dragHandleAlpha"
                        )
                        Icon(
                            painter = painterResource(R.drawable.lucide_ic_grip_vertical),
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .graphicsLayer { alpha = handleAlpha }
                                .padding(end = 20.dp)
                                .size(20.dp)
                                .pointerInput(song.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingIndex = index
                                            dragStartIndex = index
                                            dragOffsetPx = 0f
                                            isReordering = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragEnd = {
                                            if (dragStartIndex != -1 && draggingIndex != dragStartIndex) {
                                                onReorderUpNext(dragStartIndex, draggingIndex)
                                            }
                                            draggingIndex = -1
                                            dragStartIndex = -1
                                            dragOffsetPx = 0f
                                            isReordering = false
                                        },
                                        onDragCancel = {
                                            order = upNext
                                            draggingIndex = -1
                                            dragStartIndex = -1
                                            dragOffsetPx = 0f
                                            isReordering = false
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetPx += dragAmount.y
                                            val steps = (dragOffsetPx / rowHeightPx).toInt()
                                            if (steps != 0 && draggingIndex != -1) {
                                                val newIndex = (draggingIndex + steps).coerceIn(0, order.lastIndex)
                                                if (newIndex != draggingIndex) {
                                                    order = order.toMutableList().apply {
                                                        add(newIndex, removeAt(draggingIndex))
                                                    }
                                                    draggingIndex = newIndex
                                                    // A tick per swap, not just on pickup, mimics a physical detent.
                                                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                                }
                                                dragOffsetPx -= steps * rowHeightPx
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }
                }
            }
        }
        if (order.isEmpty() && history.isEmpty()) {
            item(key = "empty-queue") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = (-0.2).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        action()
    }
}

// Geometry for QueueRow (46dp art, 14dp gap), with refined typography
@Composable
private fun QueueRow(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tapScale(onClick)
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AlbumArt(song = song, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
