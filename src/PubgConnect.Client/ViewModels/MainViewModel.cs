using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows.Input;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PubgConnect.Client.Services;
using PubgConnect.Shared;

namespace PubgConnect.Client.ViewModels
{
    public partial class MainViewModel : ViewModelBase
    {
        private readonly IApiClient _apiClient;
        private readonly IRealtimeClient _realtimeClient;
        private readonly IGameDetectorService _gameDetector;
        private readonly INotificationService _notificationService;
        private readonly ISystemTrayService _systemTray;
        private readonly IServerDiscoveryService _discoveryService;

        [ObservableProperty]
        private object? _currentViewModel;

        [ObservableProperty]
        private string _activeTab = "Friends";

        [ObservableProperty]
        private UserDto? _currentUser;

        [ObservableProperty]
        private string _myStatusText = "⚫ Offline";

        [ObservableProperty]
        private bool _isLoggedIn;

        [ObservableProperty]
        private int _pendingRequestsCount;

        // Active Child ViewModels
        public FriendsViewModel FriendsVM { get; }
        public AddFriendViewModel AddFriendVM { get; }
        public RequestsViewModel RequestsVM { get; }
        public SettingsViewModel SettingsVM { get; }

        private readonly Dictionary<string, FriendDto> _knownFriendStates = new();

        public MainViewModel(
            IApiClient apiClient,
            IRealtimeClient realtimeClient,
            IGameDetectorService gameDetector,
            INotificationService notificationService,
            IStartupService startupService,
            ISystemTrayService systemTray,
            IServerDiscoveryService discoveryService)
        {
            _apiClient = apiClient;
            _realtimeClient = realtimeClient;
            _gameDetector = gameDetector;
            _notificationService = notificationService;
            _systemTray = systemTray;
            _discoveryService = discoveryService;

            _notificationService.SetSystemTrayFallback(_systemTray);

            FriendsVM = new FriendsViewModel(_apiClient);
            AddFriendVM = new AddFriendViewModel(_apiClient);
            RequestsVM = new RequestsViewModel(_apiClient);
            SettingsVM = new SettingsViewModel(_apiClient, _notificationService, startupService, _gameDetector);

            ShowLogin();

            // Run Background Auto-Discovery and Auto-Login
            _ = Task.Run(async () =>
            {
                try
                {
                    var discoveredUrl = await _discoveryService.AutoDiscoverServerUrlAsync(_apiClient.BaseUrl);
                    if (!string.IsNullOrWhiteSpace(discoveredUrl) && discoveredUrl != _apiClient.BaseUrl)
                    {
                        _apiClient.BaseUrl = discoveredUrl;
                        App.Current.Dispatcher.Invoke(() =>
                        {
                            SettingsVM.ServerUrl = discoveredUrl;
                            if (CurrentViewModel is LoginViewModel loginVm)
                            {
                                loginVm.ServerUrl = discoveredUrl;
                            }
                        });
                    }
                }
                catch { }

                // Auto-login with saved token
                try
                {
                    var savedToken = ApiClient.LoadSavedToken();
                    if (!string.IsNullOrWhiteSpace(savedToken))
                    {
                        _apiClient.Token = savedToken;
                        var profile = await _apiClient.GetMeAsync();
                        if (profile != null)
                        {
                            await App.Current.Dispatcher.InvokeAsync(async () =>
                            {
                                await OnLoginSuccessAsync();
                            });
                            return;
                        }
                    }
                }
                catch { }

                // Start game monitoring even before login so GameLoop launches trigger the app
                _gameDetector.StartMonitoring();
            });

            // Wire realtime event handlers
            _realtimeClient.FriendStatusChanged += OnFriendStatusChanged;
            _realtimeClient.FriendRequestReceived += OnFriendRequestReceived;
            _realtimeClient.FriendRequestAccepted += OnFriendRequestAccepted;
            _realtimeClient.FriendRemoved += OnFriendRemoved;

            // Wire PUBG process detector handlers
            _gameDetector.PubgStatusChanged += OnLocalPubgStatusChanged;
            _gameDetector.StatusUpdated += OnGameDetectorStatusUpdated;
        }

        public void ShowLogin()
        {
            CurrentViewModel = new LoginViewModel(_apiClient, ShowRegister, OnLoginSuccessAsync);
        }

        public void ShowRegister()
        {
            CurrentViewModel = new RegisterViewModel(_apiClient, ShowLogin, OnLoginSuccessAsync);
        }

        private async Task OnLoginSuccessAsync()
        {
            IsLoggedIn = true;
            CurrentUser = await _apiClient.GetMeAsync();

            if (CurrentUser != null)
            {
                // Connect to SignalR real-time hub
                await _realtimeClient.StartAsync(CurrentUser.Id, _apiClient.BaseUrl);

                // Start game process monitoring
                _gameDetector.StartMonitoring();

                // Initial load of friends and requests
                await RefreshAllDataAsync();

                // Switch view to Friends list
                NavigateTab("Friends");
            }
        }

        [RelayCommand]
        public void NavigateTab(string tabName)
        {
            ActiveTab = tabName;
            switch (tabName)
            {
                case "Friends":
                    CurrentViewModel = FriendsVM;
                    _ = FriendsVM.LoadFriendsAsync();
                    break;
                case "AddFriend":
                    CurrentViewModel = AddFriendVM;
                    break;
                case "Requests":
                    CurrentViewModel = RequestsVM;
                    _ = RequestsVM.LoadRequestsAsync();
                    break;
                case "Settings":
                case "GameLoop":
                    CurrentViewModel = SettingsVM;
                    SettingsVM.RefreshGameLoopStatus();
                    break;
            }
        }

        [RelayCommand]
        public async Task LogoutAsync()
        {
            _gameDetector.StopMonitoring();
            await _realtimeClient.StopAsync();

            ApiClient.ClearSavedToken();
            _apiClient.Token = string.Empty;
            CurrentUser = null;
            IsLoggedIn = false;

            ShowLogin();
        }

        private async Task RefreshAllDataAsync()
        {
            var friends = await _apiClient.GetFriendsAsync();
            FriendsVM.UpdateFriendsList(friends);

            _knownFriendStates.Clear();
            foreach (var f in friends)
            {
                _knownFriendStates[f.Id] = f;
            }

            var requests = await _apiClient.GetPendingRequestsAsync();
            PendingRequestsCount = requests.Count;

            UpdateStatusDisplay();
        }

        private void OnFriendStatusChanged(FriendDto updatedFriend)
        {
            // Check for OFFLINE -> ONLINE / PLAYING transition for Toast Notification
            if (_knownFriendStates.TryGetValue(updatedFriend.Id, out var previousState))
            {
                if (previousState.Status == UserStatus.Offline && updatedFriend.Status != UserStatus.Offline)
                {
                    // Trigger Windows Toast Notification!
                    _notificationService.ShowFriendStartedPubgToast(updatedFriend.Username, updatedFriend.IsNotificationMuted);
                }
            }
            else if (updatedFriend.Status != UserStatus.Offline)
            {
                _notificationService.ShowFriendStartedPubgToast(updatedFriend.Username, updatedFriend.IsNotificationMuted);
            }

            _knownFriendStates[updatedFriend.Id] = updatedFriend;

            // Update friends view model list live
            _ = App.Current.Dispatcher.InvokeAsync(async () =>
            {
                var list = await _apiClient.GetFriendsAsync();
                FriendsVM.UpdateFriendsList(list);
                UpdateStatusDisplay();
            });
        }

        private void OnFriendRequestReceived()
        {
            _notificationService.ShowGenericToast("🎮 PUBG Connect", "You received a new friend request!");
            _ = App.Current.Dispatcher.InvokeAsync(async () =>
            {
                var reqs = await _apiClient.GetPendingRequestsAsync();
                PendingRequestsCount = reqs.Count;
                if (ActiveTab == "Requests") await RequestsVM.LoadRequestsAsync();
            });
        }

        private void OnFriendRequestAccepted()
        {
            _ = App.Current.Dispatcher.InvokeAsync(async () =>
            {
                await RefreshAllDataAsync();
            });
        }

        private void OnFriendRemoved(string friendUserId)
        {
            _knownFriendStates.Remove(friendUserId);
            _ = App.Current.Dispatcher.InvokeAsync(async () =>
            {
                var list = await _apiClient.GetFriendsAsync();
                FriendsVM.UpdateFriendsList(list);
                UpdateStatusDisplay();
            });
        }

        private async void OnLocalPubgStatusChanged(object? sender, bool isPlaying)
        {
            var targetStatus = isPlaying ? UserStatus.PlayingPubg : UserStatus.Online;
            await _realtimeClient.SendHeartbeatAsync(targetStatus);
            
            App.Current.Dispatcher.Invoke(() =>
            {
                UpdateStatusDisplay();
                if (isPlaying && SettingsVM.StartWithGameLoop)
                {
                    _systemTray.ShowFromTray();
                }
            });
        }

        private void OnGameDetectorStatusUpdated(object? sender, EventArgs e)
        {
            App.Current.Dispatcher.Invoke(UpdateStatusDisplay);
        }

        private void UpdateStatusDisplay()
        {
            MyStatusText = _gameDetector.StatusDescription;
            _systemTray.UpdateTrayStatus(MyStatusText, FriendsVM.OnlineCount);
        }
    }
}
