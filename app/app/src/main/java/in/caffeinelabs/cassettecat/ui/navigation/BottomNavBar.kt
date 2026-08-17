package `in`.caffeinelabs.cassettecat.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R
import `in`.caffeinelabs.cassettecat.ui.util.tapScale
import `in`.caffeinelabs.cassettecat.ui.util.tapScaleSelectable

private data class NavItem(val route: String, val iconRes: Int, val label: String)

private val items = listOf(
    NavItem(MainRoute.HOME, R.drawable.lucide_ic_house, "Home"),
    NavItem(MainRoute.SEARCH, R.drawable.lucide_ic_search, "Search"),
    NavItem(MainRoute.LIBRARY, R.drawable.lucide_ic_music, "Library"),
    NavItem(MainRoute.SETTINGS, R.drawable.lucide_ic_settings, "Preferences")
)

private fun parentTabRoute(currentRoute: String?): String? = when {
    currentRoute == MainRoute.STATS || currentRoute?.startsWith("${MainRoute.SETTINGS}/") == true -> MainRoute.SETTINGS
    currentRoute?.startsWith("${MainRoute.LIBRARY}/") == true -> MainRoute.LIBRARY
    else -> currentRoute
}

// Keep all destinations legible and evenly aligned above the system gesture area.
@Composable
fun BottomNavBar(
    currentRoute: String?,
    contentAlpha: Float = 1f,
    onNavigate: (String) -> Unit,
    onSearchLongPress: () -> Unit = {}
) {
    val activeTabRoute = parentTabRoute(currentRoute)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp)
            .alpha(contentAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = activeTabRoute == item.route
            val tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            val onClick = { if (currentRoute != item.route) onNavigate(item.route) }
            val interactionModifier = if (contentAlpha > 0.5f) {
                if (item.route == MainRoute.SEARCH) Modifier.tapScaleSelectable(onClick = onClick, onLongClick = onSearchLongPress)
                else Modifier.tapScale(onClick)
            } else Modifier
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(interactionModifier)
                    .padding(vertical = 8.dp)
            ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(item.label, style = MaterialTheme.typography.labelLarge, color = tint)
            }
        }
    }
}
