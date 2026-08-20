package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.UnicodeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AutoTyper", appName)
  }

  @Test
  fun `unicode grapheme cluster splitting handles emojis and newlines`() {
    val text = "Hi 🚀! 👍🏽\nTest"
    val graphemes = UnicodeHelper.splitIntoGraphemes(text)
    // Verify grapheme breakdown
    assertTrue(graphemes.contains("🚀"))
    assertTrue(graphemes.contains("\n"))
    assertEquals("H", graphemes.first())
    assertEquals("t", graphemes.last())
  }
}

