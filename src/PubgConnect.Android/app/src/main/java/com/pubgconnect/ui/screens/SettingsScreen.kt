package com.pubgconnect.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubgconnect.api.ApiClient
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

    var shareStatus by remember(currentUser) { mutableStateOf(currentUser?.shareStatus ?: true) }
    var allowFriendRequests by remember(currentUser) { mutableStateOf(currentUser?.allowFriendRequests ?: true) }
    var showPlayingDuration by remember(currentUser) { mutableStateOf(currentUser?.showPlayingDuration ?: true) }

    var notificationsEnabled by remember { mutableStateOf(viewModel.sessionManager.isNotificationsEnabled) }
    var soundEnabled by remember { mutableStateOf(viewModel.sessionManager.isSoundEnabled) }
    var vibrateEnabled by remember { mutableStateOf(viewModel.sessionManager.isVibrateEnabled) }

    var serverUrl by remember { mutableStateOf(viewModel.sessionManager.serverUrl) }

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
            text = "Manage privacy, notifications, and connection",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        PubgDivider(modifier = Modifier.padding(bottom = 16.dp))

        // Profile Overview Card
        GlassCard {
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
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Friend ID: ",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    text = currentUser?.friendId ?: "------",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Controls Card (Section 15)
        GlassCard {
            Text(
                text = "🔒 Privacy & Status Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SettingSwitchRow(
                title = "Share PUBG Status",
                subtitle = "Broadcast when you are playing PUBG Mobile to friends",
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
                subtitle = "Display elapsed playing time to friends (e.g. 14 min)",
                checked = showPlayingDuration,
                onCheckedChange = {
                    showPlayingDuration = it
                    viewModel.updatePrivacySettings(shareStatus, allowFriendRequests, showPlayingDuration)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Controls Card (Section 16)
        GlassCard {
            Text(
                text = "🔔 Desktop & Push Notifications",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SettingSwitchRow(
                title = "Push Notifications",
                subtitle = "Receive alerts when friends launch PUBG on PC or Android",
                checked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                    viewModel.sessionManager.isNotificationsEnabled = it
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingSwitchRow(
                title = "Notification Sound",
                subtitle = "Play notification sound when friend goes online",
                checked = soundEnabled,
                onCheckedChange = {
                    soundEnabled = it
                    viewModel.sessionManager.isSoundEnabled = it
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingSwitchRow(
                title = "Vibrate on Alert",
                subtitle = "Vibrate device when friend starts PUBG",
                checked = vibrateEnabled,
                onCheckedChange = {
                    vibrateEnabled = it
                    viewModel.sessionManager.isVibrateEnabled = it
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Server Connection Card
        GlassCard {
            Text(
                text = "🌐 Backend Server Connection",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ModernTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = "Server URL",
                placeholder = "http://10.0.2.2:5000"
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryButton(
                text = "Update Server URL",
                onClick = {
                    viewModel.sessionManager.serverUrl = serverUrl
                    ApiClient.updateBaseUrl(serverUrl)
                    viewModel.startRealtimeAndSync()
                    Toast.makeText(context, "Server URL updated & synced!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Logout Button
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Text(text = "🚪  Log Out", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
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
