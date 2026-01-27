package com.example.notely

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

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
     * 1. Generates a random Salt.
     * 2. Combines Salt + PIN.
     * 3. Hashes the result.
     * 4. Stores both the Salt and the Hash.
     */
    fun savePin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)

        sharedPreferences.edit()
            .putString("USER_PIN_HASH", hash)
            .putString("USER_PIN_SALT", salt)
            .apply()
    }

    /**
     * 1. Retrieves the saved Salt.
     * 2. Combines Saved Salt + Input PIN.
     * 3. Hashes it.
     * 4. Compares with stored Hash.
     */
    fun checkPin(inputPin: String): Boolean {
        val storedHash = sharedPreferences.getString("USER_PIN_HASH", null) ?: return false
        val storedSalt = sharedPreferences.getString("USER_PIN_SALT", null) ?: return false

        val inputHash = hashPin(inputPin, storedSalt)
        return storedHash == inputHash
    }

    fun isPinSet(): Boolean {
        return sharedPreferences.contains("USER_PIN_HASH")
    }

    // --- Helpers ---

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16) // 16 bytes is a standard salt size
        random.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    // Standard SHA-256 Hashing
    private fun hashPin(pin: String, salt: String): String {
        val combinedString = salt + pin // The "Secret Sauce"
        val bytes = combinedString.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(bytes)
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }
}