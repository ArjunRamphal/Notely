package com.example.notely

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteToEdit: Note?,
    isAutoSaveEnabled: Boolean,
    onAutoSave: (String, String, String, String, String) -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
    onNavigateBack: (String, String, String, String, String) -> Unit,
    onDiscard: () -> Unit
) {
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var tags by remember { mutableStateOf(noteToEdit?.tags ?: "") }
    var currentFontName by remember { mutableStateOf(noteToEdit?.fontName ?: "Default") }
    var showFontMenu by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val availableFonts = listOf("Default", "Modern", "Elegant", "Handwriting", "Code")
    val canBold = remember(currentFontName) { supportsBold(currentFontName) }
    val canItalic = remember(currentFontName) { supportsItalic(currentFontName) }

    val richTextState = remember {
        RichTextState(
            initialText = noteToEdit?.content ?: "",
            initialStyles = noteToEdit?.styleMetadata ?: ""
        )
    }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(richTextState.text)) }

    val secureKeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        autoCorrect = false,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Default
    )

    // --- AUTO-SAVE LOGIC ---
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (isAutoSaveEnabled && (title.isNotBlank() || tags.isNotBlank() || textFieldValue.text.isNotBlank())) {
                    onAutoSave(title, richTextState.text, currentFontName, tags, StyleSerializer.serialize(richTextState.spans))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(title, textFieldValue.text, tags, currentFontName, richTextState.spans) {
        delay(3000)
        if (isAutoSaveEnabled && (title.isNotBlank() || tags.isNotBlank() || textFieldValue.text.isNotBlank())) {
            onAutoSave(title, richTextState.text, currentFontName, tags, StyleSerializer.serialize(richTextState.spans))
        }
    }

    // --- BACK NAVIGATION LOGIC ---
    val handleBackPress = {
        val hasContent = title.isNotBlank() || tags.isNotBlank() || textFieldValue.text.isNotBlank()

        if (!hasContent) {
            // Note is completely empty, silently exit without prompting or saving
            onDiscard()
        } else if (isAutoSaveEnabled) {
            // Note has content and auto-save is enabled, proceed with auto-routing
            onNavigateBack(title, richTextState.text, currentFontName, tags, StyleSerializer.serialize(richTextState.spans))
        } else {
            // Note has content but auto-save is disabled, prompt user
            showDiscardDialog = true
        }
    }

    BackHandler {
        handleBackPress()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteToEdit == null) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = { handleBackPress() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (canBold) {
                        IconButton(onClick = {
                            richTextState.toggleSelection(
                                start = textFieldValue.selection.min,
                                end = textFieldValue.selection.max,
                                toggleBold = true,
                                toggleItalic = false
                            )
                        }) {
                            val tint = if (richTextState.isTypingBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            Text("B", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize, color = tint)
                        }
                    }

                    if (canItalic) {
                        IconButton(onClick = {
                            richTextState.toggleSelection(
                                start = textFieldValue.selection.min,
                                end = textFieldValue.selection.max,
                                toggleBold = false,
                                toggleItalic = true
                            )
                        }) {
                            val tint = if (richTextState.isTypingItalic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            Text("I", fontStyle = FontStyle.Italic, fontSize = MaterialTheme.typography.titleLarge.fontSize, fontFamily = FontFamily.Serif, color = tint)
                        }
                    }

                    Box {
                        IconButton(onClick = { showFontMenu = true }) { Icon(Icons.Default.MoreVert, "Fonts") }
                        DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                            availableFonts.forEach { font ->
                                DropdownMenuItem(
                                    text = { Text(text = font, fontFamily = getFontFamily(font)) },
                                    onClick = {
                                        currentFontName = font
                                        showFontMenu = false
                                        if (!supportsBold(font)) richTextState.isTypingBold = false
                                        if (!supportsItalic(font)) richTextState.isTypingItalic = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = {
                        onSave(title, richTextState.text, currentFontName, tags, StyleSerializer.serialize(richTextState.spans))
                    }) {
                        Icon(Icons.Default.Check, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(16.dp)
                .fillMaxSize()
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = getFontFamily(currentFontName)),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = secureKeyboardOptions.copy(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )

            TextField(
                value = tags,
                onValueChange = { tags = it },
                placeholder = { Text("Tags (comma separated)", style = MaterialTheme.typography.bodyMedium) },
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = secureKeyboardOptions.copy(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val cursorIndex = newValue.selection.start
                    richTextState.onTextChange(newValue.text, cursorIndex)
                    textFieldValue = newValue
                },
                placeholder = { Text("Start typing...") },
                visualTransformation = remember(richTextState.spans, richTextState.text) {
                    VisualTransformation { _ -> TransformedText(richTextState.getAnnotatedString(), OffsetMapping.Identity) }
                },
                keyboardOptions = secureKeyboardOptions,
                textStyle = TextStyle(fontFamily = getFontFamily(currentFontName), fontSize = MaterialTheme.typography.bodyLarge.fontSize, color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )
        }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Unsaved Changes") },
                text = { Text("Auto-save is disabled. Do you want to save your changes before exiting?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDiscardDialog = false
                        onSave(title, richTextState.text, currentFontName, tags, StyleSerializer.serialize(richTextState.spans))
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDiscardDialog = false
                        onDiscard()
                    }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                }
            )
        }
    }
}