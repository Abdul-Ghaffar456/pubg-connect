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
import com.pubgconnect.models.ActivityItemDto
import com.pubgconnect.ui.components.GlassCard
import com.pubgconnect.ui.components.PlatformBadge
import com.pubgconnect.ui.components.PubgDivider
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun ActivityScreen(viewModel: MainViewModel) {
    val activities by viewModel.activities.collectAsState()

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
                    text = "RECENT ACTIVITY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Live cross-platform game launches",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            IconButton(onClick = { viewModel.loadActivity() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Activity",
                    tint = AccentGreen
                )
            }
        }

        PubgDivider(modifier = Modifier.padding(bottom = 16.dp))

        if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent activity recorded.\nWhen friends start PUBG Mobile on GameLoop or Android, it will appear here.",
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
                items(activities, key = { it.id }) { activity ->
                    ActivityCard(activity = activity)
                }
            }
        }
    }
}

@Composable
fun ActivityCard(activity: ActivityItemDto) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🎮", fontSize = 24.sp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activity.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activity.actionDescription,
                        fontSize = 13.sp,
                        color = AccentGreen
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(platform = activity.platform)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activity.timestamp.take(19).replace("T", " "),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
