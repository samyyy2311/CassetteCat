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
}
