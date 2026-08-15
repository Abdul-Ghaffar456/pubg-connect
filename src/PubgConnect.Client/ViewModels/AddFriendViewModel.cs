using System;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PubgConnect.Client.Services;
using PubgConnect.Shared;

namespace PubgConnect.Client.ViewModels
{
    public partial class AddFriendViewModel : ViewModelBase
    {
        private readonly IApiClient _apiClient;

        [ObservableProperty]
        private string _searchFriendId = string.Empty;

        [ObservableProperty]
        private UserDto? _foundUser;

        [ObservableProperty]
        private string _statusMessage = string.Empty;

        [ObservableProperty]
        private bool _isSuccessMessage;

        [ObservableProperty]
        private bool _isBusy;

        public AddFriendViewModel(IApiClient apiClient)
        {
            _apiClient = apiClient;
        }

        [RelayCommand]
        private async Task SearchAsync()
        {
            if (string.IsNullOrWhiteSpace(SearchFriendId))
            {
                StatusMessage = "Please enter a 6-character Friend ID (e.g. A7K92D).";
                IsSuccessMessage = false;
                FoundUser = null;
                return;
            }

            IsBusy = true;
            StatusMessage = string.Empty;
            FoundUser = null;

            var user = await _apiClient.SearchFriendAsync(SearchFriendId.Trim());
            IsBusy = false;

            if (user != null)
            {
                FoundUser = user;
                StatusMessage = $"User '{user.Username}' found!";
                IsSuccessMessage = true;
            }
            else
            {
                StatusMessage = "No user found with that Friend ID.";
                IsSuccessMessage = false;
            }
        }

        [RelayCommand]
        private async Task SendRequestAsync()
        {
            if (FoundUser == null) return;

            IsBusy = true;
            var (success, message) = await _apiClient.SendFriendRequestAsync(FoundUser.FriendId);
            IsBusy = false;

            StatusMessage = message;
            IsSuccessMessage = success;
        }
    }
}
