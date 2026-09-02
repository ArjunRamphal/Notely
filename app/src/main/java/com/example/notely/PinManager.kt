package com.example.notely

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

class PinManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("notely_secure_prefs", Context.MODE_PRIVATE)

    private val KEY_ALIAS = "notely_pin_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"

    // --- SECURITY CONFIGURATION ---
    private val ITERATIONS = 100_000
    private val KEY_LENGTH = 256
    private val SALT_LENGTH = 16

    private val MAX_ATTEMPTS = 5
    private val BASE_LOCKOUT_DURATION_MS = 1_800_000L

    init {
        createKeyStoreKey()
    }

    // --- MAIN PIN LOGIC ---

    fun savePin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        val dataToStore = "$salt:$hash"
        val encryptedData = encryptData(dataToStore)

        sharedPreferences.edit()
            .putString("ENCRYPTED_PIN_DATA", encryptedData)
            .putInt("FAILED_ATTEMPTS", 0)
            .putLong("LOCKOUT_TIMESTAMP", 0)
            .putInt("LOCKOUT_MULTIPLIER", 1)
            .apply()
    }

    fun checkPin(inputPin: String): Boolean {
        if (isLockedOut()) return false

        if (verifyMainPinSilently(inputPin)) {
            resetFailures()
            return true
        } else {
            incrementFailures()
            return false
        }
    }

    fun verifyMainPinSilently(inputPin: String): Boolean {
        val encryptedData = sharedPreferences.getString("ENCRYPTED_PIN_DATA", null) ?: return false
        return try {
            val decryptedString = decryptData(encryptedData)
            val parts = decryptedString.split(":")
            if (parts.size != 2) return false
            val storedSalt = parts[0]
            val storedHash = parts[1]
            val inputHash = hashPin(inputPin, storedSalt)
            inputHash == storedHash
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isPinSet(): Boolean {
        return sharedPreferences.contains("ENCRYPTED_PIN_DATA")
    }

    // --- SELF-DESTRUCT PIN LOGIC ---

    fun saveSelfDestructPin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        val dataToStore = "$salt:$hash"
        val encryptedData = encryptData(dataToStore)

        sharedPreferences.edit()
            .putString("ENCRYPTED_SD_PIN_DATA", encryptedData)
            .apply()
    }

    fun checkSelfDestructPin(inputPin: String): Boolean {
        val encryptedData = sharedPreferences.getString("ENCRYPTED_SD_PIN_DATA", null) ?: return false
        return try {
            val decryptedString = decryptData(encryptedData)
            val parts = decryptedString.split(":")
            if (parts.size != 2) return false
            val storedSalt = parts[0]
            val storedHash = parts[1]
            val inputHash = hashPin(inputPin, storedSalt)
            inputHash == storedHash
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isSelfDestructPinSet(): Boolean {
        return sharedPreferences.contains("ENCRYPTED_SD_PIN_DATA")
    }

    fun clearSelfDestructPin() {
        sharedPreferences.edit().remove("ENCRYPTED_SD_PIN_DATA").apply()
    }

    // --- SETTINGS LOGIC ---

    fun isLockoutEnabled(): Boolean {
        return sharedPreferences.getBoolean("LOCKOUT_ENABLED", true)
    }

    fun setLockoutEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("LOCKOUT_ENABLED", enabled).apply()
        if (!enabled) resetFailures()
    }

    fun wipeAllSecureData() {
        sharedPreferences.edit().clear().apply()
    }

    // --- ENCRYPTION LOGIC (Hardware Backed) ---

    private fun createKeyStoreKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun encryptData(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        val iv = cipher.iv
        val encryption = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encryption.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryption, 0, combined, iv.size, encryption.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptData(base64Data: String): String {
        val combined = Base64.decode(base64Data, Base64.NO_WRAP)
        val iv = ByteArray(12)
        val encryptedData = ByteArray(combined.size - 12)

        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, encryptedData, 0, encryptedData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val decoded = cipher.doFinal(encryptedData)
        return String(decoded, Charsets.UTF_8)
    }

    // --- HELPERS (Rate Limiting & Hashing) ---

    private fun isLockedOut(): Boolean {
        if (!isLockoutEnabled()) return false
        val lockoutTime = sharedPreferences.getLong("LOCKOUT_TIMESTAMP", 0)
        return System.currentTimeMillis() < lockoutTime
    }

    private fun incrementFailures() {
        if (!isLockoutEnabled()) return

        val failures = sharedPreferences.getInt("FAILED_ATTEMPTS", 0) + 1
        val editor = sharedPreferences.edit()

        if (failures >= MAX_ATTEMPTS) {
            val multiplier = sharedPreferences.getInt("LOCKOUT_MULTIPLIER", 1)
            val duration = BASE_LOCKOUT_DURATION_MS * multiplier

            editor.putLong("LOCKOUT_TIMESTAMP", System.currentTimeMillis() + duration)
            editor.putInt("LOCKOUT_MULTIPLIER", multiplier * 2)
            editor.putInt("FAILED_ATTEMPTS", 0)
        } else {
            editor.putInt("FAILED_ATTEMPTS", failures)
        }
        editor.apply()
    }

    fun resetFailures() {
        sharedPreferences.edit()
            .putInt("FAILED_ATTEMPTS", 0)
            .putLong("LOCKOUT_TIMESTAMP", 0)
            .putInt("LOCKOUT_MULTIPLIER", 1)
            .apply()
    }

    fun getFailedAttempts(): Int {
        if (!isLockoutEnabled()) return 0
        return sharedPreferences.getInt("FAILED_ATTEMPTS", 0)
    }

    fun getRemainingLockoutTime(): Long {
        if (!isLockoutEnabled()) return 0

        val lockoutTime = sharedPreferences.getLong("LOCKOUT_TIMESTAMP", 0)
        val now = System.currentTimeMillis()
        return if (now < lockoutTime) (lockoutTime - now) / 1000 else 0
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun hashPin(pin: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
}