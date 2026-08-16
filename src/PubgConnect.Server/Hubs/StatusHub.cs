using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.SignalR;
using PubgConnect.Server.Services;
using PubgConnect.Shared;

namespace PubgConnect.Server.Hubs
{
    public class StatusHub : Hub
    {
        private readonly IUserService _userService;
        private static readonly ConcurrentDictionary<string, string> ConnectionToUserMap = new();
        private static readonly ConcurrentDictionary<string, ConcurrentDictionary<string, byte>> UserToConnectionsMap = new();

        public StatusHub(IUserService userService)
        {
            _userService = userService;
        }

        public async Task RegisterConnection(string userId)
        {
            if (string.IsNullOrWhiteSpace(userId)) return;

            ConnectionToUserMap[Context.ConnectionId] = userId;

            var userConns = UserToConnectionsMap.GetOrAdd(userId, _ => new ConcurrentDictionary<string, byte>());
            userConns[Context.ConnectionId] = 0;

            // Add this connection to the user's SignalR group for safe broadcast
            await Groups.AddToGroupAsync(Context.ConnectionId, $"user_{userId}");

            var user = _userService.GetUserById(userId);
            if (user != null)
            {
                // Set status to Online if currently Offline
                if (user.Status == UserStatus.Offline)
                {
                    _userService.UpdateUserStatus(userId, UserStatus.Online);
                    await NotifyFriendsStatusChanged(userId, UserStatus.Online);
                }
            }
        }

        public async Task SendHeartbeat(HeartbeatRequest req)
        {
            if (!ConnectionToUserMap.TryGetValue(Context.ConnectionId, out var userId)) return;

            var user = _userService.GetUserById(userId);
            if (user == null) return;

            var oldStatus = user.Status;
            var newStatus = _userService.UpdateUserStatus(userId, req.Status, req.Platform);

            if (oldStatus != newStatus)
            {
                await NotifyFriendsStatusChanged(userId, newStatus);
            }
        }

        public override async Task OnDisconnectedAsync(Exception? exception)
        {
            if (ConnectionToUserMap.TryRemove(Context.ConnectionId, out var userId))
            {
                bool isLastConnection = false;
                if (UserToConnectionsMap.TryGetValue(userId, out var userConns))
                {
                    userConns.TryRemove(Context.ConnectionId, out _);
                    if (userConns.IsEmpty)
                    {
                        UserToConnectionsMap.TryRemove(userId, out _);
                        isLastConnection = true;
                    }
                }

                try
                {
                    await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"user_{userId}");
                }
                catch { }

                if (isLastConnection)
                {
                    _userService.UpdateUserStatus(userId, UserStatus.Offline, PlatformType.None);
                    await NotifyFriendsStatusChanged(userId, UserStatus.Offline);
                }
            }

            await base.OnDisconnectedAsync(exception);
        }

        public async Task NotifyFriendsStatusChanged(string userId, UserStatus newStatus)
        {
            var user = _userService.GetUserById(userId);
            if (user == null) return;

            var friendUserIds = _userService.GetFriendUserIds(userId);

            var dto = new FriendDto
            {
                Id = user.Id,
                Username = user.Username,
                FriendId = user.FriendId,
                Status = user.ShareStatus ? newStatus : UserStatus.Offline,
                Platform = user.ShareStatus ? user.Platform : PlatformType.None,
                PubgStartedAt = (user.ShareStatus && newStatus == UserStatus.PlayingPubg) ? user.PubgStartedAt : null,
                ShowPlayingDuration = user.ShowPlayingDuration
            };

            // Safely notify each friend using SignalR groups (100% thread-safe)
            foreach (var friendUserId in friendUserIds)
            {
                try
                {
                    await Clients.Group($"user_{friendUserId}").SendAsync(SignalREvents.FriendStatusChanged, dto);
                }
                catch { }
            }
        }

        public static List<string> GetUserConnections(string userId)
        {
            if (UserToConnectionsMap.TryGetValue(userId, out var userConns))
            {
                return userConns.Keys.ToList();
            }
            return new List<string>();
        }
    }
}
