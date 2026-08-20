package `in`.caffeinelabs.cassettecat.data.streaming

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UntrustedCertificateException(
    val fingerprint: String,
    cause: Throwable
) : CertificateException(cause)

fun X509Certificate.sha256Fingerprint(): String =
    MessageDigest.getInstance("SHA-256").digest(encoded).joinToString(":") { "%02X".format(it) }

tailrec fun Throwable.findUntrustedCertificateCause(): UntrustedCertificateException? = when {
    this is UntrustedCertificateException -> this
    cause != null && cause !== this -> cause!!.findUntrustedCertificateCause()
    else -> null
}

// Flat set of approved fingerprints, not bound to a hostname: a self-signed cert's
// identity is the fingerprint itself, the user already verified it out-of-band.
private object CertificatePinStore {
    @Volatile var pinned: Set<String> = emptySet()
}

private fun systemDefaultTrustManager(): X509TrustManager {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(null as KeyStore?)
    return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
}

@Suppress("CustomX509TrustManager")
private class TofuTrustManager(private val systemDefault: X509TrustManager) : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
        systemDefault.checkClientTrusted(chain, authType)

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        try {
            systemDefault.checkServerTrusted(chain, authType)
        } catch (systemFailure: CertificateException) {
            val leaf = chain.firstOrNull() ?: throw systemFailure
            val fingerprint = leaf.sha256Fingerprint()
            if (fingerprint !in CertificatePinStore.pinned) {
                throw UntrustedCertificateException(fingerprint, systemFailure)
            }
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = systemDefault.acceptedIssuers
}

val tofuTrustManager: X509TrustManager = TofuTrustManager(systemDefaultTrustManager())

val tofuSslSocketFactory: SSLSocketFactory = SSLContext.getInstance("TLS").apply {
    init(null, arrayOf<TrustManager>(tofuTrustManager), null)
}.socketFactory

private val Context.certificatePinDataStore by preferencesDataStore(name = "certificate_pins")
private val PINNED_FINGERPRINTS = stringSetPreferencesKey("pinned_fingerprints")

class CertificatePinRepository(private val context: Context) {
    suspend fun loadIntoMemory() {
        CertificatePinStore.pinned =
            context.certificatePinDataStore.data.map { it[PINNED_FINGERPRINTS] ?: emptySet() }.first()
    }

    suspend fun pin(fingerprint: String) {
        context.certificatePinDataStore.edit { prefs ->
            prefs[PINNED_FINGERPRINTS] = (prefs[PINNED_FINGERPRINTS] ?: emptySet()) + fingerprint
        }
        CertificatePinStore.pinned = CertificatePinStore.pinned + fingerprint
    }
}
