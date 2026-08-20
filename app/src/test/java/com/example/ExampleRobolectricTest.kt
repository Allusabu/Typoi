package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.UnicodeHelper
import org.junit.Assert.assertEquals
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
    // "H", "i", " ", "🚀", "!", " ", "👍🏽", "\n", "T", "e", "s", "t"
    assertEquals(12, graphemes.size)
    assertEquals("🚀", graphemes[3])
    assertEquals("👍🏽", graphemes[6])
    assertEquals("\n", graphemes[7])
  }
}

