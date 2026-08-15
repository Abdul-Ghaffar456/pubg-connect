using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using PubgConnect.Shared;

namespace PubgConnect.Server.Services
{
    public class UserEntity
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string Username { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string PasswordHash { get; set; } = string.Empty;
        public string FriendId { get; set; } = string.Empty;
        public UserStatus Status { get; set; } = UserStatus.Offline;
        public PlatformType Platform { get; set; } = PlatformType.None;
        public DateTime? PubgStartedAt { get; set; }
        public DateTime LastSeenAt { get; set; } = DateTime.UtcNow;
        public bool ShareStatus { get; set; } = true;
        public bool AllowFriendRequests { get; set; } = true;
        public bool ShowPlayingDuration { get; set; } = true;
        
        // Friend IDs set
        public HashSet<string> Friends { get; set; } = new();
        // Muted friends (FriendUserId -> bool)
        public HashSet<string> MutedFriends { get; set; } = new();
    }

    public class FriendRequestEntity
    {
        public string RequestId { get; set; } = Guid.NewGuid().ToString();
        public string SenderId { get; set; } = string.Empty;
        public string ReceiverId { get; set; } = string.Empty;
        public DateTime SentAt { get; set; } = DateTime.UtcNow;
    }

    public interface IUserService
    {
        AuthResponse Register(RegisterRequest req);
        AuthResponse Login(LoginRequest req);
        UserDto? GetUserById(string userId);
        UserDto? SearchUserByFriendId(string friendId);
        (bool Success, string Message) SendFriendRequest(string senderId, string targetFriendId);
        (bool Success, string Message) RespondFriendRequest(string userId, string requestId, bool accept);
        (bool Success, string Message) RemoveFriend(string userId, string friendUserId);
        List<FriendDto> GetFriends(string userId);
        List<FriendRequestDto> GetPendingRequests(string userId);
        (bool Success, string Message) ToggleMuteFriend(string userId, string friendUserId, bool mute);
        UserStatus UpdateUserStatus(string userId, UserStatus status, PlatformType platform = PlatformType.None);
        List<string> CheckExpiredHeartbeats(TimeSpan timeout);
        HashSet<string> GetFriendUserIds(string userId);
        void UpdateUserSettings(string userId, UpdateSettingsRequest req);
        List<ActivityItemDto> GetRecentActivity(string userId, int limit = 20);
        void RegisterDeviceToken(string userId, string token);
    }

    public class UserService : IUserService
    {
        private readonly ConcurrentDictionary<string, UserEntity> _usersById = new();
        private readonly ConcurrentDictionary<string, UserEntity> _usersByEmail = new();
        private readonly ConcurrentDictionary<string, UserEntity> _usersByFriendId = new();
        private readonly ConcurrentDictionary<string, FriendRequestEntity> _friendRequests = new();
        private readonly List<ActivityItemDto> _recentActivities = new();
        private readonly ConcurrentDictionary<string, HashSet<string>> _userDeviceTokens = new();

        public UserService()
        {
            // Seed initial test users for immediate demo/testing
            SeedUser("Ali", "ali@pubg.com", "password123", "A7K92D");
            SeedUser("Ahmed", "ahmed@pubg.com", "password123", "B3M88X");
            SeedUser("Hassan", "hassan@pubg.com", "password123", "C9P41Z");
            SeedUser("Usman", "usman@pubg.com", "password123", "D5R20Y");

            // Make Ali and Ahmed friends by default
            var ali = _usersByEmail["ali@pubg.com"];
            var ahmed = _usersByEmail["ahmed@pubg.com"];
            ali.Friends.Add(ahmed.Id);
            ahmed.Friends.Add(ali.Id);

            // Add sample recent activities
            _recentActivities.Add(new ActivityItemDto
            {
                UserId = ali.Id,
                Username = "Ali",
                Platform = PlatformType.GameLoop,
                Timestamp = DateTime.UtcNow.AddMinutes(-14),
                ActionDescription = "Started PUBG Mobile"
            });
            _recentActivities.Add(new ActivityItemDto
            {
                UserId = ahmed.Id,
                Username = "Ahmed",
                Platform = PlatformType.Android,
                Timestamp = DateTime.UtcNow.AddMinutes(-45),
                ActionDescription = "Started PUBG Mobile"
            });
        }

        private void SeedUser(string username, string email, string password, string friendId)
        {
            var user = new UserEntity
            {
                Username = username,
                Email = email.ToLowerInvariant(),
                PasswordHash = HashPassword(password),
                FriendId = friendId,
                Status = UserStatus.Offline,
                Platform = PlatformType.None
            };
            _usersById[user.Id] = user;
            _usersByEmail[user.Email] = user;
            _usersByFriendId[user.FriendId] = user;
        }

        public AuthResponse Register(RegisterRequest req)
        {
            if (string.IsNullOrWhiteSpace(req.Email) || string.IsNullOrWhiteSpace(req.Password) || string.IsNullOrWhiteSpace(req.Username))
            {
                return new AuthResponse { Success = false, Message = "Username, email, and password are required." };
            }

            var emailClean = req.Email.Trim().ToLowerInvariant();
            if (_usersByEmail.ContainsKey(emailClean))
            {
                return new AuthResponse { Success = false, Message = "An account with this email already exists." };
            }

            string friendId;
            do
            {
                friendId = GenerateFriendId();
            } while (_usersByFriendId.ContainsKey(friendId));

            var user = new UserEntity
            {
                Username = req.Username.Trim(),
                Email = emailClean,
                PasswordHash = HashPassword(req.Password),
                FriendId = friendId,
                Status = UserStatus.Offline,
                Platform = PlatformType.None,
                LastSeenAt = DateTime.UtcNow
            };

            _usersById[user.Id] = user;
            _usersByEmail[user.Email] = user;
            _usersByFriendId[user.FriendId] = user;

            return new AuthResponse
            {
                Success = true,
                Message = "Registration successful.",
                Token = user.Id,
                User = MapToUserDto(user)
            };
        }

        public AuthResponse Login(LoginRequest req)
        {
            if (string.IsNullOrWhiteSpace(req.Email) || string.IsNullOrWhiteSpace(req.Password))
            {
                return new AuthResponse { Success = false, Message = "Email and password are required." };
            }

            var emailClean = req.Email.Trim().ToLowerInvariant();
            if (!_usersByEmail.TryGetValue(emailClean, out var user) || user.PasswordHash != HashPassword(req.Password))
            {
                return new AuthResponse { Success = false, Message = "Invalid email or password." };
            }

            user.LastSeenAt = DateTime.UtcNow;

            return new AuthResponse
            {
                Success = true,
                Message = "Login successful.",
                Token = user.Id,
                User = MapToUserDto(user)
            };
        }

        public UserDto? GetUserById(string userId)
        {
            return _usersById.TryGetValue(userId, out var user) ? MapToUserDto(user) : null;
        }

        public UserDto? SearchUserByFriendId(string friendId)
        {
            var cleanId = friendId.Trim().ToUpperInvariant();
            return _usersByFriendId.TryGetValue(cleanId, out var user) ? MapToUserDto(user) : null;
        }

        public (bool Success, string Message) SendFriendRequest(string senderId, string targetFriendId)
        {
            var cleanFriendId = targetFriendId.Trim().ToUpperInvariant();
            if (!_usersByFriendId.TryGetValue(cleanFriendId, out var targetUser))
            {
                return (false, "User with this Friend ID was not found.");
            }

            if (targetUser.Id == senderId)
            {
                return (false, "You cannot send a friend request to yourself.");
            }

            if (!targetUser.AllowFriendRequests)
            {
                return (false, "This user is currently not accepting friend requests.");
            }

            var sender = _usersById[senderId];
            if (sender.Friends.Contains(targetUser.Id))
            {
                return (false, "You are already friends with this user.");
            }

            var existingReq = _friendRequests.Values.FirstOrDefault(r => r.SenderId == senderId && r.ReceiverId == targetUser.Id);
            if (existingReq != null)
            {
                return (false, "Friend request already sent.");
            }

            var request = new FriendRequestEntity
            {
                SenderId = senderId,
                ReceiverId = targetUser.Id
            };
            _friendRequests[request.RequestId] = request;

            return (true, $"Friend request sent to {targetUser.Username}!");
        }

        public (bool Success, string Message) RespondFriendRequest(string userId, string requestId, bool accept)
        {
            if (!_friendRequests.TryGetValue(requestId, out var request) || request.ReceiverId != userId)
            {
                return (false, "Friend request not found or unauthorized.");
            }

            _friendRequests.TryRemove(requestId, out _);

            if (!accept)
            {
                return (true, "Friend request declined.");
            }

            if (_usersById.TryGetValue(request.SenderId, out var sender) && _usersById.TryGetValue(userId, out var receiver))
            {
                lock (sender.Friends) sender.Friends.Add(receiver.Id);
                lock (receiver.Friends) receiver.Friends.Add(sender.Id);
                return (true, $"You are now friends with {sender.Username}!");
            }

            return (false, "Error establishing friendship.");
        }

        public (bool Success, string Message) RemoveFriend(string userId, string friendUserId)
        {
            if (_usersById.TryGetValue(userId, out var user))
            {
                lock (user.Friends) user.Friends.Remove(friendUserId);
            }

            if (_usersById.TryGetValue(friendUserId, out var friend))
            {
                lock (friend.Friends) friend.Friends.Remove(userId);
            }

            return (true, "Friend removed.");
        }

        public List<FriendDto> GetFriends(string userId)
        {
            if (!_usersById.TryGetValue(userId, out var user)) return new List<FriendDto>();

            var friends = new List<FriendDto>();
            foreach (var friendId in user.Friends)
            {
                if (_usersById.TryGetValue(friendId, out var f))
                {
                    var isMuted = user.MutedFriends.Contains(f.Id);
                    
                    // Respect privacy settings: If friend disables ShareStatus, show Offline
                    var effectiveStatus = f.ShareStatus ? f.Status : UserStatus.Offline;
                    var effectivePlatform = f.ShareStatus ? f.Platform : PlatformType.None;
                    var pubgStarted = f.ShareStatus ? f.PubgStartedAt : null;

                    friends.Add(new FriendDto
                    {
                        Id = f.Id,
                        Username = f.Username,
                        FriendId = f.FriendId,
                        Status = effectiveStatus,
                        Platform = effectivePlatform,
                        PubgStartedAt = pubgStarted,
                        IsNotificationMuted = isMuted,
                        ShowPlayingDuration = f.ShowPlayingDuration
                    });
                }
            }

            // Order by Online/Playing top, then by Username
            return friends
                .OrderByDescending(f => f.Status != UserStatus.Offline)
                .ThenBy(f => f.Username)
                .ToList();
        }

        public List<FriendRequestDto> GetPendingRequests(string userId)
        {
            var requests = _friendRequests.Values.Where(r => r.ReceiverId == userId).ToList();
            var result = new List<FriendRequestDto>();
            foreach (var req in requests)
            {
                if (_usersById.TryGetValue(req.SenderId, out var sender))
                {
                    result.Add(new FriendRequestDto
                    {
                        RequestId = req.RequestId,
                        SenderId = sender.Id,
                        SenderUsername = sender.Username,
                        SenderFriendId = sender.FriendId,
                        SentAt = req.SentAt
                    });
                }
            }
            return result;
        }

        public (bool Success, string Message) ToggleMuteFriend(string userId, string friendUserId, bool mute)
        {
            if (!_usersById.TryGetValue(userId, out var user)) return (false, "User not found");

            lock (user.MutedFriends)
            {
                if (mute) user.MutedFriends.Add(friendUserId);
                else user.MutedFriends.Remove(friendUserId);
            }

            return (true, mute ? "Notifications muted for this friend." : "Notifications enabled for this friend.");
        }

        public UserStatus UpdateUserStatus(string userId, UserStatus status, PlatformType platform = PlatformType.None)
        {
            if (_usersById.TryGetValue(userId, out var user))
            {
                var prevStatus = user.Status;
                user.Status = status;
                user.Platform = (status == UserStatus.Offline) ? PlatformType.None : (platform != PlatformType.None ? platform : user.Platform);
                user.LastSeenAt = DateTime.UtcNow;

                if (status == UserStatus.PlayingPubg)
                {
                    if (prevStatus != UserStatus.PlayingPubg)
                    {
                        user.PubgStartedAt = DateTime.UtcNow;

                        // Add to recent activities
                        lock (_recentActivities)
                        {
                            _recentActivities.Insert(0, new ActivityItemDto
                            {
                                UserId = user.Id,
                                Username = user.Username,
                                Platform = user.Platform,
                                Timestamp = DateTime.UtcNow,
                                ActionDescription = "Started PUBG Mobile"
                            });

                            if (_recentActivities.Count > 50)
                            {
                                _recentActivities.RemoveAt(_recentActivities.Count - 1);
                            }
                        }
                    }
                }
                else
                {
                    user.PubgStartedAt = null;
                }

                return user.Status;
            }
            return UserStatus.Offline;
        }

        public List<ActivityItemDto> GetRecentActivity(string userId, int limit = 20)
        {
            if (!_usersById.TryGetValue(userId, out var user)) return new List<ActivityItemDto>();

            lock (_recentActivities)
            {
                // Return activities of the user + their friends
                var allowedUserIds = new HashSet<string>(user.Friends) { user.Id };
                return _recentActivities
                    .Where(a => allowedUserIds.Contains(a.UserId))
                    .Take(limit)
                    .ToList();
            }
        }

        public void RegisterDeviceToken(string userId, string token)
        {
            if (string.IsNullOrWhiteSpace(userId) || string.IsNullOrWhiteSpace(token)) return;

            var tokens = _userDeviceTokens.GetOrAdd(userId, _ => new HashSet<string>());
            lock (tokens)
            {
                tokens.Add(token);
            }
        }

        public List<string> CheckExpiredHeartbeats(TimeSpan timeout)
        {
            var expiredUserIds = new List<string>();
            var now = DateTime.UtcNow;

            foreach (var user in _usersById.Values)
            {
                if (user.Status != UserStatus.Offline)
                {
                    if (now - user.LastSeenAt > timeout)
                    {
                        user.Status = UserStatus.Offline;
                        user.Platform = PlatformType.None;
                        user.PubgStartedAt = null;
                        expiredUserIds.Add(user.Id);
                    }
                }
            }

            return expiredUserIds;
        }

        public HashSet<string> GetFriendUserIds(string userId)
        {
            if (_usersById.TryGetValue(userId, out var user))
            {
                return new HashSet<string>(user.Friends);
            }
            return new HashSet<string>();
        }

        public void UpdateUserSettings(string userId, UpdateSettingsRequest req)
        {
            if (_usersById.TryGetValue(userId, out var user))
            {
                user.ShareStatus = req.ShareStatus;
                user.AllowFriendRequests = req.AllowFriendRequests;
                user.ShowPlayingDuration = req.ShowPlayingDuration;
            }
        }

        private static UserDto MapToUserDto(UserEntity u)
        {
            return new UserDto
            {
                Id = u.Id,
                Username = u.Username,
                Email = u.Email,
                FriendId = u.FriendId,
                Status = u.Status,
                Platform = u.Platform,
                PubgStartedAt = u.PubgStartedAt,
                LastSeenAt = u.LastSeenAt,
                ShareStatus = u.ShareStatus,
                AllowFriendRequests = u.AllowFriendRequests,
                ShowPlayingDuration = u.ShowPlayingDuration
            };
        }

        private static string GenerateFriendId()
        {
            const string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            var random = new Random();
            return new string(Enumerable.Repeat(chars, 6).Select(s => s[random.Next(s.Length)]).ToArray());
        }

        private static string HashPassword(string password)
        {
            using var sha = System.Security.Cryptography.SHA256.Create();
            var bytes = System.Text.Encoding.UTF8.GetBytes(password + "_pubgconnect_salt");
            var hash = sha.ComputeHash(bytes);
            return Convert.ToBase64String(hash);
        }
    }
}
