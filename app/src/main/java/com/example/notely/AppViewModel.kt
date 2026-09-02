package com.example.notely

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("notely_prefs", Context.MODE_PRIVATE)

    // THEME
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkTheme = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newSetting = !_isDarkTheme.value
        _isDarkTheme.value = newSetting
        prefs.edit().putBoolean("is_dark_mode", newSetting).apply()
    }

    // CLIPBOARD SECURITY
    private val _isClipboardClearEnabled = MutableStateFlow(prefs.getBoolean("clipboard_autoclear", false))
    val isClipboardClearEnabled = _isClipboardClearEnabled.asStateFlow()

    fun toggleClipboardClear() {
        val newSetting = !_isClipboardClearEnabled.value
        _isClipboardClearEnabled.value = newSetting
        prefs.edit().putBoolean("clipboard_autoclear", newSetting).apply()
    }

    // SCRAMBLE KEYPAD
    private val _isPinScrambleEnabled = MutableStateFlow(prefs.getBoolean("pin_scramble_enabled", false))
    val isPinScrambleEnabled = _isPinScrambleEnabled.asStateFlow()

    fun togglePinScramble() {
        val newSetting = !_isPinScrambleEnabled.value
        _isPinScrambleEnabled.value = newSetting
        prefs.edit().putBoolean("pin_scramble_enabled", newSetting).apply()
    }

    // AUTO-SAVE NOTES
    private val _isAutoSaveEnabled = MutableStateFlow(prefs.getBoolean("auto_save_enabled", true))
    val isAutoSaveEnabled = _isAutoSaveEnabled.asStateFlow()

    fun toggleAutoSave() {
        val newSetting = !_isAutoSaveEnabled.value
        _isAutoSaveEnabled.value = newSetting
        prefs.edit().putBoolean("auto_save_enabled", newSetting).apply()
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

        if (_isClipboardClearEnabled.value) {
            try {
                val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                if (clipboard.hasPrimaryClip()) {
                    val clip = android.content.ClipData.newPlainText("Notely", "")
                    clipboard.setPrimaryClip(clip)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    // PIN LOGIC
    private val pinManager = PinManager(application)
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    private val _isPinSet = MutableStateFlow(pinManager.isPinSet())
    val isPinSet = _isPinSet.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _lockoutTimeRemaining = MutableStateFlow(0L)
    val lockoutTimeRemaining = _lockoutTimeRemaining.asStateFlow()

    private val _isPinLockoutEnabled = MutableStateFlow(pinManager.isLockoutEnabled())
    val isPinLockoutEnabled = _isPinLockoutEnabled.asStateFlow()

    // SELF-DESTRUCT STATE
    private val _isSelfDestructSet = MutableStateFlow(pinManager.isSelfDestructPinSet())
    val isSelfDestructSet = _isSelfDestructSet.asStateFlow()

    init {
        val remaining = pinManager.getRemainingLockoutTime()
        if (remaining > 0) {
            startLockoutTimer()
        }
    }

    fun togglePinLockout() {
        val newState = !_isPinLockoutEnabled.value
        pinManager.setLockoutEnabled(newState)
        _isPinLockoutEnabled.value = newState

        if (!newState) {
            _lockoutTimeRemaining.value = 0L
            if (_errorMessage.value?.contains("attempts left") == true || _errorMessage.value?.contains("Too many attempts") == true) {
                _errorMessage.value = null
            }
        }
    }

    private fun startLockoutTimer() {
        viewModelScope.launch {
            while (true) {
                val remaining = pinManager.getRemainingLockoutTime()
                _lockoutTimeRemaining.value = remaining
                if (remaining <= 0) {
                    _errorMessage.value = null
                    break
                }
                delay(1000)
            }
        }
    }

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

    fun setSelfDestructPin(newPin: String): Boolean {
        if (pinManager.verifyMainPinSilently(newPin)) {
            _errorMessage.value = "Self-Destruct PIN cannot be the same as your main PIN."
            return false
        }
        if (newPin.length >= 4) {
            pinManager.saveSelfDestructPin(newPin)
            _isSelfDestructSet.value = true
            _errorMessage.value = null
            return true
        }
        return false
    }

    fun removeSelfDestructPin() {
        pinManager.clearSelfDestructPin()
        _isSelfDestructSet.value = false
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun checkPin(inputPin: String) {
        // Intercept for Self-Destruct
        if (pinManager.isSelfDestructPinSet() && pinManager.checkSelfDestructPin(inputPin)) {
            executeSelfDestruct()
            return
        }

        // Standard Auth Flow
        val remainingTime = pinManager.getRemainingLockoutTime()
        if (remainingTime > 0) {
            startLockoutTimer()
            return
        }

        if (pinManager.checkPin(inputPin)) {
            _isAuthenticated.value = true
            _errorMessage.value = null
            _lockoutTimeRemaining.value = 0L
        } else {
            _isAuthenticated.value = false
            val newRemainingTime = pinManager.getRemainingLockoutTime()
            if (newRemainingTime > 0) {
                startLockoutTimer()
            } else {
                if (pinManager.isLockoutEnabled()) {
                    val attemptsLeft = 5 - pinManager.getFailedAttempts()
                    _errorMessage.value = "Incorrect PIN. $attemptsLeft attempts left."
                } else {
                    _errorMessage.value = "Incorrect PIN."
                }
            }
        }
    }

    fun verifyOldPin(inputPin: String): Boolean {
        return pinManager.checkPin(inputPin)
    }

    private fun executeSelfDestruct() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            pinManager.wipeAllSecureData()
            prefs.edit().clear().apply()

            launch(Dispatchers.Main) {
                _isPinSet.value = false
                _isSelfDestructSet.value = false
                _isAuthenticated.value = false
                _isBiometricEnabled.value = false
                _isClipboardClearEnabled.value = false
                _isPinScrambleEnabled.value = false  // Reset scramble toggle
                _isAutoSaveEnabled.value = true
                _isDarkTheme.value = false
                _autoLockTimeout.value = 0L
                _isPinLockoutEnabled.value = true
                _errorMessage.value = "Device data wiped for security."
            }
        }
    }

    // BIOMETRIC SECURITY
    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    fun toggleBiometricAuth(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun canUseBiometrics(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(activity: FragmentActivity) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    _isAuthenticated.value = true
                    _errorMessage.value = null
                    _lockoutTimeRemaining.value = 0L
                    pinManager.resetFailures()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        _errorMessage.value = "Biometric Error: $errString"
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Notely")
            .setSubtitle("Confirm your identity")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // NOTE LOGIC
    private val database = NoteDatabase.getDatabase(application)
    private val noteDao = database.noteDao()

    val allNotes = noteDao.getAllNotes()

    suspend fun saveNoteSynchronous(
        existingId: Int?,
        title: String,
        content: String,
        fontName: String,
        tags: String,
        styleMetadata: String
    ): Int {
        val note = Note(
            id = existingId ?: 0,
            title = title,
            content = content,
            fontName = fontName,
            tags = tags,
            styleMetadata = styleMetadata,
            timestamp = System.currentTimeMillis()
        )
        return if (existingId == null || existingId == 0) {
            noteDao.insert(note).toInt()
        } else {
            noteDao.update(note)
            existingId
        }
    }

    fun addNote(title: String, content: String, fontName: String, tags: String, styleMetadata: String) {
        viewModelScope.launch {
            noteDao.insert(Note(title = title, content = content, fontName = fontName, tags = tags, styleMetadata = styleMetadata))
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

    // ENCRYPTED IMPORT / EXPORT LOGIC
    private val gson = Gson()
    private val SALT_SIZE = 16
    private val IV_SIZE = 12
    private val KEY_SIZE = 256
    private val ITERATIONS = 65536

    fun exportEncryptedNotes(uri: Uri, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val notesList = allNotes.first()
                val jsonString = gson.toJson(notesList)
                val plainBytes = jsonString.toByteArray(Charsets.UTF_8)
                val salt = ByteArray(SALT_SIZE)
                SecureRandom().nextBytes(salt)
                val secretKey = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(plainBytes)

                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(salt)
                    output.write(iv)
                    output.write(encryptedBytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Export Failed: ${e.message}"
            }
        }
    }

    fun importEncryptedNotes(uri: Uri, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                    val salt = ByteArray(SALT_SIZE)
                    if (input.read(salt) != SALT_SIZE) throw Exception("Invalid file format")
                    val iv = ByteArray(IV_SIZE)
                    if (input.read(iv) != IV_SIZE) throw Exception("Invalid file format")
                    val encryptedBytes = input.readBytes()

                    val secretKey = deriveKey(password, salt)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val spec = GCMParameterSpec(128, iv)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                    val plainBytes = cipher.doFinal(encryptedBytes)
                    val jsonString = String(plainBytes, Charsets.UTF_8)
                    val type = object : TypeToken<List<Note>>() {}.type
                    val importedNotes: List<Note> = gson.fromJson(jsonString, type)

                    importedNotes.forEach { note ->
                        noteDao.insert(note.copy(id = 0))
                    }
                }
                _errorMessage.value = "Import Successful"
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Import Failed: Incorrect Password or Corrupt File"
            }
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_SIZE)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}