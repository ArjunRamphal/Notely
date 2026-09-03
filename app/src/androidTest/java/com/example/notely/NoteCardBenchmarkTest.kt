package com.example.notely

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureNanoTime

@RunWith(AndroidJUnit4::class)
class NoteCardBenchmarkTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun benchmarkNoteCardRecomposition() {
        val trigger = mutableStateOf(0)
        val note = Note(
            id = 1,
            title = "Test Note",
            content = "This is a test note content.",
            tags = "work, personal, important, urgent, ideas, thoughts, notes",
            timestamp = System.currentTimeMillis(),
            isFavorite = false,
            styleMetadata = "",
            fontName = "Modern"
        )

        composeTestRule.setContent {
            val state = trigger.value
            NoteCard(
                note = note,
                onNoteClick = {},
                onDeleteClick = {},
                onFavoriteClick = {}
            )
        }

        // Warmup
        for (i in 0 until 100) {
            trigger.value = i
            composeTestRule.waitForIdle()
        }

        val iterations = 500
        val times = mutableListOf<Long>()

        for (i in 100 until (100 + iterations)) {
            val time = measureNanoTime {
                trigger.value = i
                composeTestRule.waitForIdle()
            }
            times.add(time)
        }

        val avgTime = times.average()
        println("BENCHMARK_RESULT: Average recomposition time for NoteCard: ${avgTime / 1_000_000.0} ms")
    }
}
