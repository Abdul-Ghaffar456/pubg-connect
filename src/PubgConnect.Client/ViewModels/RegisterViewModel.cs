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

        [RelayCommand]
        private async Task RegisterAsync()
        {
            if (string.IsNullOrWhiteSpace(Username) || string.IsNullOrWhiteSpace(Email) || string.IsNullOrWhiteSpace(Password))
            {
                ErrorMessage = "All fields are required.";
                return;
            }

            IsBusy = true;
            ErrorMessage = string.Empty;

            var res = await _apiClient.RegisterAsync(Username, Email, Password);
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
