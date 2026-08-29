package `in`.caffeinelabs.cassettecat.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.ui.util.hapticClick

@Composable
fun TransportButton(
    iconRes: Int,
    size: Dp,
    tint: Color,
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    accented: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressDepth by animateDpAsState(
        targetValue = if (pressed) 3.dp else 0.dp,
        animationSpec = tween(80, easing = LinearEasing),
        label = "transportPressDepth"
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) 0.dp else 5.dp,
        animationSpec = tween(80, easing = LinearEasing),
        label = "transportElevation"
    )
    val borderColor = if (accented) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
    val capBrush = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerLowest)
    )
    Box(
        modifier = modifier
            .size(size)
            .offset { IntOffset(0, pressDepth.roundToPx()) }
            .shadow(elevation, CircleShape)
            .clip(CircleShape)
            .background(capBrush)
            .border(if (accented) 1.5.dp else 1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = hapticClick(onClick)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.42f)
        )
    }
}
