using System;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PubgConnect.Client.Services;

namespace PubgConnect.Client.ViewModels
{
    public partial class SettingsViewModel : ViewModelBase
    {
        private readonly IApiClient _apiClient;
        private readonly INotificationService _notificationService;
        private readonly IStartupService _startupService;
        private readonly IGameDetectorService _gameDetector;

        [ObservableProperty]
        private string _serverUrl = "http://localhost:5000";

        [ObservableProperty]
        private string _serverSaveMessage = string.Empty;

        [ObservableProperty]
        private bool _notificationsEnabled = true;

        [ObservableProperty]
        private bool _notificationSoundEnabled = true;

        [ObservableProperty]
        private bool _startWithWindows;

        [ObservableProperty]
        private bool _startWithGameLoop = true;

        [ObservableProperty]
        private bool _shareStatus = true;

        [ObservableProperty]
        private bool _allowFriendRequests = true;

        [ObservableProperty]
        private bool _showPlayingDuration = true;

        [ObservableProperty]
        private bool _isGameLoopDetected;

        [ObservableProperty]
        private bool _isPubgDetected;

        [ObservableProperty]
        private string _gameLoopStatusText = string.Empty;

        [ObservableProperty]
        private bool _isSimulatedMode;

        [ObservableProperty]
        private bool _simulatedPubgActive;

        public SettingsViewModel(
            IApiClient apiClient,
            INotificationService notificationService,
            IStartupService startupService,
            IGameDetectorService gameDetector)
        {
            _apiClient = apiClient;
            _notificationService = notificationService;
            _startupService = startupService;
            _gameDetector = gameDetector;

            ServerUrl = _apiClient.BaseUrl;
            NotificationsEnabled = _notificationService.IsNotificationsEnabled;
            NotificationSoundEnabled = _notificationService.IsSoundEnabled;
            StartWithWindows = _startupService.IsStartWithWindowsEnabled();
            StartWithGameLoop = _startupService.IsStartWithGameLoopEnabled();

            RefreshGameLoopStatus();

            _gameDetector.StatusUpdated += (s, e) => RefreshGameLoopStatus();
        }

        public void RefreshGameLoopStatus()
        {
            IsGameLoopDetected = _gameDetector.IsGameLoopRunning;
            IsPubgDetected = _gameDetector.IsPubgRunning;
            GameLoopStatusText = _gameDetector.StatusDescription;
            IsSimulatedMode = _gameDetector.IsSimulatedMode;
            ServerUrl = _apiClient.BaseUrl;
        }

        [RelayCommand]
        private void SaveServerUrl()
        {
            if (!string.IsNullOrWhiteSpace(ServerUrl))
            {
                _apiClient.BaseUrl = ServerUrl;
                ServerSaveMessage = "Server URL saved!";
            }
        }

        partial void OnNotificationsEnabledChanged(bool value)
        {
            _notificationService.IsNotificationsEnabled = value;
        }

        partial void OnNotificationSoundEnabledChanged(bool value)
        {
            _notificationService.IsSoundEnabled = value;
        }

        partial void OnStartWithWindowsChanged(bool value)
        {
            _startupService.SetStartWithWindows(value);
        }

        partial void OnStartWithGameLoopChanged(bool value)
        {
            _startupService.SetStartWithGameLoop(value);
        }

        partial void OnShareStatusChanged(bool value) => _ = SavePrivacySettingsAsync();
        partial void OnAllowFriendRequestsChanged(bool value) => _ = SavePrivacySettingsAsync();
        partial void OnShowPlayingDurationChanged(bool value) => _ = SavePrivacySettingsAsync();

        private async Task SavePrivacySettingsAsync()
        {
            await _apiClient.UpdateSettingsAsync(ShareStatus, AllowFriendRequests, ShowPlayingDuration);
        }

        [RelayCommand]
        private void RedetectGameLoop()
        {
            _gameDetector.CheckNow();
            RefreshGameLoopStatus();
        }

        [RelayCommand]
        private void ToggleSimulationMode()
        {
            IsSimulatedMode = !IsSimulatedMode;
            _gameDetector.IsSimulatedMode = IsSimulatedMode;
            if (IsSimulatedMode)
            {
                _gameDetector.SetSimulatedPubgState(SimulatedPubgActive);
            }
            RefreshGameLoopStatus();
        }

        [RelayCommand]
        private void ToggleSimulatedPubg()
        {
            SimulatedPubgActive = !SimulatedPubgActive;
            _gameDetector.SetSimulatedPubgState(SimulatedPubgActive);
            RefreshGameLoopStatus();
        }
    }
}
