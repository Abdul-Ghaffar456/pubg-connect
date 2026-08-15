package com.pubgconnect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubgconnect.models.FriendDto
import com.pubgconnect.models.UserStatus
import com.pubgconnect.ui.components.GlassCard
import com.pubgconnect.ui.components.PlatformBadge
import com.pubgconnect.ui.components.PubgDivider
import com.pubgconnect.ui.components.StatusDot
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun FriendsScreen(viewModel: MainViewModel) {
    val friends by viewModel.friends.collectAsState()
    val onlineCount = friends.count { it.status != UserStatus.OFFLINE }
    val totalCount = friends.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FRIENDS LIST",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "$onlineCount online of $totalCount friends",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            IconButton(onClick = { viewModel.loadFriends() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = AccentGreen
                )
            }
        }

        PubgDivider(modifier = Modifier.padding(bottom = 16.dp))

        if (friends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No friends added yet.\nGo to 'Add Friend' and enter your friend's 6-character ID!",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(friends, key = { it.id }) { friend ->
                    FriendCard(
                        friend = friend,
                        onToggleMute = { viewModel.toggleMuteFriend(friend) },
                        onRemove = { viewModel.removeFriend(friend.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FriendCard(
    friend: FriendDto,
    onToggleMute: () -> Unit,
    onRemove: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                StatusDot(status = friend.status)

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = friend.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${friend.friendId})",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val statusText = when (friend.status) {
                        UserStatus.PLAYING_PUBG -> "🟢 Playing PUBG Mobile"
                        UserStatus.ONLINE -> "🔵 Online"
                        UserStatus.OFFLINE -> "⚫ Offline"
                    }

                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = if (friend.status == UserStatus.PLAYING_PUBG) AccentGreen else TextSecondary
                    )

                    // Platform and Duration tag (if playing)
                    if (friend.status == UserStatus.PLAYING_PUBG) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlatformBadge(platform = friend.platform)
                            Spacer(modifier = Modifier.width(6.dp))
                            if (friend.showPlayingDuration) {
                                Text(
                                    text = "• ${friend.playingDurationMinutes} min",
                                    fontSize = 11.sp,
                                    color = AccentGreen
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons (Mute / Remove)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleMute) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Toggle Mute",
                        tint = if (friend.isNotificationMuted) TextMuted else AccentGreen
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Friend",
                        tint = AccentRed
                    )
                }
            }
        }
    }
}
