package `in`.caffeinelabs.cassettecat

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.ui.navigation.CassetteCatNavHost
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.util.LocalAppPreferences
import `in`.caffeinelabs.cassettecat.ui.util.ScreenshotCaptureEvents

object AppShortcutAction {
    const val SHUFFLE_ALL = "in.caffeinelabs.cassettecat.action.SHUFFLE_ALL"
    const val PLAY_FAVORITES = "in.caffeinelabs.cassettecat.action.PLAY_FAVORITES"
    const val PLAY_RADIO_FAVORITES = "in.caffeinelabs.cassettecat.action.PLAY_RADIO_FAVORITES"
    const val PLAY_MEDIA = "in.caffeinelabs.cassettecat.action.PLAY_MEDIA"
    val all = setOf(SHUFFLE_ALL, PLAY_FAVORITES, PLAY_RADIO_FAVORITES, PLAY_MEDIA)
}

class MainActivity : ComponentActivity() {
    private val shortcutAction = mutableStateOf<String?>(null)
    private val shortcutQuery = mutableStateOf<String?>(null)
    // Initializer must be SDK-gated too, not just the register/unregister calls, or this crashes pre-14.
    private val screenshotCallback: Activity.ScreenCaptureCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Activity.ScreenCaptureCallback { ScreenshotCaptureEvents.notifyCaptured() }
        } else {
            null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShortcutIntent(intent)
        requestHighestRefreshRate()
        // The app draws the artist/album hero behind the status bar. A transparent dark style
        // lets that artwork continue to the very top while retaining readable light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val context = LocalContext.current
            val appPreferencesRepository = remember { AppPreferencesRepository(context) }
            val preferences by appPreferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())

            CompositionLocalProvider(LocalAppPreferences provides preferences) {
                CassetteCatTheme(
                    accent = preferences.themeAccent,
                    customAccentColor = preferences.customAccentColor,
                    isAmoled = preferences.amoledDarkTheme,
                    appFontFamily = preferences.appFontFamily
                ) {
                    // Navigation owns safe insets per surface so immersive views can intentionally
                    // draw behind the system bars without changing ordinary screens.
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        CassetteCatNavHost(
                            shortcutAction = shortcutAction.value,
                            shortcutQuery = shortcutQuery.value,
                            onShortcutHandled = { shortcutAction.value = null; shortcutQuery.value = null },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        shortcutAction.value = intent?.action?.takeIf { it in AppShortcutAction.all }
        shortcutQuery.value = intent?.getStringExtra("query")
    }

    private fun requestHighestRefreshRate() {
        @Suppress("DEPRECATION")
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay
        val highestRefreshRate = display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredRefreshRate = highestRefreshRate }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            screenshotCallback?.let { runCatching { registerScreenCaptureCallback(mainExecutor, it) } }
        }
    }

    override fun onResume() {
        super.onResume()
        requestHighestRefreshRate()
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            screenshotCallback?.let { runCatching { unregisterScreenCaptureCallback(it) } }
        }
        super.onStop()
    }
}
