package com.example.notely

import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextHelperTest {

    @Test
    fun deserialize_withInvalidData_ignoresInvalidEntries() {
        // Let's pass a completely invalid string
        val result1 = StyleSerializer.deserialize("invalid")
        assertEquals("Should return empty list for completely invalid data", emptyList<StyleSpan>(), result1)

        // Let's pass partial invalid
        val result2 = StyleSerializer.deserialize("invalid,0:5:BI")
        assertEquals("Should ignore invalid part and parse valid part", 1, result2.size)
        assertEquals(0, result2[0].start)
        assertEquals(5, result2[0].end)
        assertEquals(true, result2[0].isBold)
        assertEquals(true, result2[0].isItalic)

        // Out of bounds (missing part 2)
        val result3 = StyleSerializer.deserialize("0:5")
        assertEquals(emptyList<StyleSpan>(), result3)

        // Not an integer
        val result4 = StyleSerializer.deserialize("a:5:B")
        assertEquals(emptyList<StyleSpan>(), result4)
    }
}
