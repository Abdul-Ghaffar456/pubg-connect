package com.pubgconnect.detection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pubgconnect.models.PlatformType
import com.pubgconnect.models.UserStatus
import com.pubgconnect.preferences.UserSessionManager
import com.pubgconnect.realtime.SignalRClient
import com.pubgconnect.ui.MainActivity
import kotlinx.coroutines.*

class PubgDetectionService : Service() {

    companion object {
        private const val TAG = "PubgDetectionService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "pubg_service_channel"

        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, PubgDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PubgDetectionService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var sessionManager: UserSessionManager
    private val signalRClient = SignalRClient()
    private var isPlayingPubgState = false

    override fun onCreate() {
        super.onCreate()
        sessionManager = UserSessionManager(this)
        createNotificationChannel()

        val notification = buildForegroundNotification("Active • Monitoring PUBG Mobile")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        isRunning = true
        Log.d(TAG, "PubgDetectionService started.")

        // Connect real-time client if logged in
        val user = sessionManager.currentUser
        if (user != null && sessionManager.isLoggedIn) {
            signalRClient.connect(user.id, sessionManager.serverUrl)
        }

        startDetectionLoop()
    }

    private fun startDetectionLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val isSimulated = sessionManager.isSimulationMode
                    var isCurrentlyPlaying = if (isSimulated) {
                        SimulationController.isSimulatedPubgActive.value
                    } else {
                        PubgDetector.isPubgRunning(applicationContext)
                    }

                    // Double-confirmation verification to prevent false positive notifications
                    if (isCurrentlyPlaying && !isPlayingPubgState) {
                        delay(2500)
                        isCurrentlyPlaying = if (isSimulated) {
                            SimulationController.isSimulatedPubgActive.value
                        } else {
                            PubgDetector.isPubgRunning(applicationContext)
                        }
                    }

                    if (isCurrentlyPlaying != isPlayingPubgState) {
                        isPlayingPubgState = isCurrentlyPlaying
                        Log.i(TAG, "PUBG state changed -> isPlaying: $isPlayingPubgState")

                        val newStatus = if (isPlayingPubgState) UserStatus.PLAYING_PUBG else UserStatus.ONLINE
                        signalRClient.sendHeartbeat(newStatus, PlatformType.ANDROID)

                        val notifText = if (isPlayingPubgState) {
                            "🟢 Playing PUBG Mobile (Android)"
                        } else {
                            "⚫ Active • Monitoring PUBG Mobile"
                        }
                        updateNotification(notifText)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in detection loop: ${e.message}")
                }

                // Adaptive sleep: 8s if playing, 15s if idle
                val checkInterval = if (isPlayingPubgState) 8_000L else 15_000L
                delay(checkInterval)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PUBG Connect Background Monitor",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps PUBG Mobile status detection active with minimal power consumption."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PUBG Connect")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildForegroundNotification(statusText))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob.cancel()
        signalRClient.disconnect()
        Log.d(TAG, "PubgDetectionService destroyed.")
    }
}
