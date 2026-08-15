package com.pubgconnect.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pubgconnect.models.FriendDto
import com.pubgconnect.models.PlatformType
import com.pubgconnect.preferences.UserSessionManager
import com.pubgconnect.ui.MainActivity

object PubgNotificationManager {

    private const val CHANNEL_ID = "pubg_friend_alerts"
    private const val CHANNEL_NAME = "PUBG Friend Alerts"
    private var isChannelCreated = false

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isChannelCreated) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when your friends start PUBG Mobile on PC GameLoop or Android"
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            isChannelCreated = true
        }
    }

    /**
     * Triggers smart notification ONLY when a friend transitions from OFFLINE -> PLAYING_PUBG
     */
    fun showFriendStartedPubgNotification(context: Context, friend: FriendDto) {
        val session = UserSessionManager(context)

        // Respect global notification setting & per-friend mute
        if (!session.isNotificationsEnabled || friend.isNotificationMuted) {
            return
        }

        createNotificationChannel(context)

        val platformText = when (friend.platform) {
            PlatformType.GAMELOOP -> "🖥 GameLoop"
            PlatformType.ANDROID -> "📱 Android"
            else -> "🎮 PUBG Mobile"
        }

        val title = "🎮 PUBG CONNECT"
        val message = "${friend.username} is Online! Started PUBG Mobile ($platformText)"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            friend.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\nTap to join or view friend list."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (session.isSoundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        if (session.isVibrateEnabled) {
            builder.setVibrate(longArrayOf(0, 250, 100, 250))
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(friend.id.hashCode(), builder.build())
    }

    fun showGenericNotification(context: Context, title: String, message: String, notifId: Int = 100) {
        val session = UserSessionManager(context)
        if (!session.isNotificationsEnabled) return

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notifId, builder.build())
    }
}
