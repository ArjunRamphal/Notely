package com.example.notely

import org.junit.Assert.assertEquals
import org.junit.Test

class StyleSerializerTest {

    @Test
    fun serialize_emptyList_returnsEmptyString() {
        val spans = emptyList<StyleSpan>()
        val result = StyleSerializer.serialize(spans)
        assertEquals("", result)
    }

    @Test
    fun serialize_singleSpanNoStyles_returnsBasicFormat() {
        val spans = listOf(StyleSpan(start = 0, end = 5, isBold = false, isItalic = false))
        val result = StyleSerializer.serialize(spans)
        assertEquals("0:5:", result)
    }

    @Test
    fun serialize_singleSpanBold_returnsBoldFormat() {
        val spans = listOf(StyleSpan(start = 2, end = 8, isBold = true, isItalic = false))
        val result = StyleSerializer.serialize(spans)
        assertEquals("2:8:B", result)
    }

    @Test
    fun serialize_singleSpanItalic_returnsItalicFormat() {
        val spans = listOf(StyleSpan(start = 10, end = 15, isBold = false, isItalic = true))
        val result = StyleSerializer.serialize(spans)
        assertEquals("10:15:I", result)
    }

    @Test
    fun serialize_singleSpanBoldAndItalic_returnsCombinedFormat() {
        val spans = listOf(StyleSpan(start = 5, end = 12, isBold = true, isItalic = true))
        val result = StyleSerializer.serialize(spans)
        assertEquals("5:12:BI", result)
    }

    @Test
    fun serialize_multipleSpans_returnsCommaSeparatedString() {
        val spans = listOf(
            StyleSpan(start = 0, end = 4, isBold = true, isItalic = false),
            StyleSpan(start = 5, end = 9, isBold = false, isItalic = true),
            StyleSpan(start = 10, end = 15, isBold = true, isItalic = true)
        )
        val result = StyleSerializer.serialize(spans)
        assertEquals("0:4:B,5:9:I,10:15:BI", result)
    }
}
