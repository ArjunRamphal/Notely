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
    fun deserialize_invalidString_ignoresInvalidEntries() {
        val data = "0:5:B,invalid,11:15:BI"
        val expected = listOf(
            StyleSpan(start = 0, end = 5, isBold = true, isItalic = false),
            StyleSpan(start = 11, end = 15, isBold = true, isItalic = true)
        )
        val result = StyleSerializer.deserialize(data)
        assertEquals(expected, result)
    }
}
