using System;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PubgConnect.Client.Services;

namespace PubgConnect.Client.ViewModels
{
    public partial class RegisterViewModel : ViewModelBase
    {
        private readonly IApiClient _apiClient;
        private readonly Action _onNavigateToLogin;
        private readonly Func<Task> _onRegisterSuccess;

        [ObservableProperty]
        private string _username = string.Empty;

        [ObservableProperty]
        private string _email = string.Empty;

        [ObservableProperty]
        private string _password = string.Empty;

        [ObservableProperty]
        private string _errorMessage = string.Empty;

        [ObservableProperty]
        private bool _isBusy;

        public RegisterViewModel(IApiClient apiClient, Action onNavigateToLogin, Func<Task> onRegisterSuccess)
        {
            _apiClient = apiClient;
            _onNavigateToLogin = onNavigateToLogin;
            _onRegisterSuccess = onRegisterSuccess;
        }

        partial void OnUsernameChanged(string value) => ErrorMessage = string.Empty;
        partial void OnEmailChanged(string value) => ErrorMessage = string.Empty;
        partial void OnPasswordChanged(string value) => ErrorMessage = string.Empty;

        [RelayCommand]
        private async Task RegisterAsync()
        {
            var trimmedUsername = Username?.Trim() ?? string.Empty;
            var trimmedEmail = Email?.Trim() ?? string.Empty;

            if (string.IsNullOrWhiteSpace(trimmedUsername))
            {
                ErrorMessage = "Display name is required.";
                return;
            }

            if (trimmedUsername.Length < 3 || trimmedUsername.Length > 20)
            {
                ErrorMessage = "Display name must be between 3 and 20 characters.";
                return;
            }

            if (string.IsNullOrWhiteSpace(trimmedEmail))
            {
                ErrorMessage = "Email address is required.";
                return;
            }

            if (!System.Text.RegularExpressions.Regex.IsMatch(trimmedEmail, @"^[^@\s]+@[^@\s]+\.[^@\s]+$"))
            {
                ErrorMessage = "Please enter a valid email address (e.g. name@example.com).";
                return;
            }

            if (string.IsNullOrWhiteSpace(Password))
            {
                ErrorMessage = "Password is required.";
                return;
            }

            if (Password.Length < 6)
            {
                ErrorMessage = "Password must be at least 6 characters long.";
                return;
            }

            IsBusy = true;
            ErrorMessage = string.Empty;

            var res = await _apiClient.RegisterAsync(trimmedUsername, trimmedEmail, Password);
            IsBusy = false;

            if (res.Success && res.User != null)
            {
                await _onRegisterSuccess();
            }
            else
            {
                ErrorMessage = res.Message;
            }
        }

        [RelayCommand]
        private void GoToLogin()
        {
            _onNavigateToLogin();
        }
    }
}
