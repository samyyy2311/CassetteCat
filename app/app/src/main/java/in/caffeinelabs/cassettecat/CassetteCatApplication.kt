package `in`.caffeinelabs.cassettecat

import android.app.Application
import `in`.caffeinelabs.cassettecat.data.device.listDesktopInOutputSwitcher
import `in`.caffeinelabs.cassettecat.data.diagnostics.CrashLogRepository
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.data.streaming.CertificatePinRepository
import `in`.caffeinelabs.cassettecat.data.streaming.blackoutActive
import `in`.caffeinelabs.cassettecat.data.streaming.pinnedHostnameVerifier
import `in`.caffeinelabs.cassettecat.data.streaming.tofuSslSocketFactory
import `in`.caffeinelabs.cassettecat.data.streaming.ServerAddresses
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.streaming.StreamQuality
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import `in`.caffeinelabs.cassettecat.data.streaming.RemoteAlbumArtLoader

class CassetteCatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogRepository(this).install()
        HttpsURLConnection.setDefaultSSLSocketFactory(tofuSslSocketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier(pinnedHostnameVerifier)
        runBlocking { CertificatePinRepository(this@CassetteCatApplication).loadIntoMemory() }
        // Read before anything can make a request, then kept in step with the setting.
        val blackout = ServiceSettingsRepository(this).settings.map { it.offlineBlackoutMode }
        blackoutActive = runBlocking { blackout.first() }
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            blackout.collect {
                blackoutActive = it
                ServerAddresses.checkAgain()
            }
        }
        listDesktopInOutputSwitcher(this)
        ServerAddresses.start(this)
        RemoteAlbumArtLoader.diskDir = java.io.File(cacheDir, "server_covers")
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            AppPreferencesRepository(this@CassetteCatApplication).preferences.collect {
                StreamQuality.wifiKbps = it.streamQualityWifiKbps
                StreamQuality.mobileKbps = it.streamQualityMobileKbps
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        `in`.caffeinelabs.cassettecat.ui.components.trimAlbumArtCaches(this, level)
        `in`.caffeinelabs.cassettecat.ui.components.trimArtistImageCaches(level)
    }

    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        super.onLowMemory()
        `in`.caffeinelabs.cassettecat.ui.components.trimAlbumArtCaches(this, android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        `in`.caffeinelabs.cassettecat.ui.components.trimArtistImageCaches(android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}
