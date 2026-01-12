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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteToEdit: Note?,
    onSave: (String, String, String) -> Unit, // Accepts: Title, Content, FontName
    onCancel: () -> Unit
) {
    // 1. STATE VARIABLES
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var content by remember { mutableStateOf(noteToEdit?.content ?: "") }

    // Default to "Default" font if none is saved
    var currentFontName by remember { mutableStateOf(noteToEdit?.fontName ?: "Default") }
    var showFontMenu by remember { mutableStateOf(false) }

    // This list must match the keys in FontUtils.kt
    val availableFonts = listOf("Default", "Modern", "Elegant", "Handwriting", "Code")

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
                    // --- FONT SELECTOR MENU ---
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
                                    }
                                )
                            }
                        }
                    }

                    // --- SAVE BUTTON ---
                    IconButton(onClick = { onSave(title, content, currentFontName) }) {
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
            // 2. TITLE INPUT
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = getFontFamily(currentFontName) // Live Preview of Font
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. CONTENT INPUT
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text(
                        "Start typing...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = TextStyle(
                    fontFamily = getFontFamily(currentFontName), // Live Preview of Font
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