package `in`.caffeinelabs.cassettecat.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.caffeinelabs.cassettecat.ui.navigation.SmoothEasing
import `in`.caffeinelabs.cassettecat.ui.navigation.TRANSITION_MS

// Fades and settles in on entry, like a display powering on, instead of appearing static.
@Composable
fun OnboardingHeroIcon(iconRes: Int, modifier: Modifier = Modifier, blobSize: Dp = 180.dp, iconSize: Dp = 84.dp) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(TRANSITION_MS * 2, easing = SmoothEasing), label = "heroAlpha")
    val scale by animateFloatAsState(if (visible) 1f else 0.92f, tween(TRANSITION_MS * 2, easing = SmoothEasing), label = "heroScale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(blobSize)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                        )
                    )
                )
        )
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun OnboardingProgressDots(currentStep: Int, modifier: Modifier = Modifier, totalSteps: Int = 4) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SETUP ${currentStep + 1} OF $totalSteps",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSteps) { index ->
                // Each dot ticks in slightly after the last, like an LED meter filling, no bounce.
                val filled = index <= currentStep
                val color by animateColorAsState(
                    targetValue = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    animationSpec = tween(TRANSITION_MS, delayMillis = if (filled) index * 40 else 0, easing = SmoothEasing),
                    label = "dotFill"
                )
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
