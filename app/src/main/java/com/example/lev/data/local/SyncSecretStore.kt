package com.example.lev.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random

/**
 * Guarda do segredo do sync (M7 §F): cifrado com Android Keystore (AES-GCM).
 * Nunca fica no Room, no BuildConfig ou em texto puro. O que vai para o
 * SharedPreferences privado é só IV + criptograma em Base64.
 */
class SyncSecretStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSecret(): Boolean = prefs.contains(KEY_PAYLOAD)

    fun getSecret(): String? {
        val payload = prefs.getString(KEY_PAYLOAD, null) ?: return null
        return runCatching { decrypt(payload) }.getOrNull()
    }

    fun saveSecret(secret: String) {
        prefs.edit().putString(KEY_PAYLOAD, encrypt(secret)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_PAYLOAD).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String): String {
        val combined = Base64.decode(payload, Base64.NO_WRAP)
        require(combined.size > GCM_IV_BYTES) { "payload inválido" }
        val iv = combined.copyOfRange(0, GCM_IV_BYTES)
        val cipherText = combined.copyOfRange(GCM_IV_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    companion object {
        private const val PREFS_NAME = "lev_sync_secret"
        private const val KEY_PAYLOAD = "secret_payload_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "lev_sync_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128

        /** IV aleatório para testes de formato (sem Keystore). */
        internal fun randomIv(): ByteArray = Random.nextBytes(GCM_IV_BYTES)
    }
}
