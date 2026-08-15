using System;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PubgConnect.Client.Services;

namespace PubgConnect.Client.ViewModels
{
    public partial class LoginViewModel : ViewModelBase
    {
        private readonly IApiClient _apiClient;
        private readonly Action _onNavigateToRegister;
        private readonly Func<Task> _onLoginSuccess;

        [ObservableProperty]
        private string _email = "ali@pubg.com";

        [ObservableProperty]
        private string _password = "password123";

        [ObservableProperty]
        private string _serverUrl = "http://localhost:5000";

        [ObservableProperty]
        private bool _showServerSettings;

        [ObservableProperty]
        private string _errorMessage = string.Empty;

        [ObservableProperty]
        private bool _isBusy;

        public LoginViewModel(IApiClient apiClient, Action onNavigateToRegister, Func<Task> onLoginSuccess)
        {
            _apiClient = apiClient;
            _onNavigateToRegister = onNavigateToRegister;
            _onLoginSuccess = onLoginSuccess;
            ServerUrl = _apiClient.BaseUrl;
        }

        partial void OnServerUrlChanged(string value)
        {
            _apiClient.BaseUrl = value;
        }

        [RelayCommand]
        private void ToggleServerSettings()
        {
            ShowServerSettings = !ShowServerSettings;
        }

        [RelayCommand]
        private async Task LoginAsync()
        {
            if (string.IsNullOrWhiteSpace(Email) || string.IsNullOrWhiteSpace(Password))
            {
                ErrorMessage = "Please enter your email and password.";
                return;
            }

            _apiClient.BaseUrl = ServerUrl;

            IsBusy = true;
            ErrorMessage = string.Empty;

            var res = await _apiClient.LoginAsync(Email, Password);
            IsBusy = false;

            if (res.Success && res.User != null)
            {
                await _onLoginSuccess();
            }
            else
            {
                ErrorMessage = res.Message;
            }
        }

        [RelayCommand]
        private void GoToRegister()
        {
            _onNavigateToRegister();
        }
    }
}
