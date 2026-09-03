package com.example.notely

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FontUtilsTest(private val fontName: String, private val expectedFamily: FontFamily) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: getFontFamily({0}) = {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Modern", ModernFontFamily),
                arrayOf("Elegant", ElegantFontFamily),
                arrayOf("Handwriting", HandwritingFontFamily),
                arrayOf("Code", FontFamily.Monospace),
                arrayOf("Unknown", FontFamily.Default),
                arrayOf("", FontFamily.Default),
                arrayOf("modern", FontFamily.Default)
            )
        }
    }

    @Test
    fun testGetFontFamily() {
        val actualFamily = getFontFamily(fontName)
        assertEquals(expectedFamily, actualFamily)
    }
}
