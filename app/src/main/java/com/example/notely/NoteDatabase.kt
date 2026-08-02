package com.example.notely

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// 1. DATA ENTITY
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val fontName: String = "Default",
    val tags: String = "",
    val styleMetadata: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

// 2. DAO
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isFavorite DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    // CRITICAL: Return Long so we know the generated ID for auto-saving
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}

// 3. DATABASE CONFIGURATION
@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = true,
    autoMigrations = []
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        private const val KEY_ALIAS = "notely_db_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREFS_NAME = "notely_db_prefs"
        private const val ENCRYPTED_KEY_NAME = "encrypted_db_key_blob"

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                // 1. Load SQLCipher native libraries
                System.loadLibrary("sqlcipher")

                // 2. Retrieve (or generate) the raw 32-byte key using Keystore
                val passphrase = getOrGenerateKey(context)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "notely_secure.db"
                )
                    .openHelperFactory(factory) // 3. Enable Encryption
                    .build()

                // --- MEMORY HYGIENE ---
                passphrase.fill(0)

                INSTANCE = instance
                instance
            }
        }

        // --- NATIVE KEYSTORE LOGIC ---
        private fun getOrGenerateKey(context: Context): ByteArray {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val encryptedBlob = prefs.getString(ENCRYPTED_KEY_NAME, null)

            // 1. Ensure the Keystore "Master Key" exists
            createKeystoreKey()

            return if (encryptedBlob != null) {
                // 2a. If we have a saved DB key, decrypt it using the Master Key
                try {
                    decryptKey(encryptedBlob)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If decryption fails (e.g. key invalidated), we must recreate.
                    generateAndSaveNewKey(prefs)
                }
            } else {
                // 2b. Generate a new random DB key, encrypt it, and save it
                generateAndSaveNewKey(prefs)
            }
        }

        private fun generateAndSaveNewKey(prefs: android.content.SharedPreferences): ByteArray {
            // Generate random 32 bytes (The actual DB password)
            val newKey = ByteArray(32)
            SecureRandom().nextBytes(newKey)

            // Encrypt it using the Keystore Master Key
            val encryptedBlob = encryptKey(newKey)

            // Save the encrypted blob to standard prefs
            prefs.edit().putString(ENCRYPTED_KEY_NAME, encryptedBlob).apply()

            return newKey
        }

        // --- LOW LEVEL CRYPTO ---
        private fun createKeystoreKey() {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()

                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        }

        private fun getMasterKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        private fun encryptKey(data: ByteArray): String {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data)

            // Combine IV + Encrypted Data (we need the IV to decrypt later)
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        private fun decryptKey(base64Data: String): ByteArray {
            val combined = Base64.decode(base64Data, Base64.NO_WRAP)

            // GCM standard IV length is 12 bytes
            val iv = ByteArray(12)
            val encryptedBytes = ByteArray(combined.size - 12)

            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)

            return cipher.doFinal(encryptedBytes)
        }
    }
}