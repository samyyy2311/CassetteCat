package `in`.caffeinelabs.cassettecat.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// A thin bar sweeping along the top edge while something loads, the same as the desktop app's.
@Composable
fun LoadingBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth().height(2.5.dp),
        color = MaterialTheme.colorScheme.tertiary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
        gapSize = 0.dp
    )
}
