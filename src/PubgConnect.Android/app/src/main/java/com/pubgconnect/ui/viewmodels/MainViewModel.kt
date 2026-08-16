package com.pubgconnect.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.pubgconnect.api.ApiClient
import com.pubgconnect.detection.PubgDetectionService
import com.pubgconnect.detection.PubgDetector
import com.pubgconnect.detection.SimulationController
import com.pubgconnect.models.*
import com.pubgconnect.notifications.PubgNotificationManager
import com.pubgconnect.preferences.UserSessionManager
import com.pubgconnect.realtime.SignalRClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PubgMainVM"
    }

    val sessionManager = UserSessionManager(application)
    private val signalRClient = SignalRClient()
    private var friendSyncJob: Job? = null

    // UI States
    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow(sessionManager.currentUser)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendDto>>(emptyList())
    val friends: StateFlow<List<FriendDto>> = _friends.asStateFlow()

    private val _activities = MutableStateFlow<List<ActivityItemDto>>(emptyList())
    val activities: StateFlow<List<ActivityItemDto>> = _activities.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<FriendRequestDto>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequestDto>> = _pendingRequests.asStateFlow()

    private val _searchResultUser = MutableStateFlow<UserDto?>(null)
    val searchResultUser: StateFlow<UserDto?> = _searchResultUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(PubgDetector.hasUsageStatsPermission(application))
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val _isPubgInstalled = MutableStateFlow(PubgDetector.isPubgInstalled(application))
    val isPubgInstalled: StateFlow<Boolean> = _isPubgInstalled.asStateFlow()

    val isSimulatedPubgActive = SimulationController.isSimulatedPubgActive

    // Memory of known friend states to detect OFFLINE -> PLAYING transitions strictly
    private val knownFriendStates = mutableMapOf<String, FriendDto>()

    init {
        PubgNotificationManager.createNotificationChannel(application)
        ApiClient.updateBaseUrl(sessionManager.serverUrl)

        setupSignalRHandlers()

        if (sessionManager.isLoggedIn) {
            validateSavedSession()
        }
    }

    private fun validateSavedSession() {
        val token = sessionManager.token ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().getMe("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    sessionManager.currentUser = user
                    _currentUser.value = user
                    startRealtimeAndSync()
                } else if (response.code() == 401) {
                    logout()
                    _statusMessage.value = "Your session expired after the server update. Please sign in again."
                } else {
                    startRealtimeAndSync()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not validate saved session: ${e.message}")
                startRealtimeAndSync()
            }
        }
    }

    private fun setupSignalRHandlers() {
        signalRClient.onFriendStatusChanged = { updatedFriend ->
            handleFriendStatusChanged(updatedFriend)
        }

        signalRClient.onConnectionStateChanged = { connected ->
            if (connected) {
                loadFriends()
                loadActivity()
            }
        }

        signalRClient.onFriendRequestReceived = {
            viewModelScope.launch {
                PubgNotificationManager.showGenericNotification(
                    getApplication(),
                    "🎮 PUBG CONNECT",
                    "You received a new friend request!"
                )
                loadPendingRequests()
            }
        }

        signalRClient.onFriendRequestAccepted = {
            viewModelScope.launch {
                loadFriends()
                loadActivity()
            }
        }

        signalRClient.onFriendRemoved = { friendUserId ->
            knownFriendStates.remove(friendUserId)
            viewModelScope.launch {
                loadFriends()
            }
        }
    }

    private fun handleFriendStatusChanged(updatedFriend: FriendDto) {
        val previous = knownFriendStates[updatedFriend.id]

        // Strictly check for OFFLINE -> PLAYING_PUBG transition to trigger notification
        if (previous == null || previous.status == UserStatus.OFFLINE) {
            if (updatedFriend.status == UserStatus.PLAYING_PUBG) {
                PubgNotificationManager.showFriendStartedPubgNotification(getApplication(), updatedFriend)
            }
        }

        knownFriendStates[updatedFriend.id] = updatedFriend

        // Update the visible list immediately. Waiting for a REST round-trip can
        // reintroduce stale data if it races with the real-time notification.
        _friends.value = _friends.value.map { friend ->
            if (friend.id == updatedFriend.id) updatedFriend else friend
        }

        // Refresh friend list and activities
        viewModelScope.launch {
            loadFriends()
            loadActivity()
        }
    }

    fun startRealtimeAndSync() {
        val user = sessionManager.currentUser
        if (user != null) {
            signalRClient.connect(user.id, sessionManager.serverUrl)
            loadFriends()
            loadActivity()
            loadPendingRequests()
            startPeriodicFriendSync()

            // Start foreground detection service
            PubgDetectionService.start(getApplication())
        }
    }

    private fun startPeriodicFriendSync() {
        friendSyncJob?.cancel()
        friendSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                loadFriends()
            }
        }
    }

    fun refreshUsageAccessStatus() {
        _hasUsageAccess.value = PubgDetector.hasUsageStatsPermission(getApplication())
        _isPubgInstalled.value = PubgDetector.isPubgInstalled(getApplication())
    }

    // --- Authentication ---

    fun login(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = ApiClient.getService().login(LoginRequest(email, pass))
                if (res.isSuccessful && res.body()?.success == true) {
                    val auth = res.body()!!
                    sessionManager.token = auth.token
                    sessionManager.currentUser = auth.user
                    _currentUser.value = auth.user
                    _isLoggedIn.value = true

                    startRealtimeAndSync()
                    onResult(true, "Login successful!")
                } else {
                    onResult(false, res.body()?.message ?: "Invalid email or password.")
                }
            } catch (e: Exception) {
                onResult(false, "Connection error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(username: String, email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = ApiClient.getService().register(RegisterRequest(username, email, pass))
                if (res.isSuccessful && res.body()?.success == true) {
                    val auth = res.body()!!
                    sessionManager.token = auth.token
                    sessionManager.currentUser = auth.user
                    _currentUser.value = auth.user
                    _isLoggedIn.value = true

                    startRealtimeAndSync()
                    onResult(true, "Account registered successfully!")
                } else {
                    onResult(false, res.body()?.message ?: "Registration failed.")
                }
            } catch (e: Exception) {
                onResult(false, "Connection error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        friendSyncJob?.cancel()
        friendSyncJob = null
        PubgDetectionService.stop(getApplication())
        signalRClient.disconnect()
        sessionManager.logout()
        _isLoggedIn.value = false
        _currentUser.value = null
        _friends.value = emptyList()
        _activities.value = emptyList()
        _pendingRequests.value = emptyList()
        knownFriendStates.clear()
    }

    // --- Friends & Activity ---

    fun loadFriends() {
        val token = sessionManager.token ?: return
        viewModelScope.launch {
            try {
                val res = ApiClient.getService().getFriends("Bearer $token")
                if (res.isSuccessful) {
                    val list = res.body() ?: emptyList()
                    _friends.value = list
                    for (f in list) {
                        knownFriendStates[f.id] = f
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading friends: ${e.message}")
            }
        }
    }

    fun loadActivity() {
        val token = sessionManager.token ?: return
        viewModelScope.launch {
            try {
                val res = ApiClient.getService().getActivity("Bearer $token")
                if (res.isSuccessful) {
                    _activities.value = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading activity: ${e.message}")
            }
        }
    }

    fun loadPendingRequests() {
        val token = sessionManager.token ?: return
        viewModelScope.launch {
            try {
                val res = ApiClient.getService().getPendingRequests("Bearer $token")
                if (res.isSuccessful) {
                    _pendingRequests.value = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading requests: ${e.message}")
            }
        }
    }

    fun searchFriend(friendId: String, onComplete: (Boolean, String) -> Unit) {
        val token = sessionManager.token ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = ApiClient.getService().searchFriend("Bearer $token", friendId)
                if (res.isSuccessful && res.body() != null) {
                    _searchResultUser.value = res.body()
                    onComplete(true, "User found!")
                } else {
                    _searchResultUser.value = null
                    onComplete(false, "No user found with Friend ID: $friendId")
                }
            } catch (e: Exception) {
                onComplete(false, "Search failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendFriendRequest(friendId: String, onComplete: (Boolean, String) -> Unit) {
        val token = sessionManager.token ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = ApiClient.getService().sendFriendRequest("Bearer $token", SendFriendRequestDto(friendId))
                if (res.isSuccessful) {
                    _searchResultUser.value = null
                    onComplete(true, res.body()?.message ?: "Friend request sent!")
                } else {
                    val errorMessage = try {
                        res.errorBody()?.charStream()?.use {
                            Gson().fromJson(it, SimpleResponse::class.java)?.message
                        }
                    } catch (_: Exception) {
                        null
                    }

                    if (res.code() == 401) {
                        logout()
                    }
                    onComplete(
                        false,
                        errorMessage ?: if (res.code() == 401) {
                            "Session expired. Please sign in again."
                        } else {
                            "Failed to send friend request (error ${res.code()})."
                        }
                    )
                }
            } catch (e: Exception) {
                onComplete(false, "Request error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun respondToFriendRequest(requestId: String, accept: Boolean) {
        val token = sessionManager.token ?: return
        viewModelScope.launch {
            try {
                val res = ApiClient.getService().respondFriendRequest(
                    "Bearer $token",
                    RespondFriendRequestDto(requestId, accept)
                )
                if (res.isSuccessful) {
                    loadPendingRequests()
                    loadFriends()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error responding to request: ${e.message}")
            }
        }
    }

    fun removeFriend(friendUserId: String) {
        val token = sessionManager.token ?: return
        viewModelScope.launch {
            try {
                val res = ApiClient.getService().removeFriend("Bearer $token", friendUserId)
                if (res.isSuccessful) {
                    loadFriends()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing friend: ${e.message}")
            }
        }
    }

    fun toggleMuteFriend(friend: FriendDto) {
        val token = sessionManager.token ?: return
        val newMute = !friend.isNotificationMuted
        viewModelScope.launch {
            try {
                val res = ApiClient.getService().toggleMuteFriend(
                    "Bearer $token",
                    ToggleMuteRequest(friend.id, newMute)
                )
                if (res.isSuccessful) {
                    loadFriends()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling mute: ${e.message}")
            }
        }
    }

    fun updatePrivacySettings(shareStatus: Boolean, allowRequests: Boolean, showDuration: Boolean) {
        val token = sessionManager.token ?: return
        viewModelScope.launch {
            try {
                ApiClient.getService().updateSettings(
                    "Bearer $token",
                    UpdateSettingsRequest(shareStatus, allowRequests, showDuration)
                )
                val user = sessionManager.currentUser?.copy(
                    shareStatus = shareStatus,
                    allowFriendRequests = allowRequests,
                    showPlayingDuration = showDuration
                )
                sessionManager.currentUser = user
                _currentUser.value = user
            } catch (e: Exception) {
                Log.e(TAG, "Error updating settings: ${e.message}")
            }
        }
    }

    fun toggleSimulatedPubg() {
        SimulationController.toggleSimulatedState()
    }
}
