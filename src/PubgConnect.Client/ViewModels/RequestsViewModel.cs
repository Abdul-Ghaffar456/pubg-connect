using System;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PubgConnect.Client.Services;
using PubgConnect.Shared;

namespace PubgConnect.Client.ViewModels
{
    public partial class RequestsViewModel : ViewModelBase
    {
        private readonly IApiClient _apiClient;

        [ObservableProperty]
        private ObservableCollection<FriendRequestDto> _pendingRequests = new();

        [ObservableProperty]
        private bool _isBusy;

        [ObservableProperty]
        private string _statusMessage = string.Empty;

        public RequestsViewModel(IApiClient apiClient)
        {
            _apiClient = apiClient;
        }

        public async Task LoadRequestsAsync()
        {
            IsBusy = true;
            var requests = await _apiClient.GetPendingRequestsAsync();
            IsBusy = false;

            PendingRequests = new ObservableCollection<FriendRequestDto>(requests);
        }

        [RelayCommand]
        private async Task AcceptRequestAsync(FriendRequestDto? req)
        {
            if (req == null) return;

            IsBusy = true;
            var (success, message) = await _apiClient.RespondFriendRequestAsync(req.RequestId, accept: true);
            IsBusy = false;

            if (success)
            {
                PendingRequests.Remove(req);
                StatusMessage = message;
            }
        }

        [RelayCommand]
        private async Task DeclineRequestAsync(FriendRequestDto? req)
        {
            if (req == null) return;

            IsBusy = true;
            var (success, message) = await _apiClient.RespondFriendRequestAsync(req.RequestId, accept: false);
            IsBusy = false;

            if (success)
            {
                PendingRequests.Remove(req);
                StatusMessage = message;
            }
        }
    }
}
