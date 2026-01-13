package com.example.notely

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

data class StyleSpan(
    val start: Int,
    val end: Int,
    val isBold: Boolean,
    val isItalic: Boolean
)

object StyleSerializer {
    fun deserialize(data: String): List<StyleSpan> {
        if (data.isBlank()) return emptyList()
        return data.split(",").mapNotNull { entry ->
            try {
                val parts = entry.split(":")
                StyleSpan(parts[0].toInt(), parts[1].toInt(), parts[2].contains("B"), parts[2].contains("I"))
            } catch (e: Exception) { null }
        }
    }

    fun serialize(spans: List<StyleSpan>): String {
        return spans.joinToString(",") { span ->
            "${span.start}:${span.end}:${if (span.isBold) "B" else ""}${if (span.isItalic) "I" else ""}"
        }
    }
}

class RichTextState(
    initialText: String,
    initialStyles: String
) {
    var text by androidx.compose.runtime.mutableStateOf(initialText)
    var spans by androidx.compose.runtime.mutableStateOf(StyleSerializer.deserialize(initialStyles))

    // Typing toggles
    var isTypingBold by androidx.compose.runtime.mutableStateOf(false)
    var isTypingItalic by androidx.compose.runtime.mutableStateOf(false)

    fun getAnnotatedString(): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            spans.forEach { span ->
                val start = span.start.coerceIn(0, text.length)
                val end = span.end.coerceIn(0, text.length)
                if (start < end) {
                    addStyle(
                        style = SpanStyle(
                            fontWeight = if (span.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (span.isItalic) FontStyle.Italic else FontStyle.Normal
                        ),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    fun onTextChange(newText: String, cursorIndex: Int) {
        val diff = newText.length - text.length

        // 1. Shift existing spans
        val shiftedSpans = spans.mapNotNull { span ->
            if (span.start >= cursorIndex - diff) {
                span.copy(start = span.start + diff, end = span.end + diff)
            } else if (span.end > cursorIndex - diff) {
                if (diff < 0) { // Deletion
                    val newEnd = (span.end + diff).coerceAtLeast(span.start)
                    if (newEnd == span.start) null else span.copy(end = newEnd)
                } else { // Insertion
                    span.copy(end = span.end + diff)
                }
            } else {
                span
            }
        }

        // 2. Add new typing style if strictly inserting text
        var finalSpans = shiftedSpans.toMutableList()
        if (diff > 0 && (isTypingBold || isTypingItalic)) {
            finalSpans.add(StyleSpan(cursorIndex - diff, cursorIndex, isTypingBold, isTypingItalic))
        }

        // 3. Clean up
        spans = mergeSpans(finalSpans)
        text = newText
    }

    // --- THE FIXED SELECTION LOGIC ---
    fun toggleSelection(start: Int, end: Int, toggleBold: Boolean, toggleItalic: Boolean) {
        // 1. Handle Cursor (Typing Mode)
        if (start == end) {
            if (toggleBold) isTypingBold = !isTypingBold
            if (toggleItalic) isTypingItalic = !isTypingItalic
            return
        }

        // 2. Handle Selection (Apply/Remove Style)
        // Logic: If ANY part of selection is NOT styled -> Apply Style.
        // Only if ALL of selection IS styled -> Remove Style.

        // Break timeline into atomic segments
        val boundaries = sortedSetOf(start, end)
        spans.forEach {
            if (it.start in start..end) boundaries.add(it.start)
            if (it.end in start..end) boundaries.add(it.end)
        }

        val segments = boundaries.toList().windowed(2).map { (s, e) ->
            val existing = spans.find { it.start <= s && it.end >= e }
            Triple(s, e, existing) // Start, End, ExistingSpan?
        }

        // Determine Target State
        val targetBold = if (toggleBold) !segments.all { (_, _, span) -> span?.isBold == true } else null
        val targetItalic = if (toggleItalic) !segments.all { (_, _, span) -> span?.isItalic == true } else null

        // Rebuild Spans for the selection range
        val newSpans = spans.filterNot { it.start < end && it.end > start }.toMutableList()

        spans.forEach { span ->
            if (span.start < start && span.end > start) newSpans.add(span.copy(end = start)) // Left chunk
            if (span.start < end && span.end > end) newSpans.add(span.copy(start = end)) // Right chunk
        }

        // Apply new styles to segments
        segments.forEach { (s, e, existing) ->
            val isB = if (toggleBold) targetBold!! else (existing?.isBold ?: false)
            val isI = if (toggleItalic) targetItalic!! else (existing?.isItalic ?: false)
            if (isB || isI) {
                newSpans.add(StyleSpan(s, e, isB, isI))
            }
        }

        spans = mergeSpans(newSpans)
    }

    private fun mergeSpans(raw: List<StyleSpan>): List<StyleSpan> {
        if (raw.isEmpty()) return emptyList()
        val sorted = raw.sortedBy { it.start }
        val merged = mutableListOf<StyleSpan>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            // If overlapping/adjacent AND styles match -> Merge
            if (next.start <= current.end && next.isBold == current.isBold && next.isItalic == current.isItalic) {
                current = current.copy(end = maxOf(current.end, next.end))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }
}