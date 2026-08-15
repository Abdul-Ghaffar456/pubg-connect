package com.pubgconnect.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubgconnect.models.FriendRequestDto
import com.pubgconnect.ui.components.GlassCard
import com.pubgconnect.ui.components.ModernTextField
import com.pubgconnect.ui.components.PrimaryButton
import com.pubgconnect.ui.components.PubgDivider
import com.pubgconnect.ui.components.SecondaryButton
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun RequestsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val requests by viewModel.pendingRequests.collectAsState()
    val searchResult by viewModel.searchResultUser.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(if (requests.isNotEmpty()) 0 else 1) }
    var searchFriendId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "REQUESTS & ADD",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${requests.size} pending requests",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            IconButton(onClick = {
                viewModel.loadPendingRequests()
                Toast.makeText(context, "Requests refreshed", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = AccentGreen
                )
            }
        }

        // Segmented Tab Switcher (Incoming Requests vs Add Friend)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selectedTab == 0) AccentGreen else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Requests",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) Color.White else TextSecondary,
                        fontSize = 13.sp
                    )
                    if (requests.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 0) Color.White else AccentGreen
                        ) {
                            Text(
                                text = "${requests.size}",
                                color = if (selectedTab == 0) AccentGreen else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selectedTab == 1) AccentGreen else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Add Friend",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 1) Color.White else TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        PubgDivider(modifier = Modifier.padding(bottom = 14.dp))

        if (selectedTab == 0) {
            // TAB 1: Incoming Friend Requests
            if (requests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📩", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Pending Requests",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When other players send you a friend request using your ID, they will appear here.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(requests, key = { it.requestId }) { req ->
                        RequestCard(
                            request = req,
                            onAccept = {
                                viewModel.respondToFriendRequest(req.requestId, true)
                                Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show()
                            },
                            onDecline = {
                                viewModel.respondToFriendRequest(req.requestId, false)
                                Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        } else {
            // TAB 2: Search & Add Friend
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Your Friend ID Card
                GlassCard(
                    backgroundColor = CardBg,
                    onClick = {
                        currentUser?.let { user ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Friend ID", user.friendId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Your Friend ID copied: ${user.friendId}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "YOUR FRIEND ID (TAP TO COPY)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentUser?.friendId ?: "------",
                                fontSize = 22.sp,
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
                                text = "📋 Copy ID",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input Card
                GlassCard {
                    Text(
                        text = "Search Friend by ID",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Enter a 6-character Friend ID (e.g. A7K92D)",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    ModernTextField(
                        value = searchFriendId,
                        onValueChange = { searchFriendId = it.uppercase().trim() },
                        label = "Friend ID",
                        placeholder = "e.g. B3M88X",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            if (searchFriendId.isNotBlank()) {
                                viewModel.searchFriend(searchFriendId) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }),
                        trailingIcon = {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                if (searchFriendId.isNotBlank()) {
                                    viewModel.searchFriend(searchFriendId) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentGreen)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PrimaryButton(
                        text = "Search Player",
                        onClick = {
                            focusManager.clearFocus()
                            if (searchFriendId.isNotBlank()) {
                                viewModel.searchFriend(searchFriendId) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        isLoading = isLoading
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Result Card
                searchResult?.let { user ->
                    GlassCard(
                        borderColor = AccentGreen,
                        backgroundColor = Color(0xFF132320)
                    ) {
                        Text(
                            text = "✅ PLAYER FOUND",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.username,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Friend ID: ${user.friendId}",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }

                            PrimaryButton(
                                text = "Send Request",
                                onClick = {
                                    viewModel.sendFriendRequest(user.friendId) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    request: FriendRequestDto,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = request.senderUsername,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Friend ID: ${request.senderFriendId}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGreen
                    )
                }

                Text(
                    text = request.sentAt.take(10),
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "Decline",
                    onClick = onDecline,
                    textColor = AccentRed,
                    borderColor = AccentRed.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )

                PrimaryButton(
                    text = "Accept",
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
