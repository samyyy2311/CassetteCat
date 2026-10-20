package `in`.caffeinelabs.cassettecat.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// A thin bar sweeping along the top edge while something loads, the same as the desktop app's.
@Composable
fun LoadingBar(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
    val sweep by rememberInfiniteTransition(label = "loadingBar").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_100, easing = FastOutSlowInEasing)),
        label = "loadingBarSweep"
    )
    Canvas(modifier.fillMaxWidth().height(2.5.dp)) {
        drawRect(track)
        val barWidth = size.width * 0.35f
        val x = -barWidth + (size.width + barWidth) * sweep
        drawRect(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, accent, Color.Transparent), startX = x, endX = x + barWidth),
            topLeft = Offset(x, 0f),
            size = Size(barWidth, size.height)
        )
    }
}
