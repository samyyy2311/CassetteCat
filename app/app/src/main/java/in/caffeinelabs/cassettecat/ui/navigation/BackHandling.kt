package `in`.caffeinelabs.cassettecat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

// Back is decided in one place, MainShell. Screens hand it the steps of their own that come before leaving, such
// as clearing a selection, instead of adding a BackHandler each: the BackHandler composed last wins, so one on a
// page would act even with the player open over it.
enum class BackLayer { PLAYER, PAGE }

class BackStep(val layer: BackLayer, val action: () -> Unit)

class AppBackSteps {
    private val steps = mutableStateListOf<BackStep>()

    fun latest(layer: BackLayer): BackStep? = steps.lastOrNull { it.layer == layer }

    internal fun add(step: BackStep) = steps.add(step)

    internal fun remove(step: BackStep) = steps.remove(step)
}

val LocalAppBackSteps = staticCompositionLocalOf { AppBackSteps() }

@Composable
fun AppBackHandler(enabled: Boolean, layer: BackLayer = BackLayer.PAGE, onBack: () -> Unit) {
    val steps = LocalAppBackSteps.current
    val currentOnBack by rememberUpdatedState(onBack)
    if (enabled) {
        DisposableEffect(steps, layer) {
            val step = BackStep(layer) { currentOnBack() }
            steps.add(step)
            onDispose { steps.remove(step) }
        }
    }
}
