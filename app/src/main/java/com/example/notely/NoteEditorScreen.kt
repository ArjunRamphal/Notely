package com.example.notely

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteToEdit: Note?,
    onSave: (String, String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    // 1. STATE VARIABLES
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var tags by remember { mutableStateOf(noteToEdit?.tags ?: "") }
    var currentFontName by remember { mutableStateOf(noteToEdit?.fontName ?: "Default") }
    var showFontMenu by remember { mutableStateOf(false) }

    val availableFonts = listOf("Default", "Modern", "Elegant", "Handwriting", "Code")

    // --- DYNAMIC CAPABILITY CHECK ---
    val canBold = remember(currentFontName) { supportsBold(currentFontName) }
    val canItalic = remember(currentFontName) { supportsItalic(currentFontName) }

    // 2. RICH TEXT STATE INITIALIZATION
    val richTextState = remember {
        RichTextState(
            initialText = noteToEdit?.content ?: "",
            initialStyles = noteToEdit?.styleMetadata ?: ""
        )
    }

    // This local state tracks the TextField's content and cursor
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(richTextState.text))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteToEdit == null) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // --- BOLD BUTTON ---
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
                            Text(
                                "B",
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                color = tint
                            )
                        }
                    }

                    // --- ITALIC BUTTON ---
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
                            Text(
                                "I",
                                fontStyle = FontStyle.Italic,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                fontFamily = FontFamily.Serif,
                                color = tint
                            )
                        }
                    }

                    // --- FONT MENU ---
                    Box {
                        IconButton(onClick = { showFontMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Fonts")
                        }
                        DropdownMenu(
                            expanded = showFontMenu,
                            onDismissRequest = { showFontMenu = false }
                        ) {
                            availableFonts.forEach { font ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = font, fontFamily = getFontFamily(font))
                                    },
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

                    // --- SAVE BUTTON ---
                    IconButton(onClick = {
                        onSave(
                            title,
                            richTextState.text,
                            currentFontName,
                            tags,
                            StyleSerializer.serialize(richTextState.spans)
                        )
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
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // TITLE
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text("Title", style = MaterialTheme.typography.headlineSmall)
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = getFontFamily(currentFontName)
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // TAGS INPUT
            TextField(
                value = tags,
                onValueChange = { tags = it },
                placeholder = { Text("Tags (comma separated)", style = MaterialTheme.typography.bodyMedium) },
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CONTENT (RICH TEXT EDITOR)
            TextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val cursorIndex = newValue.selection.start
                    richTextState.onTextChange(newValue.text, cursorIndex)
                    textFieldValue = newValue
                },
                placeholder = { Text("Start typing...") },

                visualTransformation = remember(richTextState.spans, richTextState.text) {
                    VisualTransformation { _ ->
                        TransformedText(
                            richTextState.getAnnotatedString(),
                            OffsetMapping.Identity
                        )
                    }
                },

                textStyle = TextStyle(
                    fontFamily = getFontFamily(currentFontName),
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxSize(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}