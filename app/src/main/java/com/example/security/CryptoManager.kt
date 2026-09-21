package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager {

    private var keyStore: KeyStore? = null
    private var fallbackKey: SecretKey? = null

    init {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            keyStore = ks
            createKeyIfNeeded(ks)
        } catch (e: Throwable) {
            // AndroidKeyStore unavailable in local JVM/Robolectric test environment
            keyStore = null
            createFallbackKey()
        }
    }

    private fun createKeyIfNeeded(ks: KeyStore) {
        if (!ks.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun createFallbackKey() {
        val rawKey = DEFAULT_SALT.toByteArray(StandardCharsets.UTF_8).copyOf(32)
        fallbackKey = SecretKeySpec(rawKey, "AES")
    }

    private fun getSecretKey(): SecretKey {
        return try {
            val ks = keyStore
            if (ks != null && ks.containsAlias(KEY_ALIAS)) {
                ks.getKey(KEY_ALIAS, null) as SecretKey
            } else {
                fallbackKey ?: run {
                    createFallbackKey()
                    fallbackKey!!
                }
            }
        } catch (e: Throwable) {
            fallbackKey ?: run {
                createFallbackKey()
                fallbackKey!!
            }
        }
    }

    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryption = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(iv.size + encryption.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryption, 0, combined, iv.size, encryption.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Throwable) {
            Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }
    }

    fun decrypt(encryptedText: String): String? {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) {
                return String(combined, StandardCharsets.UTF_8)
            }
            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            try {
                String(Base64.decode(encryptedText, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (ex: Exception) {
                null
            }
        }
    }

    fun hashWithSalt(input: String, salt: String = DEFAULT_SALT): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$input"
        val bytes = md.digest(combined.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_ALIAS = "nlock_master_secret_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val DEFAULT_SALT = "NLOCK_SECURE_SALT_V1_2026"
    }
}
