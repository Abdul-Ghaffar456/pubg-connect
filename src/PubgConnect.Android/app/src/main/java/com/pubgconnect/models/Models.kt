package com.pubgconnect.models

import com.google.gson.annotations.SerializedName

enum class UserStatus(val value: Int) {
    @SerializedName("0")
    OFFLINE(0),
    @SerializedName("1")
    ONLINE(1),
    @SerializedName("2")
    PLAYING_PUBG(2);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: OFFLINE
    }
}

enum class PlatformType(val value: Int) {
    @SerializedName("0")
    NONE(0),
    @SerializedName("1")
    GAMELOOP(1),
    @SerializedName("2")
    ANDROID(2);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: NONE
    }
}

data class UserDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("friendId") val friendId: String = "",
    @SerializedName("status") val status: UserStatus = UserStatus.OFFLINE,
    @SerializedName("platform") val platform: PlatformType = PlatformType.NONE,
    @SerializedName("pubgStartedAt") val pubgStartedAt: String? = null,
    @SerializedName("lastSeenAt") val lastSeenAt: String? = null,
    @SerializedName("shareStatus") val shareStatus: Boolean = true,
    @SerializedName("allowFriendRequests") val allowFriendRequests: Boolean = true,
    @SerializedName("showPlayingDuration") val showPlayingDuration: Boolean = true
)

data class FriendDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("friendId") val friendId: String = "",
    @SerializedName("status") val status: UserStatus = UserStatus.OFFLINE,
    @SerializedName("platform") val platform: PlatformType = PlatformType.NONE,
    @SerializedName("pubgStartedAt") val pubgStartedAt: String? = null,
    @SerializedName("isNotificationMuted") val isNotificationMuted: Boolean = false,
    @SerializedName("showPlayingDuration") val showPlayingDuration: Boolean = true,
    @SerializedName("playingDurationMinutes") val playingDurationMinutes: Int = 0
)

data class ActivityItemDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("platform") val platform: PlatformType = PlatformType.NONE,
    @SerializedName("timestamp") val timestamp: String = "",
    @SerializedName("actionDescription") val actionDescription: String = "Started PUBG Mobile"
)

data class FriendRequestDto(
    @SerializedName("requestId") val requestId: String = "",
    @SerializedName("senderId") val senderId: String = "",
    @SerializedName("senderUsername") val senderUsername: String = "",
    @SerializedName("senderFriendId") val senderFriendId: String = "",
    @SerializedName("sentAt") val sentAt: String = ""
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String = "",
    @SerializedName("token") val token: String = "",
    @SerializedName("user") val user: UserDto? = null
)

data class SendFriendRequestDto(
    @SerializedName("targetFriendId") val targetFriendId: String
)

data class RespondFriendRequestDto(
    @SerializedName("requestId") val requestId: String,
    @SerializedName("accept") val accept: Boolean
)

data class ToggleMuteRequest(
    @SerializedName("friendUserId") val friendUserId: String,
    @SerializedName("mute") val mute: Boolean
)

data class HeartbeatRequest(
    @SerializedName("status") val status: UserStatus,
    @SerializedName("platform") val platform: PlatformType = PlatformType.ANDROID
)

data class UpdateSettingsRequest(
    @SerializedName("shareStatus") val shareStatus: Boolean,
    @SerializedName("allowFriendRequests") val allowFriendRequests: Boolean,
    @SerializedName("showPlayingDuration") val showPlayingDuration: Boolean
)

data class FCMDeviceTokenRequest(
    @SerializedName("deviceToken") val deviceToken: String,
    @SerializedName("platform") val platform: PlatformType = PlatformType.ANDROID
)

data class SimpleResponse(
    @SerializedName("message") val message: String = ""
)
