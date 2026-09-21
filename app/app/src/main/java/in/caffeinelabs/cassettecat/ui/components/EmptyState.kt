package `in`.caffeinelabs.cassettecat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.cassettecat.R as AppR
import `in`.caffeinelabs.cassettecat.ui.theme.IbmPlexMonoFontFamily
import kotlinx.coroutines.launch

val ApprovedEasterEggCats = listOf(
    AppR.drawable.cat_black_cassette,
    AppR.drawable.cat_orange_headphones,
    AppR.drawable.cat_gray_dancing,
    AppR.drawable.cat_calico_player
)

@Composable
fun EmptyState(
    iconRes: Int? = null,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    catRes: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionIconRes: Int? = null,
    actionLoading: Boolean = false,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    enableEasterEgg: Boolean = true
) {
    val effectiveCatRes = catRes ?: (if (iconRes == null) AppR.drawable.cat_black_cassette else null)

    var currentCatRes by remember(effectiveCatRes) {
        mutableStateOf(effectiveCatRes)
    }
    var tapCount by remember { mutableIntStateOf(0) }
    var easterEggQuote by remember { mutableStateOf<String?>(null) }

    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentCatRes != null) {
            AnimatedVisibility(
                visible = easterEggQuote != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = easterEggQuote.orEmpty(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = IbmPlexMonoFontFamily, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Image(
                painter = painterResource(currentCatRes!!),
                contentDescription = stringResource(AppR.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (enableEasterEgg) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            tapCount++
                            val currentIndex = ApprovedEasterEggCats.indexOf(currentCatRes)
                            val nextIndex = (if (currentIndex >= 0) currentIndex + 1 else 0) % ApprovedEasterEggCats.size
                            currentCatRes = ApprovedEasterEggCats[nextIndex]

                            scope.launch {
                                launch {
                                    scale.animateTo(0.86f, spring(stiffness = Spring.StiffnessHigh))
                                    scale.animateTo(1.08f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                    scale.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
                                }
                                launch {
                                    val tilt = if (tapCount % 2 == 0) 7f else -7f
                                    rotation.animateTo(tilt, spring(stiffness = Spring.StiffnessHigh))
                                    rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            }

                            easterEggQuote = when (tapCount) {
                                1 -> "(=^･ω･^=) Meow!"
                                2 -> "🐾 Purrrrr..."
                                3 -> "CassetteCat loves music 🎵"
                                4 -> "🎧 Turn up the tape deck!"
                                5 -> "✨ Secret Cat Lounge unlocked!"
                                else -> listOf("(=^･ω･^=)", "Purr... 🐾", "Meow! 🐱", "CassetteCat 🎵", "Hi friend! 🐾").random()
                            }
                        }
                    }
            )
        } else if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .widthIn(max = 300.dp)
        )

        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                enabled = !actionLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .height(44.dp)
            ) {
                if (actionLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                } else if (actionIconRes != null) {
                    Icon(
                        painter = painterResource(actionIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        if (secondaryActionLabel != null && onSecondaryAction != null) {
            TextButton(
                onClick = onSecondaryAction,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = secondaryActionLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
