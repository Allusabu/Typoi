package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SnippetRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("autotyper_snippets", Context.MODE_PRIVATE)

    private val defaultSnippets = listOf(
        Snippet(
            id = "preset_quick_fox",
            title = "Quick Brown Fox",
            text = "The quick brown fox jumps over the lazy dog.",
            category = "Testing",
            defaultSpeedMs = 60L
        ),
        Snippet(
            id = "preset_emoji_unicode",
            title = "Unicode & Emojis",
            text = "🚀 AutoTyper v1.0 ✨ Typing letter by letter! 👍🏽 🇺🇸 🎮 🔥 漢字 & emojis handled cleanly.",
            category = "Testing",
            defaultSpeedMs = 80L
        ),
        Snippet(
            id = "preset_code",
            title = "Code / Script",
            text = "function calculateTotal(items) {\n  return items.reduce((a, b) => a + b, 0);\n}",
            category = "Development",
            defaultSpeedMs = 40L
        ),
        Snippet(
            id = "preset_email",
            title = "Professional Note",
            text = "Hello,\nThank you for reaching out. The requested task has been completed.\nBest regards,\nAutoTyper",
            category = "Templates",
            defaultSpeedMs = 70L
        ),
        Snippet(
            id = "preset_gamer",
            title = "Gamer Chat",
            text = "GG WP everyone! Great game! 🔥🎯",
            category = "Fun",
            defaultSpeedMs = 30L
        ),
        Snippet(
            id = "preset_symbols",
            title = "Symbols & Numbers",
            text = "0123456789 ~!@#$%^&*()_+ `-={}|[]:\";'<>?,./",
            category = "Testing",
            defaultSpeedMs = 50L
        )
    )

    private val _snippets = MutableStateFlow<List<Snippet>>(emptyList())
    val snippets: StateFlow<List<Snippet>> = _snippets.asStateFlow()

    init {
        loadSnippets()
    }

    private fun loadSnippets() {
        val customJson = prefs.getString(KEY_CUSTOM_SNIPPETS, null)
        val customList = mutableListOf<Snippet>()
        if (!customJson.isNullOrEmpty()) {
            try {
                val array = JSONArray(customJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    customList.add(
                        Snippet(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            text = obj.getString("text"),
                            category = obj.optString("category", "Custom"),
                            defaultSpeedMs = obj.optLong("defaultSpeedMs", 80L),
                            isCustom = true
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        _snippets.value = defaultSnippets + customList
    }

    fun addSnippet(title: String, text: String, category: String = "Custom", speedMs: Long = 80L): Snippet {
        val newSnippet = Snippet(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Untitled" },
            text = text,
            category = category.ifBlank { "Custom" },
            defaultSpeedMs = speedMs,
            isCustom = true
        )
        val currentCustom = _snippets.value.filter { it.isCustom }.toMutableList()
        currentCustom.add(0, newSnippet)
        saveCustomSnippets(currentCustom)
        _snippets.value = defaultSnippets + currentCustom
        return newSnippet
    }

    fun updateSnippet(id: String, title: String, text: String, category: String, speedMs: Long) {
        val currentCustom = _snippets.value.filter { it.isCustom }.toMutableList()
        val index = currentCustom.indexOfFirst { it.id == id }
        if (index != -1) {
            currentCustom[index] = currentCustom[index].copy(
                title = title,
                text = text,
                category = category,
                defaultSpeedMs = speedMs
            )
            saveCustomSnippets(currentCustom)
            _snippets.value = defaultSnippets + currentCustom
        }
    }

    fun deleteSnippet(id: String) {
        val currentCustom = _snippets.value.filter { it.isCustom && it.id != id }
        saveCustomSnippets(currentCustom)
        _snippets.value = defaultSnippets + currentCustom
    }

    private fun saveCustomSnippets(list: List<Snippet>) {
        val array = JSONArray()
        for (snippet in list) {
            val obj = JSONObject().apply {
                put("id", snippet.id)
                put("title", snippet.title)
                put("text", snippet.text)
                put("category", snippet.category)
                put("defaultSpeedMs", snippet.defaultSpeedMs)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_SNIPPETS, array.toString()).apply()
    }

    companion object {
        private const val KEY_CUSTOM_SNIPPETS = "custom_snippets_json"
    }
}
