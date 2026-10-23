package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.RecordingSettings
import com.example.service.ScreenRecorderManager
import com.example.service.VideoQuality
import com.example.service.WebcamShape
import com.example.service.WebcamSize

import com.example.ads.AdConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: RecordingSettings,
    totalStorageBytes: Long,
    totalRecordingsCount: Int,
    isInterstitialReady: Boolean = false,
    isRewardedReady: Boolean = false,
    isAppOpenReady: Boolean = false,
    onShowInterstitialClick: () -> Unit = {},
    onShowRewardedClick: () -> Unit = {},
    onShowAppOpenClick: () -> Unit = {},
    onBack: () -> Unit,
    onClearAllRecordings: () -> Unit,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    var showClearDialog by remember { mutableStateOf(false) }

    val mbStr = String.format(java.util.Locale.US, "%.1f MB", totalStorageBytes / (1024f * 1024f))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Quality Card
            SettingsGroupCard(title = "Video Encoding & Quality", icon = Icons.Default.Videocam) {
                Text(
                    text = "Resolution & Frame Rate",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoQuality.values().forEach { quality ->
                        FilterChip(
                            selected = settings.videoQuality == quality,
                            onClick = {
                                ScreenRecorderManager.updateSettings { it.copy(videoQuality = quality) }
                            },
                            label = { Text(quality.label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Countdown Timer Card
            SettingsGroupCard(title = "Countdown Timer", icon = Icons.Default.Timer) {
                Text(
                    text = "Pre-recording countdown before capture starts",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "None (Instant)", 3 to "3 Seconds", 5 to "5 Seconds").forEach { (sec, label) ->
                        FilterChip(
                            selected = settings.countdownSeconds == sec,
                            onClick = {
                                ScreenRecorderManager.updateSettings { it.copy(countdownSeconds = sec) }
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Webcam PIP Preferences
            SettingsGroupCard(title = "FaceCam Overlay Preferences", icon = Icons.Default.Videocam) {
                Text(text = "Overlay Shape", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WebcamShape.values().forEach { shape ->
                        FilterChip(
                            selected = settings.webcamShape == shape,
                            onClick = {
                                ScreenRecorderManager.updateSettings { it.copy(webcamShape = shape) }
                            },
                            label = { Text(if (shape == WebcamShape.CIRCLE) "Circle Bubble" else "Rounded Rect", fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "Overlay Size", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WebcamSize.values().forEach { size ->
                        FilterChip(
                            selected = settings.webcamSize == size,
                            onClick = {
                                ScreenRecorderManager.updateSettings { it.copy(webcamSize = size) }
                            },
                            label = { Text("${size.name.lowercase().replaceFirstChar { it.uppercase() }} (${size.sizeDp}dp)", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Theme Toggle Card
            SettingsGroupCard(title = "Appearance", icon = if (settings.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (settings.isDarkMode) "Linear Obsidian Dark" else "Clean Light Theme",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        modifier = Modifier.testTag("settings_theme_switch")
                    )
                }
            }

            // Keyboard Shortcuts Card
            SettingsGroupCard(title = "Keyboard & Action Shortcuts", icon = Icons.Default.Keyboard) {
                Text(
                    text = "Quick shortcuts for hardware keyboards or gamepads:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ShortcutRow("Space / Enter", "Start or Stop Recording")
                ShortcutRow("P", "Pause or Resume Recording")
                ShortcutRow("Escape / Back", "Close preview or cancel countdown")
                ShortcutRow("D", "Delete selected recording")
            }

            // Storage & Maintenance
            SettingsGroupCard(title = "Storage & Cleanup", icon = Icons.Default.Storage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Used by ScreenFlow", fontWeight = FontWeight.SemiBold)
                        Text("$totalRecordingsCount videos ($mbStr)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("clear_all_recordings_button")
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Clear All")
                    }
                }
            }

            // Google AdMob Ads Integration Center
            SettingsGroupCard(title = "Google AdMob Monetization", icon = Icons.Default.MonetizationOn) {
                Text(
                    text = "Configured and active AdMob ad units:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Banner Ad Info
                AdUnitStatusRow(
                    name = "Banner Ad",
                    id = AdConfig.BANNER_AD_ID,
                    statusText = "Active (Bottom Dock)",
                    isReady = true,
                    actionButton = null
                )

                // Interstitial Ad
                AdUnitStatusRow(
                    name = "Interstitial Ad",
                    id = AdConfig.INTERSTITIAL_AD_ID,
                    statusText = if (isInterstitialReady) "Ready to show" else "Loaded",
                    isReady = isInterstitialReady,
                    actionButton = {
                        Button(
                            onClick = onShowInterstitialClick,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("test_interstitial_ad_button")
                        ) {
                            Text("Show", fontSize = 11.sp)
                        }
                    }
                )

                // Rewarded Ad
                AdUnitStatusRow(
                    name = "Rewarded Ad",
                    id = AdConfig.REWARDED_AD_ID,
                    statusText = if (isRewardedReady) "Ready to watch" else "Loaded",
                    isReady = isRewardedReady,
                    actionButton = {
                        Button(
                            onClick = onShowRewardedClick,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("test_rewarded_ad_button")
                        ) {
                            Text("Watch", fontSize = 11.sp)
                        }
                    }
                )

                // App Open Ad
                AdUnitStatusRow(
                    name = "App Open Ad",
                    id = AdConfig.APP_OPEN_AD_ID,
                    statusText = if (isAppOpenReady) "Ready" else "Loaded",
                    isReady = isAppOpenReady,
                    actionButton = {
                        Button(
                            onClick = onShowAppOpenClick,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("test_app_open_ad_button")
                        ) {
                            Text("Show", fontSize = 11.sp)
                        }
                    }
                )
            }

            // Privacy & Security Pledge
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("100% On-Device Privacy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "ScreenFlow encodes all videos directly onto your local device hardware. No videos, audio, or metadata are ever transmitted to an external server.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Recordings?") },
            text = { Text("This will permanently delete all $totalRecordingsCount recordings ($mbStr) from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllRecordings()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
fun ShortcutRow(shortcut: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = shortcut,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AdUnitStatusRow(
    name: String,
    id: String,
    statusText: String,
    isReady: Boolean,
    actionButton: (@Composable () -> Unit)?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .background(
                                if (isReady) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = id,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            actionButton?.invoke()
        }
    }
}

