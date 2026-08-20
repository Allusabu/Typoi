package com.example.engine

import java.text.BreakIterator

object UnicodeHelper {

    /**
     * Splits a text string into individual Unicode grapheme clusters.
     * This correctly keeps surrogate pairs (emojis), skin tone modifiers,
     * zero-width joiners (ZWJ sequences), flags, combining accents, newlines,
     * and spaces intact as single atomic typing units.
     */
    fun splitIntoGraphemes(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val boundary = BreakIterator.getCharacterInstance()
        boundary.setText(text)
        val graphemes = mutableListOf<String>()
        var start = boundary.first()
        var end = boundary.next()
        while (end != BreakIterator.DONE) {
            graphemes.add(text.substring(start, end))
            start = end
            end = boundary.next()
        }
        return graphemes
    }

    /**
     * Returns a human-readable display representation for special characters.
     */
    fun formatDisplayChar(char: String): String {
        return when (char) {
            " " -> "␣ [Space]"
            "\n" -> "↵ [Enter]"
            "\t" -> "⇥ [Tab]"
            else -> char
        }
    }
}
