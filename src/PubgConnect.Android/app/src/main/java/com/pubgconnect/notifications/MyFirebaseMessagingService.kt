package com.pubgconnect.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pubgconnect.api.ApiClient
import com.pubgconnect.models.FCMDeviceTokenRequest
import com.pubgconnect.models.FriendDto
import com.pubgconnect.models.PlatformType
import com.pubgconnect.models.UserStatus
import com.pubgconnect.preferences.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "PubgFirebaseMsg"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM device token: $token")

        val session = UserSessionManager(applicationContext)
        val authToken = session.token

        if (!authToken.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ApiClient.getService().registerDeviceToken(
                        "Bearer $authToken",
                        FCMDeviceTokenRequest(deviceToken = token, platform = PlatformType.ANDROID)
                    )
                    Log.d(TAG, "FCM token registered with backend.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register FCM token with backend: ${e.message}")
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val friendName = remoteMessage.data["friend_name"] ?: "Your friend"
            val friendId = remoteMessage.data["friend_id"] ?: ""
            val platformStr = remoteMessage.data["platform"] ?: "Android"

            val platform = if (platformStr.equals("GameLoop", ignoreCase = true)) {
                PlatformType.GAMELOOP
            } else {
                PlatformType.ANDROID
            }

            if (type == "friend_pubg_started") {
                val mockFriend = FriendDto(
                    id = friendId,
                    username = friendName,
                    status = UserStatus.PLAYING_PUBG,
                    platform = platform
                )
                PubgNotificationManager.showFriendStartedPubgNotification(applicationContext, mockFriend)
            } else if (type == "friend_request") {
                PubgNotificationManager.showGenericNotification(
                    applicationContext,
                    "🎮 PUBG CONNECT",
                    "$friendName sent you a friend request!"
                )
            }
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            PubgNotificationManager.showGenericNotification(
                applicationContext,
                it.title ?: "🎮 PUBG CONNECT",
                it.body ?: "Friend status updated"
            )
        }
    }
}
