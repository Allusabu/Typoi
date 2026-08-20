package com.example

import com.example.engine.TypingEngine
import com.example.engine.TypingStatus
import com.example.engine.UnicodeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
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

    @Test
    fun testServiceLifecycleOwnerLifecycle() {
        val owner = com.example.service.ServiceLifecycleOwner()
        owner.onCreate()
        owner.onStart()
        assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, owner.lifecycle.currentState)
        owner.onStop()
        assertEquals(androidx.lifecycle.Lifecycle.State.STARTED, owner.lifecycle.currentState)
        owner.onDestroy()
        assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
    }
}


