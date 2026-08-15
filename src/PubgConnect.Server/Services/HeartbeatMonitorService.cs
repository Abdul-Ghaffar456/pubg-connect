using System;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.SignalR;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using PubgConnect.Server.Hubs;

namespace PubgConnect.Server.Services
{
    public class HeartbeatMonitorService : BackgroundService
    {
        private readonly IUserService _userService;
        private readonly IHubContext<StatusHub> _hubContext;
        private readonly ILogger<HeartbeatMonitorService> _logger;

        public HeartbeatMonitorService(IUserService userService, IHubContext<StatusHub> hubContext, ILogger<HeartbeatMonitorService> logger)
        {
            _userService = userService;
            _hubContext = hubContext;
            _logger = logger;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Heartbeat Monitor Service started.");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    // Timeout after 75 seconds of no heartbeat
                    var expiredUsers = _userService.CheckExpiredHeartbeats(TimeSpan.FromSeconds(75));
                    foreach (var userId in expiredUsers)
                    {
                        _logger.LogInformation("User {UserId} heartbeat timed out. Marked OFFLINE.", userId);

                        var friendUserIds = _userService.GetFriendUserIds(userId);
                        var user = _userService.GetUserById(userId);
                        if (user != null)
                        {
                            var dto = new Shared.FriendDto
                            {
                                Id = user.Id,
                                Username = user.Username,
                                FriendId = user.FriendId,
                                Status = Shared.UserStatus.Offline,
                                PubgStartedAt = null
                            };

                            foreach (var friendUserId in friendUserIds)
                            {
                                var connections = StatusHub.GetUserConnections(friendUserId);
                                foreach (var connId in connections)
                                {
                                    await _hubContext.Clients.Client(connId).SendAsync(Shared.SignalREvents.FriendStatusChanged, dto, stoppingToken);
                                }
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error in HeartbeatMonitorService execution.");
                }

                await Task.Delay(TimeSpan.FromSeconds(15), stoppingToken);
            }
        }
    }
}
