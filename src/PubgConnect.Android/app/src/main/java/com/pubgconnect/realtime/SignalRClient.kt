package com.pubgconnect.realtime

import android.util.Log
import com.microsoft.signalr.Action
import com.microsoft.signalr.Action1
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.microsoft.signalr.OnClosedCallback
import com.pubgconnect.models.FriendDto
import com.pubgconnect.models.HeartbeatRequest
import com.pubgconnect.models.PlatformType
import com.pubgconnect.models.UserStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SignalRClient {

    companion object {
        private const val TAG = "PubgSignalR"
    }

    private var hubConnection: HubConnection? = null
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentUserId: String = ""
    private var currentServerUrl: String = ""
    private var currentStatus: UserStatus = UserStatus.OFFLINE
    @Volatile private var reconnectEnabled = false
    @Volatile private var connectionGeneration = 0L

    // Callbacks
    var onFriendStatusChanged: ((FriendDto) -> Unit)? = null
    var onFriendRequestReceived: (() -> Unit)? = null
    var onFriendRequestAccepted: (() -> Unit)? = null
    var onFriendRemoved: ((String) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null

    val isConnected: Boolean
        get() = hubConnection?.connectionState == HubConnectionState.CONNECTED

    fun connect(userId: String, serverUrl: String) {
        stopConnection(allowReconnect = false)
        currentUserId = userId
        currentServerUrl = serverUrl
        // A running signed-in client is online even when PUBG is not running.
        if (currentStatus == UserStatus.OFFLINE) currentStatus = UserStatus.ONLINE
        reconnectEnabled = true
        val generation = ++connectionGeneration
        val hubUrl = "${serverUrl.trimEnd('/')}/hub/status"
        Log.d(TAG, "Connecting to SignalR hub at $hubUrl")

        try {
            val connection = HubConnectionBuilder.create(hubUrl).build()

            // Event Listeners with explicit Action types
            connection.on("OnFriendStatusChanged", Action1<FriendDto> { friend ->
                Log.d(TAG, "FriendStatusChanged received: ${friend.username} -> ${friend.status} (${friend.platform})")
                onFriendStatusChanged?.invoke(friend)
            }, FriendDto::class.java)

            connection.on("OnFriendRequestReceived", Action {
                Log.d(TAG, "FriendRequestReceived event received")
                onFriendRequestReceived?.invoke()
            })

            connection.on("OnFriendRequestAccepted", Action {
                Log.d(TAG, "FriendRequestAccepted event received")
                onFriendRequestAccepted?.invoke()
            })

            connection.on("OnFriendRemoved", Action1<String> { friendUserId ->
                Log.d(TAG, "FriendRemoved event received for user: $friendUserId")
                onFriendRemoved?.invoke(friendUserId)
            }, String::class.java)

            connection.onClosed(OnClosedCallback {
                Log.d(TAG, "SignalR connection closed. Scheduling automatic reconnect...")
                onConnectionStateChanged?.invoke(false)
                scope.launch {
                    delay(5000)
                    if (reconnectEnabled && generation == connectionGeneration &&
                        currentUserId.isNotBlank() && currentServerUrl.isNotBlank()) {
                        connect(currentUserId, currentServerUrl)
                    }
                }
            })

            hubConnection = connection

            // Start connection in coroutine
            scope.launch {
                try {
                    connection.start().blockingAwait()
                    connection.send("RegisterConnection", currentUserId)
                    sendHeartbeat(currentStatus, PlatformType.ANDROID)
                    Log.d(TAG, "SignalR connection successfully established.")
                    onConnectionStateChanged?.invoke(true)
                    startHeartbeatLoop()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start SignalR: ${e.message}", e)
                    onConnectionStateChanged?.invoke(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SignalR setup error: ${e.message}", e)
            onConnectionStateChanged?.invoke(false)
        }
    }

    fun sendHeartbeat(status: UserStatus, platform: PlatformType = PlatformType.ANDROID) {
        currentStatus = status
        if (isConnected) {
            scope.launch {
                try {
                    val req = HeartbeatRequest(status = status, platform = platform)
                    hubConnection?.send("SendHeartbeat", req)
                    Log.d(TAG, "Heartbeat sent: status=$status, platform=$platform")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to dispatch heartbeat: ${e.message}")
                }
            }
        }
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30_000) // 30-second heartbeat
                if (isConnected && currentStatus != UserStatus.OFFLINE) {
                    sendHeartbeat(currentStatus, PlatformType.ANDROID)
                }
            }
        }
    }

    fun disconnect() {
        reconnectEnabled = false
        connectionGeneration++
        currentStatus = UserStatus.OFFLINE
        currentUserId = ""
        currentServerUrl = ""
        stopConnection(allowReconnect = false)
        onConnectionStateChanged?.invoke(false)
    }

    private fun stopConnection(allowReconnect: Boolean) {
        reconnectEnabled = allowReconnect
        heartbeatJob?.cancel()
        heartbeatJob = null
        try {
            hubConnection?.stop()
            hubConnection = null
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting SignalR: ${e.message}")
        }
    }
}
