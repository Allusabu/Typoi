package com.example.engine

enum class TypingStatus(val label: String) {
    IDLE("Idle"),
    COUNTDOWN("Starting..."),
    TYPING("Typing"),
    PAUSED("Paused"),
    STOPPED("Stopped"),
    COMPLETED("Completed")
}

data class TypingProgress(
    val current: Int = 0,
    val total: Int = 0,
    val status: TypingStatus = TypingStatus.IDLE,
    val speedMs: Long = 100L,
    val currentGrapheme: String = "",
    val activeText: String = "",
    val countdownRemaining: Int = 0,
    val isLooping: Boolean = false,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val progressLabel: String
        get() = "$current / $total characters ($progressPercent%)"
}
