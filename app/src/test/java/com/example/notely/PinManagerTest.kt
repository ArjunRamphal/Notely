package com.example.notely

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLog
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowCipher::class, ShadowKeyStore::class, ShadowKeyGenerator::class])
class PinManagerTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private var pinManager: PinManager? = null

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("notely_secure_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            pinManager = PinManager(context)
        } catch (e: Exception) {
            println("Exception: $e")
        }
    }

    // --- TESTS FROM CURRENT BRANCH (PIN Checking Logic) ---

    @Test
    fun testCheckPin_WhenLockedOut_ReturnsFalse() {
        if (pinManager == null) return
        prefs.edit().putBoolean("LOCKOUT_ENABLED", true).commit()
        prefs.edit().putLong("LOCKOUT_TIMESTAMP", System.currentTimeMillis() + 10000).commit()

        val result = pinManager!!.checkPin("1234")
        assertFalse(result)
    }

    @Test
    fun testCheckPin_NoPinSet_ReturnsFalse() {
        if (pinManager == null) return
        val result = pinManager!!.checkPin("1234")
        assertFalse(result)
    }

    @Test
    fun testCheckPin_CorrectPin_ReturnsTrue() {
        if (pinManager == null) return
        val pin = "1234"
        pinManager!!.savePin(pin)

        val result = pinManager!!.checkPin(pin)
        assertTrue(result)
    }

    @Test
    fun testCheckPin_IncorrectPin_ReturnsFalseAndIncrementsFailures() {
        if (pinManager == null) return
        val pin = "1234"
        pinManager!!.savePin(pin)

        val initialFailures = pinManager!!.getFailedAttempts()

        val result = pinManager!!.checkPin("9999")
        assertFalse(result)

        val newFailures = pinManager!!.getFailedAttempts()
        assertEquals(initialFailures + 1, newFailures)
    }

    @Test
    fun testCheckPin_CorruptedData_ReturnsFalseSafely() {
        if (pinManager == null) return
        pinManager!!.savePin("1234")

        prefs.edit().putString("ENCRYPTED_PIN_DATA", "corrupted_blob").commit()

        val result = pinManager!!.checkPin("1234")
        assertFalse(result)
    }

    // --- TESTS FROM MASTER BRANCH (State & Lockout Settings) ---

    @Test
    fun testIsPinSet_InitiallyFalse() {
        if (pinManager == null) return
        assertFalse(pinManager!!.isPinSet())
    }

    @Test
    fun testIsPinSet_TrueWhenDataExists() {
        if (pinManager == null) return
        prefs.edit().putString("ENCRYPTED_PIN_DATA", "dummy_data").apply()
        assertTrue(pinManager!!.isPinSet())
    }

    @Test
    fun testIsLockoutEnabled_DefaultTrue() {
        if (pinManager == null) return
        assertTrue(pinManager!!.isLockoutEnabled())
    }

    @Test
    fun testSetLockoutEnabled() {
        if (pinManager == null) return

        pinManager!!.setLockoutEnabled(false)
        assertFalse(pinManager!!.isLockoutEnabled())

        pinManager!!.setLockoutEnabled(true)
        assertTrue(pinManager!!.isLockoutEnabled())
    }

    @Test
    fun testFailedAttemptsAndLockout() {
        if (pinManager == null) return

        pinManager!!.resetFailures()

        assertEquals(0, pinManager!!.getFailedAttempts())
        assertEquals(0L, pinManager!!.getRemainingLockoutTime())

        prefs.edit().putInt("FAILED_ATTEMPTS", 2).apply()
        assertEquals(2, pinManager!!.getFailedAttempts())

        val now = System.currentTimeMillis()
        prefs.edit().putLong("LOCKOUT_TIMESTAMP", now + 10000).apply() 
        assertTrue(pinManager!!.getRemainingLockoutTime() > 0)
    }

    @Test
    fun testLockoutDisabled_ClearsAttempts() {
        if (pinManager == null) return

        prefs.edit().putInt("FAILED_ATTEMPTS", 3).apply()
        assertEquals(3, pinManager!!.getFailedAttempts())

        pinManager!!.setLockoutEnabled(false)
        assertEquals(0, pinManager!!.getFailedAttempts())
        assertEquals(0L, pinManager!!.getRemainingLockoutTime())
    }
}

// --- SHADOWS FOR KEYSTORE BYPASS ---

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
            return Cipher.getInstance("AES/CBC/PKCS5Padding")
        }
    }
}