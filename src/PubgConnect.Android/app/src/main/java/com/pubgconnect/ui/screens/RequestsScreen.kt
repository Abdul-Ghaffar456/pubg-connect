package com.pubgconnect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.pubgconnect.models.FriendRequestDto
import com.pubgconnect.ui.components.GlassCard
import com.pubgconnect.ui.components.PubgDivider
import com.pubgconnect.ui.components.SecondaryButton
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun RequestsScreen(viewModel: MainViewModel) {
    val requests by viewModel.pendingRequests.collectAsState()

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
                    text = "FRIEND REQUESTS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${requests.size} pending incoming requests",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            IconButton(onClick = { viewModel.loadPendingRequests() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Requests",
                    tint = AccentGreen
                )
            }
        }

        PubgDivider(modifier = Modifier.padding(bottom = 16.dp))

        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No pending friend requests.\nWhen other players send you a request with your Friend ID, it will appear here.",
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
                items(requests, key = { it.requestId }) { req ->
                    RequestCard(
                        request = req,
                        onAccept = { viewModel.respondToFriendRequest(req.requestId, true) },
                        onDecline = { viewModel.respondToFriendRequest(req.requestId, false) }
                    )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.senderUsername,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Friend ID: ${request.senderFriendId}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Sent: ${request.sentAt.take(19).replace("T", " ")}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = "Decline",
                    onClick = onDecline,
                    textColor = AccentRed
                )

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Accept", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
