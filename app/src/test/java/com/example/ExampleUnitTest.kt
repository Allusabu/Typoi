package com.example

import com.example.engine.TypingEngine
import com.example.engine.TypingStatus
import com.example.engine.UnicodeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testUnicodeGraphemeSplitting() {
        val text = "Hello 🚀👨‍👩‍👧‍👦 world!"
        val graphemes = UnicodeHelper.splitIntoGraphemes(text)
        assertTrue(graphemes.isNotEmpty())
        assertEquals("H", graphemes[0])
    }

    @Test
    fun testTypingEngineStateTransitions() {
        TypingEngine.reset()
        assertEquals(TypingStatus.IDLE, TypingEngine.progressState.value.status)

        TypingEngine.updateSpeed(120L)
        assertEquals(120L, TypingEngine.progressState.value.speedMs)

        TypingEngine.stop()
        assertEquals(TypingStatus.STOPPED, TypingEngine.progressState.value.status)
    }
}

