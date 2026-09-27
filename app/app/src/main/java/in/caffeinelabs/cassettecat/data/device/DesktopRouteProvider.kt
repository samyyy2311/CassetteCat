package `in`.caffeinelabs.cassettecat.data.device

import android.content.Context
import android.content.IntentFilter
import androidx.mediarouter.media.MediaRouteDescriptor
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteProviderDescriptor
import androidx.mediarouter.media.MediaRouteProviderService
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouterParams
import `in`.caffeinelabs.cassettecat.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The route category the app asks Android's output switcher to list; only the paired computer carries it. */
const val DESKTOP_ROUTE_CATEGORY = "in.caffeinelabs.cassettecat.CATEGORY_DESKTOP"
private const val DESKTOP_ROUTE_ID = "desktop"

/**
 * Asks Android's output switcher to list the computer for this app, and keeps the switcher's selection in step with
 * whether the phone is controlling it, whichever side made the change.
 */
fun listDesktopInOutputSwitcher(context: Context) {
    val router = MediaRouter.getInstance(context)
    router.setRouterParams(MediaRouterParams.Builder().setOutputSwitcherEnabled(true).setTransferToLocalEnabled(true).build())
    val selector = MediaRouteSelector.Builder().addControlCategory(DESKTOP_ROUTE_CATEGORY).build()
    val desktop = DesktopRemoteRepository.getInstance(context)
    fun sync(controlling: Boolean) {
        val desktopSelected = router.selectedRoute.matchesSelector(selector)
        if (controlling && !desktopSelected) router.routes.firstOrNull { it.matchesSelector(selector) }?.select()
        // Unselecting only the computer leaves a Bluetooth route picked in the switcher as it is.
        if (!controlling && desktopSelected) router.unselect(MediaRouter.UNSELECT_REASON_STOPPED)
    }
    router.addCallback(
        selector,
        object : MediaRouter.Callback() {
            // The computer's route appears a moment after launch, when it may already be the one playing.
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                if (desktop.transferRequest.value == null) sync(desktop.state.value.controlling)
            }
        },
        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
    )
    // Synced once a move has settled, so a computer that refused the songs puts the switcher back on the phone.
    MainScope().launch {
        combine(desktop.state.map { it.controlling }, desktop.transferRequest) { controlling, pending ->
            controlling.takeIf { pending == null }
        }.filterNotNull().collect(::sync)
    }
}

/** Publishes the paired computer to Android's output switcher, next to the phone's speaker and Bluetooth. */
class DesktopRouteProviderService : MediaRouteProviderService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreateMediaRouteProvider(): MediaRouteProvider = DesktopRouteProvider(this, scope)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private class DesktopRouteProvider(context: Context, scope: CoroutineScope) : MediaRouteProvider(context) {
    private val desktop = DesktopRemoteRepository.getInstance(context)
    private val fallbackName = context.getString(R.string.desktop_remote_your_computer)

    // Re-selecting the route opens a new session before the old one closes, so playback only leaves the computer
    // when the last selection ends.
    private var selections = 0

    init {
        scope.launch {
            combine(desktop.state, desktop.status) { state, status ->
                val address = state.address ?: return@combine null
                if (state.offlineBlackout) return@combine null
                MediaRouteDescriptor.Builder(DESKTOP_ROUTE_ID, state.name ?: fallbackName)
                    .setDescription(address.host)
                    .setDeviceType(MediaRouter.RouteInfo.DEVICE_TYPE_COMPUTER)
                    .addControlFilter(IntentFilter().apply { addCategory(DESKTOP_ROUTE_CATEGORY) })
                    .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                    .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                    .setVolumeMax(100)
                    .setVolume(status?.volumePercent ?: 0)
                    // Only this app's own output switcher lists the computer.
                    .setVisibilityRestricted(setOf(context.packageName))
                    .build()
            }.collect { route ->
                descriptor = MediaRouteProviderDescriptor.Builder().apply { route?.let(::addRoute) }.build()
            }
        }
    }

    override fun onCreateRouteController(routeId: String): RouteController? =
        if (routeId == DESKTOP_ROUTE_ID) DesktopRouteController() else null

    private inner class DesktopRouteController : RouteController() {
        override fun onSelect() {
            if (selections++ == 0) desktop.requestTransfer(toDesktop = true)
        }

        override fun onUnselect(reason: Int) {
            selections = (selections - 1).coerceAtLeast(0)
            // A route change or a stop moves playback back to the phone; a disconnect leaves the computer playing.
            if (selections == 0 && reason != MediaRouter.UNSELECT_REASON_DISCONNECTED) desktop.requestTransfer(toDesktop = false)
        }

        override fun onSetVolume(volume: Int) = desktop.setVolume(volume.coerceIn(0, 100))

        override fun onUpdateVolume(delta: Int) =
            desktop.setVolume(((desktop.status.value?.volumePercent ?: 0) + delta).coerceIn(0, 100))
    }
}
