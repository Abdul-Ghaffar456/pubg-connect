using System;
using System.Collections.Concurrent;
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
        private static readonly ConcurrentDictionary<string, HashSet<string>> UserToConnectionsMap = new();

        public StatusHub(IUserService userService)
        {
            _userService = userService;
        }

        public async Task RegisterConnection(string userId)
        {
            if (string.IsNullOrWhiteSpace(userId)) return;

            ConnectionToUserMap[Context.ConnectionId] = userId;

            lock (UserToConnectionsMap)
            {
                if (!UserToConnectionsMap.TryGetValue(userId, out var connections))
                {
                    connections = new HashSet<string>();
                    UserToConnectionsMap[userId] = connections;
                }
                connections.Add(Context.ConnectionId);
            }

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
                lock (UserToConnectionsMap)
                {
                    if (UserToConnectionsMap.TryGetValue(userId, out var connections))
                    {
                        connections.Remove(Context.ConnectionId);
                        if (connections.Count == 0)
                        {
                            UserToConnectionsMap.TryRemove(userId, out _);
                            isLastConnection = true;
                        }
                    }
                }

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
            var friends = _userService.GetFriends(userId);
            var friendUserIds = _userService.GetFriendUserIds(userId);
            var user = _userService.GetUserById(userId);

            if (user == null) return;

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

            foreach (var friendUserId in friendUserIds)
            {
                if (UserToConnectionsMap.TryGetValue(friendUserId, out var connections))
                {
                    foreach (var connId in connections)
                    {
                        await Clients.Client(connId).SendAsync(SignalREvents.FriendStatusChanged, dto);
                    }
                }
            }
        }

        public static List<string> GetUserConnections(string userId)
        {
            if (UserToConnectionsMap.TryGetValue(userId, out var connections))
            {
                lock (connections)
                {
                    return new List<string>(connections);
                }
            }
            return new List<string>();
        }
    }
}
