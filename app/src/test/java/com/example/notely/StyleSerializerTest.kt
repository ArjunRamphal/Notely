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
        assertEquals("", StyleSerializer.serialize(emptyList()))
    }

    @Test
    fun serialize_validSpans_returnsString() {
        val spans = listOf(
            StyleSpan(0, 5, isBold = true, isItalic = false),
            StyleSpan(6, 10, isBold = false, isItalic = true),
            StyleSpan(11, 15, isBold = true, isItalic = true),
            StyleSpan(16, 20, isBold = false, isItalic = false)
        )

        val result = StyleSerializer.serialize(spans)

        assertEquals("0:5:B,6:10:I,11:15:BI,16:20:", result)
    }
}
