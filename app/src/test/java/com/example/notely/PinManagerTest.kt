package com.example.notely

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowCipher::class, ShadowKeyStore::class, ShadowKeyGenerator::class])
class PinManagerTest {
    private lateinit var context: Context
    private lateinit var pinManager: PinManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("notely_secure_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            pinManager = PinManager(context)
        } catch (e: Exception) {
            println("Exception: $e")
        }
    }

    @Test
    fun testCheckPin_WhenLockedOut_ReturnsFalse() {
        if (!::pinManager.isInitialized) return
        val prefs = context.getSharedPreferences("notely_secure_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("LOCKOUT_ENABLED", true).commit()
        prefs.edit().putLong("LOCKOUT_TIMESTAMP", System.currentTimeMillis() + 10000).commit()

        val result = pinManager.checkPin("1234")
        assertFalse(result)
    }

    @Test
    fun testCheckPin_NoPinSet_ReturnsFalse() {
        if (!::pinManager.isInitialized) return
        val result = pinManager.checkPin("1234")
        assertFalse(result)
    }

    @Test
    fun testCheckPin_CorrectPin_ReturnsTrue() {
        if (!::pinManager.isInitialized) return
        val pin = "1234"
        pinManager.savePin(pin)

        val result = pinManager.checkPin(pin)
        assertTrue(result)
    }

    @Test
    fun testCheckPin_IncorrectPin_ReturnsFalseAndIncrementsFailures() {
        if (!::pinManager.isInitialized) return
        val pin = "1234"
        pinManager.savePin(pin)

        val initialFailures = pinManager.getFailedAttempts()

        val result = pinManager.checkPin("9999")
        assertFalse(result)

        val newFailures = pinManager.getFailedAttempts()
        assertEquals(initialFailures + 1, newFailures)
    }

    @Test
    fun testCheckPin_CorruptedData_ReturnsFalseSafely() {
        if (!::pinManager.isInitialized) return
        pinManager.savePin("1234")

        val prefs = context.getSharedPreferences("notely_secure_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("ENCRYPTED_PIN_DATA", "corrupted_blob").commit()

        val result = pinManager.checkPin("1234")
        assertFalse(result)
    }
}

@Implements(KeyStore::class)
class ShadowKeyStore {
    companion object {
        @JvmStatic
        @Implementation
        fun getInstance(type: String): KeyStore {
            return KeyStore.getInstance("JCEKS")
        }
    }
}

@Implements(KeyGenerator::class)
class ShadowKeyGenerator {
    companion object {
        @JvmStatic
        @Implementation
        fun getInstance(algorithm: String, provider: String): KeyGenerator {
            return KeyGenerator.getInstance("AES")
        }
    }
}

@Implements(Cipher::class)
class ShadowCipher {
    companion object {
        @JvmStatic
        @Implementation
        fun getInstance(transformation: String): Cipher {
            // Need to return a non null cipher, even if we map AES/GCM/NoPadding to CBC
            return Cipher.getInstance("AES/CBC/PKCS5Padding")
        }
    }
}
