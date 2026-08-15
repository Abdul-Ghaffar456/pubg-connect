package com.pubgconnect.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubgconnect.models.FriendDto
import com.pubgconnect.models.UserStatus
import com.pubgconnect.ui.components.PlatformBadge
import com.pubgconnect.ui.components.PrimaryButton
import com.pubgconnect.ui.components.PubgDivider
import com.pubgconnect.ui.components.StatusDot
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun FriendsScreen(
    viewModel: MainViewModel,
    onNavigateToAddFriend: () -> Unit
) {
    val context = LocalContext.current
    val friends by viewModel.friends.collectAsState()
    val onlineCount = friends.count { it.status != UserStatus.OFFLINE }
    val playingCount = friends.count { it.status == UserStatus.PLAYING_PUBG }
    val totalCount = friends.size

    var friendToDelete by remember { mutableStateOf<FriendDto?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Screen Title & Refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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
                        text = "$totalCount total • $onlineCount active",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.loadFriends()
                        Toast.makeText(context, "Friends refreshed", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = AccentGreen
                    )
                }
            }

            // Quick Status Overview Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, if (playingCount > 0) AccentGreen else BorderDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🟢", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$playingCount In Game",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (playingCount > 0) AccentGreen else TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "👥", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$onlineCount Online",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }
            }

            PubgDivider(modifier = Modifier.padding(bottom = 12.dp))

            if (friends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎮", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Friends Added Yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Search a friend's 6-digit ID to receive notifications when they start playing PUBG Mobile!",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        PrimaryButton(
                            text = "+ Add First Friend",
                            onClick = onNavigateToAddFriend,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(friends, key = { it.id }) { friend ->
                        FriendCard(
                            friend = friend,
                            onToggleMute = {
                                viewModel.toggleMuteFriend(friend)
                                val state = if (friend.isNotificationMuted) "unmuted" else "muted"
                                Toast.makeText(context, "${friend.username} $state", Toast.LENGTH_SHORT).show()
                            },
                            onRemove = { friendToDelete = friend }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(70.dp))
                    }
                }
            }
        }

        // Floating Action Button to Add Friend
        FloatingActionButton(
            onClick = onNavigateToAddFriend,
            containerColor = AccentGreen,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Friend")
        }
    }

    // Delete Confirmation Dialog
    friendToDelete?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendToDelete = null },
            title = { Text(text = "Remove Friend?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to remove ${friend.username} from your friends list?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFriend(friend.id)
                        friendToDelete = null
                        Toast.makeText(context, "${friend.username} removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { friendToDelete = null }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = CardBg
        )
    }
}

@Composable
fun FriendCard(
    friend: FriendDto,
    onToggleMute: () -> Unit,
    onRemove: () -> Unit
) {
    val isPlaying = friend.status == UserStatus.PLAYING_PUBG

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFF0D1F1A) else CardBg
        ),
        border = BorderStroke(
            width = if (isPlaying) 1.5.dp else 1.dp,
            color = if (isPlaying) AccentGreen else BorderDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Avatar + Name + ID + Mute & Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar Circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) AccentGreen.copy(alpha = 0.25f) else CardHover)
                        .border(
                            1.5.dp,
                            if (isPlaying) AccentGreen else if (friend.status == UserStatus.ONLINE) AccentBlue else BorderDark,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.username.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isPlaying) AccentGreen else TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & ID Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ID: ${friend.friendId}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }

                // Actions: Mute / Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (friend.isNotificationMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = "Toggle Mute",
                            tint = if (friend.isNotificationMuted) TextMuted else AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Friend",
                            tint = AccentRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            PubgDivider(modifier = Modifier.alpha(0.35f))
            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Status (Dot + Text + Duration) & Platform Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    StatusDot(status = friend.status)
                    Spacer(modifier = Modifier.width(6.dp))

                    val (statusLabel, statusColor) = when (friend.status) {
                        UserStatus.PLAYING_PUBG -> Pair("Playing PUBG Mobile", AccentGreen)
                        UserStatus.ONLINE -> Pair("Online", AccentBlue)
                        UserStatus.OFFLINE -> Pair("Offline", TextSecondary)
                    }

                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isPlaying && friend.showPlayingDuration) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${friend.playingDurationMinutes}m)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            maxLines = 1
                        )
                    }
                }

                if (isPlaying) {
                    PlatformBadge(platform = friend.platform)
                }
            }
        }
    }
}
