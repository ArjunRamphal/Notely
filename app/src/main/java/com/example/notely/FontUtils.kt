package com.example.notely

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

fun getFontFamily(fontName: String): FontFamily {
    return when (fontName) {
        // Matches "nunito_regular.ttf"
        "Modern" -> FontFamily(Font(R.font.nunito_regular, FontWeight.Normal))

        // Matches "playfairdisplay_regular.ttf" (from your screenshot)
        "Elegant" -> FontFamily(Font(R.font.playfairdisplay_regular, FontWeight.Normal))

        // Matches "pacifico_regular.ttf"
        "Handwriting" -> FontFamily(Font(R.font.pacifico_regular, FontWeight.Normal))

        // System default
        "Code" -> FontFamily.Monospace

        else -> FontFamily.Default
    }
}