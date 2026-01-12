package com.example.notely

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val fontName: String = "Default",
    val tags: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

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

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "notely_database" // UPDATED DB NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}