using System;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.SignalR.Client;
using PubgConnect.Shared;

namespace PubgConnect.Client.Services
{
    public interface IRealtimeClient
    {
        bool IsConnected { get; }
        event Action<FriendDto>? FriendStatusChanged;
        event Action? FriendRequestReceived;
        event Action? FriendRequestAccepted;
        event Action<string>? FriendRemoved;
        event Action<bool>? ConnectionStateChanged;

        Task StartAsync(string userId, string serverUrl);
        Task StopAsync();
        Task SendHeartbeatAsync(UserStatus status);
    }

    public class SignalRRealtimeClient : IRealtimeClient, IAsyncDisposable
    {
        private HubConnection? _hubConnection;
        private System.Threading.Timer? _heartbeatTimer;
        private string _userId = string.Empty;
        private UserStatus _currentStatus = UserStatus.Offline;

        public bool IsConnected => _hubConnection?.State == HubConnectionState.Connected;

        public event Action<FriendDto>? FriendStatusChanged;
        public event Action? FriendRequestReceived;
        public event Action? FriendRequestAccepted;
        public event Action<string>? FriendRemoved;
        public event Action<bool>? ConnectionStateChanged;

        public async Task StartAsync(string userId, string serverUrl)
        {
            _userId = userId;
            var hubUrl = $"{serverUrl.TrimEnd('/')}/hub/status";

            if (_hubConnection != null)
            {
                await StopAsync();
            }

            _hubConnection = new HubConnectionBuilder()
                .WithUrl(hubUrl)
                .WithAutomaticReconnect(new[] { TimeSpan.FromSeconds(0), TimeSpan.FromSeconds(2), TimeSpan.FromSeconds(5), TimeSpan.FromSeconds(10) })
                .Build();

            // Register handlers
            _hubConnection.On<FriendDto>(SignalREvents.FriendStatusChanged, dto =>
            {
                FriendStatusChanged?.Invoke(dto);
            });

            _hubConnection.On(SignalREvents.FriendRequestReceived, () =>
            {
                FriendRequestReceived?.Invoke();
            });

            _hubConnection.On(SignalREvents.FriendRequestAccepted, () =>
            {
                FriendRequestAccepted?.Invoke();
            });

            _hubConnection.On<string>(SignalREvents.FriendRemoved, friendUserId =>
            {
                FriendRemoved?.Invoke(friendUserId);
            });

            _hubConnection.Reconnecting += ex =>
            {
                ConnectionStateChanged?.Invoke(false);
                return Task.CompletedTask;
            };

            _hubConnection.Reconnected += connectionId =>
            {
                ConnectionStateChanged?.Invoke(true);
                // Re-register user connection ID upon reconnection
                _ = _hubConnection.SendAsync("RegisterConnection", _userId);
                return Task.CompletedTask;
            };

            _hubConnection.Closed += ex =>
            {
                ConnectionStateChanged?.Invoke(false);
                return Task.CompletedTask;
            };

            try
            {
                await _hubConnection.StartAsync();
                await _hubConnection.SendAsync("RegisterConnection", _userId);
                ConnectionStateChanged?.Invoke(true);

                // Start heartbeat timer every 30 seconds
                _heartbeatTimer?.Dispose();
                _heartbeatTimer = new System.Threading.Timer(async _ => await OnHeartbeatTimerTick(), null, TimeSpan.FromSeconds(5), TimeSpan.FromSeconds(30));
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"SignalR connection failed: {ex.Message}");
                ConnectionStateChanged?.Invoke(false);
            }
        }

        public async Task StopAsync()
        {
            _heartbeatTimer?.Dispose();
            _heartbeatTimer = null;

            if (_hubConnection != null)
            {
                try
                {
                    await _hubConnection.StopAsync();
                    await _hubConnection.DisposeAsync();
                }
                catch { }
                _hubConnection = null;
            }

            ConnectionStateChanged?.Invoke(false);
        }

        public async Task SendHeartbeatAsync(UserStatus status)
        {
            _currentStatus = status;

            if (_hubConnection != null && _hubConnection.State == HubConnectionState.Connected)
            {
                try
                {
                    await _hubConnection.SendAsync("SendHeartbeat", new HeartbeatRequest
                    {
                        Status = status,
                        Platform = (status != UserStatus.Offline) ? PlatformType.GameLoop : PlatformType.None
                    });
                }
                catch (Exception ex)
                {
                    Debug.WriteLine($"Failed to send heartbeat: {ex.Message}");
                }
            }
        }

        private async Task OnHeartbeatTimerTick()
        {
            await SendHeartbeatAsync(_currentStatus);
        }

        public async ValueTask DisposeAsync()
        {
            await StopAsync();
        }
    }
}
