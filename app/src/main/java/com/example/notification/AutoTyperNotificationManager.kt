package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.TypingProgress
import com.example.engine.TypingStatus

object AutoTyperNotificationManager {

    const val CHANNEL_ID = "autotyper_controls_channel"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AutoTyper Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quick Start, Pause, and Stop controls for character-by-character auto typing"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun updateNotification(context: Context, state: TypingProgress) {
        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_START
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val contentText: String
        val isOngoing: Boolean

        when (state.status) {
            TypingStatus.COUNTDOWN -> {
                title = "AutoTyper: Starting in ${state.countdownRemaining}s..."
                contentText = if (state.activeText.isNotBlank()) "Target: \"${state.activeText.take(35)}...\"" else "Preparing to type..."
                isOngoing = true
            }
            TypingStatus.TYPING -> {
                val previewChar = if (state.currentGrapheme.isNotBlank()) " [typing '${state.currentGrapheme}']" else ""
                title = "AutoTyper: Typing (${state.progressPercent}%)"
                contentText = "${state.current}/${state.total} characters$previewChar"
                isOngoing = true
            }
            TypingStatus.PAUSED -> {
                title = "AutoTyper: Paused (${state.progressPercent}%)"
                contentText = "Paused at char ${state.current}/${state.total}. Tap Resume or Stop."
                isOngoing = true
            }
            TypingStatus.COMPLETED -> {
                title = "AutoTyper: Completed ✅"
                contentText = "Typed all ${state.total} characters. Tap Start to type again."
                isOngoing = false
            }
            TypingStatus.STOPPED -> {
                title = "AutoTyper: Stopped"
                contentText = if (state.activeText.isNotBlank()) "Ready: \"${state.activeText.take(35)}...\"" else "Tap Start to auto-type text."
                isOngoing = false
            }
            TypingStatus.IDLE -> {
                title = "AutoTyper: Ready"
                contentText = if (state.activeText.isNotBlank()) "Ready: \"${state.activeText.take(35)}...\"" else "Quick controls in notification bar."
                isOngoing = false
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_keyboard)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isOngoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add progress bar when typing, paused, or countdown
        if (state.status == TypingStatus.TYPING || state.status == TypingStatus.PAUSED) {
            if (state.total > 0) {
                builder.setProgress(state.total, state.current, false)
            }
        } else if (state.status == TypingStatus.COUNTDOWN) {
            builder.setProgress(0, 0, true)
        }

        // Add Notification Action Buttons (Start / Pause / Resume / Stop)
        when (state.status) {
            TypingStatus.TYPING, TypingStatus.COUNTDOWN -> {
                builder.addAction(
                    R.drawable.ic_notif_pause,
                    "Pause",
                    pausePendingIntent
                )
                builder.addAction(
                    R.drawable.ic_notif_stop,
                    "Stop",
                    stopPendingIntent
                )
            }
            TypingStatus.PAUSED -> {
                builder.addAction(
                    R.drawable.ic_notif_play,
                    "Resume",
                    resumePendingIntent
                )
                builder.addAction(
                    R.drawable.ic_notif_stop,
                    "Stop",
                    stopPendingIntent
                )
            }
            TypingStatus.IDLE, TypingStatus.STOPPED, TypingStatus.COMPLETED -> {
                builder.addAction(
                    R.drawable.ic_notif_play,
                    "Start",
                    startPendingIntent
                )
                if (state.current > 0 || state.status == TypingStatus.STOPPED) {
                    builder.addAction(
                        R.drawable.ic_notif_stop,
                        "Reset",
                        stopPendingIntent
                    )
                }
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission not yet granted
        }
    }

    fun cancelNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
