package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AppSettings
import com.example.data.SettingsManager
import com.example.data.Snippet
import com.example.data.SnippetRepository
import com.example.engine.TypingEngine
import com.example.engine.TypingProgress
import com.example.engine.TypingStatus
import com.example.engine.UnicodeHelper
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var snippetRepository: SnippetRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        TypingEngine.init(applicationContext)
        settingsManager = SettingsManager(this)
        snippetRepository = SnippetRepository(this)
        TypingEngine.setHapticFeedback(settingsManager.settings.value.hapticFeedback)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                AutoTyperMainScreen(
                    settingsManager = settingsManager,
                    snippetRepository = snippetRepository,
                    onOpenImeSettings = {
                        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    onSwitchKeyboard = {
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    }
                )
            }
        }
    }
}

enum class NavigationTab(val label: String, val icon: @Composable () -> Unit) {
    SANDBOX("Test Sandbox", { Icon(Icons.Default.TextFields, contentDescription = "Sandbox") }),
    TEMPLATES("Snippets", { Icon(Icons.Default.ContentCopy, contentDescription = "Snippets") }),
    SETTINGS("Settings", { Icon(Icons.Default.Settings, contentDescription = "Settings") }),
    GUIDE("Guide", { Icon(Icons.Default.Help, contentDescription = "Guide") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTyperMainScreen(
    settingsManager: SettingsManager,
    snippetRepository: SnippetRepository,
    onOpenImeSettings: () -> Unit,
    onSwitchKeyboard: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.SANDBOX) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        TypingEngine.refreshNotification()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var isImeEnabled by remember { mutableStateOf(false) }
    var isImeSelected by remember { mutableStateOf(false) }

    fun checkImeStatus() {
        val packageName = context.packageName
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledList = imm?.enabledInputMethodList ?: emptyList()
        isImeEnabled = enabledList.any { it.packageName == packageName }

        val currentIme = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: ""
        isImeSelected = currentIme.contains(packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkImeStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkImeStatus()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_screen_scaffold"),
        containerColor = Color(0xFF1C1B1F),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFD0BCFF), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = Color(0xFF381E72),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "AutoTyper",
                                color = Color(0xFFE6E1E5),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                "Character-by-character automated keyboard",
                                color = Color(0xFF938F99),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { checkImeStatus() },
                        modifier = Modifier.testTag("refresh_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh IME Status",
                            tint = Color(0xFFD0BCFF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1B1F)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1C1B1F),
                contentColor = Color(0xFFE6E1E5)
            ) {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = tab.icon,
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF381E72),
                            selectedTextColor = Color(0xFFD0BCFF),
                            indicatorColor = Color(0xFFD0BCFF),
                            unselectedIconColor = Color(0xFF938F99),
                            unselectedTextColor = Color(0xFF938F99)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Setup & Activation Status Card (Visible across screens if not fully enabled)
            ActivationBanner(
                isImeEnabled = isImeEnabled,
                isImeSelected = isImeSelected,
                onOpenImeSettings = onOpenImeSettings,
                onSwitchKeyboard = onSwitchKeyboard
            )

            when (selectedTab) {
                NavigationTab.SANDBOX -> SandboxTabContent(
                    settingsManager = settingsManager,
                    snippetRepository = snippetRepository
                )
                NavigationTab.TEMPLATES -> SnippetsTabContent(
                    snippetRepository = snippetRepository,
                    settingsManager = settingsManager,
                    onSelectSnippetForSandbox = { snippet ->
                        settingsManager.saveLastText(snippet.text)
                        selectedTab = NavigationTab.SANDBOX
                    }
                )
                NavigationTab.SETTINGS -> SettingsTabContent(
                    settingsManager = settingsManager
                )
                NavigationTab.GUIDE -> GuideTabContent(
                    onOpenSettings = onOpenImeSettings,
                    onSwitchKeyboard = onSwitchKeyboard
                )
            }
        }
    }
}

@Composable
fun ActivationBanner(
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    onOpenImeSettings: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("activation_banner_card"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B2930)
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isImeEnabled && isImeSelected) Color(0xFFD0BCFF).copy(alpha = 0.4f) else Color(0xFF49454F)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isImeEnabled && isImeSelected) Icons.Default.CheckCircle else Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = if (isImeEnabled && isImeSelected) Color(0xFFD0BCFF) else Color(0xFFFFB74D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isImeEnabled && isImeSelected) "AutoTyper is Active & Ready" else "Keyboard Setup Required",
                        color = Color(0xFFE6E1E5),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                // Status Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(
                        label = "Enabled",
                        isActive = isImeEnabled
                    )
                    StatusChip(
                        label = "Selected",
                        isActive = isImeSelected
                    )
                }
            }

            if (!isImeEnabled || !isImeSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!isImeEnabled) "Step 1: Enable AutoTyper in Android Keyboard Settings."
                    else "Step 2: Select AutoTyper as your current active keyboard.",
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isImeEnabled) {
                        Button(
                            onClick = onOpenImeSettings,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_enable_keyboard"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            )
                        ) {
                            Text("1. Enable in Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = onSwitchKeyboard,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_switch_keyboard"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isImeEnabled) Color(0xFF49454F) else Color(0xFFD0BCFF),
                            contentColor = if (!isImeEnabled) Color(0xFFE6E1E5) else Color(0xFF381E72)
                        )
                    ) {
                        Text("2. Switch Keyboard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(label: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (isActive) Color(0xFF381E72) else Color(0xFF49454F),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$label: ${if (isActive) "YES" else "NO"}",
            color = if (isActive) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SandboxTabContent(
    settingsManager: SettingsManager,
    snippetRepository: SnippetRepository,
    modifier: Modifier = Modifier
) {
    val progressState by TypingEngine.progressState.collectAsState()
    val settings by settingsManager.settings.collectAsState()
    val snippets by snippetRepository.snippets.collectAsState()

    var testFieldText by remember { mutableStateOf("") }
    var scriptInputText by remember { mutableStateOf(settings.lastInputText) }
    var selectedSpeedMs by remember { mutableStateOf(settings.defaultSpeedMs) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Live Interactive Typing Receiver Field
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sandbox_receiver_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Live Typing Receiver (Sandbox)",
                                color = Color(0xFFE6E1E5),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        if (testFieldText.isNotEmpty()) {
                            TextButton(
                                onClick = { testFieldText = "" },
                                modifier = Modifier.testTag("clear_sandbox_button")
                            ) {
                                Text("Clear Field", color = Color(0xFFF2B8B5), fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = testFieldText,
                        onValueChange = { testFieldText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 160.dp)
                            .testTag("sandbox_target_input"),
                        placeholder = {
                            Text(
                                "Tap here to focus this field, then use AutoTyper keyboard or the controls below to type letter-by-letter!",
                                color = Color(0xFF938F99),
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedContainerColor = Color(0xFF1C1B1F),
                            unfocusedContainerColor = Color(0xFF1C1B1F),
                            cursorColor = Color(0xFFD0BCFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE6E1E5),
                            fontSize = 14.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Length: ${testFieldText.length} chars",
                            color = Color(0xFF938F99),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Words: ${testFieldText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size}",
                            color = Color(0xFF938F99),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 2. Typing Progress and Live Status Dashboard
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("typing_status_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Engine Status & Progress",
                            color = Color(0xFFE6E1E5),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        // Status Badge
                        val statusColor = when (progressState.status) {
                            TypingStatus.IDLE -> Color(0xFF938F99)
                            TypingStatus.COUNTDOWN -> Color(0xFFD0BCFF)
                            TypingStatus.TYPING -> Color(0xFF81C784)
                            TypingStatus.PAUSED -> Color(0xFFFFB74D)
                            TypingStatus.STOPPED -> Color(0xFFF2B8B5)
                            TypingStatus.COMPLETED -> Color(0xFF4DD0E1)
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1C1B1F), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("main_status_badge")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (progressState.status == TypingStatus.COUNTDOWN) {
                                        "Countdown: ${progressState.countdownRemaining}s"
                                    } else {
                                        progressState.status.label
                                    },
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bar & Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = progressState.progressLabel,
                            color = Color(0xFFCAC4D0),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (progressState.currentGrapheme.isNotEmpty()) {
                            Text(
                                text = "Current: '${UnicodeHelper.formatDisplayChar(progressState.currentGrapheme)}'",
                                color = Color(0xFFD0BCFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progressState.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .testTag("main_linear_progress"),
                        color = Color(0xFFD0BCFF),
                        trackColor = Color(0xFF49454F)
                    )

                    if (progressState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Notice: ${progressState.errorMessage}",
                            color = Color(0xFFF2B8B5),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 3. Text to Type Input & Template Picker
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_config_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Text to Auto-Type",
                        color = Color(0xFFE6E1E5),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset chips
                    Text(
                        "Quick Presets:",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        snippets.take(6).forEach { snippet ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1C1B1F))
                                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                                    .clickable {
                                        scriptInputText = snippet.text
                                        selectedSpeedMs = snippet.defaultSpeedMs
                                        settingsManager.saveLastText(snippet.text)
                                        TypingEngine.updateSpeed(snippet.defaultSpeedMs)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    snippet.title,
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = scriptInputText,
                        onValueChange = {
                            scriptInputText = it
                            settingsManager.saveLastText(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 150.dp)
                            .testTag("main_script_input"),
                        placeholder = {
                            Text("Type or paste text to be automated...", color = Color(0xFF938F99), fontSize = 13.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedContainerColor = Color(0xFF1C1B1F),
                            unfocusedContainerColor = Color(0xFF1C1B1F),
                            cursorColor = Color(0xFFD0BCFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE6E1E5),
                            fontSize = 13.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Speed: $selectedSpeedMs ms / char",
                            color = Color(0xFFCAC4D0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF381E72), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                when {
                                    selectedSpeedMs <= 20L -> "🚀 Instant"
                                    selectedSpeedMs <= 60L -> "⚡ Fast"
                                    selectedSpeedMs <= 150L -> "✍️ Natural"
                                    selectedSpeedMs <= 500L -> "🐢 Slow"
                                    else -> "⏱️ Step"
                                },
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Slider(
                        value = selectedSpeedMs.toFloat(),
                        onValueChange = {
                            selectedSpeedMs = it.toLong()
                            TypingEngine.updateSpeed(selectedSpeedMs)
                            settingsManager.updateSpeed(selectedSpeedMs)
                        },
                        valueRange = 10f..2000f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("main_speed_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD0BCFF),
                            activeTrackColor = Color(0xFFD0BCFF),
                            inactiveTrackColor = Color(0xFF49454F)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Control Buttons: Start, Pause, Stop
                    val isRunning = progressState.status == TypingStatus.TYPING || progressState.status == TypingStatus.COUNTDOWN
                    val isPaused = progressState.status == TypingStatus.PAUSED

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // START
                        Button(
                            onClick = {
                                if (!isRunning) {
                                    TypingEngine.start(
                                        text = scriptInputText,
                                        speedMs = selectedSpeedMs,
                                        countdownSec = settings.countdownSec,
                                        isLoop = settings.loopRepeat
                                    )
                                }
                            },
                            enabled = scriptInputText.isNotEmpty() && !isRunning,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("main_start_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72),
                                disabledContainerColor = Color(0xFF49454F).copy(alpha = 0.5f),
                                disabledContentColor = Color(0xFF938F99)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start", fontWeight = FontWeight.Bold)
                        }

                        // PAUSE / RESUME
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
                                .height(46.dp)
                                .testTag("main_pause_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) Color(0xFFFFB74D) else Color(0xFF49454F),
                                contentColor = if (isPaused) Color(0xFF4A2800) else Color(0xFFE6E1E5),
                                disabledContainerColor = Color(0xFF2B2930),
                                disabledContentColor = Color(0xFF938F99)
                            )
                        ) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPaused) "Resume" else "Pause", fontWeight = FontWeight.SemiBold)
                        }

                        // STOP
                        Button(
                            onClick = {
                                TypingEngine.stop()
                            },
                            enabled = isRunning || isPaused,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("main_stop_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF49454F),
                                contentColor = Color(0xFFF2B8B5),
                                disabledContainerColor = Color(0xFF2B2930),
                                disabledContentColor = Color(0xFF938F99)
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SnippetsTabContent(
    snippetRepository: SnippetRepository,
    settingsManager: SettingsManager,
    onSelectSnippetForSandbox: (Snippet) -> Unit,
    modifier: Modifier = Modifier
) {
    val snippets by snippetRepository.snippets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSnippet by remember { mutableStateOf<Snippet?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(snippets) {
        listOf("All") + snippets.map { it.category }.distinct()
    }

    val filteredSnippets = remember(snippets, selectedCategory) {
        if (selectedCategory == "All") snippets else snippets.filter { it.category == selectedCategory }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Snippet Library",
                            color = Color(0xFFE6E1E5),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Pre-defined and custom text templates for rapid auto-typing",
                            color = Color(0xFF938F99),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Category Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF381E72) else Color(0xFF2B2930))
                                .border(1.dp, if (isSelected) Color(0xFFD0BCFF).copy(alpha = 0.5f) else Color(0xFF49454F), RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                cat,
                                fontSize = 12.sp,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            items(filteredSnippets, key = { it.id }) { snippet ->
                SnippetItemCard(
                    snippet = snippet,
                    onLoad = {
                        onSelectSnippetForSandbox(snippet)
                    },
                    onEdit = {
                        editingSnippet = snippet
                    },
                    onDelete = {
                        snippetRepository.deleteSnippet(snippet.id)
                    }
                )
            }
        }

        // Add Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_snippet_fab"),
            containerColor = Color(0xFFD0BCFF),
            contentColor = Color(0xFF381E72),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Snippet")
        }
    }

    if (showAddDialog) {
        SnippetEditorDialog(
            title = "New Snippet",
            initialTitle = "",
            initialText = "",
            initialCategory = "Custom",
            initialSpeed = 80L,
            onDismiss = { showAddDialog = false },
            onSave = { title, text, cat, speed ->
                snippetRepository.addSnippet(title, text, cat, speed)
                showAddDialog = false
            }
        )
    }

    editingSnippet?.let { snippet ->
        SnippetEditorDialog(
            title = "Edit Snippet",
            initialTitle = snippet.title,
            initialText = snippet.text,
            initialCategory = snippet.category,
            initialSpeed = snippet.defaultSpeedMs,
            onDismiss = { editingSnippet = null },
            onSave = { title, text, cat, speed ->
                snippetRepository.updateSnippet(snippet.id, title, text, cat, speed)
                editingSnippet = null
            }
        )
    }
}

@Composable
fun SnippetItemCard(
    snippet: Snippet,
    onLoad: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("snippet_card_${snippet.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        snippet.title,
                        color = Color(0xFFE6E1E5),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF381E72), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            snippet.category,
                            color = Color(0xFFD0BCFF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${snippet.defaultSpeedMs}ms",
                        color = Color(0xFFD0BCFF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (snippet.isCustom) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFFCAC4D0),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFF2B8B5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                snippet.text,
                color = Color(0xFFCAC4D0),
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onLoad,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    )
                ) {
                    Text("Load into AutoTyper", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SnippetEditorDialog(
    title: String,
    initialTitle: String,
    initialText: String,
    initialCategory: String,
    initialSpeed: Long,
    onDismiss: () -> Unit,
    onSave: (title: String, text: String, category: String, speed: Long) -> Unit
) {
    var snippetTitle by remember { mutableStateOf(initialTitle) }
    var snippetText by remember { mutableStateOf(initialText) }
    var category by remember { mutableStateOf(initialCategory) }
    var speedMs by remember { mutableStateOf(initialSpeed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color(0xFFE6E1E5), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = snippetTitle,
                    onValueChange = { snippetTitle = it },
                    label = { Text("Title", color = Color(0xFF938F99)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedContainerColor = Color(0xFF1C1B1F),
                        unfocusedContainerColor = Color(0xFF1C1B1F),
                        cursorColor = Color(0xFFD0BCFF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category", color = Color(0xFF938F99)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedContainerColor = Color(0xFF1C1B1F),
                        unfocusedContainerColor = Color(0xFF1C1B1F),
                        cursorColor = Color(0xFFD0BCFF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = snippetText,
                    onValueChange = { snippetText = it },
                    label = { Text("Text content", color = Color(0xFF938F99)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedContainerColor = Color(0xFF1C1B1F),
                        unfocusedContainerColor = Color(0xFF1C1B1F),
                        cursorColor = Color(0xFFD0BCFF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    "Speed: $speedMs ms / char",
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp
                )
                Slider(
                    value = speedMs.toFloat(),
                    onValueChange = { speedMs = it.toLong() },
                    valueRange = 10f..2000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD0BCFF),
                        activeTrackColor = Color(0xFFD0BCFF),
                        inactiveTrackColor = Color(0xFF49454F)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(snippetTitle, snippetText, category, speedMs) },
                enabled = snippetText.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                )
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF938F99))
            }
        },
        containerColor = Color(0xFF2B2930),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SettingsTabContent(
    settingsManager: SettingsManager,
    modifier: Modifier = Modifier
) {
    val settings by settingsManager.settings.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                "AutoTyper Preferences",
                color = Color(0xFFE6E1E5),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                "Configure default speeds, countdown delays, and tactile feedback",
                color = Color(0xFF938F99),
                fontSize = 12.sp
            )
        }

        // Speed Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFD0BCFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Default Speed", color = Color(0xFFE6E1E5), fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF381E72), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("${settings.defaultSpeedMs} ms", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = settings.defaultSpeedMs.toFloat(),
                        onValueChange = {
                            settingsManager.updateSpeed(it.toLong())
                            TypingEngine.updateSpeed(it.toLong())
                        },
                        valueRange = 10f..2000f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD0BCFF),
                            activeTrackColor = Color(0xFFD0BCFF),
                            inactiveTrackColor = Color(0xFF49454F)
                        )
                    )
                }
            }
        }

        // Countdown Delay Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFD0BCFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Start Countdown Delay", color = Color(0xFFE6E1E5), fontWeight = FontWeight.SemiBold)
                                Text("Gives you time to switch focus to target app", color = Color(0xFF938F99), fontSize = 11.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF381E72), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("${settings.countdownSec}s", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "0s (Instant)", 1 to "1s", 2 to "2s", 3 to "3s", 5 to "5s").forEach { (sec, label) ->
                            val isSelected = settings.countdownSec == sec
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF381E72) else Color(0xFF1C1B1F))
                                    .border(1.dp, if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F), RoundedCornerShape(8.dp))
                                    .clickable { settingsManager.updateCountdown(sec) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    label,
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

        // Haptic Feedback Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFFD0BCFF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Tactile Haptic Feedback", color = Color(0xFFE6E1E5), fontWeight = FontWeight.SemiBold)
                            Text("Subtle mechanical tick vibration per character typed", color = Color(0xFF938F99), fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = settings.hapticFeedback,
                        onCheckedChange = {
                            settingsManager.updateHaptic(it)
                            TypingEngine.setHapticFeedback(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF381E72),
                            checkedTrackColor = Color(0xFFD0BCFF),
                            uncheckedThumbColor = Color(0xFF938F99),
                            uncheckedTrackColor = Color(0xFF49454F)
                        )
                    )
                }
            }
        }

        // Repeat / Looping Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = Color(0xFFD0BCFF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Continuous Loop Mode", color = Color(0xFFE6E1E5), fontWeight = FontWeight.SemiBold)
                            Text("Automatically restarts typing from character 0 when finished", color = Color(0xFF938F99), fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = settings.loopRepeat,
                        onCheckedChange = {
                            settingsManager.updateLoop(it)
                            TypingEngine.setLooping(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF381E72),
                            checkedTrackColor = Color(0xFFD0BCFF),
                            uncheckedThumbColor = Color(0xFF938F99),
                            uncheckedTrackColor = Color(0xFF49454F)
                        )
                    )
                }
            }
        }

        // Notification Bar Controls Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFD0BCFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Notification Bar Controls", color = Color(0xFFE6E1E5), fontWeight = FontWeight.SemiBold)
                                Text("Quick Start, Pause, and Stop buttons in notification shade", color = Color(0xFF938F99), fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = settings.showNotificationControls,
                            onCheckedChange = {
                                settingsManager.updateNotificationControls(it)
                                TypingEngine.refreshNotification()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF381E72),
                                checkedTrackColor = Color(0xFFD0BCFF),
                                uncheckedThumbColor = Color(0xFF938F99),
                                uncheckedTrackColor = Color(0xFF49454F)
                            )
                        )
                    }
                    if (settings.showNotificationControls) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1B1F), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "▶ Start, ⏸ Pause & ⏹ Stop actions active in notification bar",
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Offline Guarantee Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "🛡️ 100% Offline & Private",
                        color = Color(0xFFD0BCFF),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "AutoTyper contains zero network calls, zero cloud telemetry, and zero third-party tracking. All typing operations, templates, and settings stay completely local on your device.",
                        color = Color(0xFFCAC4D0),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GuideTabContent(
    onOpenSettings: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                "How AutoTyper Works",
                color = Color(0xFFE6E1E5),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                "Step-by-step instructions on utilizing AutoTyper across all your favorite apps",
                color = Color(0xFF938F99),
                fontSize = 12.sp
            )
        }

        item {
            GuideStepCard(
                stepNumber = "1",
                title = "Enable the Keyboard in Android Settings",
                description = "Android requires all custom input methods to be activated in system settings for security.",
                buttonText = "Open System Keyboard Settings",
                onButtonClick = onOpenSettings
            )
        }

        item {
            GuideStepCard(
                stepNumber = "2",
                title = "Select AutoTyper Keyboard",
                description = "When you tap inside any text input field in WhatsApp, Chrome, Notes, Discord, or games, tap the keyboard switcher icon or notification to pick AutoTyper.",
                buttonText = "Open Keyboard Picker",
                onButtonClick = onSwitchKeyboard
            )
        }

        item {
            GuideStepCard(
                stepNumber = "3",
                title = "Enter Your Text & Press Start",
                description = "Enter or paste your text in the keyboard's input area, choose your speed (10ms to 2000ms), and hit Start. AutoTyper will automatically send character by character into the target field.",
                buttonText = null,
                onButtonClick = null
            )
        }

        item {
            GuideStepCard(
                stepNumber = "4",
                title = "Full Real-time Control",
                description = "Hit Pause anytime to temporarily halt typing, Resume to pick up right from that exact character, or Stop to immediately cancel and reset.",
                buttonText = null,
                onButtonClick = null
            )
        }
    }
}

@Composable
fun GuideStepCard(
    stepNumber: String,
    title: String,
    description: String,
    buttonText: String?,
    onButtonClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color(0xFFD0BCFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stepNumber, color = Color(0xFF381E72), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, color = Color(0xFFE6E1E5), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, color = Color(0xFFCAC4D0), fontSize = 12.sp)

            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onButtonClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.6f))
                ) {
                    Text(buttonText, fontSize = 12.sp, color = Color(0xFFD0BCFF), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
