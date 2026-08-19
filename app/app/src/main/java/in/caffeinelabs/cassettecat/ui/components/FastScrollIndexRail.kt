package `in`.caffeinelabs.cassettecat.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import `in`.caffeinelabs.cassettecat.ui.theme.RecordRed
import `in`.caffeinelabs.cassettecat.ui.theme.Silver
import `in`.caffeinelabs.cassettecat.ui.theme.SilverDim
import `in`.caffeinelabs.cassettecat.ui.theme.TextPrimary
import `in`.caffeinelabs.cassettecat.ui.theme.TextSecondary

private val LEADING_ARTICLE_REGEX = Regex("""^(?:the|a|an)\s+""", RegexOption.IGNORE_CASE)
private val ALL_LETTERS = listOf('#') + ('A'..'Z').toList()

data class AlphabetSection(
    val letter: Char,
    val startIndex: Int,
    val count: Int
)

fun normalizeIndexChar(raw: String): Char {
    val clean = raw.replace(LEADING_ARTICLE_REGEX, "")
        .trimStart('\'', '"', '[', '(', '{', '#', ' ', '\t', '.', '-', '_')
    val first = clean.firstOrNull()?.uppercaseChar() ?: '#'
    return if (first in 'A'..'Z') first else '#'
}

fun <T> buildSectionIndexMap(items: List<T>, labelExtractor: (T) -> String): Map<Char, AlphabetSection> {
    val counts = mutableMapOf<Char, Int>()
    val startIndices = mutableMapOf<Char, Int>()

    items.forEachIndexed { index, item ->
        val letter = normalizeIndexChar(labelExtractor(item))
        counts[letter] = (counts[letter] ?: 0) + 1
        if (letter !in startIndices) {
            startIndices[letter] = index
        }
    }

    return ALL_LETTERS.associateWith { char ->
        AlphabetSection(
            letter = char,
            startIndex = startIndices[char] ?: -1,
            count = counts[char] ?: 0
        )
    }
}

@Composable
fun <T> FastScrollIndexRail(
    items: List<T>,
    labelExtractor: (T) -> String,
    onScrollToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    itemNoun: String = "item"
) {
    if (items.size < 15) return

    val sectionMap = remember(items) { buildSectionIndexMap(items, labelExtractor) }
    val latestScroll = rememberUpdatedState(onScrollToIndex)
    val view = LocalView.current

    var isDragging by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var railHeightPx by remember { mutableIntStateOf(0) }

    fun selectLetterAtY(y: Float) {
        if (railHeightPx <= 0) return
        val fraction = (y / railHeightPx).coerceIn(0f, 0.999f)
        val letterIndex = (fraction * ALL_LETTERS.size).toInt().coerceIn(0, ALL_LETTERS.lastIndex)
        val letter = ALL_LETTERS.getOrNull(letterIndex) ?: return

        if (letter != activeLetter) {
            activeLetter = letter
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

            // Find matching section or nearest next available letter
            val targetSection = sectionMap[letter]?.takeIf { it.startIndex in items.indices }
                ?: ALL_LETTERS.drop(letterIndex).mapNotNull { sectionMap[it]?.takeIf { s -> s.startIndex in items.indices } }.firstOrNull()
                ?: sectionMap.values.lastOrNull { it.startIndex in items.indices }

            targetSection?.startIndex?.let { index ->
                if (index in items.indices) latestScroll.value(index)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(top = 8.dp, bottom = bottomPadding + 8.dp, end = 2.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Vertical rail
        Column(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .onGloballyPositioned { coordinates ->
                    railHeightPx = coordinates.size.height
                }
                .pointerInput(sectionMap) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            selectLetterAtY(offset.y)
                        },
                        onDragEnd = {
                            isDragging = false
                            activeLetter = null
                        },
                        onDragCancel = {
                            isDragging = false
                            activeLetter = null
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            selectLetterAtY(change.position.y)
                        }
                    )
                }
                .pointerInput(sectionMap) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            selectLetterAtY(offset.y)
                            tryAwaitRelease()
                            isDragging = false
                            activeLetter = null
                        }
                    )
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ALL_LETTERS.forEach { char ->
                val hasItems = (sectionMap[char]?.count ?: 0) > 0
                val isCurrent = char == activeLetter

                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = IbmPlexMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = when {
                        isCurrent -> RecordRed
                        hasItems -> Silver.copy(alpha = 0.85f)
                        else -> SilverDim.copy(alpha = 0.2f)
                    },
                    textAlign = TextAlign.Center
                )
            }
        }

        // Floating Tape Index HUD Readout
        AnimatedVisibility(
            visible = isDragging && activeLetter != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 40.dp)
        ) {
            activeLetter?.let { letter ->
                val count = sectionMap[letter]?.count ?: 0
                Surface(
                    color = Color(0xFF181614),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF33302C)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "[ $letter ]",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = IbmPlexMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ),
                            color = if (count > 0) RecordRed else TextSecondary
                        )
                        Text(
                            text = if (count > 0) "$count ${if (count == 1) itemNoun else "${itemNoun}s"}" else "none",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = IbmPlexMonoFontFamily,
                                fontSize = 10.sp
                            ),
                            color = if (count > 0) TextPrimary else SilverDim
                        )
                    }
                }
            }
        }
    }
}
