package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.SettingsManager
import com.example.engine.TypingEngine
import com.example.engine.TypingStatus

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            ACTION_START -> {
                val currentStatus = TypingEngine.progressState.value.status
                if (currentStatus == TypingStatus.PAUSED) {
                    TypingEngine.resume()
                } else if (currentStatus == TypingStatus.TYPING || currentStatus == TypingStatus.COUNTDOWN) {
                    // Already typing
                } else {
                    val settingsManager = SettingsManager(context)
                    val settings = settingsManager.settings.value
                    val currentText = TypingEngine.progressState.value.activeText
                    val textToType = if (currentText.isNotBlank()) {
                        currentText
                    } else if (settings.lastInputText.isNotBlank()) {
                        settings.lastInputText
                    } else {
                        "Hello World! AutoTyper is active."
                    }
                    TypingEngine.start(
                        text = textToType,
                        speedMs = settings.defaultSpeedMs,
                        countdownSec = settings.countdownSec,
                        isLoop = settings.loopRepeat
                    )
                }
            }
            ACTION_PAUSE -> {
                TypingEngine.pause()
            }
            ACTION_RESUME -> {
                TypingEngine.resume()
            }
            ACTION_STOP -> {
                TypingEngine.stop()
            }
        }
    }

    companion object {
        const val ACTION_START = "com.example.autotyper.ACTION_START"
        const val ACTION_PAUSE = "com.example.autotyper.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.autotyper.ACTION_RESUME"
        const val ACTION_STOP = "com.example.autotyper.ACTION_STOP"
    }
}
