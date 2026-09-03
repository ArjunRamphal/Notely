package com.example.notely

import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextHelperTest {

    @Test
    fun serialize_emptyList_returnsEmptyString() {
        val result = StyleSerializer.serialize(emptyList())
        assertEquals("", result)
    }

    @Test
    fun serialize_singleSpan_returnsCorrectString() {
        val spans = listOf(StyleSpan(start = 0, end = 5, isBold = true, isItalic = false))
        val result = StyleSerializer.serialize(spans)
        assertEquals("0:5:B", result)
    }

    @Test
    fun serialize_multipleSpans_returnsCorrectString() {
        val spans = listOf(
            StyleSpan(start = 0, end = 5, isBold = true, isItalic = false),
            StyleSpan(start = 6, end = 10, isBold = false, isItalic = true),
            StyleSpan(start = 11, end = 15, isBold = true, isItalic = true),
            StyleSpan(start = 16, end = 20, isBold = false, isItalic = false)
        )
        val result = StyleSerializer.serialize(spans)
        assertEquals("0:5:B,6:10:I,11:15:BI,16:20:", result)
    }

    @Test
    fun deserialize_emptyString_returnsEmptyList() {
        val result = StyleSerializer.deserialize("")
        assertEquals(emptyList<StyleSpan>(), result)
    }

    @Test
    fun deserialize_validString_returnsCorrectSpans() {
        val data = "0:5:B,6:10:I,11:15:BI,16:20:"
        val expected = listOf(
            StyleSpan(start = 0, end = 5, isBold = true, isItalic = false),
            StyleSpan(start = 6, end = 10, isBold = false, isItalic = true),
            StyleSpan(start = 11, end = 15, isBold = true, isItalic = true),
            StyleSpan(start = 16, end = 20, isBold = false, isItalic = false)
        )
        val result = StyleSerializer.deserialize(data)
        assertEquals(expected, result)
    }

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