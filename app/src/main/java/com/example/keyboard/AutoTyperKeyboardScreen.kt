package com.example.keyboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.data.Snippet
import com.example.data.SnippetRepository
import com.example.engine.TypingEngine
import com.example.engine.TypingProgress
import com.example.engine.TypingStatus
import com.example.engine.UnicodeHelper

@Composable
fun AutoTyperKeyboardScreen(
    settingsManager: SettingsManager,
    snippetRepository: SnippetRepository,
    onSwitchKeyboard: () -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressState by TypingEngine.progressState.collectAsState()
    val snippets by snippetRepository.snippets.collectAsState()
    val settings by settingsManager.settings.collectAsState()

    var inputText by remember { mutableStateOf(settings.lastInputText) }
    var showSnippetsPanel by remember { mutableStateOf(false) }
    var showSpeedSlider by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("autotyper_keyboard_root"),
        color = Color(0xFF1C1B1F),
        contentColor = Color(0xFFE6E1E5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // 1. Top Status & Progress Bar
            KeyboardStatusBar(
                progressState = progressState,
                onSpeedClick = { showSpeedSlider = !showSpeedSlider },
                onSnippetsClick = { showSnippetsPanel = !showSnippetsPanel },
                showSnippetsPanel = showSnippetsPanel,
                showSpeedSlider = showSpeedSlider
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Expandable Speed Slider
            AnimatedVisibility(
                visible = showSpeedSlider,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SpeedControlSection(
                    currentSpeedMs = progressState.speedMs,
                    onSpeedChange = { newSpeed ->
                        TypingEngine.updateSpeed(newSpeed)
                        settingsManager.updateSpeed(newSpeed)
                    }
                )
            }

            // 3. Expandable Snippets List
            AnimatedVisibility(
                visible = showSnippetsPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SnippetsSelectionSection(
                    snippets = snippets,
                    onSelect = { snippet ->
                        inputText = snippet.text
                        TypingEngine.updateSpeed(snippet.defaultSpeedMs)
                        showSnippetsPanel = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Text Input Field with Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        settingsManager.saveLastText(it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp, max = 88.dp)
                        .testTag("keyboard_text_input"),
                    placeholder = {
                        Text(
                            "Enter text to auto-type...",
                            color = Color(0xFF938F99),
                            fontSize = 13.sp
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFE6E1E5),
                        fontSize = 14.sp
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedContainerColor = Color(0xFF2B2930),
                        unfocusedContainerColor = Color(0xFF2B2930),
                        cursorColor = Color(0xFFD0BCFF)
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputText = "" },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("keyboard_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear text",
                                        tint = Color(0xFF938F99),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    if (clip?.hasPrimaryClip() == true && clip.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                                        val item = clip.primaryClip?.getItemAt(0)
                                        val pasted = item?.text?.toString() ?: ""
                                        if (pasted.isNotEmpty()) {
                                            inputText = pasted
                                            settingsManager.saveLastText(pasted)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("keyboard_paste_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste from clipboard",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Main AutoTyper Action Buttons: START, PAUSE/RESUME, STOP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // START BUTTON
                val isRunning = progressState.status == TypingStatus.TYPING || progressState.status == TypingStatus.COUNTDOWN
                Button(
                    onClick = {
                        if (!isRunning) {
                            TypingEngine.start(
                                text = inputText,
                                speedMs = progressState.speedMs,
                                countdownSec = settings.countdownSec,
                                isLoop = settings.loopRepeat
                            )
                        }
                    },
                    enabled = inputText.isNotEmpty() && !isRunning,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("keyboard_start_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72),
                        disabledContainerColor = Color(0xFF49454F).copy(alpha = 0.5f),
                        disabledContentColor = Color(0xFF938F99).copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start typing",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Start",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // PAUSE / RESUME BUTTON
                val isPaused = progressState.status == TypingStatus.PAUSED
                Button(
                    onClick = {
                        if (isPaused) {
                            TypingEngine.resume()
                        } else {
                            TypingEngine.pause()
                        }
                    },
                    enabled = isRunning || isPaused,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                        .testTag("keyboard_pause_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) Color(0xFFEFB8C8) else Color(0xFFCCC2DC),
                        contentColor = if (isPaused) Color(0xFF492532) else Color(0xFF332D41),
                        disabledContainerColor = Color(0xFF2B2930),
                        disabledContentColor = Color(0xFF938F99)
                    )
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        if (isPaused) "Resume" else "Pause",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // STOP BUTTON
                Button(
                    onClick = {
                        TypingEngine.stop()
                    },
                    enabled = isRunning || isPaused,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("keyboard_stop_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2B8B5),
                        contentColor = Color(0xFF601410),
                        disabledContainerColor = Color(0xFF2B2930),
                        disabledContentColor = Color(0xFF938F99)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop typing",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "Stop",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 6. Auxiliary Keyboard Utility Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch Keyboard / IME
                IconButton(
                    onClick = onSwitchKeyboard,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF2B2930), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                        .testTag("keyboard_switch_ime_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Switch Input Method",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Space Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .background(Color(0xFF2B2930), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                        .clickable { onSpace() }
                        .testTag("keyboard_space_key"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Space",
                        color = Color(0xFFCAC4D0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Backspace
                IconButton(
                    onClick = onBackspace,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF2B2930), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                        .testTag("keyboard_backspace_key")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = Color(0xFFF2B8B5),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Enter Key
                IconButton(
                    onClick = onEnter,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF381E72), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .testTag("keyboard_enter_key")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Enter / Next",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Hide Keyboard
                IconButton(
                    onClick = onHideKeyboard,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF2B2930), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                        .testTag("keyboard_hide_key")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardHide,
                        contentDescription = "Hide Keyboard",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardStatusBar(
    progressState: TypingProgress,
    onSpeedClick: () -> Unit,
    onSnippetsClick: () -> Unit,
    showSnippetsPanel: Boolean,
    showSpeedSlider: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (progressState.status) {
            TypingStatus.IDLE -> Color(0xFF938F99)
            TypingStatus.COUNTDOWN -> Color(0xFFD0BCFF)
            TypingStatus.TYPING -> Color(0xFFB4E5A2)
            TypingStatus.PAUSED -> Color(0xFFEFB8C8)
            TypingStatus.STOPPED -> Color(0xFFF2B8B5)
            TypingStatus.COMPLETED -> Color(0xFFA8C7FA)
        },
        label = "statusColor"
    )

    // Pulsing dot animation for typing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2930), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("status_indicator_badge")
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = statusColor.copy(
                                alpha = if (progressState.status == TypingStatus.TYPING) pulseAlpha else 1f
                            ),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (progressState.status == TypingStatus.COUNTDOWN) {
                        "Starting in ${progressState.countdownRemaining}s"
                    } else {
                        progressState.status.label
                    },
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Current Character Indicator (if typing or paused)
            if (progressState.currentGrapheme.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Char: ",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp
                    )
                    Text(
                        text = UnicodeHelper.formatDisplayChar(progressState.currentGrapheme),
                        color = Color(0xFFD0BCFF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick toggles for speed & presets
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Speed indicator chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showSpeedSlider) Color(0xFF381E72) else Color(0xFF1C1B1F))
                        .border(1.dp, if (showSpeedSlider) Color(0xFFD0BCFF) else Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .clickable { onSpeedClick() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                        .testTag("keyboard_speed_chip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "${progressState.speedMs}ms",
                        color = Color(0xFFE6E1E5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Templates chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showSnippetsPanel) Color(0xFF381E72) else Color(0xFF1C1B1F))
                        .border(1.dp, if (showSnippetsPanel) Color(0xFFD0BCFF) else Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .clickable { onSnippetsClick() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                        .testTag("keyboard_templates_chip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Presets",
                        color = Color(0xFFE6E1E5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (showSnippetsPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Progress text and Linear Indicator
        if (progressState.total > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${progressState.current} / ${progressState.total} chars",
                    color = Color(0xFFCAC4D0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("progress_character_counter")
                )
                Text(
                    text = "${progressState.progressPercent}%",
                    color = Color(0xFFD0BCFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { progressState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .testTag("keyboard_progress_bar"),
                color = when (progressState.status) {
                    TypingStatus.TYPING -> Color(0xFFB4E5A2)
                    TypingStatus.PAUSED -> Color(0xFFEFB8C8)
                    TypingStatus.COMPLETED -> Color(0xFFA8C7FA)
                    else -> Color(0xFFD0BCFF)
                },
                trackColor = Color(0xFF49454F)
            )
        }
    }
}

@Composable
fun SpeedControlSection(
    currentSpeedMs: Long,
    onSpeedChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Speed: $currentSpeedMs ms per char",
                    color = Color(0xFFE6E1E5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when {
                        currentSpeedMs <= 20L -> "🚀 Instant / Ultra"
                        currentSpeedMs <= 60L -> "⚡ Fast"
                        currentSpeedMs <= 150L -> "✍️ Realistic"
                        currentSpeedMs <= 500L -> "🐢 Slow"
                        else -> "⏱️ Step-by-Step"
                    },
                    color = Color(0xFFD0BCFF),
                    fontSize = 11.sp
                )
            }

            Slider(
                value = currentSpeedMs.toFloat(),
                onValueChange = { onSpeedChange(it.toLong()) },
                valueRange = 10f..2000f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("speed_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFD0BCFF),
                    activeTrackColor = Color(0xFFD0BCFF),
                    inactiveTrackColor = Color(0xFF49454F)
                )
            )

            // Preset speed chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    10L to "10ms Turbo",
                    30L to "30ms Fast",
                    80L to "80ms Normal",
                    150L to "150ms Realistic",
                    300L to "300ms Slow",
                    1000L to "1000ms Step"
                ).forEach { (speed, label) ->
                    val isSelected = currentSpeedMs == speed
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF381E72) else Color(0xFF1C1B1F))
                            .border(1.dp, if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F), RoundedCornerShape(8.dp))
                            .clickable { onSpeedChange(speed) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SnippetsSelectionSection(
    snippets: List<Snippet>,
    onSelect: (Snippet) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                "Quick Text Presets (Tap to load):",
                color = Color(0xFF938F99),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                snippets.forEach { snippet ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1C1B1F))
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                            .clickable { onSelect(snippet) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(
                                snippet.title,
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                snippet.text,
                                color = Color(0xFFCAC4D0),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
