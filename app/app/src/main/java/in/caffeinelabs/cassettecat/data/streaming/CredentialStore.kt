package `in`.caffeinelabs.cassettecat.data.streaming

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Secrets only; everything else lives in StreamingServerRepository. Plain SharedPreferences,
// not DataStore: read rarely (login time), doesn't benefit from the reactive-Flow treatment.
class CredentialStore(context: Context) {
    private val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    fun saveSubsonicPassword(password: String) = save(KEY_SUBSONIC_PASSWORD, password)
    fun getSubsonicPassword(): String? = load(KEY_SUBSONIC_PASSWORD)

    fun saveJellyfinAccessToken(token: String) = save(KEY_JELLYFIN_TOKEN, token)
    fun getJellyfinAccessToken(): String? = load(KEY_JELLYFIN_TOKEN)

    fun clear(protocol: StreamingProtocol) {
        val key = when (protocol) {
            StreamingProtocol.SUBSONIC -> KEY_SUBSONIC_PASSWORD
            StreamingProtocol.JELLYFIN -> KEY_JELLYFIN_TOKEN
        }
        prefs.edit { remove(key) }
    }

    private fun save(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val encoded = "${cipher.iv.toBase64()}:${ciphertext.toBase64()}"
        prefs.edit { putString(key, encoded) }
    }

    private fun load(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        val (ivPart, ciphertextPart) = encoded.split(":", limit = 2).let { it[0] to it[1] }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, ivPart.fromBase64()))
        }
        return cipher.doFinal(ciphertextPart.fromBase64()).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "cassettecat_credentials"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val KEY_SUBSONIC_PASSWORD = "subsonic_password"
        const val KEY_JELLYFIN_TOKEN = "jellyfin_access_token"
    }
}
