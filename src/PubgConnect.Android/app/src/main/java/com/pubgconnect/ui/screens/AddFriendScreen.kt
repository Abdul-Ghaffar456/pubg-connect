package com.pubgconnect.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubgconnect.ui.components.GlassCard
import com.pubgconnect.ui.components.ModernTextField
import com.pubgconnect.ui.components.PrimaryButton
import com.pubgconnect.ui.components.PubgDivider
import com.pubgconnect.ui.components.SecondaryButton
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun AddFriendScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var searchFriendId by remember { mutableStateOf("") }
    val searchResult by viewModel.searchResultUser.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        Text(
            text = "ADD FRIEND",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Search players by their unique 6-character Friend ID",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        PubgDivider(modifier = Modifier.padding(bottom = 16.dp))

        // Your Friend ID Card
        GlassCard(backgroundColor = CardBg) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "YOUR FRIEND ID",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentUser?.friendId ?: "------",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }

                Text(
                    text = "Share this with friends",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input Card
        GlassCard {
            ModernTextField(
                value = searchFriendId,
                onValueChange = { searchFriendId = it.uppercase().trim() },
                label = "Friend ID",
                placeholder = "e.g. B3M88X"
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryButton(
                text = "Search User",
                onClick = {
                    if (searchFriendId.isNotBlank()) {
                        viewModel.searchFriend(searchFriendId) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                isLoading = isLoading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Result Card
        searchResult?.let { user ->
            GlassCard(borderColor = AccentGreen) {
                Text(
                    text = "USER FOUND",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
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

                    SecondaryButton(
                        text = "Send Request",
                        onClick = {
                            viewModel.sendFriendRequest(user.friendId) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}
