package com.example.notely

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

class PinManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "notely_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Hashes the PIN and saves the fingerprint.
     */
    fun savePin(pin: String) {
        val hash = hashPin(pin)
        sharedPreferences.edit().putString("USER_PIN_HASH", hash).apply()
    }

    /**
     * Checks if the entered PIN matches the saved hash.
     * Returns true if correct.
     */
    fun checkPin(inputPin: String): Boolean {
        val storedHash = sharedPreferences.getString("USER_PIN_HASH", null) ?: return false
        val inputHash = hashPin(inputPin)
        return storedHash == inputHash
    }

    fun isPinSet(): Boolean {
        return sharedPreferences.contains("USER_PIN_HASH")
    }

    /**
     * Standard SHA-256 Hashing.
     */
    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(bytes)
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }
}