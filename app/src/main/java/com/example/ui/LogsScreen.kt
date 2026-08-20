package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLogger
import com.example.util.LogEntry
import com.example.util.LogLevel
import kotlinx.coroutines.launch

@Composable
fun LogsScreen(
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    onOpenImeSettings: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    onRefreshStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logs by AppLogger.logs.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
    var showDiagnosticCard by remember { mutableStateOf(true) }

    val filteredLogs = remember(logs, searchQuery, selectedFilter) {
        logs.filter { entry ->
            val matchesFilter = selectedFilter == null || entry.level == selectedFilter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                entry.message.contains(searchQuery, ignoreCase = true) ||
                        entry.tag.contains(searchQuery, ignoreCase = true) ||
                        entry.level.name.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }.reversed()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp)
            .testTag("logs_screen")
    ) {
        // TOP CONTROL CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live App Logs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFE6E1E5)
                        )
                        Text(
                            text = "${logs.size} log entries recorded",
                            fontSize = 12.sp,
                            color = Color(0xFFCAC4D0)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // COPY BUTTON (CRITICAL USER REQUEST)
                        Button(
                            onClick = { AppLogger.copyLogsToClipboard(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("copy_logs_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // CLEAR BUTTON
                        IconButton(
                            onClick = { AppLogger.clear() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF381E72).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                                .testTag("clear_logs_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = Color(0xFFF2B8B5), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SEARCH TEXT FIELD
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter logs by tag or message...", fontSize = 13.sp, color = Color(0xFF938F99)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFD0BCFF)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color(0xFF938F99))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1B1F),
                        unfocusedContainerColor = Color(0xFF1C1B1F),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // FILTER PILLS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterPill(
                        label = "ALL (${logs.size})",
                        isSelected = selectedFilter == null,
                        color = Color(0xFFD0BCFF),
                        onClick = { selectedFilter = null }
                    )
                    FilterPill(
                        label = "IME (${logs.count { it.level == LogLevel.IME }})",
                        isSelected = selectedFilter == LogLevel.IME,
                        color = Color(0xFFD0BCFF),
                        onClick = { selectedFilter = LogLevel.IME }
                    )
                    FilterPill(
                        label = "INFO (${logs.count { it.level == LogLevel.INFO }})",
                        isSelected = selectedFilter == LogLevel.INFO,
                        color = Color(0xFFB4E5A2),
                        onClick = { selectedFilter = LogLevel.INFO }
                    )
                    FilterPill(
                        label = "WARN (${logs.count { it.level == LogLevel.WARN }})",
                        isSelected = selectedFilter == LogLevel.WARN,
                        color = Color(0xFFFFD966),
                        onClick = { selectedFilter = LogLevel.WARN }
                    )
                    FilterPill(
                        label = "ERROR (${logs.count { it.level == LogLevel.ERROR }})",
                        isSelected = selectedFilter == LogLevel.ERROR,
                        color = Color(0xFFF2B8B5),
                        onClick = { selectedFilter = LogLevel.ERROR }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // DIAGNOSTIC STATUS ACCORDION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDiagnosticCard = !showDiagnosticCard },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211F26)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keyboard Activation & Device Diagnostics",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFFE6E1E5)
                        )
                    }
                    Text(
                        text = if (showDiagnosticCard) "Hide" else "Show",
                        fontSize = 11.sp,
                        color = Color(0xFFD0BCFF)
                    )
                }

                AnimatedVisibility(visible = showDiagnosticCard) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        DiagnosticItem(
                            label = "Device",
                            value = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
                        )
                        DiagnosticItem(
                            label = "Keyboard Enabled in Android Settings",
                            value = if (isImeEnabled) "YES (Active in System)" else "NO (Tap 'Enable in Settings' below)",
                            isGood = isImeEnabled
                        )
                        DiagnosticItem(
                            label = "AutoTyper Currently Selected",
                            value = if (isImeSelected) "YES (Default Input Method)" else "NO (Tap 'Switch Keyboard' below)",
                            isGood = isImeSelected
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onOpenImeSettings,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isImeEnabled) Color(0xFFD0BCFF) else Color(0xFF2B2930),
                                    contentColor = if (!isImeEnabled) Color(0xFF381E72) else Color(0xFFE6E1E5)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("1. Enable in Settings", fontSize = 11.sp)
                            }
                            Button(
                                onClick = onSwitchKeyboard,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isImeEnabled && !isImeSelected) Color(0xFFD0BCFF) else Color(0xFF2B2930),
                                    contentColor = if (isImeEnabled && !isImeSelected) Color(0xFF381E72) else Color(0xFFE6E1E5)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("2. Switch Keyboard", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // LOG ITEMS LIST
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF211F26), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (logs.isEmpty()) "No logs captured yet. Events will appear here automatically." else "No logs match current search/filter.",
                    color = Color(0xFF938F99),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141318))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredLogs, key = { it.id }) { logEntry ->
                    LogItemCard(logEntry = logEntry)
                }
            }
        }
    }
}

@Composable
fun LogItemCard(logEntry: LogEntry) {
    val levelColor = when (logEntry.level) {
        LogLevel.ERROR -> Color(0xFFF2B8B5)
        LogLevel.WARN -> Color(0xFFFFD966)
        LogLevel.INFO -> Color(0xFFB4E5A2)
        LogLevel.DEBUG -> Color(0xFFCCC2DC)
        LogLevel.IME -> Color(0xFFD0BCFF)
    }

    val badgeBg = when (logEntry.level) {
        LogLevel.ERROR -> Color(0xFF601410)
        LogLevel.WARN -> Color(0xFF4A3B00)
        LogLevel.INFO -> Color(0xFF1D3B14)
        LogLevel.DEBUG -> Color(0xFF332D41)
        LogLevel.IME -> Color(0xFF381E72)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F1D24), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF332D41), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = logEntry.level.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = logEntry.tag,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD0BCFF)
                )
            }

            Text(
                text = logEntry.formattedTime,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF938F99)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = logEntry.message,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFE6E1E5)
        )

        if (!logEntry.throwableString.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = logEntry.throwableString,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFF2B8B5)
            )
        }
    }
}

@Composable
fun FilterPill(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF1C1B1F))
            .border(1.dp, if (isSelected) color else Color(0xFF49454F), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else Color(0xFFCAC4D0)
        )
    }
}

@Composable
fun DiagnosticItem(
    label: String,
    value: String,
    isGood: Boolean? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFCAC4D0)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = when (isGood) {
                true -> Color(0xFFB4E5A2)
                false -> Color(0xFFF2B8B5)
                else -> Color(0xFFD0BCFF)
            }
        )
    }
}
