package com.example.keyboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.data.Snippet
import com.example.data.SnippetRepository
import com.example.engine.TypingEngine
import com.example.engine.TypingStatus
import com.example.engine.UnicodeHelper

enum class KeyboardLayoutMode {
    LOWERCASE,
    UPPERCASE,
    CAPSLOCK,
    NUMBERS_SYMBOLS,
    EXTRA_SYMBOLS
}

@Composable
fun MiniKeyboardView(
    settingsManager: SettingsManager,
    snippetRepository: SnippetRepository,
    onKeyTyped: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    onHideKeyboard: () -> Unit,
    onToggleFullView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressState by TypingEngine.progressState.collectAsState()
    val settings by settingsManager.settings.collectAsState()
    val snippets by snippetRepository.snippets.collectAsState()

    var keyboardMode by remember { mutableStateOf(KeyboardLayoutMode.LOWERCASE) }
    var activeAutoTypeText by remember { mutableStateOf(settings.lastInputText) }
    var showQuickSnippets by remember { mutableStateOf(false) }
    var showQuickSpeed by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mini_keyboard_surface"),
        color = Color(0xFF141318),
        contentColor = Color(0xFFE6E1E5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            // 1. POPUP MINI AUTO-TYPE CONTROL BAR WITH START / PAUSE / STOP
            MiniAutoTypeControlBar(
                progressState = progressState,
                activeText = activeAutoTypeText,
                onStart = {
                    if (activeAutoTypeText.isNotEmpty()) {
                        TypingEngine.start(
                            text = activeAutoTypeText,
                            speedMs = progressState.speedMs,
                            countdownSec = settings.countdownSec,
                            isLoop = settings.loopRepeat
                        )
                    }
                },
                onPause = { TypingEngine.pause() },
                onResume = { TypingEngine.resume() },
                onStop = { TypingEngine.stop() },
                onPasteClipboard = {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (clip?.hasPrimaryClip() == true && clip.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                        val item = clip.primaryClip?.getItemAt(0)
                        val pasted = item?.text?.toString() ?: ""
                        if (pasted.isNotEmpty()) {
                            activeAutoTypeText = pasted
                            settingsManager.saveLastText(pasted)
                        }
                    }
                },
                onToggleSnippets = {
                    showQuickSnippets = !showQuickSnippets
                    if (showQuickSnippets) showQuickSpeed = false
                },
                onToggleSpeed = {
                    showQuickSpeed = !showQuickSpeed
                    if (showQuickSpeed) showQuickSnippets = false
                },
                onToggleFullView = onToggleFullView,
                showQuickSnippets = showQuickSnippets,
                showQuickSpeed = showQuickSpeed
            )

            // 2. Expandable Quick Snippets Tray
            AnimatedVisibility(
                visible = showQuickSnippets,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                QuickSnippetTray(
                    snippets = snippets,
                    onSelect = { snippet ->
                        activeAutoTypeText = snippet.text
                        settingsManager.saveLastText(snippet.text)
                        TypingEngine.updateSpeed(snippet.defaultSpeedMs)
                        showQuickSnippets = false
                    }
                )
            }

            // 3. Expandable Quick Speed Selector
            AnimatedVisibility(
                visible = showQuickSpeed,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                QuickSpeedTray(
                    currentSpeedMs = progressState.speedMs,
                    onSelectSpeed = { newSpeed ->
                        TypingEngine.updateSpeed(newSpeed)
                        settingsManager.updateSpeed(newSpeed)
                        showQuickSpeed = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 4. MINI KEYBOARD KEYS
            MiniKeyboardKeyMatrix(
                mode = keyboardMode,
                onModeChange = { keyboardMode = it },
                onKeyTyped = onKeyTyped,
                onBackspace = onBackspace,
                onEnter = onEnter,
                onSpace = onSpace,
                onSwitchKeyboard = onSwitchKeyboard,
                onHideKeyboard = onHideKeyboard
            )
        }
    }
}

@Composable
fun MiniAutoTypeControlBar(
    progressState: com.example.engine.TypingProgress,
    activeText: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onPasteClipboard: () -> Unit,
    onToggleSnippets: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleFullView: () -> Unit,
    showQuickSnippets: Boolean,
    showQuickSpeed: Boolean,
    modifier: Modifier = Modifier
) {
    val isRunning = progressState.status == TypingStatus.TYPING || progressState.status == TypingStatus.COUNTDOWN
    val isPaused = progressState.status == TypingStatus.PAUSED

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF211F26), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        // Top Row: Status badge & Preview text & Full View Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Indicator Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        when (progressState.status) {
                            TypingStatus.TYPING -> Color(0xFFB4E5A2).copy(alpha = 0.2f)
                            TypingStatus.PAUSED -> Color(0xFFEFB8C8).copy(alpha = 0.2f)
                            TypingStatus.COUNTDOWN -> Color(0xFFD0BCFF).copy(alpha = 0.2f)
                            else -> Color(0xFF381E72).copy(alpha = 0.4f)
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        when (progressState.status) {
                            TypingStatus.TYPING -> Color(0xFFB4E5A2)
                            TypingStatus.PAUSED -> Color(0xFFEFB8C8)
                            TypingStatus.COUNTDOWN -> Color(0xFFD0BCFF)
                            else -> Color(0xFF49454F)
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            when (progressState.status) {
                                TypingStatus.TYPING -> Color(0xFFB4E5A2)
                                TypingStatus.PAUSED -> Color(0xFFEFB8C8)
                                TypingStatus.COUNTDOWN -> Color(0xFFD0BCFF)
                                else -> Color(0xFF938F99)
                            },
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (progressState.status) {
                        TypingStatus.COUNTDOWN -> "${progressState.countdownRemaining}s"
                        TypingStatus.TYPING -> "Typing ${progressState.progressPercent}%"
                        TypingStatus.PAUSED -> "Paused"
                        TypingStatus.COMPLETED -> "Done!"
                        else -> "Ready"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (progressState.status) {
                        TypingStatus.TYPING -> Color(0xFFB4E5A2)
                        TypingStatus.PAUSED -> Color(0xFFEFB8C8)
                        TypingStatus.COUNTDOWN -> Color(0xFFD0BCFF)
                        else -> Color(0xFFE6E1E5)
                    }
                )
            }

            // Text Preview or Active char
            Text(
                text = if (progressState.currentGrapheme.isNotEmpty()) {
                    "Char: [${UnicodeHelper.formatDisplayChar(progressState.currentGrapheme)}]"
                } else if (activeText.isNotEmpty()) {
                    activeText.take(18) + if (activeText.length > 18) "..." else ""
                } else {
                    "Tap Paste or Preset"
                },
                fontSize = 11.sp,
                color = Color(0xFFCAC4D0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )

            // Speed Chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showQuickSpeed) Color(0xFF381E72) else Color(0xFF2B2930))
                    .border(1.dp, if (showQuickSpeed) Color(0xFFD0BCFF) else Color(0xFF49454F), RoundedCornerShape(6.dp))
                    .clickable { onToggleSpeed() }
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("${progressState.speedMs}ms", fontSize = 10.sp, color = Color(0xFFE6E1E5))
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Presets Chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showQuickSnippets) Color(0xFF381E72) else Color(0xFF2B2930))
                    .border(1.dp, if (showQuickSnippets) Color(0xFFD0BCFF) else Color(0xFF49454F), RoundedCornerShape(6.dp))
                    .clickable { onToggleSnippets() }
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Presets", fontSize = 10.sp, color = Color(0xFFE6E1E5))
                Icon(
                    imageVector = if (showQuickSnippets) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Switch to Full Keyboard Editor
            IconButton(
                onClick = onToggleFullView,
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(0xFF2B2930), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(6.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Expand Full Controls",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Active Progress Bar (if active)
        if (progressState.total > 0 && (isRunning || isPaused)) {
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { progressState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isPaused) Color(0xFFEFB8C8) else Color(0xFFB4E5A2),
                trackColor = Color(0xFF49454F)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // MAIN ACTION BUTTONS: START, PAUSE/RESUME, STOP, PASTE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // START BUTTON
            Button(
                onClick = onStart,
                enabled = activeText.isNotEmpty() && !isRunning,
                modifier = Modifier
                    .weight(1.3f)
                    .height(36.dp)
                    .testTag("mini_start_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72),
                    disabledContainerColor = Color(0xFF2B2930),
                    disabledContentColor = Color(0xFF938F99)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Start", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // PAUSE / RESUME BUTTON
            Button(
                onClick = { if (isPaused) onResume() else onPause() },
                enabled = isRunning || isPaused,
                modifier = Modifier
                    .weight(1.1f)
                    .height(36.dp)
                    .testTag("mini_pause_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) Color(0xFFEFB8C8) else Color(0xFFCCC2DC),
                    contentColor = if (isPaused) Color(0xFF492532) else Color(0xFF332D41),
                    disabledContainerColor = Color(0xFF2B2930),
                    disabledContentColor = Color(0xFF938F99)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(if (isPaused) "Resume" else "Pause", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }

            // STOP BUTTON
            Button(
                onClick = onStop,
                enabled = isRunning || isPaused,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("mini_stop_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF2B8B5),
                    contentColor = Color(0xFF601410),
                    disabledContainerColor = Color(0xFF2B2930),
                    disabledContentColor = Color(0xFF938F99)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Stop", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }

            // QUICK PASTE BUTTON
            IconButton(
                onClick = onPasteClipboard,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF2B2930), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                    .testTag("mini_paste_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste Clipboard",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun QuickSnippetTray(
    snippets: List<Snippet>,
    onSelect: (Snippet) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        snippets.forEach { snippet ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2B2930))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                    .clickable { onSelect(snippet) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${snippet.title}: ${snippet.text.take(12)}",
                    fontSize = 11.sp,
                    color = Color(0xFFD0BCFF),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun QuickSpeedTray(
    currentSpeedMs: Long,
    onSelectSpeed: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            10L to "10ms Ultra",
            30L to "30ms Fast",
            80L to "80ms Normal",
            150L to "150ms Realistic",
            300L to "300ms Slow",
            1000L to "1s Step"
        ).forEach { (speed, label) ->
            val isSelected = currentSpeedMs == speed
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF381E72) else Color(0xFF2B2930))
                    .border(1.dp, if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F), RoundedCornerShape(8.dp))
                    .clickable { onSelectSpeed(speed) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun MiniKeyboardKeyMatrix(
    mode: KeyboardLayoutMode,
    onModeChange: (KeyboardLayoutMode) -> Unit,
    onKeyTyped: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUpper = mode == KeyboardLayoutMode.UPPERCASE || mode == KeyboardLayoutMode.CAPSLOCK

    val row1 = when (mode) {
        KeyboardLayoutMode.NUMBERS_SYMBOLS -> listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        KeyboardLayoutMode.EXTRA_SYMBOLS -> listOf("~", "`", "|", "^", "=", "<", ">", "{", "}", "\\")
        else -> listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map { if (isUpper) it.uppercase() else it }
    }

    val row2 = when (mode) {
        KeyboardLayoutMode.NUMBERS_SYMBOLS -> listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
        KeyboardLayoutMode.EXTRA_SYMBOLS -> listOf("%", "*", ":", ";", "!", "?", "'", "\"", "[", "]")
        else -> listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map { if (isUpper) it.uppercase() else it }
    }

    val row3 = when (mode) {
        KeyboardLayoutMode.NUMBERS_SYMBOLS -> listOf("*", "\"", "'", ":", ";", "!", "?")
        KeyboardLayoutMode.EXTRA_SYMBOLS -> listOf("€", "£", "¥", "₹", "°", "•", "©")
        else -> listOf("z", "x", "c", "v", "b", "n", "m").map { if (isUpper) it.uppercase() else it }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ROW 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            row1.forEach { key ->
                MiniKey(
                    text = key,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onKeyTyped(key)
                        if (mode == KeyboardLayoutMode.UPPERCASE) {
                            onModeChange(KeyboardLayoutMode.LOWERCASE)
                        }
                    }
                )
            }
        }

        // ROW 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (mode == KeyboardLayoutMode.LOWERCASE || mode == KeyboardLayoutMode.UPPERCASE || mode == KeyboardLayoutMode.CAPSLOCK) {
                Spacer(modifier = Modifier.weight(0.5f))
            }
            row2.forEach { key ->
                MiniKey(
                    text = key,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onKeyTyped(key)
                        if (mode == KeyboardLayoutMode.UPPERCASE) {
                            onModeChange(KeyboardLayoutMode.LOWERCASE)
                        }
                    }
                )
            }
            if (mode == KeyboardLayoutMode.LOWERCASE || mode == KeyboardLayoutMode.UPPERCASE || mode == KeyboardLayoutMode.CAPSLOCK) {
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }

        // ROW 3: SHIFT / MODE TOGGLE + KEYS + BACKSPACE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift / Symbol page toggle
            if (mode == KeyboardLayoutMode.NUMBERS_SYMBOLS) {
                MiniSpecialKey(
                    text = "=\\<",
                    modifier = Modifier.weight(1.4f),
                    isAccent = true,
                    onClick = { onModeChange(KeyboardLayoutMode.EXTRA_SYMBOLS) }
                )
            } else if (mode == KeyboardLayoutMode.EXTRA_SYMBOLS) {
                MiniSpecialKey(
                    text = "?123",
                    modifier = Modifier.weight(1.4f),
                    isAccent = true,
                    onClick = { onModeChange(KeyboardLayoutMode.NUMBERS_SYMBOLS) }
                )
            } else {
                // Shift key
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(38.dp)
                        .background(
                            if (mode == KeyboardLayoutMode.CAPSLOCK) Color(0xFF381E72)
                            else if (mode == KeyboardLayoutMode.UPPERCASE) Color(0xFF49454F)
                            else Color(0xFF2B2930),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (mode == KeyboardLayoutMode.CAPSLOCK) Color(0xFFD0BCFF) else Color(0xFF49454F),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onModeChange(
                                when (mode) {
                                    KeyboardLayoutMode.LOWERCASE -> KeyboardLayoutMode.UPPERCASE
                                    KeyboardLayoutMode.UPPERCASE -> KeyboardLayoutMode.CAPSLOCK
                                    KeyboardLayoutMode.CAPSLOCK -> KeyboardLayoutMode.LOWERCASE
                                    else -> KeyboardLayoutMode.LOWERCASE
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardCapslock,
                        contentDescription = "Shift",
                        tint = if (mode != KeyboardLayoutMode.LOWERCASE) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Keys
            row3.forEach { key ->
                MiniKey(
                    text = key,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onKeyTyped(key)
                        if (mode == KeyboardLayoutMode.UPPERCASE) {
                            onModeChange(KeyboardLayoutMode.LOWERCASE)
                        }
                    }
                )
            }

            // Backspace Key
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(38.dp)
                    .background(Color(0xFF2B2930), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = Color(0xFFF2B8B5),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ROW 4: ?123, COMMA, SPACE, PERIOD, ENTER, SWITCH IME, HIDE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode toggle: ?123 vs ABC
            MiniSpecialKey(
                text = if (mode == KeyboardLayoutMode.LOWERCASE || mode == KeyboardLayoutMode.UPPERCASE || mode == KeyboardLayoutMode.CAPSLOCK) "?123" else "ABC",
                modifier = Modifier.weight(1.3f),
                isAccent = true,
                onClick = {
                    onModeChange(
                        if (mode == KeyboardLayoutMode.LOWERCASE || mode == KeyboardLayoutMode.UPPERCASE || mode == KeyboardLayoutMode.CAPSLOCK) {
                            KeyboardLayoutMode.NUMBERS_SYMBOLS
                        } else {
                            KeyboardLayoutMode.LOWERCASE
                        }
                    )
                }
            )

            // Switch IME
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(Color(0xFF2B2930), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                    .clickable { onSwitchKeyboard() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Language, contentDescription = "Switch Keyboard", tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
            }

            // Comma
            MiniKey(
                text = ",",
                modifier = Modifier.weight(0.9f),
                onClick = { onKeyTyped(",") }
            )

            // Space Bar
            Box(
                modifier = Modifier
                    .weight(3.2f)
                    .height(38.dp)
                    .background(Color(0xFF2B2930), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text("Space", color = Color(0xFFCAC4D0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Period
            MiniKey(
                text = ".",
                modifier = Modifier.weight(0.9f),
                onClick = { onKeyTyped(".") }
            )

            // Enter / Action Key
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(38.dp)
                    .background(Color(0xFF381E72), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { onEnter() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Enter", tint = Color(0xFFD0BCFF), modifier = Modifier.size(18.dp))
            }

            // Hide keyboard key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(Color(0xFF2B2930), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                    .clickable { onHideKeyboard() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardHide, contentDescription = "Hide Keyboard", tint = Color(0xFF938F99), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MiniKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2B2930))
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE6E1E5),
            fontFamily = FontFamily.Default
        )
    }
}

@Composable
fun MiniSpecialKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isAccent) Color(0xFF381E72) else Color(0xFF2B2930))
            .border(1.dp, if (isAccent) Color(0xFFD0BCFF).copy(alpha = 0.4f) else Color(0xFF49454F), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAccent) Color(0xFFD0BCFF) else Color(0xFFCAC4D0)
        )
    }
}

