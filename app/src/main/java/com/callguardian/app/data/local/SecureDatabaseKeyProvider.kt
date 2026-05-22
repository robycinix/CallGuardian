package com.callguardian.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureDatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("secure_database_key", Context.MODE_PRIVATE)

    fun getOrCreatePassphrase(): ByteArray {
        val storedCipherText = preferences.getString(KEY_CIPHER_TEXT, null)
        val storedIv = preferences.getString(KEY_IV, null)
        if (storedCipherText != null && storedIv != null) {
            return runCatching {
                decrypt(storedCipherText, storedIv)
            }.getOrElse {
                Log.e(TAG, "Stored SQLCipher passphrase cannot be decrypted; resetting local encrypted storage", it)
                resetEncryptedStorage()
                createAndStorePassphrase()
            }
        }

        return createAndStorePassphrase()
    }

    private fun createAndStorePassphrase(): ByteArray {
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encrypted = encrypt(passphrase)
        preferences.edit()
            .putString(KEY_CIPHER_TEXT, encrypted.cipherText)
            .putString(KEY_IV, encrypted.iv)
            .apply()
        return passphrase
    }

    private fun encrypt(value: ByteArray): EncryptedValue {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return EncryptedValue(
            cipherText = Base64.encodeToString(cipher.doFinal(value), Base64.NO_WRAP),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
        )
    }

    private fun decrypt(cipherText: String, iv: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP))
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun resetEncryptedStorage() {
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                if (containsAlias(KEY_ALIAS)) {
                    deleteEntry(KEY_ALIAS)
                }
            }
        }.onFailure {
            Log.w(TAG, "Unable to delete invalid Android Keystore entry", it)
        }

        preferences.edit().clear().commit()
        context.deleteDatabase(DATABASE_NAME)
    }

    private data class EncryptedValue(val cipherText: String, val iv: String)

    private companion object {
        const val TAG = "SecureDatabaseKey"
        const val DATABASE_NAME = "callguardian.db"
        const val KEY_ALIAS = "callguardian_sqlcipher_key"
        const val KEY_CIPHER_TEXT = "cipher_text"
        const val KEY_IV = "iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
