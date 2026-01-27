package com.example.notely

import android.content.Context
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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}

// 3. DATABASE CONFIGURATION
@Database(
    entities = [Note::class],
    version = 1, // Reset to 1 since this is a fresh start
    exportSchema = true,
    autoMigrations = []
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                // 1. Load SQLCipher native libraries
                System.loadLibrary("sqlcipher")

                // 2. Retrieve or Generate the Secret Key securely
                val passphrase = getOrGenerateKey(context)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "notely_secure.db"
                )
                    .openHelperFactory(factory) // 3. Enable Encryption
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // --- HELPER: Secure Key Management ---
        private fun getOrGenerateKey(context: Context): ByteArray {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "secure_db_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val existingKey = sharedPreferences.getString("db_key", null)
            if (existingKey != null) {
                return Base64.decode(existingKey, Base64.DEFAULT)
            }

            val newKey = ByteArray(32)
            SecureRandom().nextBytes(newKey)

            val keyString = Base64.encodeToString(newKey, Base64.DEFAULT)
            sharedPreferences.edit().putString("db_key", keyString).apply()

            return newKey
        }
    }
}