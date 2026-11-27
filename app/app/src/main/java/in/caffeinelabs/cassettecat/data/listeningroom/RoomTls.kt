package `in`.caffeinelabs.cassettecat.data.listeningroom

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import `in`.caffeinelabs.cassettecat.data.streaming.sha256Fingerprint
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ServerSocketFactory
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val ROOM_KEY_ALIAS = "listening_room"

/**
 * A TLS server for a new room, with a fresh key from AndroidKeyStore, which also signs the certificate itself, so
 * the private key never leaves the keystore. The room socket and the audio relay share it.
 */
internal fun newRoomServerSocketFactory(): ServerSocketFactory {
    KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE).apply {
        initialize(
            KeyGenParameterSpec.Builder(ROOM_KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                // TLS 1.2 signs a digest it computed itself, so the key must also accept one unhashed.
                .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
                .build()
        )
    }.generateKeyPair()
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        .apply { init(keyStore, null) }.keyManagers
    return SSLContext.getInstance("TLS").apply { init(keyManagers, null, null) }.serverSocketFactory
}

/**
 * A host's certificate is self-signed and new for each room, so a guest trusts the one it first meets and then
 * only that one, on reconnects and for the audio relay. Anyone else on the network then sees only encrypted traffic.
 */
@Suppress("CustomX509TrustManager")
internal class RoomTrustManager(private val expectedFingerprint: String?) : X509TrustManager {
    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        val leaf = chain.firstOrNull() ?: throw CertificateException("The room sent no certificate")
        if (expectedFingerprint != null && leaf.sha256Fingerprint() != expectedFingerprint) {
            throw CertificateException("The room's certificate changed")
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
        throw CertificateException("Rooms don't accept client certificates")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    fun socketFactory(): SSLSocketFactory = SSLContext.getInstance("TLS").apply { init(null, arrayOf(this@RoomTrustManager), null) }.socketFactory
}
