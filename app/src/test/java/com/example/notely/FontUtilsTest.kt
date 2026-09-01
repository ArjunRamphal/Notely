package com.example.notely

import org.junit.Test
import org.junit.Assert.*

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
