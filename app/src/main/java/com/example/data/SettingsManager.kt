package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val defaultSpeedMs: Long = 80L,
    val countdownSec: Int = 0,
    val hapticFeedback: Boolean = true,
    val loopRepeat: Boolean = false,
    val lastInputText: String = "Hello World! AutoTyper is ready to type letter by letter 🚀"
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("autotyper_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            defaultSpeedMs = prefs.getLong(KEY_SPEED, 80L),
            countdownSec = prefs.getInt(KEY_COUNTDOWN, 0),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            loopRepeat = prefs.getBoolean(KEY_LOOP, false),
            lastInputText = prefs.getString(KEY_LAST_TEXT, "Hello World! AutoTyper is ready to type letter by letter 🚀") ?: ""
        )
    }

    fun updateSpeed(speedMs: Long) {
        prefs.edit().putLong(KEY_SPEED, speedMs).apply()
        _settings.value = _settings.value.copy(defaultSpeedMs = speedMs)
    }

    fun updateCountdown(sec: Int) {
        prefs.edit().putInt(KEY_COUNTDOWN, sec).apply()
        _settings.value = _settings.value.copy(countdownSec = sec)
    }

    fun updateHaptic(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _settings.value = _settings.value.copy(hapticFeedback = enabled)
    }

    fun updateLoop(loop: Boolean) {
        prefs.edit().putBoolean(KEY_LOOP, loop).apply()
        _settings.value = _settings.value.copy(loopRepeat = loop)
    }

    fun saveLastText(text: String) {
        prefs.edit().putString(KEY_LAST_TEXT, text).apply()
        _settings.value = _settings.value.copy(lastInputText = text)
    }

    companion object {
        private const val KEY_SPEED = "pref_speed_ms"
        private const val KEY_COUNTDOWN = "pref_countdown_sec"
        private const val KEY_HAPTIC = "pref_haptic"
        private const val KEY_LOOP = "pref_loop"
        private const val KEY_LAST_TEXT = "pref_last_text"
    }
}
