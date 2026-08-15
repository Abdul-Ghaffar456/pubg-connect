package com.pubgconnect.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubgconnect.api.ApiClient
import com.pubgconnect.detection.PubgDetector
import com.pubgconnect.ui.components.GlassCard
import com.pubgconnect.ui.components.ModernTextField
import com.pubgconnect.ui.components.PrimaryButton
import com.pubgconnect.ui.components.PubgDivider
import com.pubgconnect.ui.components.SecondaryButton
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val isPubgInstalled by viewModel.isPubgInstalled.collectAsState()
    val isSimulatedActive by viewModel.isSimulatedPubgActive.collectAsState()

    var shareStatus by remember(currentUser) { mutableStateOf(currentUser?.shareStatus ?: true) }
    var allowFriendRequests by remember(currentUser) { mutableStateOf(currentUser?.allowFriendRequests ?: true) }
    var showPlayingDuration by remember(currentUser) { mutableStateOf(currentUser?.showPlayingDuration ?: true) }

    var notificationsEnabled by remember { mutableStateOf(viewModel.sessionManager.isNotificationsEnabled) }
    var soundEnabled by remember { mutableStateOf(viewModel.sessionManager.isSoundEnabled) }
    var vibrateEnabled by remember { mutableStateOf(viewModel.sessionManager.isVibrateEnabled) }

    var isSimModeEnabled by remember { mutableStateOf(viewModel.sessionManager.isSimulationMode) }
    var serverUrl by remember { mutableStateOf(viewModel.sessionManager.serverUrl) }

    LaunchedEffect(Unit) {
        viewModel.refreshUsageAccessStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "SETTINGS & PRIVACY",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Manage detection, notifications, and cloud sync",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        PubgDivider(modifier = Modifier.padding(bottom = 16.dp))

        // 1. Account Profile Card
        GlassCard(
            onClick = {
                currentUser?.let { user ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Friend ID", user.friendId)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Friend ID copied: ${user.friendId}", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.username ?: "Player",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = currentUser?.email ?: "",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Friend ID: ${currentUser?.friendId ?: "------"} (Tap to copy)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardHover,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Text(
                        text = "📋 Copy",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. PUBG Mobile Auto-Detection & Permissions Card
        GlassCard(
            borderColor = if (hasUsageAccess) AccentGreen else AccentYellow
        ) {
            Text(
                text = "🎮 Game Detection & Permissions",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Usage Access
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Android Usage Access",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (hasUsageAccess) "✅ Enabled - Auto-detects PUBG launch" else "⚠️ Required to auto-detect game launch",
                        fontSize = 11.sp,
                        color = if (hasUsageAccess) AccentGreen else AccentYellow
                    )
                }

                if (!hasUsageAccess) {
                    SecondaryButton(
                        text = "Enable",
                        onClick = { PubgDetector.openUsageStatsSettings(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            PubgDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Game Package Status
            Text(
                text = if (isPubgInstalled) "✅ PUBG Mobile Installed on Device" else "ℹ️ PUBG Mobile Not Installed (Test Mode available below)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPubgInstalled) AccentGreen else TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            PubgDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Simulation Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Interactive Test Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Simulate PUBG playing status without running the game",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = isSimModeEnabled,
                    onCheckedChange = {
                        isSimModeEnabled = it
                        viewModel.sessionManager.isSimulationMode = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentGreen,
                        checkedTrackColor = CardHover
                    )
                )
            }

            if (isSimModeEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSimulatedActive) "🟢 In Game" else "⚫ Offline",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSimulatedActive) AccentGreen else TextSecondary
                    )

                    PrimaryButton(
                        text = if (isSimulatedActive) "Simulate Close" else "Simulate Play",
                        onClick = { viewModel.toggleSimulatedPubg() },
                        modifier = Modifier.width(150.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Privacy Controls Card
        GlassCard {
            Text(
                text = "🔒 Privacy & Visibility",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SettingSwitchRow(
                title = "Share PUBG Status",
                subtitle = "Broadcast when you are playing PUBG to friends",
                checked = shareStatus,
                onCheckedChange = {
                    shareStatus = it
                    viewModel.updatePrivacySettings(shareStatus, allowFriendRequests, showPlayingDuration)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingSwitchRow(
                title = "Allow Friend Requests",
                subtitle = "Allow other users to search and add you by Friend ID",
                checked = allowFriendRequests,
                onCheckedChange = {
                    allowFriendRequests = it
                    viewModel.updatePrivacySettings(shareStatus, allowFriendRequests, showPlayingDuration)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingSwitchRow(
                title = "Show Playing Duration",
                subtitle = "Display elapsed playing time to friends",
                checked = showPlayingDuration,
                onCheckedChange = {
                    showPlayingDuration = it
                    viewModel.updatePrivacySettings(shareStatus, allowFriendRequests, showPlayingDuration)
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Notification Controls Card
        GlassCard {
            Text(
                text = "🔔 Notifications & Alerts",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SettingSwitchRow(
                title = "Push Notifications",
                subtitle = "Alert when friends start PUBG on GameLoop or Mobile",
                checked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                    viewModel.sessionManager.isNotificationsEnabled = it
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingSwitchRow(
                title = "Notification Sound",
                subtitle = "Play sound when alert arrives",
                checked = soundEnabled,
                onCheckedChange = {
                    soundEnabled = it
                    viewModel.sessionManager.isSoundEnabled = it
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingSwitchRow(
                title = "Vibrate on Alert",
                subtitle = "Vibrate phone when friend goes online",
                checked = vibrateEnabled,
                onCheckedChange = {
                    vibrateEnabled = it
                    viewModel.sessionManager.isVibrateEnabled = it
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Cloud Server Connection Card
        GlassCard {
            Text(
                text = "🌐 Cloud Server Connection",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            ModernTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = "Server URL",
                placeholder = "https://pubgconnect-backend.onrender.com"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SecondaryButton(
                text = "Save & Reconnect",
                onClick = {
                    viewModel.sessionManager.serverUrl = serverUrl
                    ApiClient.updateBaseUrl(serverUrl)
                    viewModel.startRealtimeAndSync()
                    Toast.makeText(context, "Server URL updated & synced!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. Logout Button
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = "🚪  Log Out", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentGreen,
                checkedTrackColor = CardHover
            )
        )
    }
}
