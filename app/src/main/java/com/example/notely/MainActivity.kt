package com.example.notely

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer // Added for the new setting
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

enum class AppScreen { NotesList, Editor, Settings, ChangePin }
enum class ChangePinStage { VerifyOld, SetNew }

class MainActivity : FragmentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            val isDark by viewModel.isDarkTheme.collectAsState()
            MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !isDark
                            isAppearanceLightNavigationBars = !isDark
                        }
                    }
                }
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(viewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) viewModel.onAppStop()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppStart()
    }
}

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val isPinSet by viewModel.isPinSet.collectAsState()
    val isAutoSaveEnabled by viewModel.isAutoSaveEnabled.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.NotesList) }
    var currentNote by remember { mutableStateOf<Note?>(null) }
    var changePinStage by remember { mutableStateOf(ChangePinStage.VerifyOld) }

    if (isAuthenticated) {
        when (currentScreen) {
            AppScreen.NotesList -> {
                NotesScreen(
                    viewModel = viewModel,
                    onAddClick = {
                        currentNote = null
                        currentScreen = AppScreen.Editor
                    },
                    onNoteClick = { note ->
                        currentNote = note
                        currentScreen = AppScreen.Editor
                    },
                    onSettingsClick = {
                        currentScreen = AppScreen.Settings
                    }
                )
            }
            AppScreen.Editor -> {
                NoteEditorScreen(
                    noteToEdit = currentNote,
                    isAutoSaveEnabled = isAutoSaveEnabled,
                    onAutoSave = { title, content, fontName, tags, styleData ->
                        if (isAutoSaveEnabled && (title.isNotBlank() || content.isNotBlank() || tags.isNotBlank())) {
                            viewModel.viewModelScope.launch {
                                val newId = viewModel.saveNoteSynchronous(
                                    currentNote?.id, title, content, fontName, tags, styleData
                                )
                                if (currentNote == null || currentNote?.id != newId) {
                                    currentNote = Note(id = newId, title = title, content = content, fontName = fontName, tags = tags, styleMetadata = styleData)
                                } else {
                                    currentNote = currentNote?.copy(title = title, content = content, fontName = fontName, tags = tags, styleMetadata = styleData)
                                }
                            }
                        }
                    },
                    onSave = { title, content, fontName, tags, styleData ->
                        // Manual Save (Always executes)
                        if (title.isNotBlank() || content.isNotBlank() || tags.isNotBlank()) {
                            viewModel.viewModelScope.launch {
                                viewModel.saveNoteSynchronous(currentNote?.id, title, content, fontName, tags, styleData)
                            }
                        }
                        currentScreen = AppScreen.NotesList
                        currentNote = null
                    },
                    onNavigateBack = { title, content, fontName, tags, styleData ->
                        // Navigate Back (Only saves if Auto-Save is on)
                        if (title.isNotBlank() || content.isNotBlank() || tags.isNotBlank()) {
                            viewModel.viewModelScope.launch {
                                viewModel.saveNoteSynchronous(currentNote?.id, title, content, fontName, tags, styleData)
                            }
                        }
                        currentScreen = AppScreen.NotesList
                        currentNote = null
                    },
                    onDiscard = {
                        // Exits without hitting the database
                        currentScreen = AppScreen.NotesList
                        currentNote = null
                    }
                )
            }
            AppScreen.Settings -> {
                SettingsScreen(
                    onBackClick = { currentScreen = AppScreen.NotesList },
                    onChangePinClick = {
                        changePinStage = ChangePinStage.VerifyOld
                        currentScreen = AppScreen.ChangePin
                    },
                    viewModel = viewModel
                )
            }
            AppScreen.ChangePin -> {
                BackHandler {
                    currentScreen = AppScreen.Settings
                }
                ChangePinScreen(
                    stage = changePinStage,
                    viewModel = viewModel,
                    onSuccess = {
                        changePinStage = ChangePinStage.SetNew
                    },
                    onFinished = {
                        currentScreen = AppScreen.Settings
                    },
                    onCancel = {
                        currentScreen = AppScreen.Settings
                    }
                )
            }
        }
    } else {
        if (isPinSet) {
            PinScreen(title = "", viewModel = viewModel, isSetup = false)
        } else {
            PinScreen(title = "Setup Notely PIN", viewModel = viewModel, isSetup = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: AppViewModel,
    onAddClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onSettingsClick: () -> Unit
) {
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    val isDark by viewModel.isDarkTheme.collectAsState()
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredNotes = notes.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true) ||
                it.tags.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DockedSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {},
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text("Search...") },
                    modifier = Modifier.weight(1f)
                ) {}
                IconButton(onClick = { viewModel.toggleTheme() }, modifier = Modifier.size(48.dp)) {
                    Icon(if (isDark) Icons.Default.Face else Icons.Default.Star, "Toggle Theme")
                }
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Settings, "Settings")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) { Icon(Icons.Default.Add, "Add Note") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredNotes) { note ->
                NoteCard(
                    note = note,
                    onNoteClick = onNoteClick,
                    onDeleteClick = { noteToDelete = note },
                    onFavoriteClick = { viewModel.toggleFavorite(note) }
                )
            }
        }
        if (noteToDelete != null) {
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                title = { Text("Delete Note?") },
                text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteNote(noteToDelete!!); noteToDelete = null }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onNoteClick: (Note) -> Unit,
    onDeleteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNoteClick(note) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = getFontFamily(note.fontName),
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Row {
                    IconButton(onClick = { onFavoriteClick(note) }) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteClick(note) }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            val styledContent = remember(note.content, note.styleMetadata) {
                val spans = StyleSerializer.deserialize(note.styleMetadata)
                buildAnnotatedString {
                    append(note.content)
                    spans.forEach { span ->
                        if (span.end <= note.content.length) {
                            addStyle(
                                style = SpanStyle(
                                    fontWeight = if (span.isBold) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (span.isItalic) FontStyle.Italic else FontStyle.Normal
                                ),
                                start = span.start,
                                end = span.end
                            )
                        }
                    }
                }
            }

            Text(
                text = styledContent,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = getFontFamily(note.fontName),
                maxLines = 3
            )

            if (note.tags.isNotEmpty()) {
                val tagList = remember(note.tags) {
                    note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(tagList) { tag ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val dateText = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(note.timestamp))
            Text(dateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class SettingsPage { Root, Privacy, Data }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onChangePinClick: () -> Unit,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(SettingsPage.Root) }

    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var tempPassword by remember { mutableStateOf("") }
    var selectedImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val isClipboardClearEnabled by viewModel.isClipboardClearEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isAutoSaveEnabled by viewModel.isAutoSaveEnabled.collectAsState()
    val isPinLockoutEnabled by viewModel.isPinLockoutEnabled.collectAsState() // NEW
    val canUseBiometrics = remember { viewModel.canUseBiometrics(context) }

    BackHandler(enabled = true) {
        if (currentPage != SettingsPage.Root) {
            currentPage = SettingsPage.Root
        } else {
            onBackClick()
        }
    }

    val currentTimeout by viewModel.autoLockTimeout.collectAsState()
    var showTimeoutDialog by remember { mutableStateOf(false) }
    val timeoutOptions = mapOf(
        0L to "Immediately",
        60_000L to "1 Minute",
        300_000L to "5 Minutes",
        1_800_000L to "30 Minutes",
        -1L to "Never"
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            viewModel.exportEncryptedNotes(it, tempPassword)
            tempPassword = ""
            Toast.makeText(context, "Encrypted Export Started...", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedImportUri = it
            showImportPasswordDialog = true
        }
    }

    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(when(currentPage) {
                        SettingsPage.Root -> "Settings"
                        SettingsPage.Privacy -> "Privacy & Security"
                        SettingsPage.Data -> "Data Management"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPage != SettingsPage.Root) {
                            currentPage = SettingsPage.Root
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            when (currentPage) {
                SettingsPage.Root -> {
                    ListItem(
                        headlineContent = { Text("Privacy & Security") },
                        leadingContent = { Icon(Icons.Default.Security, null) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier
                            .clickable { currentPage = SettingsPage.Privacy }
                            .fillMaxWidth()
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Data Management") },
                        leadingContent = { Icon(Icons.Default.Storage, null) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier
                            .clickable { currentPage = SettingsPage.Data }
                            .fillMaxWidth()
                    )
                    HorizontalDivider()
                }

                SettingsPage.Privacy -> {
                    ListItem(
                        headlineContent = { Text("Change PIN") },
                        leadingContent = { Icon(Icons.Default.Lock, null) },
                        modifier = Modifier
                            .clickable { onChangePinClick() }
                            .fillMaxWidth()
                    )
                    HorizontalDivider()

                    // NEW: Lockout Timer Toggle
                    ListItem(
                        headlineContent = { Text("PIN Lockout Timer") },
                        supportingContent = { Text("Progressive lockout after 5 failed attempts") },
                        leadingContent = { Icon(Icons.Default.Timer, null) },
                        trailingContent = {
                            Switch(
                                checked = isPinLockoutEnabled,
                                onCheckedChange = { viewModel.togglePinLockout() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()

                    if (canUseBiometrics) {
                        ListItem(
                            headlineContent = { Text("Biometric Unlock") },
                            supportingContent = { Text("Use Fingerprint or Face ID") },
                            leadingContent = { Icon(Icons.Default.Fingerprint, null) },
                            trailingContent = {
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { viewModel.toggleBiometricAuth(it) }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider()
                    }

                    ListItem(
                        headlineContent = { Text("Auto-lock Timeout") },
                        supportingContent = { Text(timeoutOptions[currentTimeout] ?: "Immediately") },
                        leadingContent = { Icon(Icons.Default.DateRange, null) },
                        modifier = Modifier
                            .clickable { showTimeoutDialog = true }
                            .fillMaxWidth()
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Clear Clipboard on Exit") },
                        supportingContent = { Text("Prevents leaks to other apps") },
                        leadingContent = { Icon(Icons.Default.ContentPaste, null) },
                        trailingContent = {
                            Switch(
                                checked = isClipboardClearEnabled,
                                onCheckedChange = { viewModel.toggleClipboardClear() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()
                }

                SettingsPage.Data -> {
                    ListItem(
                        headlineContent = { Text("Auto-Save Notes") },
                        supportingContent = { Text("Automatically save drafts while typing") },
                        leadingContent = { Icon(Icons.Default.Save, null) },
                        trailingContent = {
                            Switch(
                                checked = isAutoSaveEnabled,
                                onCheckedChange = { viewModel.toggleAutoSave() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Export Encrypted Backup") },
                        supportingContent = { Text("Save password-protected file") },
                        leadingContent = { Icon(Icons.Default.Upload, null) },
                        modifier = Modifier
                            .clickable {
                                tempPassword = ""
                                showExportPasswordDialog = true
                            }
                            .fillMaxWidth()
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Import Backup") },
                        supportingContent = { Text("Restore from file") },
                        leadingContent = { Icon(Icons.Default.Download, null) },
                        modifier = Modifier
                            .clickable { importLauncher.launch(arrayOf("*/*")) }
                            .fillMaxWidth()
                    )
                    HorizontalDivider()
                }
            }
        }

        if (showExportPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showExportPasswordDialog = false },
                title = { Text("Set Backup Password") },
                text = {
                    Column {
                        Text("Enter a password to encrypt this backup file. You will need this password to restore it.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempPassword,
                            onValueChange = { tempPassword = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (tempPassword.isNotEmpty()) {
                            showExportPasswordDialog = false
                            exportLauncher.launch("notely_backup.enc")
                        }
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showExportPasswordDialog = false }) { Text("Cancel") } }
            )
        }

        if (showImportPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showImportPasswordDialog = false },
                title = { Text("Unlock Backup") },
                text = {
                    Column {
                        Text("Enter the password used to encrypt this file.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempPassword,
                            onValueChange = { tempPassword = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (tempPassword.isNotEmpty() && selectedImportUri != null) {
                            viewModel.importEncryptedNotes(selectedImportUri!!, tempPassword)
                            tempPassword = ""
                            showImportPasswordDialog = false
                            Toast.makeText(context, "Restoring...", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Restore") }
                },
                dismissButton = { TextButton(onClick = { showImportPasswordDialog = false }) { Text("Cancel") } }
            )
        }

        if (showTimeoutDialog) {
            AlertDialog(
                onDismissRequest = { showTimeoutDialog = false },
                title = { Text("Auto-lock Timeout") },
                text = {
                    Column {
                        timeoutOptions.forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setAutoLockTimeout(value)
                                        showTimeoutDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (currentTimeout == value), onClick = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = label)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showTimeoutDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun ChangePinScreen(
    stage: ChangePinStage,
    viewModel: AppViewModel,
    onSuccess: () -> Unit,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(stage) { focusRequester.requestFocus() }
    val title = if (stage == ChangePinStage.VerifyOld) "Enter Current PIN" else "Enter New PIN"

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { newPin ->
                if (newPin.all { it.isDigit() } && newPin.length <= 4) {
                    pin = newPin
                    if (pin.length == 4) {
                        if (stage == ChangePinStage.VerifyOld) {
                            if (viewModel.verifyOldPin(pin)) { pin = ""; error = null; onSuccess() }
                            else { pin = ""; error = "Incorrect PIN" }
                        } else {
                            viewModel.setPin(pin)
                            onFinished()
                        }
                    }
                }
            },
            label = { Text("PIN") },
            textStyle = MaterialTheme.typography.headlineMedium,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error != null,
            singleLine = true,
            modifier = Modifier.width(200.dp).focusRequester(focusRequester)
        )
        if (error != null) { Spacer(modifier = Modifier.height(16.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
fun PinScreen(title: String, viewModel: AppViewModel, isSetup: Boolean) {
    var pin by remember { mutableStateOf("") }
    val error by viewModel.errorMessage.collectAsState()
    val lockoutTime by viewModel.lockoutTimeRemaining.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    val activity = remember(context) { context.findFragmentActivity() }
    val isLockedOut = lockoutTime > 0

    LaunchedEffect(isLockedOut) {
        if (!isSetup && isBiometricEnabled && activity != null && !isLockedOut) {
            viewModel.showBiometricPrompt(activity)
        }
        if (!isLockedOut) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (title.isNotEmpty()) { Text(text = title, style = MaterialTheme.typography.headlineMedium); Spacer(modifier = Modifier.height(32.dp)) }

        OutlinedTextField(
            value = pin,
            onValueChange = { newPin ->
                if (!isLockedOut && newPin.all { it.isDigit() } && newPin.length <= 4) {
                    pin = newPin
                    if (pin.length == 4) {
                        if (isSetup) viewModel.setPin(pin)
                        else {
                            viewModel.checkPin(pin)
                            if (viewModel.isAuthenticated.value == false) pin = ""
                        }
                    }
                }
            },
            label = { Text("Enter PIN") },
            textStyle = MaterialTheme.typography.headlineMedium,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error != null || isLockedOut,
            singleLine = true,
            enabled = !isLockedOut, // Prevents typing while locked out
            modifier = Modifier.width(200.dp).let {
                if (!isLockedOut) it.focusRequester(focusRequester) else it
            }
        )

        if (isLockedOut) {
            Spacer(modifier = Modifier.height(16.dp))
            val hours = lockoutTime / 3600
            val minutes = (lockoutTime % 3600) / 60
            val seconds = lockoutTime % 60

            // Format time accurately since it doubles and can stretch into hours
            val formattedTime = if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }

            Text(
                text = "Locked out. Try again in $formattedTime",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        } else if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        // Keep Biometric Button accessible (Can bypass the PIN timer penalty)
        if (!isSetup && isBiometricEnabled && activity != null) {
            Spacer(modifier = Modifier.height(32.dp))
            IconButton(
                onClick = { viewModel.showBiometricPrompt(activity) },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Fingerprint, "Unlock with Biometrics", modifier = Modifier.fillMaxSize())
            }
        }
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}