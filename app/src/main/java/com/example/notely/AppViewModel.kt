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
        // 1. Check for lockout FIRST
        val remainingTime = pinManager.getRemainingLockoutTime()
        if (remainingTime > 0) {
            _errorMessage.value = "Too many attempts. Try again in ${remainingTime}s"
            _isAuthenticated.value = false
            return
        }

        // 2. Verify PIN
        if (pinManager.checkPin(inputPin)) {
            _isAuthenticated.value = true
            _errorMessage.value = null
        } else {
            _isAuthenticated.value = false

            // Check if this specific failure just triggered a lockout
            val newRemainingTime = pinManager.getRemainingLockoutTime()
            if (newRemainingTime > 0) {
                _errorMessage.value = "Too many attempts. Try again in ${newRemainingTime}s"
            } else {
                _errorMessage.value = "Incorrect PIN"
            }
        }
    }

    fun verifyOldPin(inputPin: String): Boolean {
        // This relies on checkPin's return value.
        // If locked out, it returns false, acting as "Incorrect PIN" for the UI.
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
                val notesList = allNotes.first()
                val jsonString = gson.toJson(notesList)
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
                val jsonString = stringBuilder.toString()
                val type = object : TypeToken<List<Note>>() {}.type
                val importedNotes: List<Note> = gson.fromJson(jsonString, type)

                importedNotes.forEach { note ->
                    noteDao.insert(note.copy(id = 0))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}