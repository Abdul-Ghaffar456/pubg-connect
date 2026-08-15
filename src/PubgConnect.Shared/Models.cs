using System;
using System.Collections.Generic;

namespace PubgConnect.Shared
{
    public enum UserStatus
    {
        Offline = 0,
        Online = 1,
        PlayingPubg = 2
    }

    public enum PlatformType
    {
        None = 0,
        GameLoop = 1,
        Android = 2
    }

    public class UserDto
    {
        public string Id { get; set; } = string.Empty;
        public string Username { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string FriendId { get; set; } = string.Empty;
        public UserStatus Status { get; set; } = UserStatus.Offline;
        public PlatformType Platform { get; set; } = PlatformType.None;
        public DateTime? PubgStartedAt { get; set; }
        public DateTime? LastSeenAt { get; set; }
        public bool ShareStatus { get; set; } = true;
        public bool AllowFriendRequests { get; set; } = true;
        public bool ShowPlayingDuration { get; set; } = true;
    }

    public class FriendDto
    {
        public string Id { get; set; } = string.Empty;
        public string Username { get; set; } = string.Empty;
        public string FriendId { get; set; } = string.Empty;
        public UserStatus Status { get; set; } = UserStatus.Offline;
        public PlatformType Platform { get; set; } = PlatformType.None;
        public DateTime? PubgStartedAt { get; set; }
        public bool IsNotificationMuted { get; set; }
        public bool ShowPlayingDuration { get; set; } = true;
        public int PlayingDurationMinutes
        {
            get
            {
                if (Status == UserStatus.PlayingPubg && PubgStartedAt.HasValue)
                {
                    var duration = DateTime.UtcNow - PubgStartedAt.Value;
                    return (int)Math.Max(0, duration.TotalMinutes);
                }
                return 0;
            }
            set { }
        }
    }

    public class ActivityItemDto
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string UserId { get; set; } = string.Empty;
        public string Username { get; set; } = string.Empty;
        public PlatformType Platform { get; set; } = PlatformType.None;
        public DateTime Timestamp { get; set; } = DateTime.UtcNow;
        public string ActionDescription { get; set; } = string.Empty;
    }

    public class FCMDeviceTokenRequest
    {
        public string DeviceToken { get; set; } = string.Empty;
        public PlatformType Platform { get; set; } = PlatformType.Android;
    }

    public class FriendRequestDto
    {
        public string RequestId { get; set; } = string.Empty;
        public string SenderId { get; set; } = string.Empty;
        public string SenderUsername { get; set; } = string.Empty;
        public string SenderFriendId { get; set; } = string.Empty;
        public DateTime SentAt { get; set; }
    }

    public class LoginRequest
    {
        public string Email { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
    }

    public class RegisterRequest
    {
        public string Username { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
    }

    public class AuthResponse
    {
        public bool Success { get; set; }
        public string Message { get; set; } = string.Empty;
        public string Token { get; set; } = string.Empty;
        public UserDto? User { get; set; }
    }

    public class SearchUserRequest
    {
        public string FriendId { get; set; } = string.Empty;
    }

    public class SendFriendRequestDto
    {
        public string TargetFriendId { get; set; } = string.Empty;
    }

    public class RespondFriendRequestDto
    {
        public string RequestId { get; set; } = string.Empty;
        public bool Accept { get; set; }
    }

    public class ToggleMuteRequest
    {
        public string FriendUserId { get; set; } = string.Empty;
        public bool Mute { get; set; }
    }

    public class HeartbeatRequest
    {
        public UserStatus Status { get; set; }
        public PlatformType Platform { get; set; } = PlatformType.None;
    }

    public class UpdateSettingsRequest
    {
        public bool ShareStatus { get; set; }
        public bool AllowFriendRequests { get; set; }
        public bool ShowPlayingDuration { get; set; }
    }

    public static class SignalREvents
    {
        public const string FriendStatusChanged = "OnFriendStatusChanged";
        public const string FriendRequestReceived = "OnFriendRequestReceived";
        public const string FriendRequestAccepted = "OnFriendRequestAccepted";
        public const string FriendRemoved = "OnFriendRemoved";
    }
}
