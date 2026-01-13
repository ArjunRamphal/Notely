package com.example.notely

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontListFontFamily

// --- 1. MODERN FONT FAMILY (Nunito) ---
val ModernFontFamily = FontFamily(
    // Regular & Italic
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_italic, FontWeight.Normal, FontStyle.Italic),

    // Light (300)
    Font(R.font.nunito_light, FontWeight.Light),
    Font(R.font.nunito_lightitalic, FontWeight.Light, FontStyle.Italic),

    // Extra Light (200)
    Font(R.font.nunito_extralight, FontWeight.ExtraLight),
    Font(R.font.nunito_extralightitalic, FontWeight.ExtraLight, FontStyle.Italic),

    // Medium (500)
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_mediumitalic, FontWeight.Medium, FontStyle.Italic),

    // SemiBold (600)
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),

    // Bold (700)
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_bolditalic, FontWeight.Bold, FontStyle.Italic),

    // Extra Bold (800)
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),

    // Black (900)
    Font(R.font.nunito_black, FontWeight.Black),
    Font(R.font.nunito_blackitalic, FontWeight.Black, FontStyle.Italic)
)

// --- 2. ELEGANT FONT FAMILY (Playfair Display) ---
val ElegantFontFamily = FontFamily(
    // Regular & Italic
    Font(R.font.playfairdisplay_regular, FontWeight.Normal),
    Font(R.font.playfairdisplay_italic, FontWeight.Normal, FontStyle.Italic),

    // Medium (500)
    Font(R.font.playfairdisplay_medium, FontWeight.Medium),
    Font(R.font.playfairdisplay_mediumitalic, FontWeight.Medium, FontStyle.Italic),

    // SemiBold (600)
    Font(R.font.playfairdisplay_semibold, FontWeight.SemiBold),
    Font(R.font.playfairdisplay_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),

    // Bold (700)
    Font(R.font.playfairdisplay_bold, FontWeight.Bold),
    Font(R.font.playfairdisplay_bolditalic, FontWeight.Bold, FontStyle.Italic),

    // Extra Bold (800)
    Font(R.font.playfairdisplay_extrabold, FontWeight.ExtraBold),
    Font(R.font.playfairdisplay_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),

    // Black (900)
    Font(R.font.playfairdisplay_black, FontWeight.Black),
    Font(R.font.playfairdisplay_blackitalic, FontWeight.Black, FontStyle.Italic)
)

// --- 3. HANDWRITING FONT FAMILY (Pacifico) ---
val HandwritingFontFamily = FontFamily(
    Font(R.font.pacifico_regular, FontWeight.Normal)
)

// --- HELPER FUNCTION ---
fun getFontFamily(fontName: String): FontFamily {
    return when (fontName) {
        "Modern" -> ModernFontFamily
        "Elegant" -> ElegantFontFamily
        "Handwriting" -> HandwritingFontFamily
        "Code" -> FontFamily.Monospace
        else -> FontFamily.Default
    }
}

// LOGIC: Checks if the font family actually contains a Bold file
fun supportsBold(fontName: String): Boolean {
    val family = getFontFamily(fontName)

    // If it's a custom font (defined by a list of files)
    if (family is FontListFontFamily) {
        // Return true if ANY font in the list is SemiBold(600) or heavier
        return family.fonts.any { it.weight >= FontWeight.SemiBold }
    }

    // If it's a System Font (Default, Serif), Android can always synthesize bold
    return true
}

// LOGIC: Checks if the font family actually contains an Italic file
fun supportsItalic(fontName: String): Boolean {
    val family = getFontFamily(fontName)

    if (family is FontListFontFamily) {
        // Return true if ANY font in the list is explicitly Italic
        return family.fonts.any { it.style == FontStyle.Italic }
    }

    // System fonts allow synthetic italics
    return true
}