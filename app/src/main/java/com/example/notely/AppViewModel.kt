package com.example.notely

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // --- PREFERENCES ---
    private val prefs = application.getSharedPreferences("notely_prefs", Context.MODE_PRIVATE)

    // THEME
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkTheme = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newSetting = !_isDarkTheme.value
        _isDarkTheme.value = newSetting
        prefs.edit().putBoolean("is_dark_mode", newSetting).apply()
    }

    // AUTO-LOCK
    private val _autoLockTimeout = MutableStateFlow(prefs.getLong("auto_lock_timeout", 0L))
    val autoLockTimeout = _autoLockTimeout.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0

    fun setAutoLockTimeout(timeoutMs: Long) {
        _autoLockTimeout.value = timeoutMs
        prefs.edit().putLong("auto_lock_timeout", timeoutMs).apply()
    }

    fun onAppStop() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppStart() {
        if (_autoLockTimeout.value == -1L) return
        if (_isAuthenticated.value) {
            val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
            if (elapsed > _autoLockTimeout.value) {
                _isAuthenticated.value = false
            }
        }
    }

    // --- PIN LOGIC ---
    private val pinManager = PinManager(application)
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    // Check if PIN is set immediately
    private val _isPinSet = MutableStateFlow(pinManager.isPinSet())
    val isPinSet = _isPinSet.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun setPin(newPin: String) {
        if (newPin.length >= 4) {
            pinManager.savePin(newPin)
            _isPinSet.value = true
            _isAuthenticated.value = true
            _errorMessage.value = null
        } else {
            _errorMessage.value = "PIN must be at least 4 digits"
        }
    }

    fun checkPin(inputPin: String) {
        if (pinManager.checkPin(inputPin)) {
            _isAuthenticated.value = true
            _errorMessage.value = null
        } else {
            _isAuthenticated.value = false
            _errorMessage.value = "Incorrect PIN"
        }
    }

    fun verifyOldPin(inputPin: String): Boolean {
        return pinManager.checkPin(inputPin)
    }

    // --- NOTE LOGIC ---
    private val database = NoteDatabase.getDatabase(application)
    private val noteDao = database.noteDao()

    val allNotes = noteDao.getAllNotes()

    fun addNote(
        title: String,
        content: String,
        fontName: String,
        tags: String,
        styleMetadata: String
    ) {
        viewModelScope.launch {
            noteDao.insert(Note(
                title = title,
                content = content,
                fontName = fontName,
                tags = tags,
                styleMetadata = styleMetadata
            ))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteDao.delete(note) }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.update(note.copy(timestamp = System.currentTimeMillis()))
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isFavorite = !note.isFavorite)
            noteDao.update(updatedNote)
        }
    }

    // --- IMPORT / EXPORT LOGIC ---
    private val gson = Gson()

    fun exportNotes(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get snapshot of current notes
                val notesList = allNotes.first()
                // 2. Convert to JSON
                val jsonString = gson.toJson(notesList)
                // 3. Write to file
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importNotes(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Read file content
                val stringBuilder = StringBuilder()
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                // 2. Deserialize
                val jsonString = stringBuilder.toString()
                val type = object : TypeToken<List<Note>>() {}.type
                val importedNotes: List<Note> = gson.fromJson(jsonString, type)

                // 3. Insert (Reset ID to 0 to generate new IDs and avoid conflicts)
                importedNotes.forEach { note ->
                    noteDao.insert(note.copy(id = 0))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}