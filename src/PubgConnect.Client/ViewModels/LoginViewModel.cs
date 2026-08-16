using System;
using System.Text.RegularExpressions;
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
        private string _email = string.Empty;

        [ObservableProperty]
        private string _password = string.Empty;

        [ObservableProperty]
        private string _serverUrl = "https://pubgconnect-backend.onrender.com";

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

        partial void OnEmailChanged(string value)
        {
            ErrorMessage = string.Empty;
        }

        partial void OnPasswordChanged(string value)
        {
            ErrorMessage = string.Empty;
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
            var trimmedEmail = Email?.Trim() ?? string.Empty;
            if (string.IsNullOrWhiteSpace(trimmedEmail))
            {
                ErrorMessage = "Please enter your email address.";
                return;
            }

            if (!Regex.IsMatch(trimmedEmail, @"^[^@\s]+@[^@\s]+\.[^@\s]+$"))
            {
                ErrorMessage = "Please enter a valid email address (e.g. name@example.com).";
                return;
            }

            if (string.IsNullOrWhiteSpace(Password))
            {
                ErrorMessage = "Please enter your password.";
                return;
            }

            if (Password.Length < 6)
            {
                ErrorMessage = "Password must be at least 6 characters long.";
                return;
            }

            _apiClient.BaseUrl = ServerUrl;

            IsBusy = true;
            ErrorMessage = string.Empty;

            var res = await _apiClient.LoginAsync(trimmedEmail, Password);
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
