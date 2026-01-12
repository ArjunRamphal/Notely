package com.example.notely

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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

    fun savePin(pin: String) {
        sharedPreferences.edit().putString("USER_PIN", pin).apply()
    }

    fun getPin(): String? {
        return sharedPreferences.getString("USER_PIN", null)
    }

    fun isPinSet(): Boolean {
        return getPin() != null
    }
}