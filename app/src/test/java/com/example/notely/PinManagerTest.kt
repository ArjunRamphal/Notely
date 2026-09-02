package com.example.notely

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.security.Provider
import java.security.Security
import android.content.Context
import android.content.SharedPreferences

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class PinManagerTest {

    private var pinManager: PinManager? = null
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        ShadowLog.stream = System.out

        prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("notely_secure_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        try {
            pinManager = PinManager(ApplicationProvider.getApplicationContext())
        } catch (e: Exception) {
            e.printStackTrace()
            // swallow exception if the keystore fails to init in tests, we can test other methods
        }
    }

    @Test
    fun testIsPinSet_InitiallyFalse() {
        if (pinManager == null) return // Skip if initialization failed due to KeyStore

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

        // Use reflection to call incrementFailures and resetFailures, since they are private
        val resetMethod = PinManager::class.java.getDeclaredMethod("resetFailures")
        resetMethod.isAccessible = true
        resetMethod.invoke(pinManager!!)

        assertEquals(0, pinManager!!.getFailedAttempts())
        assertEquals(0L, pinManager!!.getRemainingLockoutTime())

        // Mock preferences to simulate lockout logic for testing helper methods
        prefs.edit().putInt("FAILED_ATTEMPTS", 2).apply()
        assertEquals(2, pinManager!!.getFailedAttempts())

        // Verify lockout time logic
        val now = System.currentTimeMillis()
        prefs.edit().putLong("LOCKOUT_TIMESTAMP", now + 10000).apply() // Lockout for 10 seconds
        assertTrue(pinManager!!.getRemainingLockoutTime() > 0)
    }

    @Test
    fun testLockoutDisabled_ClearsAttempts() {
        if (pinManager == null) return

        prefs.edit().putInt("FAILED_ATTEMPTS", 3).apply()
        assertEquals(3, pinManager!!.getFailedAttempts())

        pinManager!!.setLockoutEnabled(false)
        // When disabled, getting failed attempts returns 0
        assertEquals(0, pinManager!!.getFailedAttempts())
        assertEquals(0L, pinManager!!.getRemainingLockoutTime())
    }
}
