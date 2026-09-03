package com.example.notely

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// 1. Standard tests for supportsBold()
class FontUtilsTest {
    @Test
    fun supportsBold_withModernFont_returnsTrue() {
        assertTrue(supportsBold("Modern"))
    }

    @Test
    fun supportsBold_withElegantFont_returnsTrue() {
        assertTrue(supportsBold("Elegant"))
    }

    @Test
    fun supportsBold_withHandwritingFont_returnsFalse() {
        assertFalse(supportsBold("Handwriting"))
    }

    @Test
    fun supportsBold_withCodeFont_returnsTrue() {
        assertTrue(supportsBold("Code"))
    }

    @Test
    fun supportsBold_withUnknownFont_returnsTrue() {
        assertTrue(supportsBold("Unknown"))
    }
}

// 2. Parameterized tests for getFontFamily()
@RunWith(Parameterized::class)
class FontUtilsParameterizedTest(private val fontName: String, private val expectedFamily: FontFamily) {

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