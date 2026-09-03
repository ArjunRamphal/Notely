package com.example.notely

import org.junit.Assert.assertEquals
import org.junit.Test

class StyleSerializerTest {

    @Test
    fun deserialize_emptyOrBlankString_returnsEmptyList() {
        assertEquals(emptyList<StyleSpan>(), StyleSerializer.deserialize(""))
        assertEquals(emptyList<StyleSpan>(), StyleSerializer.deserialize("   "))
    }

    @Test
    fun deserialize_validData_returnsSpans() {
        // start:end:BI -> Bold, Italic
        val data = "0:5:B,6:10:I,11:15:BI,16:20:"
        val result = StyleSerializer.deserialize(data)

        assertEquals(4, result.size)

        // 0:5:B
        assertEquals(0, result[0].start)
        assertEquals(5, result[0].end)
        assertEquals(true, result[0].isBold)
        assertEquals(false, result[0].isItalic)

        // 6:10:I
        assertEquals(6, result[1].start)
        assertEquals(10, result[1].end)
        assertEquals(false, result[1].isBold)
        assertEquals(true, result[1].isItalic)

        // 11:15:BI
        assertEquals(11, result[2].start)
        assertEquals(15, result[2].end)
        assertEquals(true, result[2].isBold)
        assertEquals(true, result[2].isItalic)

        // 16:20:
        assertEquals(16, result[3].start)
        assertEquals(20, result[3].end)
        assertEquals(false, result[3].isBold)
        assertEquals(false, result[3].isItalic)
    }

    @Test
    fun deserialize_malformedData_ignoresInvalidEntries() {
        // "abc" is totally invalid
        // "0:5" missing flags part
        // "start:end:B" non-integer coordinates
        val data = "abc,0:5,start:end:B"
        val result = StyleSerializer.deserialize(data)

        assertEquals(emptyList<StyleSpan>(), result)
    }

    @Test
    fun deserialize_mixedValidAndInvalid_parsesValidOnly() {
        val data = "invalid_entry,0:5:B,another_invalid,6:10:I"
        val result = StyleSerializer.deserialize(data)

        assertEquals(2, result.size)

        assertEquals(0, result[0].start)
        assertEquals(5, result[0].end)
        assertEquals(true, result[0].isBold)
        assertEquals(false, result[0].isItalic)

        assertEquals(6, result[1].start)
        assertEquals(10, result[1].end)
        assertEquals(false, result[1].isBold)
        assertEquals(true, result[1].isItalic)
    }

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