package com.example.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.inputmethod.InputConnection
import com.example.data.SettingsManager
import com.example.notification.AutoTyperNotificationManager
import com.example.util.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Singleton TypingEngine that handles character-by-character auto-typing into
 * any focused Android InputConnection.
 */
object TypingEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var typingJob: Job? = null
    private var notifJob: Job? = null

    private val _progressState = MutableStateFlow(TypingProgress())
    val progressState: StateFlow<TypingProgress> = _progressState.asStateFlow()

    @Volatile
    private var isPaused = false

    @Volatile
    private var currentSpeedMs: Long = 100L

    // Supplier for the active InputConnection (from InputMethodService or Sandbox test field)
    private var inputConnectionSupplier: (() -> InputConnection?)? = null
    private var appContext: Context? = null
    private var hapticFeedbackEnabled: Boolean = false

    fun init(context: Context) {
        val app = context.applicationContext
        this.appContext = app
        AppLogger.i("TypingEngine", "TypingEngine initialized")

        // Initialize notification channel and reactive notification bar updater
        AutoTyperNotificationManager.createNotificationChannel(app)

        notifJob?.cancel()
        notifJob = engineScope.launch {
            _progressState.collect { progress ->
                val ctx = appContext ?: return@collect
                val settings = SettingsManager(ctx).settings.value
                if (settings.showNotificationControls) {
                    AutoTyperNotificationManager.updateNotification(ctx, progress)
                } else {
                    AutoTyperNotificationManager.cancelNotification(ctx)
                }
            }
        }
    }

    fun refreshNotification() {
        val ctx = appContext ?: return
        val settings = SettingsManager(ctx).settings.value
        if (settings.showNotificationControls) {
            AutoTyperNotificationManager.updateNotification(ctx, _progressState.value)
        } else {
            AutoTyperNotificationManager.cancelNotification(ctx)
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        this.hapticFeedbackEnabled = enabled
    }

    fun setInputConnectionSupplier(supplier: (() -> InputConnection?)?) {
        this.inputConnectionSupplier = supplier
        AppLogger.d("TypingEngine", "InputConnection supplier updated (hasSupplier=${supplier != null})")
    }

    fun updateSpeed(speedMs: Long) {
        val clamped = speedMs.coerceIn(10L, 2000L)
        this.currentSpeedMs = clamped
        _progressState.update { it.copy(speedMs = clamped) }
        AppLogger.d("TypingEngine", "Speed updated to ${clamped}ms")
    }

    fun setLooping(loop: Boolean) {
        _progressState.update { it.copy(isLooping = loop) }
    }

    /**
     * Starts typing character-by-character.
     * Cancels any running job first to prevent duplicate typing.
     */
    fun start(
        text: String,
        speedMs: Long = currentSpeedMs,
        countdownSec: Int = 0,
        isLoop: Boolean = _progressState.value.isLooping
    ) {
        AppLogger.i("TypingEngine", "start() requested. Text length=${text.length}, speed=${speedMs}ms, countdown=${countdownSec}s, loop=$isLoop")
        // Stop any active job immediately
        typingJob?.cancel()
        isPaused = false
        updateSpeed(speedMs)

        val graphemes = UnicodeHelper.splitIntoGraphemes(text)
        if (graphemes.isEmpty()) {
            AppLogger.w("TypingEngine", "start() aborted: Text is empty")
            _progressState.update {
                it.copy(
                    status = TypingStatus.IDLE,
                    current = 0,
                    total = 0,
                    activeText = "",
                    currentGrapheme = "",
                    errorMessage = "Text is empty"
                )
            }
            return
        }

        _progressState.update {
            it.copy(
                current = 0,
                total = graphemes.size,
                status = if (countdownSec > 0) TypingStatus.COUNTDOWN else TypingStatus.TYPING,
                activeText = text,
                currentGrapheme = "",
                countdownRemaining = countdownSec,
                isLooping = isLoop,
                errorMessage = null
            )
        }

        typingJob = engineScope.launch {
            try {
                // Countdown if requested
                if (countdownSec > 0) {
                    AppLogger.d("TypingEngine", "Beginning countdown: $countdownSec seconds")
                    for (c in countdownSec downTo 1) {
                        _progressState.update { it.copy(countdownRemaining = c, status = TypingStatus.COUNTDOWN) }
                        delay(1000L)
                    }
                }

                _progressState.update { it.copy(status = TypingStatus.TYPING, countdownRemaining = 0) }
                AppLogger.i("TypingEngine", "Typing started: ${graphemes.size} characters")

                do {
                    var index = 0
                    while (index < graphemes.size) {
                        // Check for pause
                        while (isPaused) {
                            delay(50L)
                        }

                        val grapheme = graphemes[index]
                        _progressState.update {
                            it.copy(
                                current = index + 1,
                                currentGrapheme = grapheme,
                                status = if (isPaused) TypingStatus.PAUSED else TypingStatus.TYPING
                            )
                        }

                        // Commit character to active InputConnection on Main dispatcher
                        val committed = withContext(Dispatchers.Main) {
                            val ic = inputConnectionSupplier?.invoke()
                            if (ic != null) {
                                ic.commitText(grapheme, 1)
                                true
                            } else {
                                AppLogger.w("TypingEngine", "InputConnection not available at character $index ('${UnicodeHelper.formatDisplayChar(grapheme)}')")
                                false
                            }
                        }

                        if (hapticFeedbackEnabled) {
                            triggerHaptic()
                        }

                        // Wait for configured per-character speed delay
                        delay(currentSpeedMs)
                        index++
                    }

                } while (_progressState.value.isLooping)

                AppLogger.i("TypingEngine", "Typing completed successfully")
                _progressState.update {
                    it.copy(
                        status = TypingStatus.COMPLETED,
                        current = it.total,
                        currentGrapheme = ""
                    )
                }
            } catch (e: CancellationException) {
                AppLogger.i("TypingEngine", "Typing cancelled by user")
                _progressState.update {
                    it.copy(
                        status = TypingStatus.STOPPED,
                        currentGrapheme = ""
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("TypingEngine", "Typing error: ${e.message}", e)
                _progressState.update {
                    it.copy(
                        status = TypingStatus.STOPPED,
                        errorMessage = e.message ?: "Typing error"
                    )
                }
            }
        }
    }

    /**
     * Pauses the active typing process.
     */
    fun pause() {
        if (_progressState.value.status == TypingStatus.TYPING || _progressState.value.status == TypingStatus.COUNTDOWN) {
            isPaused = true
            AppLogger.i("TypingEngine", "Typing paused at character ${_progressState.value.current}/${_progressState.value.total}")
            _progressState.update { it.copy(status = TypingStatus.PAUSED) }
        }
    }

    /**
     * Resumes typing from current character.
     */
    fun resume() {
        if (_progressState.value.status == TypingStatus.PAUSED) {
            isPaused = false
            AppLogger.i("TypingEngine", "Typing resumed")
            _progressState.update { it.copy(status = TypingStatus.TYPING) }
        }
    }

    /**
     * Stops and cancels typing immediately.
     */
    fun stop() {
        isPaused = false
        typingJob?.cancel()
        typingJob = null
        AppLogger.i("TypingEngine", "Typing stopped")
        _progressState.update {
            it.copy(
                status = TypingStatus.STOPPED,
                currentGrapheme = ""
            )
        }
    }

    /**
     * Resets state to Idle.
     */
    fun reset() {
        stop()
        _progressState.update {
            it.copy(
                current = 0,
                total = 0,
                status = TypingStatus.IDLE,
                currentGrapheme = "",
                activeText = "",
                errorMessage = null
            )
        }
    }

    private fun triggerHaptic() {
        val ctx = appContext ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(10)
                }
            }
        } catch (_: Exception) {}
    }
}
