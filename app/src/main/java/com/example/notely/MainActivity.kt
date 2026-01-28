package com.example.notely

import android.app.Activity
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

enum class AppScreen { NotesList, Editor, Settings, ChangePin }
enum class ChangePinStage { VerifyOld, SetNew }

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Secure flag to prevent screenshots/recent apps preview
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

    // Navigation State
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
                BackHandler {
                    currentScreen = AppScreen.NotesList
                    currentNote = null
                }
                NoteEditorScreen(
                    noteToEdit = currentNote,
                    onSave = { title, content, fontName, tags, styleData ->
                        if (currentNote == null) {
                            viewModel.addNote(title, content, fontName, tags, styleData)
                        } else {
                            viewModel.updateNote(
                                currentNote!!.copy(
                                    title = title,
                                    content = content,
                                    fontName = fontName,
                                    tags = tags,
                                    styleMetadata = styleData
                                )
                            )
                        }
                        currentScreen = AppScreen.NotesList
                        currentNote = null
                    },
                    onCancel = {
                        currentScreen = AppScreen.NotesList
                        currentNote = null
                    }
                )
            }
            AppScreen.Settings -> {
                BackHandler {
                    currentScreen = AppScreen.NotesList
                }
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
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val tagList = note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onChangePinClick: () -> Unit,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val currentTimeout by viewModel.autoLockTimeout.collectAsState()
    var showTimeoutDialog by remember { mutableStateOf(false) }
    val timeoutOptions = mapOf(
        0L to "Immediately",
        60_000L to "1 Minute",
        300_000L to "5 Minutes",
        1_800_000L to "30 Minutes",
        -1L to "Never"
    )

    // --- EXPORT LAUNCHER ---
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportNotes(it)
            Toast.makeText(context, "Exporting notes...", Toast.LENGTH_SHORT).show()
        }
    }

    // --- IMPORT LAUNCHER ---
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importNotes(it)
            Toast.makeText(context, "Importing notes...", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // PIN SETTINGS
            ListItem(
                headlineContent = { Text("Change PIN") },
                leadingContent = { Icon(Icons.Default.Lock, null) },
                modifier = Modifier.clickable { onChangePinClick() }.fillMaxWidth()
            )
            HorizontalDivider()

            // AUTO LOCK SETTINGS
            ListItem(
                headlineContent = { Text("Auto-lock Timeout") },
                supportingContent = { Text(timeoutOptions[currentTimeout] ?: "Immediately") },
                leadingContent = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.clickable { showTimeoutDialog = true }.fillMaxWidth()
            )
            HorizontalDivider()

            // DATA MANAGEMENT
            Spacer(modifier = Modifier.height(16.dp))
            Text("Data Management", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // EXPORT
            ListItem(
                headlineContent = { Text("Export Notes") },
                supportingContent = { Text("Save backup to JSON") },
                leadingContent = { Icon(Icons.Default.Upload, null) },
                modifier = Modifier.clickable { exportLauncher.launch("notely_backup.json") }.fillMaxWidth()
            )

            // IMPORT
            ListItem(
                headlineContent = { Text("Import Notes") },
                supportingContent = { Text("Restore from JSON") },
                leadingContent = { Icon(Icons.Default.Download, null) },
                modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json")) }.fillMaxWidth()
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
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.setAutoLockTimeout(value); showTimeoutDialog = false }.padding(vertical = 12.dp),
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
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (title.isNotEmpty()) { Text(text = title, style = MaterialTheme.typography.headlineMedium); Spacer(modifier = Modifier.height(32.dp)) }
        OutlinedTextField(
            value = pin,
            onValueChange = { newPin ->
                if (newPin.all { it.isDigit() } && newPin.length <= 4) {
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
            isError = error != null,
            singleLine = true,
            modifier = Modifier.width(200.dp).focusRequester(focusRequester)
        )
        if (error != null) { Spacer(modifier = Modifier.height(16.dp)); Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}