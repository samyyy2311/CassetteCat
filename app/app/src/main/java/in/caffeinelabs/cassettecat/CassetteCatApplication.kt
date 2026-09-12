package `in`.caffeinelabs.cassettecat

import android.app.Application
import `in`.caffeinelabs.cassettecat.data.diagnostics.CrashLogRepository
import `in`.caffeinelabs.cassettecat.data.streaming.CertificatePinRepository
import `in`.caffeinelabs.cassettecat.data.streaming.tofuSslSocketFactory
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.runBlocking

class CassetteCatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogRepository(this).install()
        HttpsURLConnection.setDefaultSSLSocketFactory(tofuSslSocketFactory)
        runBlocking { CertificatePinRepository(this@CassetteCatApplication).loadIntoMemory() }
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
