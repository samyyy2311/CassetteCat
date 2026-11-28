package `in`.caffeinelabs.cassettecat.ui.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Activity-level screenshot notifications for contextual UI suggestions.
object ScreenshotCaptureEvents {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun notifyCaptured() {
        _events.tryEmit(Unit)
    }
}
