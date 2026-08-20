package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO,
    WARN,
    ERROR,
    DEBUG,
    IME
}

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwableString: String? = null
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

object AppLogger {
    private const val MAX_LOGS = 500
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    init {
        log(LogLevel.INFO, "AppLogger", "Logger initialized on Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) - ${Build.MANUFACTURER} ${Build.MODEL}")
    }

    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String, tr: Throwable? = null) = log(LogLevel.ERROR, tag, message, tr)
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun ime(tag: String, message: String) = log(LogLevel.IME, tag, message)

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String, tr: Throwable? = null) {
        val stackTrace = tr?.let { 
            try {
                Log.getStackTraceString(it)
            } catch (_: Throwable) {
                it.stackTraceToString()
            }
        }
        try {
            when (level) {
                LogLevel.INFO -> Log.i(tag, message, tr)
                LogLevel.WARN -> Log.w(tag, message, tr)
                LogLevel.ERROR -> Log.e(tag, message, tr)
                LogLevel.DEBUG -> Log.d(tag, message, tr)
                LogLevel.IME -> Log.i("IME-$tag", message, tr)
            }
        } catch (_: Throwable) {
            // Unmocked Log in plain JVM tests
            println("[$level] [$tag] $message")
        }

        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            throwableString = stackTrace
        )

        val current = _logs.value.toMutableList()
        if (current.size >= MAX_LOGS) {
            current.removeAt(0)
        }
        current.add(entry)
        _logs.value = current
    }

    fun clear() {
        _logs.value = emptyList()
        log(LogLevel.INFO, "AppLogger", "Logs cleared by user")
    }

    fun getAllLogsFormatted(context: Context? = null): String {
        val sb = StringBuilder()
        sb.append("=== AutoTyper System & Diagnostic Logs ===\n")
        sb.append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n")
        sb.append("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        sb.append("App Version: 1.0\n")
        sb.append("Export Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("------------------------------------------\n\n")

        val currentLogs = _logs.value
        if (currentLogs.isEmpty()) {
            sb.append("(No logs recorded yet)\n")
        } else {
            for (entry in currentLogs) {
                sb.append("[${entry.formattedTime}] [${entry.level.name}] [${entry.tag}] ${entry.message}\n")
                if (!entry.throwableString.isNullOrEmpty()) {
                    sb.append(entry.throwableString).append("\n")
                }
            }
        }
        return sb.toString()
    }

    fun copyLogsToClipboard(context: Context) {
        val formatted = getAllLogsFormatted(context)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("AutoTyper Logs", formatted)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Logs copied to clipboard (${_logs.value.size} entries)", Toast.LENGTH_SHORT).show()
    }
}
