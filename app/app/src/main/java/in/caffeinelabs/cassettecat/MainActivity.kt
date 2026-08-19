package `in`.caffeinelabs.cassettecat

import android.app.Activity
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
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferences
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.ui.navigation.CassetteCatNavHost
import `in`.caffeinelabs.cassettecat.ui.theme.CassetteCatTheme
import `in`.caffeinelabs.cassettecat.ui.util.ScreenshotCaptureEvents

class MainActivity : ComponentActivity() {
    private val screenshotCallback = Activity.ScreenCaptureCallback {
        ScreenshotCaptureEvents.notifyCaptured()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()
        // The app draws the artist/album hero behind the status bar. A transparent dark style
        // lets that artwork continue to the very top while retaining readable light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            window.isStatusBarContrastEnforced = false
            @Suppress("DEPRECATION")
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val context = LocalContext.current
            val appPreferencesRepository = remember { AppPreferencesRepository(context) }
            val preferences by appPreferencesRepository.preferences.collectAsState(initial = AppPreferences())

            CassetteCatTheme(
                accent = preferences.themeAccent,
                isAmoled = preferences.amoledDarkTheme
            ) {
                // Navigation owns safe insets per surface so immersive views can intentionally
                // draw behind the system bars without changing ordinary screens.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CassetteCatNavHost(modifier = Modifier.fillMaxSize())
                }
            }
        }
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
            runCatching { registerScreenCaptureCallback(mainExecutor, screenshotCallback) }
        }
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching { unregisterScreenCaptureCallback(screenshotCallback) }
        }
        super.onStop()
    }
}
